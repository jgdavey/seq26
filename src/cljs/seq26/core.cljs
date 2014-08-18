(ns seq26.core
    (:require-macros [cljs.core.async.macros :refer [go alt!]])
    (:require [goog.events :as events]
              [cljs.core.async :as async :refer [put! <! >! chan timeout]]
              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [seq26.hum :as hum]
              [seq26.utils :as util :refer [find-first not-nil?]]))

(enable-console-print!)

(defprotocol Instrument
  (-play-at [this midi on-at off-at]))

(def app-state
  (atom {:params {:bpm 120}}))

(defn seq26-app [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h1 nil "seq26 is working!")))))

(om/root seq26-app app-state {:target (.getElementById js/document "content")})

(def ctx (hum/create-context))
(def output (hum/create-gain ctx))

(hum/connect-output output)

(hum/set-gain-to output 1.0)

(defn bpm []
  (get-in @app-state [:params :bpm]))

(defn beat->s [b]
  (* (/ 60 (bpm)) b))

(defn s->beats [s]
  (* (/ (bpm) 60) s))

(defn- bare-oscillator [connect-to freq & [type]]
  (let [osc (.createOscillator ctx)]
    (set! (.-value (.-frequency osc)) freq)
    (set! (.-type osc) (or type "sine"))
    (hum/connect osc connect-to)
    (.start osc)
    osc))

(defn oscillator [connect-to freq & [type]]
  (let [gain (hum/create-gain ctx)
        osc (bare-oscillator gain freq type)]
    (hum/connect gain connect-to)
    gain))

(defn bell [connect-to freq]
  (let [harmonic-series [1  2  3   4.2  5.4 6.8]
        proportions     [1 0.6 0.4 0.25 0.2 0.15]
        control (hum/create-gain ctx)
        _ (hum/connect control connect-to)
        component
         (fn [harmonic proportion]
           (let [gain-node (hum/create-gain ctx (* 0.8 proportion))
                 hz (* freq harmonic)
                 osc (bare-oscillator gain-node hz)]
             (hum/connect gain-node control)))]
  (doall (map component harmonic-series proportions))
  control))

(defn on-off-inst
  "Given a map of instrument components (notes, etc), returns an Instrument"
  [notes]
  (reify Instrument
    (-play-at [_ midi on-at off-at]
      (let [note (notes midi)]
        (.setValueAtTime (.-gain note) 0.0 (- on-at 0.001))
        (.linearRampToValueAtTime (.-gain note) 1.0 on-at)
        (.setValueAtTime (.-gain note) 1.0 (- off-at 0.005))
        (.linearRampToValueAtTime (.-gain note) 0.0 (+ off-at 0.005))))))

(defn env-off-inst
  [notes & [decay]]
  (reify Instrument
    (-play-at [_ midi on-at off-at]
      (let [note (notes midi)
            decay (or decay 1.4)] ; length of decay, in seconds
        (.linearRampToValueAtTime (.-gain note) 0.0 (- on-at 0.001))
        (.cancelScheduledValues (.-gain note) on-at)
        (.linearRampToValueAtTime (.-gain note) 1.0 on-at)
        (.linearRampToValueAtTime (.-gain note) 0.8 (+ on-at 0.01))
        (.setValueAtTime (.-gain note) 0.8 off-at)
        (.exponentialRampToValueAtTime (.-gain note) 0.01 (+ off-at decay))
        (.setValueAtTime (.-gain note) 0 (+ off-at decay 0.001))))))

(defn make-inst [instrument-fn midi-notes]
  (let [notes (zipmap midi-notes
                      (map #(instrument-fn output (hum/midi->hz %))
                           midi-notes))]
    (env-off-inst notes)))


(def dejitter-factor (beat->s 1)) ; one beat of dejitter

(defn current-beat [zero]
  (s->beats (+ (- (.-currentTime ctx) zero) dejitter-factor)))

(defn scheduler
  "Takes an 'instrument'.
  Returns a core/async channel; each message to this channel must be
  a note, and will be scheduled with -play-at
  Notes should be scheduled in order."
  [inst]
  (let [zero-t (+ (.-currentTime ctx) dejitter-factor) ; avoid jitter
        c (chan 8)]
    (go (loop []
          (when-let [n (<! c)]
            (let [beat (current-beat zero-t)]
              (when (> (- (:beat n) beat) 0) ; when more than a beat ahead (jitter applied)
                (<! (timeout (* 1000 (beat->s 1))))) ; wait for one beat, then proceed
              (let [{:keys [midi beat length]} n
                    from (+ zero-t (beat->s beat))
                    until (+ from (beat->s length))]
                (-play-at inst midi from until))
              (recur)))))
    c))

(defn play
  ([notes]
   (play notes oscillator))
  ([notes inst-fn]
   (let [inst (make-inst inst-fn (set (map :midi notes)))
         sch (scheduler inst)]
     (async/onto-chan sch notes))))


(comment

(play [{:midi 69 :beat 0 :length 1}
       {:midi 73 :beat 0.25 :length 1}
       {:midi 76 :beat 0.5 :length 1}
       {:midi 52 :beat 1 :length 0.5}

       {:midi 69 :beat 2 :length 1}
       {:midi 73 :beat 2 :length 1}
       {:midi 76 :beat 2 :length 1}
       {:midi 52 :beat 3 :length 0.5}

       {:midi 69 :beat 4 :length 1}
       {:midi 74 :beat 4.25 :length 1}
       {:midi 76 :beat 4.5 :length 1}
       {:midi 52 :beat 5 :length 0.5}

       {:midi 69 :beat 6 :length 1}
       {:midi 74 :beat 6 :length 1}
       {:midi 76 :beat 6 :length 1}
       {:midi 52 :beat 7 :length 0.5} ]
      bell)

(swap! app-state assoc-in [:params :bpm] 100)

)
