(ns ila-prototype.core
  (:require muuntaja.core
            ring.middleware.params
            reitit.coercion.spec
            reitit.ring
            reitit.ring.coercion
            reitit.ring.middleware.muuntaja
            [ring.adapter.jetty :as ring-jetty]))

(def sample-resource
  {:name :resources/sample
   :get {:handler (fn [_request]
                    {:status 200
                     :body {:status "OK"}})}})

(def router
  (reitit.ring/router
   [["/sample" sample-resource]]
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
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "3000"))]
    (.join (start-server port))))

(comment
  ;; start the server from within an nrepl session
  (start-server 3000))
