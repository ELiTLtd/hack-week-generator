(ns voila-api.events
  (:require [clojure.spec.alpha :as s]
            [clojure.set :as set]
            [clojure.test.check.generators :as gen]
            [tick.alpha.api :as t]
            [clojure.string :as str])
  (:import (clojure.lang PersistentQueue)
           (java.util Locale)))

(s/def :events/learning-event                               ;; we might want to create a system-event too, but have no example files atm
  (s/keys :req-un [:events/actor
                   :events/activity
                   :events/event-id
                   :events/event-timestamp
                   :events/ua]
          :opt-un [:events/assigned-path-id]))              ;; might not need this, gwen's not sure what it does.

(s/def :events/actor
  (s/keys :req-un [:events/actor-local-id                   ;; e.g. C1 ID
                   :events/actor-global-id                  ;; e.g. Gigya ID
                   :events/country
                   :events/org
                   :events/role]))

#_(s/def :events/activity                                   ;; "Entity" in xAPI spec
    (s/keys :req-un [:events/activity-id
                     :events/activity-timestamp
                     :events/class-id
                     :events/product
                     :events/verb
                     :events/result]))

(s/def :events/activity                                     ;; "Entity" in xAPI spec
  (s/keys :req-un [:events/activity-id
                   :events/activity-timestamp
                   :events/class-id
                   :events/product
                   :events/verb]
          :opt-un [:events/result]))

(s/def :events/product
  (s/keys :req-un [:events/product-code
                   :events/bundle-codes]))

(def valid-response-examples
  #{"{\"score\":{\"scaled\":0.7,\"min\":0,\"raw\":70,\"max\":100},\"comment\":\"<p><strong>1)</strong> <strong>In city have a</strong> restaurant<strong>s</strong>, museums, malls, store in center the city ...</p><p>2) But <strong>i </strong>don't like São Paulo because everyone is busy, and <strong>things always full</strong>.</p><p><br></p><p>1) Which structure means \\\"exist\\\"? Is it plural or singular?</p><p>2) But I don't like São Paulo... things (verb missing) always full. </p><p><br></p><p><br></p><p>Check my comments and rewrite the task.</p>\"}"

    "{\"response\":{\"text\":\"<p>Cereal with milk</p><p>First you need to find a plate to serve the cereal, then you need to fill the plate with the cereal and finally you need to add some milk and enjoy.</p>\"}}"

    "{\"score\":{\"scaled\":0.6,\"min\":0,\"raw\":60,\"max\":100},\"comment\":\"<p>What is about managing the time?</p>\"}"

    "{\"score\":{\"scaled\":0.14285714285714285,\"max\":14,\"raw\":2,\"min\":0}}"

    "{\"response\":{\"audioPath\":\"https://content.cambridgeone.org/submissions/cup1/evpel1/8efd7945ac58410c9c78c2ec7f9de236/154452400355115445249315331544525003248/0.mp3\"}}"

    "{\"score\":{\"scaled\":0.9333333333333333,\"max\":15,\"raw\":14,\"min\":0}}"})

(s/def :events/activity-id uuid?)                           ;; "entity item-code" in xAPI spec
(s/def :events/activity-timestamp inst?)                    ;; "entity timestamp"
(s/def :events/actor-global-id uuid?)                       ;; "ext_user_id" / "extStudentID"
(s/def :events/actor-local-id uuid?)                        ;; "actor uuid" / "actorid"
(s/def :events/assigned-path-id string?)                    ;; unknown
(s/def :events/bundle-codes (s/coll-of (s/and string? (complement str/blank?)) :distinct true :min-count 1 :gen-max 3))
(s/def :events/class-id uuid?)                              ;; "classid" / "groupid"
(s/def :events/country (set (Locale/getISOCountries)))      ;; we can technically also get this from gigya
(s/def :events/event-id uuid?)                              ;; "uuid"
(s/def :events/event-timestamp inst?)                       ;; "created" / "modified"
(s/def :events/org string?)
(s/def :events/product-code string?)
(s/def :events/result string?)                              ;; score (scorable task), html text (writing task) or s3 bucket file (audio task)
(s/def :events/role #{"student" "teacher" "creator"})
(s/def :events/verb #{"attempted" "closed" "completed" "evaluated" "experienced" "launched" "submitted"
                      "downloaded"})                        ;; excluding teacher events such as "evaluated" for now since we're missing example files
(s/def :events/ua string?)

;; ---------------------
;;  Generators
;; ---------------------

(defn add-result-if-correct-event-type
  [event]
  (let [verb (get-in event [:activity :verb])]
    (if (contains? #{"attempted" "submitted"} verb)
      (assoc-in event [:activity :result]
                (rand-nth (seq valid-response-examples)))
      (update-in event [:activity] dissoc :result))))

(defn add-timestamps
  [event]
  (-> event
      (assoc-in,,, [:activity :activity-timestamp]
                   (t/- (t/now)
                        (t/new-duration (rand-int 60) :seconds)))
      (assoc,,, :event-timestamp (t/now))))

(defn generate-event
  []
  (nth (gen/sample
         (gen/fmap #(->> %
                         (add-timestamps)
                         (add-result-if-correct-event-type))
                   (s/gen :events/learning-event 10))) 9))

(defn generate-event-batch
  [n]
  (repeatedly n #(generate-event)))

(defn keys-in [data]
  "Gets key paths ending in leaves from a map, outputs as vector of vectors."
  (if (map? data)
    (->> data
         (mapcat (fn [[k value]]
                   (let [sub-maps (keys-in value)
                         nested (map #(into [k] %) (remove empty? sub-maps))]
                     (if (seq nested)
                       nested
                       [[k]]))))
         (vec))
    []))

(defn get-paths-for
  "Given a list of keywords, get a vector containing each of their path-vectors"
  [event-map key-list]
  (->> (map #(if (some (fn [k] (= k (last %))) key-list)
               %)
            (keys-in event-map))
       (remove nil?)))

(defn generate-event-from-data
  [key-value-map]
  (let [event (generate-event)]
    (->> (reduce (fn [e key-path]
                   (if-let [value (get key-value-map (last key-path))]
                     (assoc-in e key-path value)))
                 event
                 (get-paths-for event (keys key-value-map)))
         (add-result-if-correct-event-type))))

(comment
  (generate-event-from-data {:product-code "this" :bundle-codes ["test" "works!"]}))

(defn queue
  ([] (PersistentQueue/EMPTY))
  ([coll]
   (reduce conj PersistentQueue/EMPTY coll)))

;; ---------------------
;; Light testing
;; ---------------------

(comment
  (generate-event))

(comment
  (generate-event-batch 3))

