(ns towncrier.utils
  (:require [mount.core :as mount]))

(defn get-marg
  [default & path]
  (get-in (mount/args) path default))
