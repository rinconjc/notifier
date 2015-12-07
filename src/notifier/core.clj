(ns notifier.core
  (:gen-class)
  (:require [chime :refer [chime-at]]
            [clj-time
             [core :as t]
             [periodic :refer [periodic-seq]]]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [net.cgrand.enlive-html :as html]
            [org.httpkit.client :as http])
  (:import [java.io FileReader PushbackReader]
           java.net.URL
           java.text.DecimalFormat
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
  (let [rows (-> "http://www.marketindex.com.au/all-ordinaries" URL. html/html-resource
                 (html/select [:#asx_sp_table :tbody :tr]))
        price-fmt (DecimalFormat. "$#.0#")]
    (for [row rows :let[cols (-> row (html/select [:td]))
                        code (:content (second cols))
                        price (.parse price-fmt (-> cols (nth 3) :content))]]
      [code price])))

(defn publish-event [event & values]
  (let[url (format  "https://maker.ifttt.com/trigger/%s/with/key/%s" event @ifttt-key)
       body (into {} (map-indexed
                      (fn[i v] [(->> i inc (str "value") keyword) v]) values))
       {:keys[status body]} @(http/post url {:headers {"Content-Type" "application/json"}
                                             :body (and values (json/write-str body))})]
    (println "published event:" url " result:" status ":" body)
    status))

(defn persistent-atom [file initial]
  (let[data (atom (try
                    (-> file (FileReader.) (PushbackReader.) (edn/read))
                    (catch Exception e
                      (println "failed reading file " file ":" e)
                      initial)))]
    (add-watch data :key
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
              (publish-event (str "BTC-AUD-" c) (str price)
                             (format "%+.1f%%" (-> change (* 100) (/ last-price)))
                             (format "%+.1f" change))
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
