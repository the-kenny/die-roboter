(ns die.test.roboter
  (:refer-clojure :exclude [future send-off])
  (:use [die.roboter]
        [clojure.test])
  (:require [com.mefesto.wabbitmq :as wabbit]
            [clojure.tools.logging :as log])
  (:import (java.util.concurrent TimeUnit)))

(.setLevel (java.util.logging.Logger/getLogger "die.roboter")
           java.util.logging.Level/ALL) ; TODO: no-op

(def state (atom {}))

(defn clear-queues! [queue-name]
  (with-robots {}
    (wabbit/with-queue queue-name
      (doall (take 100 (wabbit/consuming-seq true 1))))))

(defn work-fixture [f]
  (clear-queues! "die.roboter.work")
  (let [worker (clojure.core/future (work))]
    (reset! state {})
    (try (f)
         (finally (.cancel worker true)
                  (remove-watch state :unblocker)))))

(defmacro with-block [n body]
  `(let [blockers# (repeat ~n (promise))
         blocked# (atom blockers#)]
     (add-watch state :unblocker (fn [& _#]
                                   (deliver (first @blocked#) true)
                                   (swap! blocked# rest)))
     ~body
     (.get (clojure.core/future (doseq [b# blockers#] @b#))
           1 TimeUnit/SECONDS)))

(use-fixtures :each work-fixture)

(deftest test-send-off
  (with-block 1
    (send-off `(swap! state assoc :ran true)))
  (is (:ran @state)))

(deftest test-future
  (is (= 1 (.get (future 1) 100 TimeUnit/MILLISECONDS))))

(def bound :root)

(deftest test-broadcast
  (with-block 2
    ;; TODO: these jerks aren't pulling their weight
    (let [worker1 (clojure.core/future
                   (binding [bound 1]
                     (work-on-broadcast)))
          worker2 (clojure.core/future
                   (binding [bound 2]
                     (work-on-broadcast)))]
      (try (broadcast `(swap! state assoc bound true))
           (finally (.cancel worker1 true)
                    (.cancel worker2 true)))))
  (is (= {1 true 2 true} @state)))
