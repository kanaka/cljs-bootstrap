#!/bin/bash

set -e

VERBOSE=${VERBOSE:-}
TARGET_WEB=${TARGET_NODE:-repl-web.js}
TARGET_NODE=${TARGET_NODE:-repl-node.js}
TOP_DIR=${TOP_DIR:-.cljs_bootstrap/goog/}
DEP_FILE=${DEP_FILE:-../deps.js}
COMPILER_JAR=${COMPILER_JAR:-compiler.jar}
BASE_PATCH=${BASE_PATCH:-script/base.patch}
CORE_EDN=${CORE_EDN:-.cljs_bootstrap/cljs/core.cljs.cache.aot.edn}

# Canonicalize paths
TARGET_WEB=$(readlink -f ${TARGET_WEB})
TARGET_NODE=$(readlink -f ${TARGET_NODE})
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


echo "Adding patched base.js and deps files to the beginning of deps"
cp ${TOP_DIR}/base.js ${TOP_DIR}/base-node.js
if [ "${VERBOSE}" ]; then
    patch ${TOP_DIR}/base-node.js ${BASE_PATCH}
else
    patch -s ${TOP_DIR}/base-node.js ${BASE_PATCH}
fi
deps="base-node.js deps.js ${DEP_FILE} ${deps}"

# Start with empty file
cat /dev/null > ${TARGET_WEB}

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
[ "${VERBOSE}" ] && echo "${cmd} >> ${TARGET_WEB}"
${cmd} >> ${TARGET_WEB} 

echo "Adding initalization calls to ${TARGET_WEB}"
echo "
if (goog.NODE_JS) {
    cljs_bootstrap.node._main.apply({}, process.argv);
} else {
    cljs_bootstrap.core.init_repl('default');
}" >> ${TARGET_WEB}

# Node specific changes

echo "Copying ${TARGET_WEB} to ${TARGET_NODE}"
echo "#!/usr/bin/env node" > ${TARGET_NODE}
cat ${TARGET_WEB} >> ${TARGET_NODE}
chmod +x ${TARGET_NODE}

echo "Enabling NODE_JS setting in ${TARGET_NODE}"
# TODO: Use `--define goog.NODE_JS=true` above if only it would work
sed -i 's@\(goog.NODE_JS *= *\)false;@\1true;@' ${TARGET_NODE}

echo "Finished creating ${TARGET_WEB} and ${TARGET_NODE}"
