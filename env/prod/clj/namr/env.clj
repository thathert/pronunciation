(ns namr.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[namr started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[namr has shut down successfully]=-"))
   :middleware identity})
