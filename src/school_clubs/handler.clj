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
            [ring.util.io :refer [piped-input-stream]]
            [ring.util.response :refer [redirect]]))

(def data (atom {}))

(defn display-type [club]
  (case (:type club)
    :repeatable "Can be repeated"
    :once "Only once"
    :repeated "Repeated"))

(defn url
  ([prefix key] (url prefix key ""))
  ([prefix key suffix] (str prefix (url-encode key) suffix))
  )

(html/deftemplate index "templates/index.html" [] [:form#upload] (html/append (html/html-snippet (anti-forgery-field))))

(html/deftemplate clubs "templates/clubs.html" [clubs]
                  [:form] (html/append (html/html-snippet (anti-forgery-field)))
                  [:ul#club-list :li] (html/clone-for [club clubs]
                                                      [:.name] (html/content (:name (second club)))
                                                      [:.day] (html/content (:day (second club)))
                                                      [:.teacher] (html/content (:teacher (second club)))
                                                      [:.size] (html/content (str (:size (second club))))
                                                      [:.type] (html/content (display-type (second club)))
                                                      [:a] (html/set-attr :href (url "/club/" (first club) "/update"))))
                                                      

(html/deftemplate allocations "templates/allocations.html" []
                  [:ul#clubs :li] (html/clone-for [club (:clubs @data)]
                                                  [:a] (html/content (:name (second club)))
                                                  [:a] (html/set-attr :href (url "/club/" (first club))))
                  [:ul#classes :li] (html/clone-for [group (:groups @data)]
                                                    [:a] (html/content group)
                                                    [:a] (html/set-attr :href (url "/class/" group))))

(html/deftemplate edit-club "templates/edit-club.html" [club-key club]
                  [:form] (html/append (html/html-snippet (anti-forgery-field)))
                  [:h2] (html/content (:name club))
                  [:span.teacher] (html/content (:teacher club))
                  [:span.day] (html/content (:day club)))

(html/deftemplate view-club "templates/view-club.html" [key club]
                  [:h2] (html/content (:name club))
                  [:span.teacher] (html/content (:teacher club))
                  [:span.day] (html/content (:day club))
                  [:div.term] (html/clone-for [i (range (count (:allocations club)))]
                                              [:form] (html/append (html/html-snippet (anti-forgery-field)))
                                              [:h4] (html/content (str "Term " (inc i)))
                                              [:input.term] (html/set-attr :value i)
                                              [:ul :li] (html/clone-for [name (nth (:allocations club) i)]
                                                                        [:span] (html/content name)
                                                                        [:a] (html/set-attr :href (str "/club/" key "/" i "/" name "/remove")))))
                  

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
  (redirect "/clubs"))

(defn run-allocation []
  (swap! data allocate)
  (println @data)
  (redirect "/allocations"))


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
  

(defn club-page-template [club term]
  [[:heading {:style {:align :center}} (:name club)]
   [:heading {:style {:size 12 :align :center}} (str (:teacher club) "-" (:day club))]
   [:heading {:style {:size 12 :align :center}} (str "Term " (inc term))]
   [:spacer]
   (into [:list {:symbol ""}] (nth (:allocations club) term))
   [:pagebreak]])


(defn create-club-pdf [clubs terms out]
  (try
    (let [pages (for [x (vals clubs) y (range terms)] (club-page-template x y))
          data (into [{:title "Clubs"}] cat pages)]
      (println "Data" data)
      (pdf data out)
      (.flush out))  
    (catch Exception e (.printStackTrace e))))
      
      
(defn download-club-pdf [clubs terms]
  {:status 200
   :headers {"Content-Type" "application/pdf, application/octet-stream"             
             "Content-Disposition" "attachment; filename=\"clubs.pdf\""}
   :body (piped-input-stream (partial create-club-pdf clubs terms))})
       

(defn create-class-pdf [])


(defn add-pupil [initial club-key club term pupil]
  (let [updated (swap! initial allocate-pupil club term pupil)]
    (view-club club-key ((:clubs updated) club-key))))


(defn remove-pupil [initial club term pupil]
  (swap! initial deallocate club term pupil)
  (redirect (str "/club/" club)))


(defn update-club [club-key club type size]
  (swap! data config-club club-key club type size)
  (redirect "/clubs"))
  

(defroutes app-routes
  (GET "/" [] (index))
  (POST "/upload" {params :params} (upload params))
  (POST "/allocate" [] (run-allocation))
  (GET "/allocations" [] (allocations))
  (GET "/clubs" [] (clubs (:clubs @data)))
  (GET "/club/:club-key" [club-key] (view-club club-key ((:clubs @data) club-key)))
  (POST "/club/:club-key" [club-key name term] (add-pupil data club-key ((:clubs @data) club-key) term name))
  (GET "/club/:club-key/update" [club-key] (edit-club club-key ((:clubs @data) club-key)))
  (POST "/club/:club-key/update" [club-key type size] (update-club club-key ((:clubs @data) club-key) type size))
  (GET "/club/:club-key/:term/:pupil/remove" [club-key term pupil] (remove-pupil data club-key term pupil))
  (GET "/class/:class" [class] (view-class class))
  (GET "/download/pupils" [] (create-pupil-csv (:pupils @data)))
  (GET "/download/clubs" [] (download-club-pdf (:clubs @data) (:terms @data)))
  (GET "/download/class" [] (create-class-pdf))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
