(ns generator-client.main
  (:require [ajax.core :as ajax]
            [cljs-bean.core :refer [bean ->js ->clj]]
            [clojure.string :as string]
            ["evergreen-ui"
             :refer
             [Badge Pane Text TextInputField Strong Button SelectMenu Textarea
             majorScale minorScale Heading Code UnorderedList ListItem default-theme
             Paragraph Spinner]]
            goog.object
            ["react" :as react]
            ["react-dom" :as react-dom]
            [reagent.core :as reagent]
            reagent.dom))

(goog-define api-root "")

(defonce state
  (reagent/atom
   {:input-updated 1
    :input ""
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
         (merge {:height (majorScale 60)
                 :min-height (majorScale 40)
                 :width (majorScale 90)
                 :border :none
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

(defn event-select
  [{:keys [selected-event-type on-select-event event-types] :as props} & children]
  (into [:> SelectMenu
         {:title "Select Event Type"
          :options event-types
          :selected (:value selected-event-type)
          :on-select (fn [item]
                       (-> item
                           (js->clj :keywordize-keys true)
                           on-select-event))
          :close-on-select true}]
        (if (not-empty children)
          children
          [[:> Button
            {:flex-grow 1
             :justify-content :center
             :margin-right (majorScale 1)}
            (or (:label selected-event-type)
                "Select an event type...")]])))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(def default-model-options
  {:add_words 3
   :top_k 50
   :top_p 0.9
   :temperature 1.0
   :num_return_sequences 10})

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

(defn generate-text
  [type]
  (fn []
    (if-let [input (:input @state)]
      (let [max-length (+ (get-in @state [:options :add_words] (:add_words default-model-options))
                          (count (string/split input " ")))
            options (merge default-model-options (:options @state) {:max_length max-length})]
        (swap! state assoc
               :results :loading
               :time-taken nil
               :request-start (js/Date.now))
        (ajax/POST (api "/generators")
            {:format :json
             :response-format :json
             :params {:model (if (= :extend type) "buddy.v1" "macaulay.v1")
                      :input input
                      :options (if (= :extend type) options
                                   {:top_k 50
                                    :sample_size 10})}
             :handler (fn [response]
                        (swap! state assoc :results (get response "output"))
                        (swap! state assoc :time-taken (- (js/Date.now) (:request-start @state))))
             :error-handler error-handler})))))

(defn input-text-area
  [state]
  ^{:key (:input @state)}
  [:> Textarea
   {:flex-grow 4
    :placeholder "Write an exam question stem..."
    :default-value (:input @state)
    :on-blur (fn [event]
               (swap! state assoc :input (-> event .-target .-value)))}])

(defn active-example?
  [state example]
  (= example (get-in @state [:active-result])))

(defn text-generation
  [state]
  [:> Pane
   (merge {:width          (majorScale 90)
           :border         :default
           :display        :flex
           :flex-direction :column
           :padding        (majorScale 3)
           :margin         (majorScale 2)})
   [input-text-area state]
   [:> Pane
    [model-option {:label "Add Words"
                   :key :add_words
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
      :on-click (generate-text :extend)}
     "Extend (Ctrl-E)"]
    [:> Button
     {:flex-grow 1
      :justify-content :center
      :on-click (generate-text :replace)}
     "Replace (Ctrl-R)"]]
   (if (= :loading (:results @state))
     [:> Spinner {:margin-x :auto :margin-y (majorScale 2)}]
     (doall
      (for [example (distinct (:results @state))]
        ^{:key example}
        [:> Pane {:elevation (if (active-example? state example) 2 0)
                  :flex-direction :row
                  :margin-top (majorScale 1)
                  :padding (majorScale 1)
                  :border-width 1
                  :border-color "#DDEBF7"
                  :border-style "solid"
                  :background-color (if (active-example? state example) "#DDEBF7" "white")
                  :on-mouse-enter (fn [] (swap! state assoc :active-result example))
                  :on-mouse-leave (fn [] (swap! state dissoc :active-result))
                  :on-click (fn [event]
                              (swap! state assoc :input example :results nil :time-taken nil)
                              (swap! state update :input-updated inc))}
         [:> Paragraph example]])))
   (when-let [time-taken (:time-taken @state)]
     [:> Paragraph {:margin-top (majorScale 1)
                    :padding (minorScale 1)}
      (str "Time taken: " time-taken "ms")])])

(defn app
  [state]
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
  (reagent.dom/render [app state]
                      (js/document.getElementById "root"))
  (set! (.-onkeydown js/document)
        (fn [event]
          (let [key-map {{:ctrl true
                          :key "e"}
                         (fn []
                           ((generate-text :extend)))
                         {:ctrl true
                          :key "r"}
                         (fn []
                           ((generate-text :replace)))}]
            (when-let [key-action (key-map {:ctrl (.-ctrlKey event)
                                            :key (.-key event)})]
              (.preventDefault event)
              (key-action))))))

(defn ^:export init
  []
  (start))


(comment
  (set! (.-onkeydown js/document)
        (fn [event]
          #p (bean event)
          (let [key-map {{:ctrl true
                          :key "e"}
                         (fn []
                           ((generate-text :extend)))
                         {:ctrl true
                          :key "r"}
                         (fn []
                           ((generate-text :replace)))}]
            (when-let [key-action (key-map {:ctrl (.-ctrlKey event)
                                            :key (.-key event)})]
              (.preventDefault event)
              (key-action)))))
  )
