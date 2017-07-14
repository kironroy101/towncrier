(ns towncrier.window
  (:require [clj-time.core :as time]
            [clj-time.coerce :as tcoerce]
            [clojure.core.async :as async]
            [clojure.test :as test]
            ))


(defn dedupe-sliding-channels
  "Returns an [in out] where in and out are channels. Values placed
  onto on channel are deduped for a sliding window of the provided
  window-duration. For example, (>! in :a) executed multiple times
  during an interval less than the window-duration will result in only
  a single :a value getting placed on the out channel."
  [window-duration]
  (let [msg-buffer (atom [])
        in-buffer?
        (fn [v]
          (some (fn [bv]
                  (= v (:data bv)))
                @msg-buffer))
        clean-expired
        (fn [buf]
          (filter
           (fn [bv]
             (let [t (:time bv)]
               (time/after? t (-> window-duration time/millis time/ago))))
           buf))
        conj-with-buffer (fn [v]
                           (swap! msg-buffer
                                  conj
                                  {:time (time/now)
                                   :data v}))
        in (async/chan 10)
        out (async/chan 10)]

    (async/go-loop []
      (let [v (async/<! in)]
        (swap! msg-buffer clean-expired)
        (when (some? v)
          (when-not (in-buffer? v)
            (conj-with-buffer v)
            (async/>! out v))
          (recur))))
    [in out]))

(test/deftest test-dedupe-sliding-channels
  (let [test-coll (atom [])
        [in out] (dedupe-sliding-channels 1000)]
    (async/thread
      (dotimes [_ 3]
        (doseq [v [:a :b :c]]
          (async/>!! in v)))
      (Thread/sleep 3000)
      (dotimes [_ 3]
        (doseq [v [:a :b :c]]
          (async/>!! in v)))
      (dotimes [_ 7]
        (swap! test-coll conj
               (first (async/alts!! [out (async/timeout 500)])))))
    (Thread/sleep 5000)
    (async/close! in)
    (async/close! out)
    (test/is
     (= @test-coll
        [:a :b :c :a :b :c nil]))
    ))

(comment
  (test/run-tests)
  )
