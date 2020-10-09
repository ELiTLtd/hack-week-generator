(ns voila-api.content
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def :content/content-instance   ;; we might want to create a system-event too, but have no example files atm
  (s/keys :req-un [:content/uuid
                   :content/name
                   :content/body]
          :opt-un [:content/class
                   :content/meta-tags]))

(s/def :content/uuid uuid?)
(s/def :content/name string?)
(s/def :content/body string?)
(s/def :content/class string?)
(s/def :content/meta-tags (s/coll-of string? :distinct true :count (rand-int 5)))

(defn generate-content
  []
  (gen/sample (s/gen :content/content-instance) 1))

(defn generate-content-with-id
  [id]
  (assoc-in (generate-content) [:uuid] id))

(comment
  (generate-content))
