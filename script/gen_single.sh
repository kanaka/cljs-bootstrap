#!/bin/bash

set -e

TARGET=repl-all.js
TOP_DIR=.cljs_bootstrap/goog/
dep_file=../deps.js
COMPILER_JAR=${COMPILER_JAR:-compiler.jar}
BASE_PATCH=${BASE_PATCH:-script/base.patch}

# Canonicalize paths
TARGET=$(readlink -f ${TARGET})
TOP_DIR=$(readlink -f ${TOP_DIR})
if [ ! -f "${COMPILER_JAR}" ]; then
    echo "Could not locate ${COMPILER_JAR}"
    exit 1
fi
COMPILER_JAR=$(readlink -f ${COMPILER_JAR})


#goog_deps="$(cat ${TOP_DIR}/deps.js| awk -F"'" '{print $2}')"
goog_deps="
    object/object.js
    debug/error.js
    dom/nodetype.js
    string/string.js
    asserts/asserts.js
    array/array.js
    string/stringbuffer.js
    "
#deps="$(cat ${TOP_DIR}/${dep_file} | awk -F'"' '{print $2}' | egrep -v '^base.js$|core\$macros.js')"
deps="$(cat ${TOP_DIR}/${dep_file} | awk -F'"' '{print $2}' | egrep -v '^base.js$')"

# Google Closure deps if they actually exist
real_goog_deps=""
for gdep in ${goog_deps}; do
    if [ -f "${TOP_DIR}/${gdep}" ]; then
        real_goog_deps="${real_goog_deps} ${gdep}"
    else
        #echo "Skipping non-existent: ${dep}"
        true
    fi
done
deps="${real_goog_deps} ${deps}"


### Add Closure library node bootstrap and then base.js at beginning
echo "Patching base.js"
cp ${TOP_DIR}/base.js ${TOP_DIR}/base-node.js
patch ${TOP_DIR}/base-node.js ${BASE_PATCH}
#deps="bootstrap/nodejs.js base-node.js deps.js ../deps.js ${deps}"
#deps="../../node_modules/readline-sync/lib/readline-sync.js base-node.js deps.js ../deps.js ${deps}"
deps="base-node.js deps.js ../deps.js ${deps}"

declare -A dep_map

echo "Creating ${TARGET}"
cat /dev/null > ${TARGET}

echo "Adding node bootstrap"
echo '
var fs        = require("fs");
var vm        = require("vm");
function nodeGlobalRequire(file) {
  var _module = global.module, _exports = global.exports;
  global.module = undefined;
  global.exports = undefined;
  vm.runInThisContext(fs.readFileSync(file), file);
  global.exports = _exports;
  global.module = _module;
}
var goog = {nodeGlobalRequire: nodeGlobalRequire};
global.goog = goog;
' >> ${TARGET}

cmd="java -jar ${COMPILER_JAR} \
          --compilation_level WHITESPACE_ONLY \
          --formatting PRETTY_PRINT"
#          --process_common_js_modules \
#          --common_js_entry_module=.cljs_bootstrap/cljs_bootstrap/repl.js"
for dep in ${deps}; do
    if [ -z "${dep_map[${dep}]}" ]; then
        dep_map[${dep}]=done
        echo "Adding: ${dep}"
        cmd="${cmd} --js ${dep}"
    fi
done

# Grab these before we change directories
CORE_EDN="$(node -e 'console.log(require("util").inspect(require("fs").readFileSync("resources/cljs/core.cljs.cache.aot.edn", "utf-8")))')"
MACROS_EDN="$(node -e 'console.log(require("util").inspect(require("fs").readFileSync(".cljs_bootstrap/cljs/core\$macros.cljc.cache.edn", "utf-8")))')"

echo "Doing closure compilation"
cd ${TOP_DIR}
echo "${cmd} >> ${TARGET}"
${cmd} >> ${TARGET} 

echo "Patching up ${TARGET}"
# TODO: Use `--define goog.NODE_JS=true` above if only it would work
sed -i 's@\(goog.NODE_JS *= *\)false;@\1true;@' ${TARGET}

echo "Adding inline edn caches"
echo "
core_edn_cache = ${CORE_EDN};
macros_edn_cache = ${MACROS_EDN};
" >> ${TARGET}

echo "Adding calls to load_cache and read_eval_print_loop"
echo "cljs_bootstrap.repl.load_edn_caches(core_edn_cache, macros_edn_cache);" >> ${TARGET}
echo "cljs_bootstrap.repl.read_eval_print_loop();" >> ${TARGET}
