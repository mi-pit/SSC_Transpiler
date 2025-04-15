#!/bin/bash

if ! test -f "$1"; then
  echo "File '$1' does not exist."
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ORIGINAL_C_CODE_FILE="$1".c
STRUCT_CODE_FILE="$1""--code.c"
HEADER_FILE="$1"--header.h

if ! python3.10 "$SCRIPT_DIR/Convertor.py" "$1";
then
  exit 1
fi

clang-format -i "$ORIGINAL_C_CODE_FILE"
clang-format -i "$STRUCT_CODE_FILE"
clang-format -i "$HEADER_FILE"

#[DEBUG] echo "$ORIGINAL_C_CODE_FILE" "$STRUCT_CODE_FILE"
gcc --std=c99 -Wpedantic -Wall -Wextra -Werror -o "${1%.*}" "$ORIGINAL_C_CODE_FILE" "$STRUCT_CODE_FILE"
