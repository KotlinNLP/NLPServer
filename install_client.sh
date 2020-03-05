#! /bin/sh

set -e

OUTPUT_DIR=target/api_client
VERSION="0.1.0"

rm -rf ${OUTPUT_DIR}
mkdir -p ${OUTPUT_DIR}

export JAVA_POST_PROCESS_FILE="/usr/local/bin/clang-format -i"

openapi-generator generate \
 -i openapi.yaml \
 -g java \
 -o ${OUTPUT_DIR} \
 --group-id com.kotlinnlp \
 --artifact-id api \
 --artifact-version ${VERSION} \
 --api-package com.kotlinnlp.api \
 --model-package com.kotlinnlp.api.model

cd ${OUTPUT_DIR}
mvn clean install
