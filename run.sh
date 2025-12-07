#!/bin/bash

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

java -jar "$BASE_DIR/sscc.jar" "$@"
