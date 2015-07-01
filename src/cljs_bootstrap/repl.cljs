(ns cljs-bootstrap.repl
  (:require-macros [cljs.env.macros :refer [ensure with-compiler-env]]
                   [cljs.analyzer.macros :refer [no-warn]])
  (:require [cljs.pprint :refer [pprint]]
            [cljs.tagged-literals :as tags]
            [cljs.tools.reader :as r]
            [cljs.tools.reader.reader-types :refer [string-push-back-reader]]
            [cljs.analyzer :as ana]
            [cljs.compiler :as c]
            [cljs.env :as env]
            [cljs.reader :as edn]))

(def DEBUG false)

(set! *target* "nodejs")
(cljs.core/enable-console-print!)

(if DEBUG
  (prn "DEBUG start cljs-bootstrap.repl"))

;;(apply load-file ["./.cljs_node_repl/cljs/core$macros.js"])
(if (goog/isProvided_ "cljs.core$macros")
  true ;; Already loaded, probably single file build
  (apply load-file ["./.cljs_bootstrap/cljs/core$macros.js"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Synchronous line-at-a-time input
;;   probably UNIX specific

;;(def rl (js/require "readline-sync"))
(defn get-line [prompt]
  (let [fs (js/require "fs")
        _ (.write (.-stdout js/process) prompt)
        fd (.openSync fs "/dev/tty" "r")
        buf (js/Buffer. 1024)
        sz (.readSync fs fd buf 0 1024)]
    (if (= 0 sz)
      nil
      (.toString buf "utf8" 0 (- sz 1)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Compiler environment

(def cenv (env/default-compiler-env))

;; load edn namespace caches into compiler environment
(defn load-edn-caches [core-edn macros-edn]
  (swap! cenv assoc-in [::ana/namespaces 'cljs.core]
    (edn/read-string core-edn))
  (swap! cenv assoc-in [::ana/namespaces 'cljs.core$macros]
    (edn/read-string macros-edn)))

;; load namespace cache files
(defn load-edn-cache-files []
  (let [fs (js/require "fs")
        core-edn (.readFileSync fs "resources/cljs/core.cljs.cache.aot.edn" "utf8")
        macros-edn (.readFileSync fs ".cljs_bootstrap/cljs/core$macros.cljc.cache.edn" "utf8")]
    (load-edn-caches core-edn macros-edn)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main REPL loop
;;   - call load-edn-caches or load-edn-cache-files first to setup
;;     compiler environment

(defn read-eval-print-loop []
  (binding [ana/*cljs-ns* 'cljs-bootstrap.repl
            *ns* (create-ns 'cljs-bootstrap.repl)
            r/*data-readers* tags/*cljs-data-readers*]
    (with-compiler-env cenv
      (let [env (assoc (ana/empty-env) :context :expr
                                       :ns {:name 'cljs-bootstrap.repl}
                                       :def-emits-var true)]
        (loop []
          (when-let [line (get-line "cljs-bootstrap.repl> ")]
            (when DEBUG (prn "line:" line))
            (try
              (let [form (r/read-string line)
                    _ (when DEBUG (prn "form:" form))
                    ast (ana/analyze env form)
                    _ (when DEBUG (prn "ast:" ast))
                    js (with-out-str
                         (ensure
                          (c/emit ast)))
                    _ (when DEBUG (prn "js:" js))]
                (prn (js/eval js)))
              (catch js/Error e
                (.log js/console (.-stack e))))
            (recur)))))))
