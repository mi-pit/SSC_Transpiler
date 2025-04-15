#!/bin/bash

ORIGINAL_C_CODE_FILE="$1".c
STRUCT_CODE_FILE="$1""--code.c"
HEADER_FILE="$1"--header.h

python3.10 "***REMOVED***MyC/Convertor.py" "$1" 2>/dev/null || exit 1
clang-format -i "$ORIGINAL_C_CODE_FILE"
clang-format -i "$STRUCT_CODE_FILE"
clang-format -i "$HEADER_FILE"

#[DEBUG] echo "$ORIGINAL_C_CODE_FILE" "$STRUCT_CODE_FILE"
gcc --std=c99 -Wpedantic -Wall -Wextra -Werror -o "${1%.*}" "$ORIGINAL_C_CODE_FILE" "$STRUCT_CODE_FILE"
