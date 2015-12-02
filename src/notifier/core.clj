(ns notifier.core
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]
           [java.util Base64])
  (:require [clojure.data.json :as json]
            [clj-time.core :as t]
            [chime :refer [chime-at]]
            [clj-time.periodic :refer [periodic-seq]]
            [org.httpkit.client :as http])
  (:gen-class))

(def ifttt-key (delay (System/getProperty "ifttt-key")))

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
    (println "retrieved btc price:" status ", error:" error ", price:" last-price)
    (if error (println "failed with " error)
        last-price)))

(defn publish-event [level price percent]
  (let[url (str "https://maker.ifttt.com/trigger/BTC-AUD-%s/with/key/%" level @ifttt-key)
       {:keys[status body]} (http/post url {:headers {"Content-Type:" "application/json"}
                                            :body (json/write-str {:value1 price :value2 percent})})]
    (println "published event:" url " result:" status ":" body)))

(defn mk-price-nofifier [changes]
  (let[last-prices (atom {})]
    (fn[]
      (if-let [price (get-btc-price)]
        (doseq [c changes :let [last-price (@last-prices c)]]
          (if-let [change (and last-price (- price last-price))]
            (when (>= (Math/abs change) c)
              (publish-event c price (/ change last-price))
              (swap! last-prices assoc c price))
            (swap! last-prices assoc c price)))))))

(defn schedule []
  (let [notifier (mk-price-nofifier [10 20 30 40 50 100])
        times (periodic-seq (t/now) (-> 2 t/minutes))]
    (chime-at times (fn[time]
                      (println "chiming at " time)
                      (notifier)))))

(defn -main
  "Starting notifier..."
  [& args]
  (println "Starting notifier..")
  (schedule)
  (Thread/sleep (Long/MAX_VALUE)))
