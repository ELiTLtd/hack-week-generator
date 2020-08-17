(ns voila.core
  (:require muuntaja.core
            ring.middleware.params
            reitit.coercion.spec
            reitit.ring
            reitit.ring.coercion
            reitit.ring.middleware.muuntaja
            [ring.adapter.jetty :as ring-jetty])
  (:gen-class))


(def sample-resource
  {:name :resources/sample
   :get {:handler (fn [_request]
                    {:status 200
                     :body {:status "TEST"}})}})

(def healthcheck-resource
  {:name :resources/healthcheck
   :get {:handler (fn [_request]
                    {:status 200
                     :body {:status "TEST"}})}})

(def router
  (reitit.ring/router
   [["/healthcheck" healthcheck-resource]
    ["/sample" sample-resource]]
   {:data {:muuntaja muuntaja.core/instance
           :middleware [reitit.ring.middleware.muuntaja/format-middleware
                        reitit.ring.coercion/coerce-exceptions-middleware
                        reitit.ring.coercion/coerce-request-middleware
                        reitit.ring.coercion/coerce-response-middleware]}}))

(def root-handler
  (reitit.ring/ring-handler router
                            (reitit.ring/create-default-handler)))

(defonce server-instance (atom nil))

(defn stop-server
  []
  (when @server-instance
    (.stop @server-instance)))

(defn start-server
  [port]
  (stop-server)
  (reset! server-instance
          (ring-jetty/run-jetty #'root-handler
                                {:port port
                                 :join? true})))

(defn -main
  [& args]
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "9000"))]
    (.join (start-server port))))

(comment
  ;; start the server from within an nrepl session
  (prn @server-instance)
  (stop-server)
  (start-server 9000))
