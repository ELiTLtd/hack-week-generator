(ns generator-client.main
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
  (reagent/atom
   {:selected-user          nil
    :selected-event-type    nil
    :event-data             ""
    :input-type             "generator-input"
    :learner-representation "{}"
    :recommendations        "{}"}))

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
         (merge {:height         (majorScale 60)
                 :min-height     (majorScale 40)
                 :width          (majorScale 90)
                 :border         :default
                 :display        :flex
                 :flex-direction :column
                 :padding        (majorScale 3)
                 :margin         (majorScale 2)}
                (dissoc props :title))]
        (cond-> children
                title (conj [:> Heading
                             {:size          600
                              :margin-top    0
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
               :color        color}
     text]))

(defn learner-detail-item
  [name value]
  [:> ListItem {:min-height      (majorScale 3)
                :list-style-type :none
                :display         :flex
                :justify-content :space-between}
   [:> Strong {:margin-right (majorScale 1)} name]
   (if (string? value)
     [:> Text value]
     value)])

(defn learner-details-list
  [{:keys [learner-id :global-learner-id
           skill-level :skill-level
           latest-activity :activity-id
           activity-history :activity-history
           age :age
           country :country
           l1 :l1
           classes :classes] :as learner-details}]
  [:> Pane
   {:display         :flex
    :flex-grow       4
    :align-items     :center
    :justify-content :center}
   [:> UnorderedList
    {:min-width (majorScale 60)}
    [learner-detail-item "Id" [:> Code (str (:global-learner-id learner-details))]]
    [learner-detail-item "Classes" (clojure.string/join ", " classes)]
    [learner-detail-item "Age" age]
    [learner-detail-item "Skill Level" skill-level]
    [learner-detail-item "Country" (get-country country)]
    [learner-detail-item "First Language" (get-language l1)]
    [learner-detail-item
     "Activity History"
     (into [:> Pane {:display :inline}]
           (for [activity activity-history]
             (when-let [name (not-empty (get-in activity [:activity :activity-id]))]
               [colorful-badge (str name)])))]]])

(defn learner-representation
  [state]
  [major-pane {:title "Learner Representation"}
   [learner-details-list (:learner-representation @state)]
   [:> Pane
    {:margin-top (majorScale 1)}
    [:> Button
     {:on-click (fn []
                  (if-let [user-id (get-in @state [:selected-user :label])]
                    (ajax/GET (api "/users/" user-id "/representation")
                              {:handler #(swap! state assoc :learner-representation %)})
                    (js/console.log "No user-id set")))}
     "Get Learner Representation"]]])

(defn recommendations
  [state]
  [major-pane {:title "Recommendations"}
   [:> Code
    {:flex-grow 4
     :overflow  "scroll"}
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
         {:title           "Select User"
          :options         users
          :selected        (:value selected-user)
          :on-select       (fn [item]
                             (-> item
                                 (js->clj :keywordize-keys true)
                                 on-select-user))
          :close-on-select true}]
        (if (not-empty children)
          children
          [[:> Button
            {:flex-grow       1
             :justify-content :center
             :margin-right    (majorScale 1)}
            (or (:label selected-user)
                "Select a user...")]])))

(defn event-select
  [{:keys [selected-event-type on-select-event event-types] :as props} & children]
  (into [:> SelectMenu
         {:title           "Select Event Type"
          :options         event-types
          :selected        (:value selected-event-type)
          :on-select       (fn [item]
                             (-> item
                                 (js->clj :keywordize-keys true)
                                 on-select-event))
          :close-on-select true}]
        (if (not-empty children)
          children
          [[:> Button
            {:flex-grow       1
             :justify-content :center
             :margin-right    (majorScale 1)}
            (or (:label selected-event-type)
                "Select an event type...")]])))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(defn events
  [state]
  [major-pane {:title "Send Events"}
   (condp = (:input-type @state)
     "generator-input" [:> Textarea
                        {:flex-grow   4
                         :placeholder "{\"type\": \"event\", ...}"
                         :hint        "Must be valid json"
                         :value       (:event-data @state "")
                         :on-click    (fn [_]
                                        (do (swap! state assoc :input-type "user-input")
                                            (js/console.log "changing state to " (:input-type @state))))
                         :on-change   (fn [event]
                                        (swap! state assoc :event-data (-> event .-target .-value)))}]
     "user-input" [:> Textarea
                   {:flex-grow     4
                    :placeholder   "{\"type\": \"event\", ...}"
                    :hint          "Must be valid json"
                    :default-value (:event-data @state "")
                    :on-change     (fn [event]
                                     (swap! state assoc :event-data (-> event .-target .-value)))}])
   [:> Pane
    {:display         :flex
     :flex-direction  :row
     :justify-content :space-between
     :margin-top      (majorScale 1)}
    [user-select
     {:selected-user  (:selected-user @state)
      :on-select-user #(swap! state assoc :selected-user %)
      :users          (:users @state)}]
    [event-select
     {:selected-event-type (:selected-event-type @state)
      :on-select-event     #(swap! state assoc :selected-event-type %)
      :event-types         (:event-types @state)
      }]
    [:> Button
     {:flex-grow       1
      :justify-content :center
      :margin-right    (majorScale 1)
      :on-click        (fn []
                         (ajax/GET (dev-api "/events")
                                   {:response-format :json
                                    :params          {:user-id    (:label (:selected-user @state))
                                                      :event-type (clojure.string/lower-case (:label (:selected-event-type @state)))}
                                    :handler         (fn [event] (do
                                                                   (swap! state assoc :input-type "generator-input")
                                                                   (js/console.log "changing state to " (:input-type @state))
                                                                   (swap! state assoc :event-data (-> event
                                                                                                      clj->js
                                                                                                      js/JSON.stringify))
                                                                   (js/console.log "Event generated!")))}))}
     "Generate Event"]
    [:> Button
     {:flex-grow       1
      :justify-content :center
      :on-click        (fn []
                         (do
                           (if-let [event-data (:event-data @state)]
                             (ajax/POST (dev-api "/events")
                                        {:format        :json
                                         :params        {:event-data (js/JSON.parse event-data)}
                                         :handler       (fn [response] (js/console.log response))
                                         :error-handler error-handler})
                             (js/console "No user-id set"))
                           (swap! state assoc :input-type "generator-input")
                           (swap! state assoc :event-data "Event sent for processing!")))}
     "Send Event"]]])

(defn app
  []
  [:> Pane
   [:> Pane {:height           (majorScale 7)
             :min-height       56
             :margin           (majorScale -1)
             :padding-left    (majorScale 3)
             :display :flex
             :align-items :center
             :background-color "#8723ff"
             }
    [:img {:src "./img/cup-logo.svg"}]]
   [:> Pane
    {:display         :flex
     :justify-content :center
     :align-content   :center
     :align-items     :center
     :flex-wrap       :wrap
     :flex            1}
    [events state]
    [learner-representation state]
    [recommendations state]]])

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
