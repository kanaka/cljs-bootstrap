(ns cljs-bootstrap.repl
  (:require [cljs.analyzer :as ana]
            [cljs.reader :as edn]
            [cljs.js :as cljs]))

(def DEBUG false)

(cljs.core/enable-console-print!)

;;(def vm (js/require "vm"))

(def cstate (cljs/empty-state))

(defn init-repl [mode]
  (set! *target* mode)
  ;; Setup the initial repl namespace
  (cljs/eval-str cstate
                 "(ns cljs.user)"
                 'cljs.user
                 {:eval cljs/js-eval}
                 (fn [res] nil)))

;; load edn namespace caches into compiler environment
(defn load-edn-caches [core-edn]
  (swap! cstate assoc-in [::ana/namespaces 'cljs.core]
    (edn/read-string core-edn)))

;; load namespace cache files
(defn load-edn-cache-files []
  (let [fs (js/require "fs")
        core-edn (.readFileSync fs ".cljs_bootstrap/cljs/core.cljs.cache.aot.edn" "utf8")]
    (load-edn-caches core-edn)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main REPL loop
;;   - before calling read-eval*, call init-repl to initialize the
;;     repl namespace and then load-edn-caches or load-edn-cache-files
;;     to setup compiler environment

(defn read-eval-print [s cb]
  (cljs/eval-str cstate
                 s
                 'cljs.user
                 {:verbose DEBUG
                  :eval cljs/js-eval
                  :context :expr
                  :def-emits-var true}
                 (fn [res]
                   (when DEBUG (prn :result-data res))
                   (if (contains? res :value)
                     (cb true (pr-str (:value res)))
                     (cb false (:error res))))))

;; Node mode REPL
(defn read-eval-print-loop []
  (.start (js/require "repl")
          #js {:prompt "cljs.user> "
               :input (.-stdin js/process)
               :output (.-stdout js/process)
               :eval (fn [cmd ctx filename cb]
                       (read-eval-print cmd #(if %1
                                               (cb %2)
                                               (cb (pr-str %2)))))}))

