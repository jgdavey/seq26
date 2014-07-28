(ns seq26.core
    (:require-macros [cljs.core.async.macros :refer [go alt!]])
    (:require [goog.events :as events]
              [cljs.core.async :as async :refer [put! <! >! chan timeout]]
              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [seq26.hum :as hum]
              [seq26.utils :as util :refer [find-first not-nil?]]))

(enable-console-print!)

(def audio-context (hum/create-context))

(def app-state
  (atom {:things []}))

(defn seq26-app [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h1 nil "seq26 is working!")))))

(om/root seq26-app app-state {:target (.getElementById js/document "content")})



(def ctx (hum/create-context))
(def vco (hum/create-osc ctx :square))
(def vcf (hum/create-biquad-filter ctx))
(def output (hum/create-gain ctx))

(hum/connect vco vcf)
(hum/connect vcf output)

(hum/start-osc vco)

(hum/connect-output output)

(def notes (async/to-chan (mapv hum/midi->hz [42 44 45 48 42])))

(go (loop []
      (hum/note-on output vco (<! notes) :time (+ 1 (hum/curr-time ctx)))
      (<! (timeout 1000))
      (if-let [n (<! notes)]
        (do
          (hum/set-freq vco n)
          (<! (timeout 200))
          (recur))
        (do
          (<! (timeout 200))
          (hum/note-off output)))))
