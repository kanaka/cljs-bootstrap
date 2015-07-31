#!/usr/bin/env node
require('./.cljs_bootstrap/goog/bootstrap/nodejs.js')
require('./.cljs_bootstrap/deps.js')
goog.require('cljs_bootstrap.repl');

cljs_bootstrap.repl._main.apply({}, process.argv);
