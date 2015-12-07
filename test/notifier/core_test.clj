(ns notifier.core-test
  (:require [clojure.test :refer :all]
            [notifier.core :refer :all]))

(deftest publish-test
  (testing "testing publishing an event"
    (is (= 200 (publish-event 10 500.0 2.0)))))

(deftest fetch-asx-prices
  (testing "parsing asx prices"
    (let [prices (get-asx-prices)]
      (println "asx stock prices:" prices)
      (is (> (count prices) 100)))))
