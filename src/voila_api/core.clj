(ns voila-api.core
  (:require muuntaja.core
            ring.middleware.params
            reitit.coercion
            reitit.coercion.spec
            reitit.ring
            reitit.ring.coercion
            reitit.ring.middleware.muuntaja
            reitit.swagger
            reitit.swagger-ui
            [ring.adapter.jetty :as ring-jetty]
            [voila-api.users :as users])
  (:gen-class))

(def healthcheck-resource
  {:name :resources/healthcheck
   :get {:handler (constantly
                   {:status 200
                    :body {:status "OK"}})}})

(def recommendations-resource
  {:name :resources/recommentations
   :get {:summary "Get a user's content recommendations"
         :parameters {:path {:user-id int?}}
         :handler (constantly {:status 200
                               :body {:items []}})}})

(def learner-representation-resource
  {:name :resources/learner-representation
   :parameters {:path {:user-id int?}}
   :get {:summary "Get a user's learner representation"
         :handler (fn [{:keys [parameters]}]
                    (let [user-id (get-in parameters [:path :user-id])]
                      {:status 200
                       :body (users/get-user user-id)}))}})

(def root-handler
  (reitit.ring/ring-handler
   (reitit.ring/router
    [["/healthcheck" healthcheck-resource]
     ["/api/v1" {:coercion reitit.coercion.spec/coercion
                 :compile reitit.coercion/compile-request-coercers}
      ["/users/:user-id/recommendations" recommendations-resource]
      ["/users/:user-id/representation" learner-representation-resource]]
     ["" {:no-doc true}
      ["/swagger.json" {:get (reitit.swagger/create-swagger-handler)}]
      ["/api-docs/*" {:get (reitit.swagger-ui/create-swagger-ui-handler)}]
      ["/internal/app/*" (reitit.ring/create-file-handler)]]]
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
