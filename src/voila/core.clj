(ns voila.core
  (:require [clojure.spec.alpha :as s]
            muuntaja.core
            ring.middleware.params
            reitit.coercion.spec
            reitit.ring
            reitit.ring.coercion
            reitit.ring.middleware.muuntaja
            [ring.adapter.jetty :as ring-jetty]
            [voila.users :as user])
  (:gen-class))

(def example-event
  {:type "task-attempt"
   :data {:user-id "4815694e-0b85-4d71-9aea-fac8707daf2b"
          :task {:id "a694da2f-87e9-4832-878a-c0e623c36348"}}})

(s/def ::type string?)
(s/def ::data map?)

(s/def ::event
  (s/keys :req [::type ::data]))

(s/valid? ::event {:voila.core/type "blah blah"
                   :voila.core/data {}})

(def user-resource
  {:name :resources/user
   :get {:handler (fn [_request]
                    (let [user-id (get-in _request [:path-params :user-id])]
                    {:status 200
                     :body {:status "TEST 5"
                            :user (voila.users/get-user (Integer/valueOf user-id))}}))}})

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

(def router
  (reitit.ring/router
   [["/healthcheck" healthcheck-resource]
    ["/sample" sample-resource]
    ["/user/:user-id" user-resource]]
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
          (ring-jetty/run-jetty root-handler
                                {:port port
                                 :join? false})))

(defn -main
  [& args]
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "9000"))]
    (.join (start-server port))))

(comment
  ;; start the server from within an nrepl session
  (prn @server-instance)
  (stop-server)
  (start-server 9000))
