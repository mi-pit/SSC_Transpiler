# SuperStruct-C transpiler

---

## Notice

This project includes components from the [ANTLR parser generator](https://www.antlr.org),
licensed under the BSD 3-Clause License.

---

## Requirements

Java & a C compiler

---

---

# SSC code

---

## Features

SSC is (supposed to be) a superset of C, with the added superstructs (`object` keyword).
More features to come, if I get around to it.

### Superstructs

SuperStructs are sort of a mid-way point between structs and classes;
they have fields and methods, but no inheritance.

Syntax is like regular C structs, except you can define methods (just like functions).
Method call outside a superstruct-method definition depends on whether variable is a pointer or not
(just like structs `.` for local, `->` for pointer).
Self-reference in a method is `this`, followed by `->` to access a field or call a method.

Methods may be declared `meta`, `pure` or `private`:

- `pure` methods don't modify the SS in any way (is passed as `const`)
- `meta` (`static` in most other languages) functions behave like normal C functions, namespaced
  – those aren't called on an SS, rather as `SSName::methodname(‹args›)`
- `private` members (fields or methods) are not visible outside the superstruct's "namespace"

Methods may not be declared both pure and static, since static methods don't operate on a superstruct

One may get references to non-static member functions using the following syntax: `SSName::methodname` (no parens)

---

### Templates

Do you ever get tired of having to write function templates with preprocessor macros?

```
#define DEFINE_CMP_FUNCTION( TYPE )                                 \
    static int cmp_##TYPE( const void *p1, const void *p2 )         \
    {                                                               \
        TYPE val_1 = *( TYPE * ) p1;                                \
        TYPE val_2 = *( TYPE * ) p2;                                \
                                                                    \
        if ( val_1 > val_2 )                                        \
            return 1;                                               \
        if ( val_1 < val_2 )                                        \
            return -1;                                              \
        return 0;                                                   \
    }
```

And then having to define them manually AND having to keep track of which ones
have already been defined?

```
DEFINE_CMP_FUNCTION( char )
DEFINE_CMP_FUNCTION( int )
// ...
```

#### Syntax

Keyword `tmpl` followed by `<`, identifier list, `>`

For example:

```c
tmpl<T>
object Superstruct
{
    T item;
    // ...
};
```

Dispatches of a given template (like `object Superstruct<int>` or perhaps `Superstruct<int>::some_meta_method()`)
are emitted at compile time, resulting in something like the following code
(actual identifiers may -- and probably will -- differ).

```
struct __ssc_tmpl_Superstruct__int
{
    int item;
    // ...
};
```

---

### Lambda functions

// no captures yet

Syntax

`| [ parameters ] | --> return-type (optional: attributes, specifiers) { function-body }`

---

### Flag sets

Similar to enums, for easily declaring loads of flags
without having to assign them manually.

```
flagset ‹Identifier› {
    ‹Ident1›,
    ‹Ident2› [= ‹initializer›],
    ...
};
```

Values may be initialized or uninitialized.

If left uninitialized, they get assigned the lowest available power of two.

Values may not be initialized to any arbitrary value,
rather only to zero or a bitwise-or of previous values.
This means that the flag values may be aliased (`flag2 = flag1`) and combined
(`flag_compound = flag1 | flag2`)

The underlying type of the resulting enum is the smallest unsigned integer
which is able to hold all the values (requires <stdint.h>)


---

### Switch expressions

(inspired by Java)

Treated as expressions of the specified type (lambda syntax -- `-> type-name`)

Like `switch` statements, but may be called on strings

```
const char *string = "...";
const int i = swex ( string ) -> int {
    case "string" => 1;
    case "..."    => 0;
    
    default => {
        warn( "invalid input" );
        return -1;
    }
};
```

Branch results may be either an expression of the specified type, or compound statements.
Compound statements are treated as separate functions

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

## Relation to C++

This language is ***NOT*** a subset of C++ with a strange keyword for structs/classes.
This language is, as opposed to C++, fully backwards- AND forwards\*-compatible with plain C.

\* as in -gets transpiled to plain C

This includes (among other things):

- `void *` genericness
- references to non-static member functions (`SSName::methodname`)
- redefinition of member as different kind of symbol (you can have both a function and a field with the same identifier)
- VLAs
- the `embed` directive (not in C++ yet as of writing this)
- field designators specified in arbitrary order or not at all
- `enum` "integer-ness"

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

Table might not be accurate. Run `--help` to see all canonical options

| Short | Long                 | Description                                         |
|-------|----------------------|-----------------------------------------------------|
| `-h`  | `--help`             | print help and exit                                 |
| `-v`  | `--verbose`          | verbose -- print all stages                         |
| `-s`  | `--no-stop-on-error` | stop if transpilation of any file fails             |
| `-d`  | `--dir     ‹dir›`    | process all files in the given directory            |
| `-c`  | `--compile ‹bin›`    | compile the output of all given files into a binary |
|       | `--debug`            | print debug info                                    |
|       | `--debug!`           | print debug info about antlr parsing                |
| `--`  |                      | treat all following arguments as file names         |

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

object Adder {
    int x;
    private flagset AdderFlags flags;

    void add(int add) {
        this->x += add;
    }

    void inc() {
        ++this->x;
    }

    object Adder *get_own_address() {
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
    object Adder add = { 0 };
    // add.x == 0
    add.add( 2 );
    // add.x == 2

    object Adder *ptr = calloc(1, sizeof(object Adder));
    // assume non-null; ptr->x == 0
    ptr->inc();
    // ptr->x == 1

    typedef object Adder Adder;
    Adder *addp = add.get_own_address();
    (void) addp;
}
```
