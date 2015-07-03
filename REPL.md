# cljs-bootstrap REPL

This is fork of https://github.com/swannodette/cljs-bootstrap that
adds a very simple bootstrapped REPL. This means that it can run with
only JavaScirpt files (i.e. without a JVM). Also included is a Clojure
build script that compiles the ClojureScript compiler to JavaScript.

This is a work in progress and many things work but there are still
many bugs (see examples below).

## Just the REPL please ##

If you just want to the bootstrapped ClojureScript REPL, you try the
web version at
[clojurescript.net](http://clojurescript.net)
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
#'cljs-bootstrap.repl/x
cljs-bootstrap.repl> (def foo (fn [a b] (* a b)))
#'cljs-bootstrap.repl/foo
cljs-bootstrap.repl> (foo 6 7)
42
cljs-bootstrap.repl> (defn bar [a b] (* a b))
#'cljs-bootstrap.repl/bar
cljs-bootstrap.repl> (bar 7 8)
56
cljs-bootstrap.repl> (let* [x 7 y (+ 1 x)] y)
8
cljs-bootstrap.repl> (meta (with-meta [2 3 4] {:a 123}))
{:a 123}
cljs-bootstrap.repl> (let [[x y] [3 4] z (+ x y)] (* x y z))
84
cljs-bootstrap.repl> (and 1 2 3 4)
4
cljs-bootstrap.repl> (map #(.toUpperCase %) ["hello" "allcaps" "world"])
("HELLO" "ALLCAPS" "WORLD")
cljs-bootstrap.repl> (.toString (reify Object (toString [this] "hello")))
"hello"
cljs-bootstrap.repl> (defprotocol IFoo (foo [this]))
nil
cljs-bootstrap.repl> (foo (reify IFoo (foo [this] (prn "lots of foo"))))
"lots of foo"
nil
cljs-bootstrap.repl> (deftype Bar [] IFoo (foo [this] (prn "some bar too")))
cljs-bootstrap.repl/Bar
cljs-bootstrap.repl> (foo (Bar.))
"some bar too"
nil

```

* Try some things that do not work yet:

```clojure
cljs-bootstrap.repl> (defprotocol IFoo (foo [this]))
cljs-bootstrap.repl> (defrecord Baz [b] IFoo (foo [this] (prn "some baz:" b)))  ; CLJS-1321 & CLJS-1325
Error: Can't set! local var or non-mutable field
...

cljs-bootstrap.repl> 0  ; CLJS-1326
Error: Invalid number format [0]
...

cljs-bootstrap.repl> (load-file "simple.cljs")  ; Treated as JS file
#<SyntaxError: simple.cljs:1
(prn "here we are")
     ^^^^^^^^^^^^^
Unexpected string>
...

cljs-bootstrap.repl> (ns cljs-bootstrap.my-ns)  ; Does nothing
nil

```
