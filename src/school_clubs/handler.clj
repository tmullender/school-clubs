(ns school-clubs.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [school-clubs.allocations :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/refresh" (process-responses "uploaded.csv"))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
