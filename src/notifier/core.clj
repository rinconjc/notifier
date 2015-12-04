(ns notifier.core
  (:gen-class)
  (:require [chime :refer [chime-at]]
            [clj-time
             [core :as t]
             [periodic :refer [periodic-seq]]]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [org.httpkit.client :as http])
  (:import [java.io FileReader PushbackReader]
           java.util.Base64
           javax.crypto.Mac
           javax.crypto.spec.SecretKeySpec))

;; ASX-ordinaries http://www.marketindex.com.au/all-ordinaries

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
    (println "retrieved btc status:" status ", error:" error ", price:" last-price)
    (if error (println "failed with " error)
        last-price)))

(defn get-asx-prices []
  )

(defn publish-event [level price percent]
  (let[url (format  "https://maker.ifttt.com/trigger/BTC-AUD-%s/with/key/%s" level @ifttt-key)
       {:keys[status body]} @(http/post url {:headers {"Content-Type" "application/json"}
                                            :body (json/write-str {:value1 (str price)
                                                                   :value2 (str percent)})})]
    (println "published event:" url " result:" status ":" body)
    status))

(defn persistent-atom [file initial]
  (let[data (atom (try
                    (-> file (FileReader.) (PushbackReader.) (edn/read))
                    (catch Exception e
                      (println "failed reading file " file ":" e)
                      initial)))]
    (add-watch data
               (fn[k r o n]
                 (spit file (pr-str n))))
    data))

(defn mk-price-nofifier [changes file]
  (let[last-prices (persistent-atom file {})]
    (fn[]
      (if-let [price (get-btc-price)]
        (doseq [c changes :let [last-price (@last-prices c)]]
          (if-let [change (and last-price (- price last-price))]
            (when (>= (Math/abs change) c)
              (publish-event c price (-> change (* 100) (/ last-price)))
              (swap! last-prices assoc c price))
            (swap! last-prices assoc c price)))))))

(defn schedule []
  (let [notifier (mk-price-nofifier [5 10 20 30 40 50 100] "btc-prices.edn")
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
