#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

function parse_args() {
  structs=""
  positional_args=()

  while [[ $# -gt 0 ]]; do
    case "$1" in
    --structs=*)
      structs="${1#*=}"
      shift
      ;;
    -*)
      echo "Unknown option: $1" >&2
      shift
      ;;
    *)
      positional_args+=("$1")
      shift
      ;;
    esac
  done
}

parse_args "$@"
# echo "Structs: $structs"
# echo "Positional: ${positional_args[@]}"

echo -n "> Converting '$@' ..."

if ! python3.10 "$SCRIPT_DIR/Convertor.py" "$@"; then
  echo -e "\nerror: Conversion failed" >&2
  exit 1
else
  echo " done"
fi

function format_and_test_file_if_exists() {
  local file="$1"

  if [[ -f "$file" ]]; then
    echo "> Formatting $file with clang-format..."
    clang-format -i "$file"

    echo "> Checking syntax of $file with gcc..."
    gcc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file"

    if [[ $? -eq 0 ]]; then
      echo ">> Syntax check passed for $file"
    else
      echo ">> Syntax check failed for $file"
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
