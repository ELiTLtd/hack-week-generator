(ns voila-api.recommendation-engine
  (:require [voila-api.content :as content]
            [voila-api.events :as events]))

;; --- Some test content and events

(def event-instances (repeatedly 3 #(voila-api.events/generate-activity)))

(def content-instances (repeatedly 3 #(voila-api.content/generate-content)))

;; --------------------------------
;; Experimenting with some sorting
;; and general Clojure practice
;; (most of it can probably be
;; removed later)
;; --------------------------------

(defn get-global-id
  [event]
  (get-in event [:events/actor :events/actor-global-id]))

(defn get-content-id
  [content]
  (:content/uuid content))

(defn get-ids
  [content-list id-locator]
  (map id-locator content-list))

(defn get-sorted-ids-from-collection
  [collection id-locator]
  (sort (get-ids collection id-locator)))

;; Test that it works
(get-sorted-ids-from-collection content-instances get-content-id)

;; --------------------------------
;; The actual necessary code for
;; sorting
;; --------------------------------

(defn sort-events-by-id
  [collection]
  (sort-by #(get-in (:actor %) [:actor-global-id]) collection))

(defn sort-content-by-id
  [collection]
  (sort-by :uuid collection))

;; Test that it works
(sort-events-by-id event-instances)
(sort-content-by-id content-instances)
