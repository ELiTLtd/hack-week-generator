(ns voila-api.users
  (:import (java.util UUID Locale))
  (:require [voila-api.content :as content-gen]))

(def classes ["C1" "CSAT" "TOPIC" "IELTS"])

(defn generate-user []
  {:global-learner-id (UUID/randomUUID)
   :classes           (set (repeatedly (inc (rand-int (count classes))) #(rand-nth classes)))
   :age               (rand-int 100)
   :skill-level       (rand-int 100)
   :activity-history  (repeatedly (rand-int 10) #(content-gen/generate-content))
   :country           (rand-nth (Locale/getISOCountries))
   :l1                (rand-nth (Locale/getISOLanguages))
   })

(defn generate-user-with-id [id]
  (assoc-in (generate-user) [:global-learner-id] id))


(defn generate-users
  [num]
  (zipmap (range num)
          (take num
                (repeatedly generate-user))))

(def users
  (generate-users 10))

(defn get-user
  [user-id]
  (get users user-id))
