(ns hack-week-generator-api.unit.user-test
  (:require [clojure.test :refer :all]
            [hack-week-generator-api.users :as users]))

(deftest user-unit-tests
  (let [test-users (users/generate-users 100)]
    (doseq [user (vals test-users)]
      (is (<= 0 (:age user) 100)))))
