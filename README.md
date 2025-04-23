# SuperStruct-C transpiler

## Notice

This project includes components from the [ANTLR parser generator](https://www.antlr.org),
licensed under the BSD 3-Clause License.

## Requirements

`clang-format`, `python` with the `antlr4` package

```bash
pip install antlr4-python3-runtime
```

## Usage

### SSC code

SSC is (supposed to be) a superset of C, with the added `superstructs`
(more features to come, if I get around to it).

SuperStructs are sort of a mid-way point between structs and classes;
they have fields and methods, but no inheritance.

Syntax is like regular C structs, except you can define methods (just like functions).
Method call outside a superstruct-method definition depends on whether variable is a pointer or not
(just like structs `.` for local, `->` for pointer).
Self-reference in a method is `this`, followed by `->` to access a field or call a method

Example code (other examples are in the SSC_code directory):

```SSC
superstruct Adder {
    int x;
    
    void add(int add) {
        this->x += add;
    }
    
    void inc() {
        ++this->x;
    }
    
    superstruct Adder *get_own_address() {
        return this;
    }
};

int main(void) {
    superstruct Adder add = { 0 };
    // add.x == 0
    add.add( 2 );
    // add.x == 2
    
    superstruct Adder *ptr = calloc(1, sizeof(superstruct Adder));
    // assume non-null; ptr->x == 0
    ptr->inc();
    // ptr->x == 1
}
```

### Compilation

`transpiler.sh` generates three files:

`‹ssc file›.ssc` – source code for SuperStructC

`"‹scc file›.c"` – this file contains all regular C code (except directives)

`"‹ssc file›-ss.c"` – this file contains all superstruct methods and C-structs

`"‹ssc file›.h"` – this file contains all directives and function prototypes

both .c files contain (as the first line) `#include "‹ssc file›.h"`

#### Example:

```bash
# in git repo directory
./transpiler.sh SSC_code/example.ssc
cat SSC_code/example.c SSC_code/example-ss.c SSC_code/example.h
```
