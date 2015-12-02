(ns notifier.core
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]
           [java.util Base64]
           [org.httpkit.client :as http])
  (:require [clojure.data.json :as json])
  (:gen-class))

(def last-price (atom nil))

(defn sign-req [uri key body]
  (let [mac (doto (Mac/getInstance "HmacSHA512")
              (.init (SecretKeySpec. (.getBytes key) "HmacSHA512")))
        input (str uri \n (System/currentTimeMillis) (if body (str \n body)))]

    (.encodeToString (Base64/getEncoder)
                     (.doFinal mac (.getBytes input "UTF-8")))))

(defn get-btc-price []
  (let [{:keys [status headers body error] :as resp} @(http/get "https://api.btcmarkets.net/market/BTC/AUD/tick")
        data (json/read-str body)
        last-price (data "lastPrice")]
    (if error (println "failed with " error)
        last-price)))

(defn mk-price-nofifier [changes]
  (let[last-prices (atom (into {} (for [c changes] [c (atom nil)])))]
    (fn[]
      (if-let [price (get-btc-price)]
        (doseq [c changes :let [last-price (@last-prices c)]]
          (if @last-price
            (let [change (@last-price (- price @last-price))]
              (when (> change c)
                (notify-change c price (/ change @last-price))
                (assoc! )))
            (reset! last-price price)))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
