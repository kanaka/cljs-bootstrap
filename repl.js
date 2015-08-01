#!/usr/bin/env node
require('./.cljs_bootstrap/goog/bootstrap/nodejs.js')
require('./.cljs_bootstrap/deps.js')
goog.require('cljs_bootstrap.node');

cljs_bootstrap.node._main.apply({}, process.argv);
