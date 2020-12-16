(ns generator-client.main
  (:require [ajax.core :as ajax]
            ["evergreen-ui"
             :refer
             [Badge Pane Text TextInputField Strong Button SelectMenu Textarea
             majorScale Heading Code UnorderedList ListItem default-theme
             Paragraph Spinner]]
            goog.object
            ["react" :as react]
            ["react-dom" :as react-dom]
            [reagent.core :as reagent]
            reagent.dom))

(goog-define api-root "")

(defonce state
  (reagent/atom
   {:input nil
    :results nil
    :options nil
    :time-taken nil
    :request-start nil}))

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

(def default-model-options
  {:max_length 40
   :top_k 50
   :top_p 0.9
   :temperature 1.0
   :num_return_sequences 5})

(defn model-option
  [{:keys [label key coercer-fn] :as opts}]
  [:> TextInputField
   {:label label
    :default-value (key default-model-options)
    :on-change (fn [event]
                 (let [value (-> event
                                 .-target
                                 .-value
                                 coercer-fn)]
                   (swap! state assoc-in [:options key] value)))}])

(defn text-generation
  [state]
  [:> Pane
   (merge {:width          (majorScale 90)
           :border         :default
           :display        :flex
           :flex-direction :column
           :padding        (majorScale 3)
           :margin         (majorScale 2)})
   [:> Textarea
    {:flex-grow   4
     :placeholder "Write an exam question stem..."
     :default-value (:input @state)
     :on-change (fn [event]
                  (swap! state assoc :input (-> event .-target .-value)))}]
   [:> Pane
    [model-option {:label "Max Length"
                   :key :max_length
                   :coercer-fn js/parseInt}]
    [model-option {:label "Top K"
                   :key :top_k
                   :coercer-fn js/parseInt}]
    [model-option {:label "Top P"
                   :key :top_p
                   :coercer-fn js/parseFloat}]
    [model-option {:label "Temperature"
                   :key :temperature
                   :coercer-fn js/parseFloat}]
    [model-option {:label "Number of Examples"
                   :key :num_return_sequences
                   :coercer-fn js/parseInt}]]
   [:> Pane
    {:display :flex
     :flex-direction :row
     :justify-content :space-between
     :margin-top (majorScale 1)}
    [:> Button
     {:flex-grow 1
      :justify-content :center
      :on-click (fn []
                  (if-let [input (:input @state)]
                    (let [options (merge default-model-options (:options @state))]
                      (swap! state assoc
                             :results :loading
                             :time-taken nil
                             :request-start (js/Date.now))
                      (ajax/POST (api "/generators")
                          {:format :json
                           :response-format :json
                           :params {:model "buddy.v1"
                                    :input input
                                    :options options}
                           :handler (fn [response]
                                      (swap! state assoc :results (get response "output"))
                                      (swap! state assoc :time-taken (- (js/Date.now) (:request-start @state))))
                           :error-handler error-handler}))))}
     "Generate Examples"]]
   (if (= :loading (:results @state))
     [:> Spinner {:margin-x :auto :margin-y (majorScale 2)}]
     (for [example (:results @state)]
       ^{:key example}
       [:> Paragraph {:flex-direction :row
                      :margin-top (majorScale 1)
                      :padding (majorScale 1)} example]))
   (when-let [time-taken (:time-taken @state)]
     [:> Paragraph (str "Time taken: " time-taken "ms")])])

(defn app
  []
  [:> Pane {:margin (majorScale -1)}
   [:> Pane {:height (majorScale 7)
             :min-height 56
             :paddingX (majorScale 5)
             :display :flex
             :align-items :center
             :background-color "#084B8A"}
    [:> Heading {:color "white"} "Hack Week Generator"]]
   [:> Pane
    {:display :flex
     :justify-content :center
     :align-content :center
     :margin (majorScale 1)
     :align-items :center
     :flex-wrap :wrap
     :flex 1}
    [text-generation state]]])

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
