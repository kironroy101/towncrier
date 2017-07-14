(ns towncrier.driver
  (:require
    [towncrier.output.console :as console]
    [towncrier.output.email :as email]
    [towncrier.output.core :as outcore]
    [towncrier.output.protocols :as outproto]
    [towncrier.window :as window]
    [towncrier.threshold :as threshold]
    [cheshire.core :as json]
    [clojure.walk :as walk]
    [clojure.core.async :as async]
    [clojure.tools.logging :as log]
    [clojure.pprint :refer [ pprint ]]
    [mount.core :as mount]
    )
  (:import [towncrier.output.console ConsoleAlertOutput]
           [towncrier.output.email EmailAlertOutput]))

(def console-output (ConsoleAlertOutput.))

#_(outproto/alert! console-output {:hello :console-output})


(defn monitor-threshold [t-conf outputs]
  (let [poll-s (:poll t-conf)
        window-s (:window t-conf)
        [dedupe-in dedupe-out]
        (window/dedupe-sliding-channels (* 1000 window-s))
        cmd-ch (async/chan)
        running (atom true)]

    (async/go-loop []
      (case (async/<! cmd-ch)
        :stop (do (reset! running false)
                  (async/close! cmd-ch)
                  (log/debug "Stopping threshold monitor" t-conf))
        (recur)))

    (async/go-loop []
      (when @running
        (let [tcheck (threshold/conforms-threshold?
                      t-conf)]
          (when-not tcheck
            (async/>! dedupe-in t-conf)))
        (async/<! (async/timeout (* 1000 poll-s)))
        (recur)))

    (async/go-loop []
      (when-not @running
        (async/close! dedupe-in)
        (async/close! dedupe-out))

      (when @running
        (let [out-val (async/<! dedupe-out)]
          (when (some? out-val)
            (doseq [o outputs]
              (outproto/alert! o out-val))))
        (recur)))

    cmd-ch))

(defn build-outputs [conf]
  (let [o-confs (:outputs conf)]
    (into [] (map outcore/build-output o-confs))))

(defn start-monitors [conf]
  (let [outputs (build-outputs conf)]
    (into []
          (map
           (fn [t-conf]
             (monitor-threshold (threshold/inject-threshold-defaults
                                 conf t-conf)
                                outputs))
           (:thresholds conf)))))

(defn stop-monitors [mons]
  (doseq [m mons]
    (async/>!! m :stop)))

(mount/defstate monitors
  :start (start-monitors (mount/args))
  :stop (stop-monitors monitors))

(comment

  (mount.core/start #'towncrier.threshold/es-client)

  (def conf (walk/keywordize-keys
                (json/parse-string (slurp "config.json"))))

  (mount/start-with-args conf #'monitors)
  (mount/stop #'monitors)

  )
