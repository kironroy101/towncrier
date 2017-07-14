(ns towncrier.output.core
  (:require [towncrier.output.protocols :as proto]
            [towncrier.output.email :as email]
            [clojure.pprint :as pprint]
            [clj-time.core :as time]
            [clj-time.format :as tformat]
            [clj-time.coerce :as tcoerce])
  (:import [towncrier.output.console ConsoleAlertOutput]
           [towncrier.output.email EmailAlertOutput]))

(defmulti build-output :type)

(defmethod build-output "email"
  [conf]
  (email/email-output
   conf
   (fn [a] (str
            "Trawler"
            (if (contains? a :name)
              (format " | Alert '%s'" (:name a))
              (" Alert"))))
   (fn [a]
     (str
      (format "Threshold Violation occurred at: %s\n Threshold:\n %s"
              (tformat/unparse
               (:mysql tformat/formatters) (time/now))
              (with-out-str (pprint/pprint a)))))))

(defmethod build-output "console"
  [_]
  (ConsoleAlertOutput.))


(comment

  (tformat/show-formatters)
  (with-out-str (pprint/pprint (assoc {:a "Hello"}
                                      :violation-time
                                      (tformat/unparse (:mysql tformat/formatters) (time/now)  ))))
  )
