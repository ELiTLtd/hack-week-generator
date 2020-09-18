(ns voila-api.core-test
  (:require [clojure.test :refer :all]
            [jsonista.core :as json]
            [ring.mock.request :as ring-mock]
            [voila-api.core :as core]))

(deftest a-test
  (testing "And it's fixed"
    (is (= 1 1))))

(defn coerce-response
  [response]
  (-> response
      (update :body
              json/read-value
              (json/object-mapper {:encode-key-fn true
                                   :decode-key-fn true}))))

(def handler
  (comp coerce-response
        core/root-handler))

(deftest we-can-reach-healthcheck
  (is (= {:body {:status "TEST"}
          :headers {"Content-Type" "application/json; charset=utf-8"}
          :status 200}
         (handler (ring-mock/request :get "/healthcheck")))))
