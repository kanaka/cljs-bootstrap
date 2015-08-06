(ns script.bootstrap.build
  (:require [clojure.java.io :as io]
            [cljs.build.api :as api]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayOutputStream]))

(defn extract-analysis-cache [out-path]
  (let [out (ByteArrayOutputStream. 1000000)
        writer (transit/writer out :json)
        cache (read-string
                (slurp (io/resource "cljs/core.cljs.cache.aot.edn")))]
    (transit/write writer cache)
    (spit (io/file out-path) (.toString out))))


(println "Building cljs_bootstrap")
(api/build (api/inputs "src/cljs_bootstrap")
  {:output-dir         ".cljs_bootstrap"
   :output-to          ".cljs_bootstrap/deps.js"
   :cache-analysis     true
   :source-map         true
   ;:source-map         ".cljs_bootstrap/source-map.json"
   :optimizations      :none
   ;:optimizations      :simple
   :static-fns         true
   :optimize-constants true
   :dump-core          false
   :verbose            true})

(println "Extracting Google Closure Library node compatibility shim")
(let [path ".cljs_bootstrap/goog/bootstrap/nodejs.js"]
  (io/make-parents path)
  (spit path (slurp (io/resource "cljs/bootstrap_node.js"))))

(println "Using transit to extract core analysis cache")
(extract-analysis-cache ".cljs_bootstrap/cljs/core.cljs.cache.aot.json")

(println "Done building cljs_bootstrap")
(System/exit 0)
