(ns voila-api.learner-representation
  (:require [voila-api.users :as users]
            [clojure.pprint :as pp]))

;; -------------------------
;; The code for storage
;; -------------------------

(defprotocol Storage
  "Stores event data."
  (store [this path data])
  (retrieve [this path]))

(defrecord AtomicStore [state]
  Storage
  (store [this path data]
    (swap! state assoc-in path data))
  (retrieve [this path]
    (get-in @state path)))

;; Persistent for the lifetime of the service
(def atomic-store
         (->AtomicStore (atom {})))

;; -------------------------
;; Testing
;; -------------------------

(def test-map {:global-learner-id #uuid "c395f629-cfab-4568-920c-682bf9a9a77b",
               :classes           #{"C1" "IELTS"},
               :first-name        "Charlie",
               :surname           "Smith",
               :age               31,
               :skill-level       91,
               :hobbies           #{"ping-pong"}})

(def new-learner {:global-learner-id #uuid "cc5248b4-116b-493a-a7c2-8bd127606e05",
                  :classes           #{"CSAT"},
                  :first-name        "Geir",
                  :surname           "Hank",
                  :age               12,
                  :skill-level       2,
                  :hobbies           #{"singing"}})

(comment (reset! learner-rep {}))
