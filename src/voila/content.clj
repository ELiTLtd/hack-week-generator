(ns voila.content
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def :content/content-instance   ;; we might want to create a system-event too, but have no example files atm
  (s/keys :req [:content/uuid
                :content/name
                :content/body]
          :opt [:content/class
                :content/meta-tags]))

(s/def :content/uuid uuid?)
(s/def :content/name string?)
(s/def :content/body string?)
(s/def :content/class string?)
(s/def :content/meta-tags (s/coll-of string? :distinct true :count (rand-int 5)))

(defn generate-content
  []
  (nth (gen/sample (s/gen :content/content-instance 10)) 9))

(generate-content)