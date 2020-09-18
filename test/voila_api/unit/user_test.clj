(ns voila-api.unit.user-test
  (:require [clojure.test :refer :all]
            [jsonista.core :as json]
            [ring.mock.request :as ring-mock]
            [voila-api.core :as core]
            [voila-api.users :as users]))

(deftest user-unit-tests
  (let [test-users (users/generate-users 100)]
    (doseq [user (vals test-users)]
      (is (<= 0 (:age user) 100)))))

(def test-user-ex (vals {8 {:first-name "Charlie", :surname "Smith", :age 62},
                   49 {:first-name "Yoongi", :surname "Smith", :age 59}}))

(:age {:first-name "Charlie", :surname "Smith", :age 62})

(doseq [x test-user-ex]
  (print (str "\nAge: " (:age x)))
  (print (str "\nName: " (:first-name x)))
  (print (str "\nAllowed age: " (is (<= 0 (:age x) 100)))))
