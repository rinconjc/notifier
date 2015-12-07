(defproject notifier "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [jarohen/chime "0.1.6"]
                 [http-kit "2.1.18"]
                 [enlive "1.1.6"]
                 [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot notifier.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
