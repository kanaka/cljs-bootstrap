(ns script.bootstrap.build
  (:require [clojure.java.io :as io]
            [cljs.build.api :as api]))

(println "Building cljs_bootstrap")
(api/build (api/inputs "src/cljs_bootstrap")
  {:output-dir         ".cljs_bootstrap"
   :output-to          ".cljs_bootstrap/deps.js"
   :cache-analysis     true
   :source-map         true
   :optimizations      :none
   :static-fns         true
   :optimize-constants true
   :verbose            true})

;; Extract Google Closure Library node compatibility shim
(let [path ".cljs_bootstrap/goog/bootstrap/nodejs.js"]
  (io/make-parents path)
  (spit path (slurp (io/resource "cljs/bootstrap_node.js"))))

(println "Done building cljs_bootstrap")
(System/exit 0)
