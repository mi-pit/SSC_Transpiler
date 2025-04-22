#!/bin/bash

if ! test -f "$1"; then
  echo "File '$1' does not exist." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

NON_SS_CODE_FILE="$1.c"
STRUCT_CODE_FILE="$1--code.c"
HEADER_FILE="$1--header.h"

if ! python3.10 "$SCRIPT_DIR/Convertor.py" "$1" "$2"; then
  echo "Conversion failed" >&2
  exit 1
fi

# $1.c may not exist
if ! test -f "$NON_SS_CODE_FILE"; then
  NON_SS_CODE_FILE=""
else
  clang-format -i "$NON_SS_CODE_FILE"
fi
clang-format -i "$STRUCT_CODE_FILE"
clang-format -i "$HEADER_FILE"

exit 0
