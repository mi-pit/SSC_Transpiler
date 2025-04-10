#!/bin/bash

python3.10 "***REMOVED***MyC/Convertor.py" "$1"  || exit 1
clang-format -i "$1".c
clang-format -i "$1".h
