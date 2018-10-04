(ns school-clubs.allocations
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :as string]
            [clj-time.format :as time]))

(defrecord Response [timestamp name group preferences])


(defn capitalize-words [s]
  (->> (string/split s #"\b")
       (map string/capitalize)
       string/join))


(defn create-response [t n g & p]
  (let [pt (time/parse (time/formatter "yyyy/MM/dd hh:mm:ss aa ZZZ") t)
        cn (capitalize-words n)]
    (Response. pt cn g (partition 3 p)))
  )


(defn parse [input]
  (with-open [reader (io/reader input)]
    (doall
      (csv/read-csv reader)))
  )


(defn add-response [collection response]
  (-> collection
      (update :pupils #(assoc % (second response) (apply create-response response)))
      (update :clubs #(into % (drop 3 response)))
      )
  )


(defn sort-responses [responses]
  (sort-by #(.getMillis (.plusYears (:timestamp %) (- (int (second (:group %)))))) < responses))


(defn process-responses [input]
  (let [records (rest (parse input))
        terms (range 1 (/ (count (first records)) 3))
        responses (reduce add-response {:pupils {} :clubs #{} :terms terms } records)]
    (sort-responses (vals (:pupils responses)))))

