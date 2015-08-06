(ns cljs-bootstrap.node
  (:require [cljs-bootstrap.core :as bootstrap]))

(cljs.core/enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main REPL loop
;;   - before calling read-eval*, call init-repl to initialize the
;;     repl namespace

;; Node mode REPL
(defn read-eval-print-loop []
  (let [node-readline (js/require "readline")
        rl (.createInterface node-readline
                             #js {:input (.-stdin js/process)
                                  :output (.-stdout js/process)})]
    (doto rl
      (.setPrompt (bootstrap/get-prompt))
      (.on "line"
          (fn [cmd]
            (bootstrap/read-eval-print cmd
                             (fn [res data]
                               (if res
                                 (println data)
                                 (bootstrap/print-error data))
                               (.setPrompt rl (bootstrap/get-prompt))
                               (.prompt rl)))))
      (.prompt))))


(defn -main [& args]
  (bootstrap/init-repl "nodejs")
  (if (> (count args) 2)
    (let [path (nth args 2)
          fs (js/require "fs")]
      (.readFile fs path "utf-8"
        (fn [err src]
          (when err
            (throw (js/Error. (str "Could not read" path))))
          (bootstrap/read-eval-print (str "(do " src ")")
                           (fn [res data]
                             (when-not res
                               (bootstrap/print-error data)
                               (.exit js/process 1)))))))
    (read-eval-print-loop)))

(set! *main-cli-fn* -main)

