(ns generator-api.core
  (:require [generator-api.generator :as generator]
            muuntaja.core
            ring.middleware.params
            reitit.coercion
            reitit.coercion.spec
            reitit.ring
            reitit.ring.coercion
            reitit.ring.middleware.muuntaja
            reitit.ring.middleware.parameters
            reitit.swagger
            reitit.swagger-ui
            [ring.adapter.jetty :as ring-jetty]
            ring.middleware.cors)
  (:gen-class))

(defn healthcheck-resource
  [components]
  {:name :resources/healthcheck
   :get  {:handler (constantly
                     {:status 200
                      :body   {:status "OK"}})}})

(defn generator-resource
  [{:keys [model-zoo]}]
  {:name :resources/generators
   :get {:summary "Get a list of available generators"
         :handler (fn [_]
                    {:status 200
                     :body {:items (keys model-zoo)
                            :total (count model-zoo)}})}
   :post {:summary    "Generate something with given paramters"
          :parameters {:body {:model string?
                              :input string?}}
          :handler (fn [{{{:keys [model input options]} :body} :parameters}]
                     (if-let [found-model (get model-zoo (keyword model))]
                       {:status 200
                        :body {:input input
                               :output (map #(re-matches #".+[.!?]")
                                            (generator/generate found-model input options))}}
                       {:status 404}))}
   :options {:no-doc true
             :handler (constantly {:status 200})}})

(comment
  (re-matches #".+\[.!?\]" "Write a short story about the day that we all started to lose our minds.\n\nWe've"))

(defn root-handler
  [components]
  (reitit.ring/ring-handler
    (reitit.ring/router
     [["/healthcheck" (healthcheck-resource components)]
       ["/api/v1"
        ["/generators" (generator-resource components)]]
       ["" {:no-doc true}
        ["/swagger.json" {:get (reitit.swagger/create-swagger-handler)}]
        ["/api-docs/*" {:get (reitit.swagger-ui/create-swagger-ui-handler)}]
        ["/internal/app/*" {:get (reitit.ring/create-file-handler)}]]]
      {:data {:coercion   reitit.coercion.spec/coercion
              :muuntaja   muuntaja.core/instance
              :middleware [reitit.ring.middleware.parameters/parameters-middleware
                           reitit.ring.middleware.muuntaja/format-middleware
                           reitit.ring.coercion/coerce-exceptions-middleware
                           reitit.ring.coercion/coerce-request-middleware
                           reitit.ring.coercion/coerce-response-middleware
                           [ring.middleware.cors/wrap-cors
                            :access-control-allow-origin [#".*"]
                            :access-control-allow-methods [:get :put :post :delete :options]]]}})
    (reitit.ring/routes
      (reitit.ring/redirect-trailing-slash-handler)
      (reitit.ring/create-default-handler))))

(defonce server-instance (atom nil))

(defn stop-server
  []
  (when @server-instance
    (.stop @server-instance)))

(defn create-components
  []
  (let [tokenizer (generator/create-tokenizer)]
    {:storage (atom {})
     :tokenizer tokenizer
     :model-zoo {:buddy.v1 (generator/create-generator-buddy.v1 tokenizer)}}))

(defonce components (create-components))

(defn start-server
  [port]
  (stop-server)
  (reset! server-instance
          (ring-jetty/run-jetty (root-handler components)
                                {:port  port
                                 :join? false})))

(defn -main
  [& args]
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "9000"))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-server))
    (start-server port)
    (println "Running")))

(comment
  ;; start the server from within an nrepl session
  (stop-server)
  (start-server 9000))
