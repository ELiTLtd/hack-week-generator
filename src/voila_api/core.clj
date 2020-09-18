(ns voila-api.core
  (:require muuntaja.core
            ring.middleware.params
            reitit.coercion.spec
            reitit.ring
            reitit.ring.coercion
            reitit.ring.middleware.muuntaja
            [ring.adapter.jetty :as ring-jetty]
            [voila-api.users :as users])
  (:gen-class))

(def user-resource
  {:name :resources/user
   :get {:handler (fn [_request]
                    (let [user-id (get-in _request [:path-params :user-id])]
                    {:status 200
                     :body {:status "TEST 5"
                            :user (users/get-user (Integer/valueOf user-id))}}))}})

(def sample-resource
  {:name :resources/sample
   :get {:handler (fn [_request]
                    {:status 200
                     :body {:status "TEST 6"}})}})

(def healthcheck-resource
  {:name :resources/healthcheck
   :get {:handler (fn [_request]
                    {:status 200
                     :body {:status "TEST"}})}})

(def root-handler
  (reitit.ring/ring-handler
   (reitit.ring/router
    [["/healthcheck" healthcheck-resource]
     ["/sample" sample-resource]
     ["/user/:user-id" user-resource]
     ["/internal/app/*" (reitit.ring/create-file-handler)]]
    {:data {:muuntaja muuntaja.core/instance
            :middleware [reitit.ring.middleware.muuntaja/format-middleware
                         reitit.ring.coercion/coerce-exceptions-middleware
                         reitit.ring.coercion/coerce-request-middleware
                         reitit.ring.coercion/coerce-response-middleware]}})
   (reitit.ring/routes
    (reitit.ring/redirect-trailing-slash-handler)
    (reitit.ring/create-default-handler))))

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
                                 :join? false})))

(defn -main
  [& args]
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "9000"))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-server))
    (start-server port)
    (println "Running")))

(comment
  ;; start the server from within an nrepl session
  (prn @server-instance)
  (stop-server)
  (start-server 9000))
