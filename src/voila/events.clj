(ns voila.events
  (:require [clojure.spec.alpha :as s]
            [clojure.set :as set]
            [clojure.test.check.generators :as gen]
            [tick.alpha.api :as t]))

(s/def :events/learning-event   ;; we might want to create a system-event too, but have no example files atm
  (s/keys :req-un [:events/actor
                :events/activity
                :events/event-id
                :events/event-timestamp
                :events/ua]
          :opt-un [:events/assigned-path-id])) ;; might not need this, gwen's not sure what it does.

(s/def :events/actor
  (s/keys :req-un [:events/actor-local-id                      ;; e.g. C1 ID
                :events/actor-global-id                     ;; e.g. Gigya ID
                :events/country
                :events/org
                :events/role]))

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

(s/def :events/activity-id string?)                         ;; "entity item-code" in xAPI spec
(s/def :events/activity-timestamp int?)                     ;; "entity timestamp"
(s/def :events/actor-global-id string?)                     ;; "ext_user_id" / "extStudentID"
(s/def :events/actor-local-id uuid?)                        ;; "actor uuid" / "actorid"
(s/def :events/assigned-path-id string?)                    ;; unknown
(s/def :events/bundle-codes (s/coll-of string? :distinct true :count (rand-int 5)))
(s/def :events/class-id uuid?)                              ;; "classid" / "groupid"
(s/def :events/country string?)                             ;; we can technically also get this from gigya
(s/def :events/event-id string?)                            ;; "uuid"
(s/def :events/event-timestamp int?)                        ;; "created" / "modified"
(s/def :events/org string?)
(s/def :events/product-code string?)
(s/def :events/result string?) ;; score (scorable task), html text (writing task) or s3 bucket file (audio task)
(s/def :events/role #{"student" "teacher" "creator"})
(s/def :events/verb #{"attempted" "closed" "completed" "evaluated" "experienced" "launched" "submitted"
                      "downloaded" }) ;; excluding teacher events such as "evaluated" for now since we're missing example files
(s/def :events/ua string?)

(defn generate-activity
  []
  (nth (gen/sample
         (gen/fmap #((fn [x] (assoc x :event-timestamp (t/now)))
                      (assoc-in % [:activity :activity-timestamp]
                                (t/- (t/now)
                                     (t/new-duration (rand-int 60) :seconds))))
                   (s/gen :events/learning-event 10))) 9))

(generate-activity)

