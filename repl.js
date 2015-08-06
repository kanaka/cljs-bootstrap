#!/usr/bin/env node
require('./.cljs_bootstrap/goog/bootstrap/nodejs.js')
require('./.cljs_bootstrap/deps.js')
goog.require('cljs_bootstrap.node');

var cache_path ='.cljs_bootstrap/cljs/core.cljs.cache.aot.json';
var json = require('fs').readFileSync(cache_path, 'utf-8');
cljs_bootstrap.core.load_core_analysis_cache(json);
cljs_bootstrap.node._main.apply({}, process.argv);
