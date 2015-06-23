require('./.cljs_bootstrap/goog/bootstrap/nodejs.js')
require('./.cljs_bootstrap/cljs_bootstrap_deps.js')

// set cljs.core._STAR_print_fn_STAR_
goog.require('cljs.core');
//cljs.core.enable_console_print_BANG_();
goog.require('cljs_bootstrap.repl');
//require('./.cljs_bootstrap/cljs_bootstrap/core.js')

cljs_bootstrap.repl.read_eval_print_loop();
