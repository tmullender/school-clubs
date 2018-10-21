(ns school-clubs.allocations-test
  (:require [clojure.test :refer :all]
            [school-clubs.allocations :refer :all]
            [clj-time.core :as time])
  (:import (school_clubs.allocations Club Pupil)))

(deftest test-app
  (testing "allocate"
    (let [response (allocate {:clubs  {"French-Tuesday"     (Club. "French" "Tuesday", "A", [[] []], :repeatable, 3)
                                       "Spanish-Tuesday"    (Club. "Spanish" "Tuesday", "A", [[] []], :repeatable, 3)
                                       "German-Wednesday"   (Club. "German", "Wednesday" "A", [[] []], :repeatable, 3)
                                       "Mandarin-Wednesday" (Club. "Mandarin" "Wednesday", "A", [[] []], :repeatable, 3)}
                              :pupils {"Tom A" (Pupil. (time/date-time 2018 10 1 1) "Tom A", "P4A", [["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"] ["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"]] [[] []])
                                       "Tom B" (Pupil. (time/date-time 2018 10 1 2) "Tom B", "P4B", [["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"] ["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"]] [[] []])
                                       "Tom C" (Pupil. (time/date-time 2018 10 1 3) "Tom C", "P6A", [["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"] ["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"]] [[] []])
                                       "Tom D" (Pupil. (time/date-time 2018 10 1 4) "Tom D", "P6B", [["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"] ["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"]] [[] []])
                                       "Tom E" (Pupil. (time/date-time 2018 10 1 5) "Tom E", "P5A", [["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"] ["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"]] [[] []])
                                       "Tom F" (Pupil. (time/date-time 2018 10 1 6) "Tom F", "P5B", [["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"] ["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"]] [[] []])
                                       "Tom G" (Pupil. (time/date-time 2018 10 1 7) "Tom G", "P7A", [["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"] ["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"]] [[] []])
                                       "Tom H" (Pupil. (time/date-time 2018 10 1 8) "Tom H", "P7B", [["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"] ["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"]] [[] []])
                                       "Tom I" (Pupil. (time/date-time 2018 10 1 9) "Tom I", "P4A", [["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"] ["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"]] [[] []])
                                       "Tom J" (Pupil. (time/date-time 2018 10 1 10) "Tom J", "P5A", [["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"] ["French (Tuesday) - A" "Spanish (Tuesday) - A" "German (Wednesday) - A"]] [[] []])}
                              :terms  2})]
      (is (= ["Tom G" "Tom H" "Tom C"] (first (:allocations ((:clubs response) "French-Tuesday")))))
      (is (= ["Tom D" "Tom E" "Tom F"] (first (:allocations ((:clubs response) "Spanish-Tuesday")))))
      (is (= ["Tom J" "Tom A" "Tom B"] (first (:allocations ((:clubs response) "German-Wednesday")))))
      (is (= ["French"] (map :name (first (:allocations ((:pupils response) "Tom G"))))))
      (is (= ["Spanish"] (map :name (first (:allocations ((:pupils response) "Tom D"))))))
      ))
  )
