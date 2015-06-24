# cljs-bootstrap REPL

This is fork of https://github.com/swannodette/cljs-bootstrap that
adds a very simple bootstrapped REPL. This means that it can run with
only JavaScirpt files (i.e. without a JVM). Also included is a Clojure
build script that compiles the ClojureScript compiler to JavaScript.

This is a work in progress and many things do not work yet. For
example, `(first [3 4 5])` works but `(def x 3)` does not yet work.

Here is the step-by-step process to get a very simple REPL going:

* First, check out the repositories:

```bash
git clone https://github.com/clojure/clojurescript
git clone https://github.com/swannodette/tools.reader
git clone https://github.com/kanaka/cljs-bootstrap
```

* Build and install the regular ClojureScript compiler and patched
  reader:

```bash
cd clojurescript
time ./script/build
# Note the version of ClojureScript that is built.
cd ..

cd tools.reader
lein install
cd ..
```

* Update the cljs-bootstrap `project.def` with the version of
  ClojureScript that was built above and then install Node/npm
  dependencies (source-map-support and readline-sync):

```bash
cd cljs-bootstrap
vi project.def # update to correct ClojureScript version
lein npm install
```

* Bootstrap build the ClojureScript compiler and simple REPL (this
  compiles to JavaScript in the `.cljs_bootstrap/` directory):

```bash
time lein run -m clojure.main script/build.clj
```

* Run the REPL using a Node launch script:

```bash
node repl.js
```

* Try some code that works:

```clojure
cljs-bootstrap.repl> :foo
:foo
cljs-bootstrap.repl> 123
123
cljs-bootstrap.repl> (+ 2 3)
5
cljs-bootstrap.repl> (second [2 3 4])
3
cljs-bootstrap.repl> ( (fn [a b] (* a b)) 3 4)
12
cljs-bootstrap.repl> ( #(* %1 %2) 4 5)
20
cljs-bootstrap.repl> ( #(* %1 %2) 4 5)
20
cljs-bootstrap.repl> (do (prn :foo) (prn :bar) :baz)
:foo
:bar
:baz
cljs-bootstrap.repl> (def x 3)
3
cljs-bootstrap.repl> (def foo (fn [a b] (* a b)))
#<function cljs_bootstrap$repl$foo(a,b){
return (a * b);
}>
cljs-bootstrap.repl> (foo 6 7)
42
cljs-bootstrap.repl> (let* [x 7 y (+ 1 x)] y)
8
```

* Try some things that do not work:

```clojure
cljs-bootstrap.repl> (defn bar [a b] (* a b))
Error
...
cljs-bootstrap.repl> (load-file "simple.cljs")  ; Treated as JS file
simple.cljs:1
(prn "here we are")
     ^^^^^^^^^^^^^
SyntaxError: Unexpected string
...

cljs-bootstrap.repl> (ns my-ns) ;; Does nothing
nil

cljs-bootstrap.repl> (and 1 2 3 4)
(let* nil)

cljs-bootstrap.repl> (let [x 2] x)
TypeError: Cannot read property 'call' of undefined
...

cljs-bootstrap.repl> (let [x 7] x)
TypeError: Cannot read property 'call' of undefined
...


```
