(ns voila-api.learner-representation
  (:require [voila-api.users :as users]
            [clojure.pprint :as pp]))

;; -------------------------
;; The code for storage
;; -------------------------

(def learner-rep (atom {}))

(defn find-learner
  [learner-id]
  (get @learner-rep learner-id))

(defn add-learner
  [data id]
  (swap! learner-rep assoc id data))                        ;; replace with better update mechanism

(defn del-learner
  [surname]
  (swap! learner-rep #(dissoc % surname)))

(defn update-skill-level
  [surname new-skill-lvl]
  (swap! learner-rep #(assoc-in % [surname :score] new-skill-lvl)))

(defn update-rep
  [data id]
  (if (some id learner-rep)
    (update-skill-level id data)
    (add-learner data id)))

#_(pp/pprint (repeatedly 3 #(add-learner (users/generate-user))))

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
