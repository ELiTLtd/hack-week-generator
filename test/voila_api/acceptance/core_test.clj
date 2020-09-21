(ns voila-api.acceptance.core-test
  (:require [clojure.test :refer :all]
            [jsonista.core :as json]
            [muuntaja.core :as muuntaja]
            [ring.mock.request :as ring-mock]
            [voila-api.acceptance.common :as common]))

(deftest a-test
  (testing "And it's fixed"
    (is (= 1 1))))

(deftest we-can-reach-healthcheck
  (is (= {:body {:status "OK"}
          :headers {"Content-Type" "application/json; charset=utf-8"}
          :status 200}
         (common/test-handler (ring-mock/request :get "/healthcheck")))))
