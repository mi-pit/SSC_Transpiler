#!/bin/bash

python3.10 "/Users/macbook/Programming/CProjects/MyC/Convertor.py" "$1"  || exit 1
clang-format -i "$1".c
clang-format -i "$1".h
