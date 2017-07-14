(ns towncrier.output.protocols)

(defprotocol AlertOutput
  (init! [this])
  (alert! [this alert])
  (close! [this]))
