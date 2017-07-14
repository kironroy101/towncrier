(ns towncrier.core
  (:require [towncrier.config :as config :refer [env]]
            [towncrier.driver :refer [monitors]]
            [mount.core :as mount :refer [defstate]]
            [clojure.tools.cli :refer [parse-opts]]
            [cheshire.core :as json]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [qbits.spandex :as s]
            [qbits.spandex.utils :as s-utils]
            [clojure.java.io :as io])
  (:import [org.yaml.snakeyaml Yaml]
           [java.util LinkedHashMap ArrayList])

  (:gen-class))

(defn read-yaml [path]
  (let [y (Yaml.)
        jyaml (.load y (io/input-stream path))]
    (-> (walk/prewalk
         (fn [v]
           (cond
             (instance? LinkedHashMap v)
             (into {} v)
             (instance? ArrayList v)
             (into [] v)
             :default v))
         jyaml)
        walk/keywordize-keys)))

(def cli-options
  [["-c" "--config CONFIG_JSON_FILE" "The config file for the shipper"]])

(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped")))

(defn start-app [args]
  (let [config-path (or (-> args
                            (parse-opts cli-options)
                            :options :config)
                        "config.yaml")
        config (read-yaml config-path)
        started (:started
                  (mount/start-with-args config))]
    (log/info "Started towncrier" started)
    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app))))

(defn -main [& args]
  (start-app args)
  (loop []
    (Thread/sleep (* 1000 60 60 24))))

(comment
  (start-app [])
  (stop-app)
  )
