(ns namr.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [namr.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[namr started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[namr has shut down successfully]=-"))
   :middleware wrap-dev})
