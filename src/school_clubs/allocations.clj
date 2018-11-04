(ns school-clubs.allocations
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :as string]
            [clj-time.format :as time]))

(def max-preference-count 3)

(defrecord Pupil [timestamp name group requests allocations])

(defrecord Club [name day teacher key description allocations type size])


(defn capitalize-words [s]
  (->> (string/split s #"\b")
       (map string/capitalize)
       string/join))


(defn create-pupil [t n g & p]
  (let [pt (time/parse (time/formatter "yyyy/MM/dd hh:mm:ss aa ZZZ") t)
        cn (string/trim (capitalize-words n))]
    (Pupil. pt cn g (partition 3 p) [[] []])))
  

(defn club-pattern [description]
  (rest (re-matches #"([^(]+) [^A-Z]+([a-zA-Z]+)[- )]+(.+)" description)))


(defn club-key [[name day teacher]]
  (str name "-" day))


(defn club-day [request]
  (second (club-pattern request)))


(defn create-club [description]
  (let [club (club-pattern description)
        key (club-key club)]
    [key (apply ->Club (concat club [key description [[] []] :repeatable 20]))]))
  

(defn parse [input]
  (with-open [reader (io/reader input)]
    (doall
      (csv/read-csv reader))))
  

(defn add-response [collection response]
  (let [choices (drop 3 response)
        pupil (apply create-pupil response)]
    (-> collection
        (update :pupils #(assoc % (:name pupil) pupil))
        (update :clubs #(into % (filter not-empty choices)))
        (update :groups #(conj % (nth response 2)))
        (assoc :terms (/ (count choices) max-preference-count)))))
        

(defn sort-responses [responses]
  (sort-by #(.getMillis (.plusYears (:timestamp %) (- (int (second (:group %)))))) < responses))


(defn process-input [input]
  (->
    (->> input
         (parse)
         (rest)
         (reduce add-response {:pupils {} :clubs #{} :groups #{}}))
    (update :clubs #(into {} (map create-club %)))))
    

(defn pupil-free? [term day pupil]
  (not-any? #(= day (club-day %)) (nth (:allocations pupil) term)))


(defn space? [term club]
  (and club (> (:size club) (count (nth (:allocations club) term)))))


(defn repeatable? [club pupil]
  (case (:type club)
    :repeatable true
    :repeated true
    :once (not-any? #(= % (:description club)) (apply concat (:allocations pupil)))))


(defn allocatable [term clubs pupil request]
  (let [club-id (club-key (club-pattern request))
        club (clubs club-id)]
    (and (space? term club) (pupil-free? term (:day club) pupil) (repeatable? club pupil))))


(defn allocate-one [term allocations pupil]
  (let [request (first (filter (partial allocatable term (:clubs allocations) pupil) (nth (:requests pupil) term)))]
    (if-not (nil? request)
      (let [club-id (club-key (club-pattern request))
            club ((:clubs allocations) club-id)
            currentPupils (nth (:allocations club) term)
            currentClubs (nth (:allocations pupil) term)
            updatedClub (update club :allocations #(assoc % term (conj currentPupils (:name pupil))))
            updatedPupil (update pupil :allocations #(assoc % term (conj currentClubs request)))]
        (println "Allocated" (:name club) "to" (:name pupil))
        (-> allocations
            (update :pupils #(assoc % (:name pupil) updatedPupil))
            (update :clubs #(assoc % club-id updatedClub))))
      allocations)))
    

(defn -remove-pupil [club term pupil]
  (update club :allocations #(update % (Integer/valueOf term) (partial filter (complement #{pupil})))))


(defn -add-pupil [club term pupil]
  (update club :allocations #(update % (Integer/valueOf term) (partial cons pupil))))


(defn deallocate [data club term pupil]
  (update data :clubs #(update % club -remove-pupil term pupil)))


(defn allocate-pupil [data club term pupil]
  (update data :clubs #(update % (:key club) -add-pupil term pupil)))


(defn allocate
  ([requests] (allocate requests 0 0))
  ([allocations term iteration]
   (println "Allocating term" (inc term) "and iteration" (inc iteration) "for" (count (:pupils allocations)) "pupils")
   (cond
     (>= term (:terms allocations)) allocations
     (>= iteration max-preference-count) (allocate allocations (inc term) 0)
     :else (recur (reduce (partial allocate-one term) allocations (sort-responses (vals (:pupils allocations)))) term (inc iteration)))))

(defn config-club [data club-key club type size]
  (update data :clubs #(assoc % club-key (-> club
                                            (assoc :type (keyword type))
                                            (assoc :size (Integer/valueOf size))))))
       
  


