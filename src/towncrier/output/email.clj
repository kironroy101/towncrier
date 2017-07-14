(ns towncrier.output.email
  (:use postal.core)
  (:use [clojure.string :only [join]])
  (:require [towncrier.output.protocols :as protos]))

(defrecord EmailAlertOutput [smtp-conf from-email to-emails subject-fn body-fn]
  protos/AlertOutput
  (init! [this])
  (alert! [this alert]
    (let [send-msg (if (some? (:smtp-conf this))
                     (partial send-message (:smtp-conf this))
                     send-message)]
      (doseq [to (:to-emails this)]
        (send-msg {:to to
                   :from (:from-email this)
                   :subject ((:subject-fn this) alert)
                   :body ((:body-fn this) alert)}))))
  (close! [this]))

(defn email-output [{:keys [smtp from to] :as output-conf}
                    subject-fn body-fn]
  (EmailAlertOutput.
   smtp
   from
   (if (string? to) [to] to)
   subject-fn
   body-fn))

(comment

  (def output (EmailAlertOutput.
               {:host "smtp.fastmail.com"
                :port 465
                :user "hiron@fastmail.fm"
                :pass "XXXXXXXXXXXXXXXX"
                :ssl true}
               "hiron@fastmail.com"
               ["me@hironroy.com"]
               (fn [a] "Trawler Alert")
               (fn [a] (pr-str a))))

  )
