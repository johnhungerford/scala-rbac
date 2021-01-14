#!/usr/bin/env bash

LOWER_CASE_MODULE=$(echo $1 | tr "[A-Z]" "[a-z]")

sbt "project rbac$(tr '[:lower:]' '[:upper:]' <<< ${LOWER_CASE_MODULE:0:1})${LOWER_CASE_MODULE:1}Example" assembly || exit 1

docker build --build-arg EXAMPLE_NAME=$LOWER_CASE_MODULE -t "${LOWER_CASE_MODULE}-example" .
