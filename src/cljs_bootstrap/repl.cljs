(ns cljs-bootstrap.repl
  (:require [cljs.js :as cljs]
            [cljs.stacktrace :as cljs-stack]))

(def DEBUG false)

(cljs.core/enable-console-print!)

(def cstate (cljs/empty-state))

(def native-eval #(throw "eval function not set"))

(defn get-native-eval []
  (if (= *target* "nodejs")
    (let [vm (js/require "vm")]
      (try
        (.install (js/require "source-map-support"))
        (catch :default _
          (println "Could not load source-map support")))
      (fn [{:keys [name source] :as res}]
        ;(.runInThisContext vm source (str (munge name) ".js"))
        (cljs/js-eval res)
        ))
    cljs/js-eval))

;; Does not fully work yet
;;(def ^:dynamic *lib-base-path* "src/")
(def ^:dynamic *lib-base-path* "src/")
(def ^:dynamic *lib-type-map*
  {'hello-world.core   :cljs
   'hello-world.macros :clj})

;; Setup source/require resolution
(defn set-load-cfg [& {:keys [lib-base-path lib-type-map]}]
  (when lib-base-path (set! *lib-base-path* lib-base-path))
  (when lib-type-map  (set! *lib-type-map*  lib-type-map))
  nil)

(defn native-load [{:keys [name macros]} cb]
  (if (contains? *lib-type-map* name)
    (let [path (str *lib-base-path* (cljs/ns->relpath name)
                    "." (cljs.core/name (get *lib-type-map* name)))]
      (if (= *target* "nodejs")
        ;; node: read file using fs module
        (let [fs (js/require "fs")]
          (.readFile fs path "utf-8"
            (fn [err src]
              (if-not err
                (cb {:lang :clj :source src})
                ;(cb (.error js/console err))
                (throw err)))))
        ;; browser: read file using XHR
        (let [url (str (.. js/window -location -origin) "/" path)
              req (doto (js/XMLHttpRequest.)
                    (.open "GET" url))]
          (set! (.-onreadystatechange req)
                (fn []
                  (when (= 4 (.-readyState req))
                    (if (= 200 (.-status req))
                      (let [src (.. req -responseText)]
                        (cb {:lang :clj :source src}))
                      (let [emsg (str "XHR load failed:" (.-status req))]
                        ;(.error js/console emsg)
                        ;(cb nil)
                        (throw (js/Error. emsg)))))))
          (.send req))))
    ;(cb (.error js/console (str "No *lib-type-map* entry for " name)))
    (throw (js/Error. (str "No *lib-type-map* entry for " name)))))


(defn init-repl [mode & load-opts]
  (set! *target* mode)
  (set! native-eval (get-native-eval))
  ;; Setup source/require resolution
  (apply set-load-cfg load-opts)
  ;; Create cljs.user
  ;; TODO: for some reason this is required when (ns) contains
  ;; a :require clause otherwise cljs.user is not setup properly
  (if (= *target* "nodejs")
    (set! (.. js/global -cljs -user) #js {})
    (set! (.. js/window -cljs -user) #js {}))
  (cljs/eval-str cstate
                 "(ns cljs.user (:require [cljs-bootstrap.repl]))"
                 ;"(ns cljs.user)"
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
                  :eval native-eval
                  :load native-load
                  :context :expr
                  :def-emits-var true}
                 (fn [{:keys [error value] :as res}]
                   (try
                     (if error
                       (cb false (.. error -cause))
                       (cb true (pr-str value)))
                     (catch :default exc
                       (cb false exc))))))

;; Node mode REPL
(defn read-eval-print-loop []
  (.start (js/require "repl")
          #js {:prompt "cljs.user> "
               :input (.-stdin js/process)
               :output (.-stdout js/process)
               :eval (fn [cmd ctx filename cb]
                       (read-eval-print cmd #(cb %2)))}))

