(ns cljs-bootstrap.repl
  (:require-macros [cljs.env.macros :refer [ensure with-compiler-env]]
                   [cljs.analyzer.macros :refer [no-warn]])
  (:require [cljs.pprint :refer [pprint]]
            [cljs.tagged-literals :as tags]
            [cljs.tools.reader :as r]
            [cljs.tools.reader.reader-types :refer [string-push-back-reader]]
            [cljs.analyzer :as ana]
            [cljs.compiler :as c]
            [cljs.env :as env]))

(def DEBUG false)

(set! *target* "nodejs")
(cljs.core/enable-console-print!)

(if DEBUG
  (prn "DEBUG start cljs-bootstrap.repl"))

;;(apply load-file ["./.cljs_node_repl/cljs/core$macros.js"])
(apply load-file ["./.cljs_bootstrap/cljs/core$macros.js"])

(def cenv (env/default-compiler-env))

;; TODO: get EOF/Ctrl-D working
(def rl (js/require "readline-sync"))

(defn read-eval-print-loop []
  (let [env (assoc (ana/empty-env) :context :expr)]
    (binding [ana/*cljs-ns* 'cljs-bootstrap.repl
              *ns* (create-ns 'cljs.analyzer)
              r/*data-readers* tags/*cljs-data-readers*]
      (with-compiler-env cenv
        (loop []
          (let [line (.question rl "cljs-bootstrap.repl> "
                                #js {:keepWhitespace true})
                _ (when DEBUG (prn "line:" line))
                form (r/read-string line)
                _ (when DEBUG (prn "form:" form))
                js (with-out-str
                     (ensure
                       (c/emit
                         (no-warn
                           (ana/analyze env form)))))
                _ (when DEBUG (prn "js:" js))]
                (println (js/eval js)))
              (recur))))))
