(defproject cljs-bootstrap "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.45"]
                 [org.clojure/tools.reader "0.10.0-SNAPSHOT"]
                 [com.cognitect/transit-clj "0.8.275"]
                 [com.cognitect/transit-cljs "0.8.220"]]
  :plugins [[lein-npm "0.5.0"]]
  :node-dependencies [[source-map-support "0.3.1"]])
