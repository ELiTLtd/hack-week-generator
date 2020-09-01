(ns voila.recommendation-engine)

;; --- Some test content and events

(def learning-event {:events/actor {:events/actor-id "ane123"
                                    :events/country "NO"
                                    :events/actor-global-id "first-id"
                                    :events/org "elit-or-npd?"
                                    :events/role "student"}
                     :events/activity {:events/activity-id "activity1"
                                       :events/activity-timestamp 12345
                                       :events/class-id "task-class1"
                                       :events/product {:events/product-code "abc"
                                                        :events/bundle-codes #{"bundle1"}}
                                       :events/verb "submitted"}
                     :events/event-id "event1"
                     :events/event-timestamp 1234567
                     :events/ua "some-platform-data"})

(def learning-event1 {:events/actor {:events/actor-id "ane123"
                                     :events/country "NO"
                                     :events/actor-global-id "234ane"
                                     :events/org "elit-or-npd?"
                                     :events/role "student"}
                      :events/activity {:events/activity-id "activity1"
                                        :events/activity-timestamp 12345
                                        :events/class-id "task-class1"
                                        :events/product {:events/product-code "abc"
                                                         :events/bundle-codes #{"bundle1"}}
                                        :events/verb "submitted"}
                      :events/event-id "event1"
                      :events/event-timestamp 1234567
                      :events/ua "some-platform-data"})

(def learning-event2 {:events/actor {:events/actor-id "ane123"
                                     :events/country "NO"
                                     :events/actor-global-id "1-third-global-id"
                                     :events/org "elit-or-npd?"
                                     :events/role "student"}
                      :events/activity {:events/activity-id "activity1"
                                        :events/activity-timestamp 12345
                                        :events/class-id "task-class1"
                                        :events/product {:events/product-code "abc"
                                                         :events/bundle-codes #{"bundle1"}}
                                        :events/verb "submitted"}
                      :events/event-id "event1"
                      :events/event-timestamp 1234567
                      :events/ua "some-platform-data"})

(def learning-events [learning-event learning-event1 learning-event2])

(def content-instance-1 {:content/uuid      "first-id"
                         :content/name      "task-a"
                         :content/body      "some html formatted text"
                         :content/class     "basic english verbs 1"
                         :content/meta-tags ["verbs" "basic"]})

(def content-instance-2 {:content/uuid      "93323"
                         :content/name      "task-b"
                         :content/body      "some html formatted text"
                         :content/class     "basic english verbs 1"
                         :content/meta-tags ["verbs" "basic"]})

(def content-instance-3 {:content/uuid      "1-third-id"
                         :content/name      "task-c"
                         :content/body      "some html formatted text"
                         :content/class     "basic english verbs 1"
                         :content/meta-tags ["verbs" "basic"]})

(def content-instances [content-instance-1, content-instance-2, content-instance-3])

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
  (sort-by #(get-in (:events/actor %) [:events/actor-global-id]) collection))

(defn sort-content-by-id
  [collection]
  (sort-by :content/uuid collection))

;; Test that it works
(sort-events-by-id learning-events)
(sort-content-by-id content-instances)