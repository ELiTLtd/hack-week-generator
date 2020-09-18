(ns anes-sandbox.json-to-event
  (:require [clojure.spec.alpha :as s]
            [jsonista.core :as json]
            [clojure.java.io :as io]
            [clojure.test.check.generators :as gen]
            [clojure.set :as set]))

(def :events/learning-event {:events/actor {:events/actor-id "ane123"
                                            :events/country "NO"
                                            :events/actor-global-id "123ane"
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

(def f (io/file "/Users/aneespeseth/Code/voila/src/voila/event.json"))

(defn get-key-value-from-json
  [json-file]
  (json/read-value (slurp json-file) (json/object-mapper {:decode-key-fn true})))

(get-key-value-from-json f)

(def raw-json-map (json/read-value (slurp f) (json/object-mapper {:decode-key-fn true})))

(defn map-json-to-event-spec
  [json-file]
  (clojure.set/rename-keys (get-key-value-from-json json-file)
                           {:created :events/event-timestamp
                            :events/actor {:events/actor-id "ane123"
                                           :events/country "NO"
                                           :events/gigya-id "123ane"
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
                            :events/ua "some-platform-data"}))

(map-json-to-event-spec f)

(defn content-gen
  []
  (gen/bind
    (s/gen (s/spec (s/keys :req [::name ::type ::wheels ::make])))
    #(gen/return (map->Car %))))

(s/def ::car (s/spec (s/keys :req [::name ::type ::wheels ::make])
                     :gen content-gen))

(gen/generate (content-gen))

(clojure.pprint/pprint (drop 198 (s/exercise ::car 200)))