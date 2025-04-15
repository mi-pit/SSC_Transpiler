# SuperStruct-C transpiler

## requires

`clang-format`, `python` with the `antlr4` package

```bash
pip install antlr4
```

## Usage

compiling `example.ssc` in SSC_code

```bash
# in git repo directory
cd SSC_code
../transpiler.sh example.ssc
./example
```

## Notice

This project includes components from the [ANTLR parser generator](https://www.antlr.org),
licensed under the BSD 3-Clause License.
