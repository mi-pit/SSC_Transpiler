#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

VERBOSE=true

function parse_args() {
  structs=""
  positional_args=()

  while [[ $# -gt 0 ]]; do
    case "$1" in
    --structs=*)
      structs="${1#*=}"
      shift
      ;;
    --help)
      echo "Imagine a help message here [TODO]"
      exit 0
      ;;
    --silent)
      VERBOSE=false
      shift
      ;;
    -*)
      echo "Unknown option: '$1'" >&2
      shift
      ;;
    *)
      positional_args+=("$1")
      shift
      ;;
    esac
  done
}

function echo-if-not-silent() {
  if $VERBOSE; then
    echo $@
  fi
}

parse_args "$@"
# echo "Structs: $structs"
# echo "Positional: ${positional_args[@]}"

echo-if-not-silent "> Converting '$@' ..."

if ! python3.10 "$SCRIPT_DIR/Convertor.py" $structs ${positional_args[@]}; then
  echo -e "error: Conversion failed" >&2
  exit 1
else
  echo-if-not-silent ">> Conversion success"
fi

function format_and_test_file_if_exists() {
  local file="$1"

  if [[ -f "$file" ]]; then
    echo-if-not-silent "> Formatting $file with clang-format..."
    clang-format -i "$file"

    echo-if-not-silent "> Checking syntax of $file with gcc..."
    gcc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file"

    if [[ $? -eq 0 ]]; then
      echo-if-not-silent ">> Syntax check passed for $file"
    else
      echo-if-not-silent ">> Syntax check failed for $file"
    fi
  fi
}

# do this for every file
for FILENAME in ${positional_args[@]}; do
  NAME_NO_EXTENTION=${FILENAME%.ssc}

  NON_SS_CODE_FILE="$NAME_NO_EXTENTION.c"
  HEADER_FILE="$NAME_NO_EXTENTION.h"
  STRUCT_CODE_FILE="$NAME_NO_EXTENTION-ss.c"

  # .c file may not exist
  format_and_test_file_if_exists "$NON_SS_CODE_FILE"
  format_and_test_file_if_exists "$STRUCT_CODE_FILE"

  clang-format -i "$HEADER_FILE" # must exist
  gcc -Wall -Wextra -pedantic -fsyntax-only "$HEADER_FILE"
done

exit 0
