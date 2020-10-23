(ns voila-api.event-processor
  (:require [voila-api.content :as content]
            [voila-api.events :as events]
            [voila-api.recommendation-engine :as recommendation-engine]
            [voila-api.learner-representation :as learner-representation]
            [jsonista.core :as json]
            [voila-api.users :as users]))

(defprotocol EventProcessor
  "Turns raw events into meaningful data."
  (process-data [this event]))

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

(defn average-score-of-n-last-activities
  [activity-history new-score n]
  (let [old-scores (->> (for [event activity-history]
                          (some-> event
                                  (get-result)
                                  (json/read-value mapper)
                                  :score
                                  :scaled))
                        (filter some?)
                        (take n))]
    (float (/ (->> old-scores
                   (reduce +)
                   (+ (or new-score 0.0)))
              (min (+ 1 (count old-scores)) n)))))

(defrecord DummyEventProcessor [storage]
  EventProcessor
  (process-data [this event]
    (let [user-id (get-user-id event)
          user-data (or (learner-representation/retrieve storage [user-id])
                        (users/generate-user-with-id user-id))
          current-activity-history (learner-representation/retrieve storage [user-id :activity-history])
          score (some-> event
                        (get-result)
                        (json/read-value mapper)
                        :score
                        :scaled)
          user-updated (-> user-data
                           (assoc :skill-level (average-score-of-n-last-activities
                                                 current-activity-history score 5))
                           (assoc :activity-history (conj current-activity-history event)))]
      (learner-representation/store storage [user-id] user-updated))))

#_(defrecord DummyBatchProcessor [storage]
    EventProcessor
    (process-data [this event-list]
      (aggregate-scores
        (let [ep (->DummyEventProcessor)]
          (doseq [representation (map #(process-data ep %) event-list)]
            (store storage (:user-id representation) representation))))))

(defn aggregate-scores
  [processed-event-list]
  (mapv (fn [[user-id values]]
          (let [scores (remove nil? (map :score values))]
            {:user-id user-id
             :score   (when (seq scores)
                        (/ (reduce + scores)
                           (count scores)))}))
        (->> processed-event-list
             (remove #(nil? (:user-id %)))
             (group-by :user-id))))

(defn group-by-id
  [data-list]
  (mapv (fn [[grp-key values]]
          (if (some? grp-key)
            (let [scores (map :score values)]
              {:user-id grp-key
               :score   (/ (reduce + scores) (count scores))})))
        (group-by :user-id data-list)))

#_(defrecord DummyBatchProcessor []
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

(comment
  (do
    (def storage learner-representation/atomic-store)
    (def test-event-processor (->DummyEventProcessor storage))
    (process-data test-event-processor
                  (events/generate-event-from-data {:actor-global-id "test-user-1"}))
    (learner-representation/retrieve storage ["test-user-1"])))

(comment
  (def test-batch-processor (->DummyBatchProcessor)))

(comment
  (def aggregated-scores
    (process-data test-batch-processor (events/generate-event-batch 15))))

(comment
  (store test-batch-processor aggregated-scores))
