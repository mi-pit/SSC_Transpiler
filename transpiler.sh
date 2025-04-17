#!/bin/bash

COMPILE_RESULT=true
while getopts ":ch" o; do
  case "${o}" in
  c)
    echo "-c selected: No gcc"
    COMPILE_RESULT=false
    ;;
  h)
    echo -e "-h\t display this message"
    echo -e "-c\t don't compile resulting .c files"
    exit 0
    ;;
  *)
    echo Invalid option \'"$OPTARG"\'
    exit 1
    ;;
  esac
done
# echo "shift $((OPTIND - 1))"
shift $((OPTIND - 1))

if ! test -f "$1"; then
  echo "File '$1' does not exist."
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ORIGINAL_C_CODE_FILE="$1".c
STRUCT_CODE_FILE="$1""--code.c"
HEADER_FILE="$1"--header.h

if ! python3.10 "$SCRIPT_DIR/Convertor.py" "$1"; then
  exit 1
fi

clang-format -i "$ORIGINAL_C_CODE_FILE"
clang-format -i "$STRUCT_CODE_FILE"
clang-format -i "$HEADER_FILE"

if $COMPILE_RESULT; then
  BINARY_NAME="${1%.*}"
  gcc --std=c99 -Wpedantic -Wall -Wextra -Werror -o "$BINARY_NAME" "$ORIGINAL_C_CODE_FILE" "$STRUCT_CODE_FILE" || exit 1
  echo "Compiled binary: '$BINARY_NAME'"
fi
