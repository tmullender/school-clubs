(ns school-clubs.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [school-clubs.handler :refer :all]))

(deftest test-app
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (some #{"Club Allocator"} (:body response)))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
