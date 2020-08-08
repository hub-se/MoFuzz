#!/bin/bash

# Check arguments
if [ $# -lt 2 ]; then
  echo "Usage: $0 TEST_CLASS TEST_METHOD" >&1
  exit 1
fi

# Figure out script absolute path
pushd `dirname $0` > /dev/null
SCRIPT_DIR=`pwd`
popd > /dev/null

#CURRENT_DATE=`date +%b%d_%H%M`
TEST_CLASS="$1"
TEST_METHOD="$2"

TEST_CLASS_QUALIFIED_NAME="de.hub.mse.emf.serge.benchmarks.$TEST_CLASS"
OUT_DIR="evaluation/results/mofuzz-cgf-cpeo/${TEST_CLASS}_${TEST_METHOD}"

FILE_BENCHMARKS="build/mofuzz-benchmarks.jar"

# Extract benchmark jar
if test -f "$FILE_BENCHMARKS"; then    
    echo "Extracting benchmark jar..."
    cd build/
    jar -xf mofuzz-benchmarks.jar
    #rm emf-generator-benchmarks.jar
    cd ..
else
     echo "No benchmark jar to extract..."
fi

REMOTE_DEBUG=""
#REMOTE_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044"

# Run test
echo "Running experiments..."
java \
  -Djanala.conf="${SCRIPT_DIR}/janala.conf" \
  ${REMOTE_DEBUG} \
  -jar mofuzz/target/mofuzz-0.0.1-SNAPSHOT-modelfuzzer-cli.jar \
	$(scripts/classpath.sh) $TEST_CLASS_QUALIFIED_NAME $TEST_METHOD -o $OUT_DIR

