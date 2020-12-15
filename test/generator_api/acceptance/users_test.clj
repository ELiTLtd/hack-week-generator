(ns hack-week-generator-api.acceptance.users-test
  (:require [clojure.test :refer :all]
            [jsonista.core :as json]
            [ring.mock.request :as ring-mock]
            [hack-week-generator-api.acceptance.common :as common]))

#_(deftest get-a-user
  (testing "all users return an OK response"
    (let [user-responses (for [n (range 1 11)]
                           (common/test-handler (ring-mock/request :get (str "/api/v1/users/" n "/representation"))))]

      (is (every? #(= (:status %) 200) user-responses))))

  (testing "users response contains correct keys"
    (is (= #{:l1 :age :skill-level :activity-history :global-learner-id :country :classes}
           (-> (common/test-handler (ring-mock/request :get "/api/v1/users/1/representation"))
               :body
               (keys)
               (set))))))
