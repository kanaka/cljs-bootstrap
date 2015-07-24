# cljs-bootstrap REPL

This is fork of https://github.com/swannodette/cljs-bootstrap that
adds a very simple bootstrapped REPL. This means that it can run with
only JavaScirpt files (i.e. without a JVM). Also included is a Clojure
build script that compiles the ClojureScript compiler to JavaScript.

This is a work in progress and many things work but there are still
many bugs (see examples below).

## Just the REPL please ##

If you just want to the bootstrapped ClojureScript REPL, you try the
web version at [clojurescript.net](http://clojurescript.net) or you
can download a pre-built Node.js version and run it like this:

```
wget https://gist.githubusercontent.com/kanaka/b588dbb6d44174672cc2/raw/90718328795e21b18b6828f91fd69b7a3da9f05b/repl-all.js
node repl-all.js
```

## Build the REPL ##

To build the bootstrapped REPL, follow these steps:

* First, check out the repositories:

```bash
git clone https://github.com/clojure/clojurescript
git clone --branch cljs-bootstrap https://github.com/swannodette/tools.reader
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

* Optionally, you can now build/compile the sources into a single
  standalone JavaScript file `repl-all.js` (as per the gist above):

```
./script/gen_single.sh
```

* Run the REPL using a Node launch script:

```bash
node repl.js
```

## Examples ##


* Try some code that works:

```clojure
cljs.user> :foo
:foo
cljs.user> 123
123
cljs.user> (+ 2 3)
5
cljs.user> (second [2 3 4])
3
cljs.user> ( (fn [a b] (* a b)) 3 4)
12
cljs.user> ( #(* %1 %2) 4 5)
20
cljs.user> ( #(* %1 %2) 4 5)
20
cljs.user> (do (prn :foo) (prn :bar) :baz)
:foo
:bar
:baz
cljs.user> (def x 3)
#'cljs.user/x
cljs.user> (def foo (fn [a b] (* a b)))
#'cljs.user/foo
cljs.user> (foo 6 7)
42
cljs.user> (defn bar [a b] (* a b))
#'cljs.user/bar
cljs.user> (bar 7 8)
56
cljs.user> (let* [x 7 y (+ 1 x)] y)
8
cljs.user> (meta (with-meta [2 3 4] {:a 123}))
{:a 123}
cljs.user> (let [[x y] [3 4] z (+ x y)] (* x y z))
84
cljs.user> (and 1 2 3 4)
4
cljs.user> (map #(.toUpperCase %) ["hello" "allcaps" "world"])
("HELLO" "ALLCAPS" "WORLD")
cljs.user> (.toString (reify Object (toString [this] "hello")))
"hello"
cljs.user> (defprotocol IFoo (foo [this]))
nil
cljs.user> (foo (reify IFoo (foo [this] (prn "lots of foo"))))
"lots of foo"
nil
cljs.user> (deftype Bar [] IFoo (foo [this] (prn "some bar too")))
cljs.user/Bar
cljs.user> (foo (Bar.))
"some bar too"
nil
cljs.user> (defrecord Baz [b] IFoo (foo [this] (prn "some baz:" b)))  ; works but warnings
WARNING: No such namespace: core, could not locate core.cljs, core.cljc, or Closure namespace ""
WARNING: Use of undeclared Var core/list
cljs.user/Baz
cljs.user> (foo (Baz. 5))
"some baz:" 5
nil
```

* Try some things that do not work yet:

```clojure
cljs.user> (load-file "simple.cljs")  ; Treated as JS file
#<SyntaxError: simple.cljs:1
(prn "here we are")
     ^^^^^^^^^^^^^
Unexpected string>
...

cljs.user> (ns cljs-bootstrap.my-ns)  ; Does nothing
nil

```
