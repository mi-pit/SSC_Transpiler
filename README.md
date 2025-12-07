# SuperStruct-C transpiler

## Notice

This project includes components from the [ANTLR parser generator](https://www.antlr.org),
licensed under the BSD 3-Clause License.

## Requirements

`clang-format`, java

## Usage

### SSC code

SSC is (supposed to be) a superset of C, with the added `superstructs`
(more features to come, if I get around to it).

SuperStructs are sort of a mid-way point between structs and classes;
they have fields and methods, but no inheritance.

Syntax is like regular C structs, except you can define methods (just like functions).
Method call outside a superstruct-method definition depends on whether variable is a pointer or not
(just like structs `.` for local, `->` for pointer).
Self-reference in a method is `this`, followed by `->` to access a field or call a method.

Methods may be declared `static` or `pure`:

- `pure` methods don't modify the SS in any way
- `static` functions behave like normal C functions
  – those aren't called on an SS, rather as `Classname::methodname(‹args›)`

#### Example code (other examples are in the `ssc-examples` directory):

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

Run `run.sh` or execute the `sscc.jar` with java directly.

`‹ssc file›.ssc` – source code for SuperStructC
