# cljs-bootstrap

This project uses bootstrapped ClojureScript to implement node.js and
web ClojureScript REPLs. The bootstrapped aspect means that the REPLs
run using only JavaScript files (i.e. without a JVM).

Note: this was originally a fork of
https://github.com/swannodette/cljs-bootstrap

This is a work in progress and many things work but there are still
bugs (see examples below).

## Just the REPL please ##

If you just want to the bootstrapped ClojureScript REPL, you try the
web version at [clojurescript.net](http://clojurescript.net) or you
can download a pre-built Node.js version and run it like this:

```
curl https://gist.githubusercontent.com/kanaka/b588dbb6d44174672cc2/raw/repl-node.js > clojurescript.js
chmod clojurescript.js
./clojurescript.js
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

* Run the REPL using a Node launch script:

```bash
node repl.js
```

* Optionally, you can now build/compile the sources into a single
  standalone JavaScript file `repl-node.js` (as per the gist above).

  * First you need to download and unzip the Google Closure compiler
    jar file:

```
wget http://dl.google.com/closure-compiler/compiler-latest.zip
unzip compiler-latest.zip
```

  * Now generate the standalone all-in-one REPL files for node.js
    (repl-node.js) and the web (repl-web.js):

```
./script/gen_single.sh
```

* Run the standalone all-in-one REPL:

```bash
./repl-node.js
```

## Dynamic namespace loading ##

To load/evaluate arbitrary libraries/namespaces you must first configure the
search path. For example, to load the "joel.core" namespace from
`src/joel/core.cljs`, you need to configure the loader like this:

```
cljs.user> (cljs-bootstrap.repl/set-load-cfg :lib-base-path "src/")
```

Now you can load it and use it like this:

```
cljs.user> (ns foo.bar (:require joel.core))
foo.bar> (joel.core/some-fn 1 2 3)
```

Macros are supported by bootstrapped ClojureScript but they still must live in
a separate file from regular ClojureScript code and are loaded using
:require-macros.



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
cljs.user> (defrecord Baz [b] IFoo (foo [this] (prn "some baz:" b)))
cljs.user/Baz
cljs.user> (foo (Baz. 5))
"some baz:" 5
nil
cljs.user> (ns foo.bar (:require [hello-world.core]) (:require-macros [hello-world.macros]))
"prn from inside hello-world.core"
"prn from inside hello-world.macros"
nil
cljs.user> (hello-world.core/mult-fn 2 3)
6
cljs.user> (hello-world.macros/mult-macro 7 8)
56
cljs.user> (doc map)
-------------------------

([f] [f coll] [f c1 c2] [f c1 c2 c3] [f c1 c2 c3 & colls])
...

```

* Try some things that do not work yet:

```clojure
cljs.user> (require ['hello-world.core])
/home/joelm/scratch/cljs-bootstrap/.cljs_bootstrap/cljs/core.cljs:8272
      x
      ^
Error: Doesn't support name: 
...

```


## License

Copyright © 2015 David Nolen, Rich Hickey, Joel Martin & Contributors

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
