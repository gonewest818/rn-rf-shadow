(ns example.app
  (:require
   ["cavy" :as cavy :refer (Tester TestHookStore hook)]
   ["expo" :as ex]
   ["react-native" :as rn]
   ["react" :as react]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [shadow.expo :as expo]
   [example.events]
   [example.subs]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

(defonce splash-img (js/require "../assets/shadow-cljs.png"))

(def test-hook-store (TestHookStore.))

(def styles
  ^js (-> {:container
           {:flex 1
            :backgroundColor "#fff"
            :alignItems "center"
            :justifyContent "center"}
           :title
           {:fontWeight "bold"
            :fontSize 24
            :color "blue"}
           :button
           {:fontWeight "bold"
            :fontSize 18
            :padding 6
            :backgroundColor "blue"
            :borderRadius 10}
           :buttonText
           {:paddingLeft 12
            :paddingRight 12
            :fontWeight "bold"
            :fontSize 18
            :color "white"}
           :label
           {:fontWeight "normal"
            :fontSize 15
            :color "blue"}}
          (clj->js)
          (rn/StyleSheet.create)))

(defn guts []
  (let [counter (rf/subscribe [:get-counter])
        generate-test-hook (-> (r/current-component)
                               (r/props)
                               :generateTestHook)]
    (fn []
      [:> rn/View {:style (.-container styles)}
       [:> rn/Text {:style (.-title styles)} "Clicked: " @counter]
       [:> rn/TouchableOpacity {:style    (.-button styles)
                                :on-press #(rf/dispatch [:inc-counter])}
        [:> rn/Text {:style (.-buttonText styles)} "Click me, I'll count"]]
       [:> rn/Image {:source splash-img :style {:width 200 :height 200}
                     :ref (generate-test-hook "LogoImage")}]
       [:> rn/Text {:style (.-label styles)} "Using: shadow-cljs+expo+reagent+re-frame"]])))

;; This is the pattern to wrap a reagent component
;; so we can then instrument (via hook) with cavy...
(def wrap-guts (-> guts r/reactify-component hook))

;; This is my guess at the cljs version of a cavy test spec
(defn cavy-test-something [^TestScope spec]
  (.describe spec "The guts"
             (fn []
               (.it spec "has a component"
                    (fn []
                      (.exists spec "LogoImage"))))))

(defn root []
  (fn []
    [:> Tester {:specs [cavy-test-something] :store test-hook-store}
     [:> wrap-guts]]))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root])))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (start))
