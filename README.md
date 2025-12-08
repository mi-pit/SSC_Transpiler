# SuperStruct-C transpiler

---

## Notice

This project includes components from the [ANTLR parser generator](https://www.antlr.org),
licensed under the BSD 3-Clause License.

---

## Requirements

java, a C compiler, clang-format (optional)

---

## SSC code

### Description

SSC is (supposed to be) a superset of C, with the added `superstructs`
(more features to come, if I get around to it).

SuperStructs are sort of a mid-way point between structs and classes;
they have fields and methods, but no inheritance.

Syntax is like regular C structs, except you can define methods (just like functions).
Method call outside a superstruct-method definition depends on whether variable is a pointer or not
(just like structs `.` for local, `->` for pointer).
Self-reference in a method is `this`, followed by `->` to access a field or call a method.

Methods may be declared `static` or `pure`:

- `pure` methods don't modify the SS in any way (is passed as `const`)
- `static` functions behave like normal C functions, namespaced
  – those aren't called on an SS, rather as `Classname::methodname(‹args›)`

### Caveats

Since this language is just a hobby project of one idiot, there will be a lot of bugs.

Hopefully, I have made the error messages at least ***somewhat*** helpful.

As a guide (not a rule):

- `Syntax` exceptions => user (writer of the ssc code) made a mistake
- `Antlr parser` exceptions => only one entity knows what the problem is and I can't talk to them since I'm an atheist.
  (problem could be in the java code, antlr grammar, ssc code being transpiled or any number of other reasons)
- Any other exception is entirely on me

### Conventions?

#### Headers

similar to c++, for class-like headers (header with one superstruct and nothing else) I use `.ssch`,
otherwise headers can just be `.h`, since it's compatible with plain C

#### Formatting

---

## Usage

### Compilation

Run `run.sh` or execute the `sscc.jar` with java directly.

`‹ssc file›.ssc` – source code for SuperStructC

#### Options

| Name              | Description                                         |
|-------------------|-----------------------------------------------------|
| `-v`              | verbose -- print all stages                         |
| `-s`              | stop if transpilation of any file fails             |
| `--debug`         | print debug info                                    |
| `--compile ‹arg›` | compile the output of all given files into a binary |

#### Example

`./run.sh main.ssc --compile main`

---

## Example code

(other examples are in the `ssc-examples` directory)

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
