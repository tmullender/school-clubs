(ns school-clubs.handler
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-pdf.core :refer [pdf template]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as html]
            [school-clubs.allocations :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.util.codec :refer [url-encode]]
            [ring.util.io :refer [piped-input-stream]]))

(def data (atom {}))

(defn display-type [club]
  (case (:type club)
    :repeatable "Can be repeated"))

(defn url [root key]
  (str root (url-encode key)))

(html/deftemplate index "templates/index.html" [] [:form#upload] (html/append (html/html-snippet (anti-forgery-field))))

(html/deftemplate clubs "templates/clubs.html" [clubs]
                  [:form] (html/append (html/html-snippet (anti-forgery-field)))
                  [:ul#club-list :li] (html/clone-for [club clubs]
                                                      [:.name] (html/content (:name (second club)))
                                                      [:.day] (html/content (:day (second club)))
                                                      [:.teacher] (html/content (:teacher (second club)))
                                                      [:.size] (html/content (str (:size (second club))))
                                                      [:.type] (html/content (display-type (second club)))
                                                      [:a] (html/set-attr :href (url "/update/" (first club)))))
                                                      

(html/deftemplate allocations "templates/allocations.html" []
                  [:ul#clubs :li] (html/clone-for [club (:clubs @data)]
                                                  [:a] (html/content (:name (second club)))
                                                  [:a] (html/set-attr :href (url "/club/" (first club))))
                  [:ul#classes :li] (html/clone-for [group (:groups @data)]
                                                    [:a] (html/content group)
                                                    [:a] (html/set-attr :href (url "/class/" group))))

(html/deftemplate edit-club "templates/edit-club.html" [club])

(html/deftemplate view-club "templates/view-club.html" [club]
                  [:h2] (html/content (:name club))
                  [:span.teacher] (html/content (:teacher club))
                  [:span.day] (html/content (:day club))
                  [:div.term] (html/clone-for [i (range (count (:allocations club)))]
                                              [:h4] (html/content (str "Term " i))
                                              [:ul :li] (html/clone-for [name (nth (:allocations club) i)] (html/content name))))
                  

(html/deftemplate view-class "templates/view-class.html" [class]
                  [:h2] (html/content class)
                  [:ul :li] (html/clone-for [pupil (filter #(= (:group %) class) (vals (:pupils @data)))]
                                            [:h4] (html/content (:name pupil))
                                            [:p.one] (html/content (string/join "," (first (:allocations pupil))))
                                            [:p.two] (html/content (string/join "," (second (:allocations pupil))))))

(defn upload [content]
  (println "Content:" content)
  (reset! data (process-input (get-in content [:csv-upload :tempfile])))
  (println @data)
  (clubs (:clubs @data)))

(defn run-allocation []
  (swap! data allocate)
  (allocations))


(defn pupil-to-row [pupil]
  (concat [(:name pupil) (:group pupil)] (map (partial string/join " and ") (:allocations pupil))))


(defn csv-writer [data out]
  (try
    (with-open [writer (io/writer out)]
      (csv/write-csv writer data)
      (.flush writer))
    (catch Exception e (.printStackTrace e))))


(defn create-pupil-csv [pupils]
  {:status 200
   :headers {"Content-Type" "application/csv, application/octet-stream"             
             "Content-Disposition" "attachment; filename=\"pupils.csv\""}
   :body (piped-input-stream (partial csv-writer (map pupil-to-row (vals pupils))))})
  

(defn club-page-template [club]
  [[:heading {:style {:align :center}} (:name club)]
   [:heading {:style {:size 12 :align :center}} (str (:teacher club) "-" (:day club))]
   [:spacer]
   (into [:paragraph {:align :center} ] cat
         (map #(vector [:heading {:style {:size 12 :align :center}} (str "Term " (inc %))]
                       (into [:list {:symbol ""}] (nth (:allocations club) %))
                       [:spacer]) (range (count (:allocations club)))))
   [:pagebreak]])


(defn create-club-pdf [clubs out]
  (try
    (let [data (into [{:title "Clubs" :left-margin 150 :right-margin 150}] cat (map club-page-template (vals clubs)))]
      (println "Data" data)
      (pdf data out)
      (.flush out))  
    (catch Exception e (.printStackTrace e))))
      
      
(defn download-club-pdf [clubs]
  {:status 200
   :headers {"Content-Type" "application/pdf, application/octet-stream"             
             "Content-Disposition" "attachment; filename=\"clubs.pdf\""}
   :body (piped-input-stream (partial create-club-pdf clubs))}) 
       

(defn create-class-pdf [])
  

(defroutes app-routes
  (GET "/" [] (index))
  (POST "/upload" {params :params} (upload params))
  (POST "/allocate" [] (run-allocation))
  (GET "/club/:club-key" [club-key] (view-club ((:clubs @data) club-key)))
  (GET "/class/:class" [class] (view-class class))
  (GET "/download/pupils" [] (create-pupil-csv (:pupils @data)))
  (GET "/download/clubs" [] (download-club-pdf (:clubs @data)))
  (GET "/download/class" [] (create-class-pdf))
  (GET "/update/:club-key" [club-key] (edit-club ((:clubs @data) club-key)))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
