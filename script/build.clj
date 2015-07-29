(ns script.bootstrap.build
  (:require [clojure.java.io :as io]
            [cljs.closure :as closure]
            [cljs.env :as env]))

(defn compile1 [copts file]
  (let [targ (io/resource file)
        _ (println "Compiling:" targ)
        core-js (closure/compile targ
                  (assoc copts
                    :output-file (closure/src-file->target-file targ)))
        deps    (closure/add-dependencies copts core-js)]
    deps))

(defn build [dir file opts]
  (let [output-dir (io/file dir)
        copts (assoc opts
                     :output-dir output-dir
                     :cache-analysis true
                     :source-map true
                     :def-emits-var true)]
    (env/with-compiler-env (env/default-compiler-env opts)
      ;; output unoptimized code and the deps file
      ;; for all compiled namespaces
      (apply closure/output-unoptimized
        (assoc copts
          :output-to (.getPath (io/file output-dir "deps.js")))
        (compile1 copts file))

      ;; Google Closure Library node compatibility shim
      (let [path (.getPath (io/file output-dir "goog/bootstrap/nodejs.js"))]
        (io/make-parents path)
        (spit path (slurp (io/resource "cljs/bootstrap_node.js")))))))


(println "Building cljs_bootstrap")
;;(build ".cljs_bootstrap" "cljs_bootstrap/core.cljs" nil)
(build ".cljs_bootstrap" "cljs_bootstrap/repl.cljs" nil)
(println "Done building cljs_bootstrap")
