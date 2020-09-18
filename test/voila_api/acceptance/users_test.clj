(ns voila-api.acceptance.users-test
  (:require [clojure.test :refer :all]
            [jsonista.core :as json]
            [ring.mock.request :as ring-mock]
            [voila-api.core :as core]
            [voila-api.core-test :refer [handler]])
  (:import (java.util UUID)))

(deftest get-a-user
  (is (every? #(= (:status %) 200)
              (map #(handler (ring-mock/request :get (str "/user/" %))) (range 1 11))))
  (is (= #{:l1 :age :skill-level :activity-history :global-learner-id :country :classes} (-> (handler (ring-mock/request :get "/user/1"))
                                                   :body
                                                   :user
                                                   (keys)
                                                   (set))))
  (is (= {:status  200,
          :body    {:status "TEST 5", :user {:age 75 :skill-level 14 :global-learner-id (UUID/randomUUID)}},
          :headers {"Content-Type" "application/json; charset=utf-8"}})))

(get-a-user)
