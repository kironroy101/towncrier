(ns user
  (:require [mount.core :as mount]
            [towncrier.core]
            ))

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(defn restart []
  (stop)
  (start))
