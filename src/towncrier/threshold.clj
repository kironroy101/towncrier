(ns towncrier.threshold
  (:require
    [cheshire.core :as json]
    [clojure.walk :as walk]
    [mount.core :as mount :refer [defstate]]
    [clojure.pprint :as pprint]
    [clojure.string :as string]
    [clojure.tools.logging :as log]
    [clojure.pprint :refer [pprint]]
    [qbits.spandex :as s]
    [qbits.spandex.utils :as s-utils]
    [towncrier.utils :refer [get-marg]]
    ))

(defstate es-client
  :start (s/client {:hosts
                    (into []
                          (map
                           (fn [{:keys [host port protocol]}]
                             (format "%s://%s:%s" protocol host port))
                           (get-marg [{:host "127.0.0.1" :port 9200 :protocol "http"}]
                                     :elasticsearch :hosts)))
                    :default-headers
                    {"Content-Type" "application/json"}})
  :stop (s/close! es-client))

(defn inject-threshold-defaults [conf threshold]
  (merge {:window (* 15 60) :poll 60}
         (select-keys (:time conf) [:window :poll])
         threshold))

(defn get-threshold-query
  [threshold]
  (let
      [q (:query threshold)
       window (:window threshold)
       service (:service q)
       search (:search q)]
    {:bool {:must (if (some? search)
                    {:match {:msg {:query search}}}
                    {:match_all {}})
            :filter
            (into []
                  (remove nil?
                          [(when (some? service)
                             {:term {:service service}})
                           {:range {:time {:gt (format "now-%ss" window)}}}]))}}
    ))

(defn conforms-threshold? [{:keys [indexPattern limit threshold] :as t-conf}]
  (let [q (get-threshold-query t-conf)
        resp (s/request es-client
                        {:url (s-utils/url [indexPattern :log-event :_search])
                         :method :get
                         :body {:query q
                                :size 0}})
        total (-> resp :body :hits :total)
        comp-fn (if (= "upper" limit)  (partial > threshold) (partial < threshold))]
    (comp-fn total)))


(comment

  (pprint (get-threshold-query
           (inject-threshold-defaults
            {
             :poll 30}
            {:indexPattern "trawler-*"
             :threshold 1
             :poll 30
             :limit "upper"
             :query {
                     :search "stop"
                     ;;:service "rabbitmq"
                     }})))

  (mount/start #'es-client)

  (def conf (walk/keywordize-keys
                (json/parse-string (slurp "config.json"))))

  (println (-> conf :thresholds first))
  (pprint (conforms-threshold?
           (-> conf :time :window)
           (-> conf :thresholds first)))

  (pprint
   (s/request es-client
              {:url (s-utils/url ["trawler-*"
                                  :log-event :_search])
               :method :get
               :body {:query
                      {:bool
                       {
                        :must {:match {:msg {:query "stop"}}}
                        :filter
                        [{:term {:service "rabbitmq"}}
                         {:range {:time {:gt "now-10m"}}}
                         ]}}
                      :size 0
                      }})
   )

  )
