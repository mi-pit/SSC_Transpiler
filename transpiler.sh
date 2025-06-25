#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

VERBOSE=true

function show_single_help_line() {
  echo -e "    $1\n        $2"
}

function show_help_message() {
  show_single_help_line "Usage" "./transpiler.sh [files|options]"
  show_single_help_line "--help" "Show this message"
  show_single_help_line "--structs='...'" "Pre-define superstruct names (needed for recognition of foreign ss's)"
  show_single_help_line "--silent" "Don't yap about everything that happens (still shows errors)"
}

if [[ $# -eq 0 ]]; then
  show_help_message
  exit 1
fi

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
      show_help_message
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

if ! python3 "$SCRIPT_DIR/Convertor.py" $structs ${positional_args[@]}; then
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
