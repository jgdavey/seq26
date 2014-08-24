(ns seq26.core
  (:require [compojure.route :as route]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.response :as response]
            [ring.util.mime-type :refer [ext-mime-type]]
            [ring.util.response :as r]
            [ring.middleware.head :refer [wrap-head]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]])
  (:import [java.net URL]))

(defn- request-path
  "Return the path + query of the request."
  [request]
  (str (:uri request)
       (if-let [query (:query-string request)]
         (str "?" query))))

(defn- content-type-response
  [resp req & [opts]]
  (if (r/get-header resp "Content-Type")
    resp
    (if-let [mime-type (ext-mime-type (:uri req) (:mime-types opts))]
      (r/content-type resp mime-type)
      resp)))

(defn wrap-content-type [handler & [opts]]
  (fn [req]
    (if-let [resp (handler req)]
      (content-type-response resp req opts))))

(defn proxy-response
  "A route that returns a 404 not found response, with its argument as the
  response body."
  [url-root]
  (->
    (fn [request]
      (let [url (URL. (str url-root (request-path request)))]
        (response/render url request)))
    wrap-content-type
    wrap-head))

(defroutes app-routes
  (route/resources "/")
  (proxy-response "http://localhost:4567"))

(def app
  (-> #'app-routes
      wrap-keyword-params
      wrap-nested-params
      wrap-params))
