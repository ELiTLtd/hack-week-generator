(ns hack-week-generator-api.acceptance.common
  "Components and utilities required for running acceptance tests"
  (:require muuntaja.core
            hack-week-generator-api.core))

(def test-handler
  "Combines the root-handler (as a ref so it's dynamically invoked) and a muuntaja
  transformer to parse responses"
  (comp (fn [response]
          (try
            (let [decoded-body
                  (muuntaja.core/decode-response-body
                   (muuntaja.core/create muuntaja.core/default-options)
                   response)]
              (assoc response :body decoded-body))
            (catch Exception _
              response)))
        (hack-week-generator-api.core/root-handler (voila-api.core/create-components))))
