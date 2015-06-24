require('./.cljs_bootstrap/goog/bootstrap/nodejs.js')
require('./.cljs_bootstrap/cljs_bootstrap_deps.js')

goog.require('cljs_bootstrap.repl');
cljs_bootstrap.repl.read_eval_print_loop();
