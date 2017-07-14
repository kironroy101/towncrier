(ns towncrier.output.console
  (:require [towncrier.output.protocols :as protos]
            [clojure.tools.logging :as log]))

(defrecord ConsoleAlertOutput []
    protos/AlertOutput
    (init! [this])
    (alert! [this alert]
      (do (println alert)
          (log/warn alert)))
    (close! [this]))
