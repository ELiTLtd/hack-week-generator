(ns voila-introspect.main
  (:require [ajax.core :as ajax]
            ["evergreen-ui"
             :refer
             [Badge Pane Text Strong Button SelectMenu Textarea majorScale Heading
              Code UnorderedList ListItem]]
            goog.object
            ["react" :as react]
            ["react-dom" :as react-dom]
            [reagent.core :as reagent]
            reagent.dom))

(goog-define api-root "")

(defonce state
  (reagent/atom {:selected-user nil
                 :event-data ""
                 :users [{:label "Test User A"
                          :value "0"}
                         {:label "Test User B"
                          :value "1"}
                         {:label "Test User C"
                          :value "2"}]
                 :learner-representation "{}"
                 :recommendations "{}"}))

(defonce example-events
  [{:actor {:actor-local-id "51ab31b7-4cb3-49e6-a8d2-adfc7bae0644"
            :actor-global-id "80292ae5-5b11-40d2-bcc0-aad4b4e57ec6"
            :country "US"
            :org "91uzl"
            :role "teacher"}
    :activity {:result "{\"score\":{\"scaled\":0.14285714285714285,\"max\":14,\"raw\":2,\"min\":0}}"
               :activity-id "79228a47-a514-4f26-abff-f7eb51307b53"
               :activity-timestamp "2020-09-28T13:50:10.191Z"
               :class-id "9f8f2459-2f93-403e-a136-7ea262af3809"
               :product {:product-code "3p0kYc"
                         :bundle-codes ["B" "z2jl0A"]}
               :verb "attempted"}
    :event-id "84c699c1-e7bf-422a-9b4d-58644d786030"
    :event-timestamp "2020-09-28T13:51:09.191Z"
    :ua ""}])

(defn api
  [& path]
  (apply str api-root "/api/v1" path))

(defn dev-api
  [& path]
  (apply str api-root "/dev" path))

(defn jsonify
  [m]
  (-> m clj->js js/JSON.stringify))

(defn major-pane
  [{:keys [title] :as props} & children]
  (into [:> Pane
         (merge {:height (majorScale 60)
                 :min-height (majorScale 40)
                 :width (majorScale 90)
                 :border :default
                 :display :flex
                 :flex-direction :column
                 :padding (majorScale 3)
                 :margin (majorScale 2)}
                (dissoc props :title))]
        (cond-> children
          title (conj [:> Heading
                       {:size 600
                        :margin-top 0
                        :margin-bottom (majorScale 2)}
                       title]))))

(def badge-colors
  ["neutral" "green" "blue" "red" "orange" "purple" "yellow" "teal"])

(defn colorful-badge
  "For the same text we should always render the same color"
  [text]
  (let [first-char-n (mod (.charCodeAt text) (count badge-colors))
        color (nth badge-colors first-char-n)]
    [:> Badge {:margin-right (majorScale 1)
               :color color}
     text]))

(defn learner-detail-item
  [name value]
  [:> ListItem {:min-height (majorScale 3)
                :list-style-type :none
                :display :flex
                :justify-content :space-between}
   [:> Strong {:margin-right (majorScale 1)} name]
   (if (string? value)
     [:> Text value]
     value)])

(defn learner-details-list
  [{:keys [global-learner-id classes age skill-level activity-history country l1] :as learner-details}]
  [:> Pane
   {:display :flex
    :flex-grow 4
    :align-items :center
    :justify-content :center}
   [:> UnorderedList
    {:min-width (majorScale 60)}
    [learner-detail-item "Id" [:> Code (str global-learner-id)]]
    [learner-detail-item "Classes" (clojure.string/join ", " classes)]
    [learner-detail-item "Age" age]
    [learner-detail-item "Skill Level" skill-level]
    [learner-detail-item "Country" country]
    [learner-detail-item "First Language" l1]
    [learner-detail-item
     "Activity History"
     (into [:> Pane {:display :inline}]
           (for [[activity] activity-history]
             (when-let [name (not-empty (:name activity))]
               [colorful-badge name])))]]])

(defn learner-representation
  [state]
  [major-pane {:title "Learner Representation"}
   [learner-details-list (:learner-representation @state)]
   [:> Pane
    {:margin-top (majorScale 1)}
    [:> Button
     {:on-click (fn []
                  (if-let [user-id (get-in @state [:selected-user :value])]
                    (ajax/GET (api "/users/" user-id "/representation")
                        {:handler #(swap! state assoc :learner-representation %)})
                    (js/console.log "No user-id set")))}
     "Get Learner Representation"]]])

(defn recommendations
  [state]
  [major-pane {:title "Recommendations"}
   [:> Code
    {:flex-grow 4
     :overflow "scroll"}
    (:recommendations @state)]
   [:> Pane
    {:padding-top (majorScale 1)}
    [:> Button
     {:on-click (fn []
                  (if-let [user-id (get-in @state [:selected-user :value])]
                    (ajax/GET (api "/users/" user-id "/recommendations")
                        {:handler #(swap! state assoc :recommendations %)})
                    (js/console.log "No user-id set")))}
     "Get Recommendations"]]])

(defn user-select
  [{:keys [selected-user on-select-user users] :as props} & children]
  (into [:> SelectMenu
         {:title "Select User"
          :options users
          :selected (:value selected-user)
          :on-select (fn [item]
                       (-> item
                           (js->clj :keywordize-keys true)
                           on-select-user))
          :close-on-select true}]
        (if (not-empty children)
          children
          [[:> Button
            {:flex-grow 1
             :justify-content :center
             :margin-right (majorScale 1)}
            (or (:label selected-user)
                "Select a user...")]])))

(defn events
  [state]
  [major-pane {:title "Send Events"}
   [:> Textarea
    {:flex-grow 4
     :placeholder "{\"type\": \"event\", ...}"
     :hint "Must be valid json"
     :value (:event-data @state "")
     :on-change (fn [event]
                  (swap! state assoc :event-data (-> event .-target .-value)))}]
   [:> Pane
    {:display :flex
     :flex-direction :row
     :justify-content :space-between
     :margin-top (majorScale 1)}
    [user-select
     {:selected-user (:selected-user @state)
      :on-select-user #(swap! state assoc :selected-user %)
      :users (:users @state)}]
    [:> Button
     {:flex-grow 1
      :justify-content :center
      :margin-right (majorScale 1)
      :on-click (fn []
                  (swap! state assoc :event-data (-> (rand-nth example-events)
                                                     clj->js
                                                     js/JSON.stringify)))}
     "Generate Event"]
    [:> Button
     {:flex-grow 1
      :justify-content :center
      :on-click (fn []
                  (if-let [event-data (:event-data @state)]
                    (ajax/POST (dev-api "/events")
                        {:params event-data
                         :handler (fn [_] (js/console.log "OK"))
                         :format :json})
                    (js/console "No user-id set")))}
     "Send Event"]]])

(defn app
  []
  [:> Pane
   {:display :flex
    :justify-content :center
    :align-content :center
    :align-items :center
    :flex-wrap :wrap}
   [events state]
   [learner-representation state]
   [recommendations state]])

(defn stop
  []
  (js/console.log "Stopping..."))

(defn start
  []
  (js/console.log "Starting...")
  (reagent.dom/render [app]
                      (js/document.getElementById "root")))

(defn ^:export init
  []
  (start))
