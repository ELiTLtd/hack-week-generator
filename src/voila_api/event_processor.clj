(ns voila-api.event-processor
  (:require [voila-api.content :as content]
            [voila-api.events :as events]
            [voila-api.recommendation-engine :as recommendation-engine]
            [voila-api.learner-representation :as learner-representation]
            [jsonista.core :as json]))

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

(def mapper
  (json/object-mapper
   {:encode-key-fn true
    :decode-key-fn true}))

(defn get-activity-id
  [event]
  (get-in event [:activity :activity-id]))

(defn get-user-id
  [event]
  (get-in event [:actor :actor-global-id]))

(defn store-in-atomic-learner-rep
  [data]
  (if data
    (learner-representation/add-learner data (:user-id data))
    (println "Could not store a nil")))

(defrecord DummyEventProcessor []
  EventProcessor
  (process-data [this event]
    (let [score (some-> event
                        (get-result)
                        (json/read-value mapper)
                        :score
                        :scaled)]
      {:score score
       :activity-id (get-activity-id event)
       :user-id (get-user-id event)}))

  Storage
  (store [this data]
    (store-in-atomic-learner-rep data)))

(defn aggregate-scores
  [processed-event-list]
  (mapv (fn [[user-id values]]
          (let [scores (remove nil? (map :score values))]
            {:user-id user-id
             :score (when (seq scores)
                      (/ (reduce + scores)
                         (count scores)))}))
        (->> processed-event-list
             (remove #(nil? (:user-id %)))
             (group-by :user-id))))

(defrecord DummyBatchProcessor []
  EventProcessor
  (process-data [this event-list]
    (aggregate-scores
      (let [ep (->DummyEventProcessor)]
        (map #(process-data ep %) event-list))))
  Storage
  (store [this data]
    (doseq [processed-event data]
      (if (some? processed-event)
        (store-in-atomic-learner-rep processed-event)))))

(defn group-by-id
  [data-list]
  (mapv (fn [[grp-key values]]
          (if (some? grp-key)
            (let [scores (map :score values)]
              {:user-id grp-key
               :score   (/ (reduce + scores) (count scores))})))
        (group-by :user-id data-list)))

(defn aggregate-scores
  [data-list]
  (into [] (map (fn [[grp-key values]]
                  (if (some? grp-key)
                    (let [scores (map :score values)]
                      {:user-id grp-key
                       :score   (/ (reduce + scores) (count scores))})))
                (group-by :user-id data-list))))

(defrecord DummyBatchProcessor []
  EventProcessor
  (process-data [this event-list]
    (aggregate-scores
      (let [ep (->DummyEventProcessor)]
        (map #(process-data ep %) event-list))))
  Storage
  (store [this data]
    (doseq [processed-event data]
      (if (some? processed-event)
        (store-in-atomic-learner-rep processed-event)))))

;; --------------------------
;; Tests
;; --------------------------

(comment (def test-event-processor (->DummyEventProcessor)))

(comment
  (def test-batch-processor (->DummyBatchProcessor)))

(comment (def aggregated-scores
           (process-data test-batch-processor (events/generate-event-batch 15))))

(comment (store test-event-processor
                (process-data test-event-processor (events/generate-event))))

(comment (store test-batch-processor aggregated-scores))
