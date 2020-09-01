(ns voila.events
  (:require [clojure.spec.alpha :as s]
            [clojure.set :as set]))

(s/def :events/learning-event   ;; we might want to create a system-event too, but have no example files atm
  (s/keys :req [:events/actor
                :events/activity
                :events/event-id
                :events/event-timestamp
                :events/ua]
          :opt [:events/assigned-path-id])) ;; might not need this, gwen's not sure what it does.

(s/def :events/actor
  (s/keys :req [:events/actor-local-id                      ;; e.g. C1 ID
                :events/actor-global-id                     ;; e.g. Gigya ID
                :events/country
                :events/org
                :events/role]))

(s/def :events/activity
  (s/keys :req [:events/activity-id
                :events/activity-timestamp
                :events/class-id
                :events/product
                :events/verb]
          :opt [:events/result]))

(s/def :events/product
  (s/keys :req [:events/product-code
                :events/bundle-codes]))

(s/def :events/activity-id string?)                         ;; "entity item-code" in xAPI spec
(s/def :events/activity-timestamp int?)                     ;; "entity timestamp"
(s/def :events/actor-global-id string?)                     ;; "ext-user-id"
(s/def :events/actor-local-id string?)                      ;; "actor uuid"
(s/def :events/assigned-path-id string?)                    ;; unknown
(s/def :events/bundle-codes (s/coll-of string?))
(s/def :events/class-id string?)
(s/def :events/country string?)                             ;; can technically also get this from gigya
(s/def :events/event-id string?)
(s/def :events/event-timestamp int?)                        ;; "created" / "modified"
(s/def :events/org string?)
(s/def :events/product-code string?)
(s/def :events/result string?) ;; score (scorable task), html text (writing task) or s3 bucket file (audio task)
(s/def :events/role #{"student" "teacher" "creator"})
(s/def :events/verb #{"attempted" "closed" "completed" "evaluated" "experienced" "launched" "submitted"
                      "downloaded" }) ;; excluding teacher events such as "evaluated" for now since we're missing example files
(s/def :events/ua string?)

