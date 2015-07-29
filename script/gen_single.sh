#!/bin/bash

set -e

VERBOSE=${VERBOSE:-}
WEB=${WEB:-}
[ "${WEB}" ] && deftarget=repl-web.js || deftarget=repl-node.js
TARGET=${TARGET:-${deftarget}}
TOP_DIR=${TOP_DIR:-.cljs_bootstrap/goog/}
DEP_FILE=${DEP_FILE:-../deps.js}
COMPILER_JAR=${COMPILER_JAR:-compiler.jar}
BASE_PATCH=${BASE_PATCH:-script/base.patch}
CORE_EDN=${CORE_EDN:-.cljs_bootstrap/cljs/core.cljs.cache.aot.edn}

# Canonicalize paths
TARGET=$(readlink -f ${TARGET})
TOP_DIR=$(readlink -f ${TOP_DIR})
if [ ! -f "${COMPILER_JAR}" ]; then
    echo "Could not locate ${COMPILER_JAR}"
    exit 1
fi
COMPILER_JAR=$(readlink -f ${COMPILER_JAR})
CORE_EDN=$(readlink -f ${CORE_EDN})


echo "Reading main deps file ${TOP_DIR}/${DEP_FILE}"
deps="$(cat ${TOP_DIR}/${DEP_FILE} | awk -F'"' '{print $2}' | egrep -v '^base.js$')"


echo "Adding Google Closure Library deps ${TOP_DIR}/deps.js"
goog_deps="$(cat ${TOP_DIR}/deps.js| awk -F"'" '{print $2}' | egrep -v '^base.js$')"
# Make sure certain deps occur first
goog_deps="debug/error.js
           string/string.js
           labs/useragent/util.js
           labs/useragent/browser.js
           labs/useragent/engine.js
           labs/useragent/platform.js
           useragent/useragent.js
           ${goog_deps}"

# Only add google closure deps that actually appear in the tree
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


if [ "${WEB}" ]; then
    echo "Adding base.js and deps files to the beginning of deps"
    deps="base.js deps.js ${DEP_FILE} ${deps}"
else
    echo "Adding patched base.js and deps files to the beginning of deps"
    cp ${TOP_DIR}/base.js ${TOP_DIR}/base-node.js
    if [ "${VERBOSE}" ]; then
        patch ${TOP_DIR}/base-node.js ${BASE_PATCH}
    else
        patch -s ${TOP_DIR}/base-node.js ${BASE_PATCH}
    fi
    deps="base-node.js deps.js ${DEP_FILE} ${deps}"

    echo "Adding Closure node bootstrap to the beginning of deps"
    deps="bootstrap/nodejs.js base-node.js deps.js ../deps.js ${deps}"
fi

# Start with empty file
cat /dev/null > ${TARGET}

cmd="java -jar ${COMPILER_JAR} \
          --compilation_level WHITESPACE_ONLY \
          --formatting PRETTY_PRINT"

# make sure each dep only is included once on the command line
declare -A dep_map
for dep in ${deps}; do
    if [ -z "${dep_map[${dep}]}" ]; then
        dep_map[${dep}]=done
        [ "${VERBOSE}" ] && echo "Adding: ${dep}"
        cmd="${cmd} --js ${dep}"
    fi
done

echo "Doing closure compilation"
cd ${TOP_DIR}
[ "${VERBOSE}" ] && echo "${cmd} >> ${TARGET}"
${cmd} >> ${TARGET} 

if [ -z "${WEB}" ]; then
    echo "Enabling NODE_JS setting in ${TARGET}"
    # TODO: Use `--define goog.NODE_JS=true` above if only it would work
    sed -i 's@\(goog.NODE_JS *= *\)false;@\1true;@' ${TARGET}
fi

echo "Adding calls to init_repl and read_eval_print_loop"
if [ "${WEB}" ]; then
  echo "cljs_bootstrap.repl.init_repl('default');" >> ${TARGET}
else
  echo "cljs_bootstrap.repl.init_repl('nodejs');" >> ${TARGET}
  echo "cljs_bootstrap.repl.read_eval_print_loop();" >> ${TARGET}
fi

echo "Finished: ${TARGET}"
