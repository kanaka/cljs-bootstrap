#!/bin/bash

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
deps="$(cat ${TOP_DIR}/${dep_file} | awk -F'"' '{print $2}' | egrep -v '^base.js$|core\$macros.js')"

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
##deps="bootstrap/nodejs.js base.js deps.js ${deps}"
##deps="base.js deps.js ${deps}"

echo "Patching base.js"
cp ${TOP_DIR}/base.js ${TOP_DIR}/base-node.js
patch ${TOP_DIR}/base-node.js ${BASE_PATCH}
#deps="bootstrap/nodejs.js base-node.js ${deps}"
deps="base-node.js ${deps}"

declare -A dep_map

echo "Creating ${TARGET}"
cat /dev/null > ${TARGET}

echo "Adding node bootstrap"
echo '
var fs        = require("fs");
var vm        = require("vm");
function nodeGlobalRequire(file) {
  var _module = global.module, _exports = global.exports;
  //var prev_goog = global.goog;
  //global.goog = goog;
  global.module = undefined;
  global.exports = undefined;
  vm.runInThisContext(fs.readFileSync(file), file);
  global.exports = _exports;
  global.module = _module;
  //global.goog = prev_goo;
}
var goog = {nodeGlobalRequire: nodeGlobalRequire};
global.goog = goog;
' >> ${TARGET}

#cat ${TOP_DIR}/bootstrap/nodejs.js \
#    | sed \
#        -e 's@^\(global.goog =.*$\)@//\1\nvar goog = {};@' \
#        -e 's@^\(nodeGlobalRequire.*base.js.*$\)@//\1\n@' \
#        >> ${TARGET}

cmd="java -jar ${COMPILER_JAR} --compilation_level WHITESPACE_ONLY --formatting PRETTY_PRINT"
for dep in ${deps}; do
    if [ -z "${dep_map[${dep}]}" ]; then
        dep_map[${dep}]=done
        echo "Adding: ${dep}"
        cmd="${cmd} --js ${dep}"
    fi
done

echo "Doing closure compilation"
cd ${TOP_DIR}
echo "${cmd} >> ${TARGET}"
${cmd} >> ${TARGET} 

echo "Patching up ${TARGET}"
#sed -i 's@\(goog.NODE_JS *= *\)false;@\1true;goog.nodeGlobalRequire=function(){console.log("NOPE!")};@' ${TARGET}
# TODO: Use `--define goog.NODE_JS=true` above if only it would work
sed -i 's@\(goog.NODE_JS *= *\)false;@\1true;@' ${TARGET}
sed -i 's@if(goog.isProvided_(name))throw @if(goog.isProvided_(name) \&\& false)throw@' ${TARGET}
#sed -i 's@if(goog.isProvided_(name))throw @Z@' ${TARGET}

echo "Adding call to read_eval_print_loop"
echo "cljs_bootstrap.repl.read_eval_print_loop();" >> ${TARGET}
