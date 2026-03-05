# SuperStruct-C transpiler

---

## Notice

This project includes components from the [ANTLR parser generator](https://www.antlr.org),
licensed under the BSD 3-Clause License.

---

## Requirements

java, a C compiler, clang-format (optional)

---

---

# SSC code

---

## Features

SSC is (supposed to be) a superset of C, with the added `superstructs`
(more features to come, if I get around to it).

### Superstructs

SuperStructs are sort of a mid-way point between structs and classes;
they have fields and methods, but no inheritance.

Syntax is like regular C structs, except you can define methods (just like functions).
Method call outside a superstruct-method definition depends on whether variable is a pointer or not
(just like structs `.` for local, `->` for pointer).
Self-reference in a method is `this`, followed by `->` to access a field or call a method.

Methods may be declared `static`, `pure` or `private`:

- `pure` methods don't modify the SS in any way (is passed as `const`)
- `static` functions behave like normal C functions, namespaced
  – those aren't called on an SS, rather as `SSName::methodname(‹args›)`
- `private` members (fields or methods) are not visible outside the superstruct's "namespace"

Methods may not be declared both pure and static, since static methods don't operate on a superstruct

One may get references to non-static member functions using the following syntax: `SSName::methodname` (no parens)

---

### Flag sets

Similar to enums, for easily declaring loads of flags
without having to assign them manually.

```
flagset ‹Identifier› {
    ‹value1›,
    ‹value2› [= ‹initializer›],
    ...
};
```

Values may be initialized or uninitialized.

If left uninitialized, they get assigned the lowest available power of two.

Values may not be initialized to any arbitrary value,
rather only to a bitwise-or of previous values.
This means that the flag values may be aliased (`flag2 = flag1`) and combined
(`flag_compound = flag1 | flag2`)

The underlying type of the resulting enum is the smallest unsigned integer
which is able to hold all the values (requires <stdint.h>)

---

### Ternary operators

You may now use `then` instead of `?`, `else` instead of `:` and `if` at the start, in a conditional expression.

`cond ? expr1 : expr2`
can be written as
`if cond then expr1 else expr2`

---

---

## Caveats

Since this language is just a hobby project of one idiot, there will be a lot of bugs.

Hopefully, I have made the error messages at least ***somewhat*** helpful.

Code in the messages in the "Replacing superstruct references" stage is a little mangled,
but should be readable and the output `.c` file doesn't get deleted, so (worst case) look there.

As a guide (not a rule):

- `Syntax` and `Preprocessor` exceptions => user (writer of the ssc code) made a mistake
- `Antlr parser` exceptions => only one entity knows what the problem is and I can't talk to them since I'm an atheist.
  (problem could be in the java code, antlr grammar, ssc code being transpiled or any number of other reasons)
- Any other exception is entirely on me

## C++

This language is ***NOT*** a subset of C++ with a strange keyword for structs/classes.
This language, as opposed to C++, supports

- `void *` genericness
- references to non-static member functions (`SSName::methodname`)
- redefinition of member as different kind of symbol (you can have both a function and a field with the same identifier)
- VLAs
- the `embed` directive (not in C++ yet as of writing this)
- field designators specified in arbitrary order or not at all
- `enum` "integer-ness"

and more!

---

## Formatting

```yml
AttributeMacros:
  - private
  - pure
```

---

## Usage

### Compilation

Run `run.sh` or execute the `sscc.jar` with java directly.

`‹ssc file›.ssc` – source code for SuperStructC

#### Options

| Name | Long name            | Description                                         |
|------|----------------------|-----------------------------------------------------|
| `-h` | `--help`             | print help and exit                                 |
| `-v` | `--verbose`          | verbose -- print all stages                         |
| `-s` | `--no-stop-on-error` | stop if transpilation of any file fails             |
| `-d` | `--dir     ‹dir›`    | process all files in the given directory            |
| `-c` | `--compile ‹bin›`    | compile the output of all given files into a binary |
|      | `--debug`            | print debug info                                    |
|      | `--debug!`           | print debug info about antlr parsing                |
| `--` |                      | treat all following arguments as file names         |

#### Example

`./run.sh main.ssc --compile main`

---

## Example code

```SSC
#include <ssclib/headers/core/types.h>
#include <stdlib.h>

flagset AdderFlags {
    LIE,
    TELL_TRUTH,
    SELF_DESTRUCT,
};

superstruct Adder {
    int x;
    private flagset AdderFlags flags;

    void add(int add) {
        this->x += add;
    }

    void inc() {
        ++this->x;
    }

    superstruct Adder *get_own_address() {
        switch (this->flags) {
            case LIE:
                return nullptr;
            case TELL_TRUTH:
                return this;
            case SELF_DESTRUCT:
                abort();

            default:
                return this;
        };
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

    typedef superstruct Adder Adder;
    Adder *addp = add.get_own_address();
    (void) addp;
}
```
