(ns seq26.utils
  (:require [cljs.reader :as reader])
  (:import [goog.ui IdGenerator]))

(def not-nil? (complement nil?))

(defn find-first [pred coll]
  (some (fn [x] (and (pred x) x)) coll))

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))
