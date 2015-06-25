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
(apply load-file ["./.cljs_bootstrap/cljs/core$macros.js"])

(def cenv (env/default-compiler-env))

;;;;;;;;;;;;;;
(def fs (js/require "fs"))

;; load cache files

(def core-edn (.readFileSync fs "resources/cljs/core.cljs.cache.aot.edn" "utf8"))

(goog/isString core-edn)

(swap! cenv assoc-in [::ana/namespaces 'cljs.core]
  (edn/read-string core-edn))

(def macros-edn (.readFileSync fs ".cljs_bootstrap/cljs/core$macros.cljc.cache.edn" "utf8"))

(goog/isString macros-edn)

(swap! cenv assoc-in [::ana/namespaces 'cljs.core$macros]
  (edn/read-string macros-edn))
;;;;;;;;;;;;;;


;; TODO: get EOF/Ctrl-D working
(def rl (js/require "readline-sync"))

(defn read-eval-print-loop []
  (binding [ana/*cljs-ns* 'cljs-bootstrap.repl
            *ns* (create-ns 'cljs-bootstrap.repl)
            r/*data-readers* tags/*cljs-data-readers*]
    (with-compiler-env cenv
      (let [env (assoc (ana/empty-env) :context :expr
                                       :ns {:name 'cljs-bootstrap.repl})]
        (loop []
          (try
            (let [line (.question rl "cljs-bootstrap.repl> "
                                  #js {:keepWhitespace true})
                  _ (when DEBUG (prn "line:" line))
                  form (r/read-string line)
                  _ (when DEBUG (prn "form:" form))
                  ast (no-warn (ana/analyze env form))
                  _ (when DEBUG (prn "ast:" ast))
                  js (with-out-str
                       (ensure
                        (c/emit ast)))
                  _ (when DEBUG (prn "js:" js))]
              (println (js/eval js)))
            (catch js/Error e
              (.log js/console (.-stack e))))
          (recur))))))
