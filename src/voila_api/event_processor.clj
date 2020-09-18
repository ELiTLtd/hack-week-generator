(ns voila-api.event-processor
  (:require [voila-api.content :as content]
            [voila-api.events :as events]
            [voila-api.recommendation-engine :as r-e]
            [voila-api.learner-representation :as l-r]))

(defprotocol EventProcessor
  "Turns raw events into meaningful data."
  (process-data [this event]))

(defprotocol Storage
  "Stores event data."
  (store [this data]))

(defn get-result
  [event]
  (get-in event [:activity :result]))

#_(defrecord RealEventProcessor [event-input]
  EventProcessor
  (process-data [this event])
  Storage
  (store [this data user-id]))

(defn get-score-scaled
  [response]
  (let [score-string "\"scaled\":"
        split-commas (clojure.string/split response #",")]
    (try
      (println response)
      (subs (first split-commas)
            (+ (count score-string) (clojure.string/index-of response score-string)))
      (catch NullPointerException e (println "no score found!")))))

(defn get-activity-id
  [event]
  (get-in event [:activity :activity-id]))

(defn get-user-id
  [event]
  (get-in event [:actor :actor-global-id]))

(defn store-in-atomic-lr
  [data]
  (if data
    (l-r/add-learner data (:user-id data))
    (println "Could not store a nil")))

(defrecord DummyEventProcessor []
  EventProcessor
  (process-data [this event]
    (try
      (let [result (get-result event)]
        {:score (get-score-scaled result)
         :activity-id (get-activity-id event)
         :user-id (get-user-id event)})
      (catch NullPointerException e
        (println "no activity response found!"))))
  Storage
  (store [this data]
    (store-in-atomic-lr data)))

;; --------------------------
;; Tests
;; --------------------------

(def test-event-processor (->DummyEventProcessor))

(let [processed-data (process-data test-event-processor (voila-api.events/generate-event))]
  (store test-event-processor processed-data))
