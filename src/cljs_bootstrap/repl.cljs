(ns cljs-bootstrap.repl
  (:require [cljs.js :as cljs]))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main REPL loop
;;   - before calling read-eval*, call init-repl to initialize the
;;     repl namespace

(defn read-eval-print [s cb]
  (cljs/eval-str cstate
                 s
                 'cljs.user
                 {:verbose DEBUG
                  :source-map true
                  :eval cljs/js-eval
                  :context :expr
                  :def-emits-var true}
                 (fn [{:keys [error value] :as res}]
                   (when DEBUG (prn :result-data res))
                   (if error
                     (cb false (str error
                                    "\n"
                                    (.. error -cause -stack)))
                     (cb true (pr-str value))))))

;; Node mode REPL
(defn read-eval-print-loop []
  (.start (js/require "repl")
          #js {:prompt "cljs.user> "
               :input (.-stdin js/process)
               :output (.-stdout js/process)
               :eval (fn [cmd ctx filename cb]
                       (read-eval-print cmd #(cb %2)))}))

