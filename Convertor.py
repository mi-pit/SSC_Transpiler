import re
import sys
from antlr4 import *
from CLexer import CLexer
from CParser import CParser
from SuperStruct import SuperStruct
from Visitors import CBaseVisitor, SuperCVisitor

# TODO:
#   [IN .h FILE] variable->function( args )
#     => Classname__function( &variable, args )
#
#   static void methodname() { ... }
#     => [non-static] void Classname_methodname() { ... }
#   Classname.methodname()
#     => Classname__methodname()
#
#   Maybe?:
#     new Classname() || new_Classname()
#
# FIXME:
#   "forward" declaration
#       maybe function headers in a master file


FILE_NAME: str = sys.argv[1]


def extract_preprocessor_directives(token_stream):
    directives: list[str] = []
    for token in token_stream.tokens:
        if token.text.startswith("#") and not token.text.startswith("#undef"):
            directives.append(token.text)
    return directives


def create_header_file(superstructs: list[SuperStruct], directives: list[str]):
    with open(FILE_NAME + ".h", "w") as f:
        file_guard_token = re.sub("[^a-zA-Z]", "_", FILE_NAME.upper()) + "_H"
        f.write(f"#ifndef {file_guard_token} /* guard */\n"
                f"#define {file_guard_token}\n")

        f.write("\n".join(directives))
        f.write("\n/* ---- END OF DIRECTIVES ---- */\n\n\n")

        # All transformed super-structs (structs and methods)
        for ss in superstructs:
            f.write(ss.to_c_code() + "\n")

        f.write(f"#endif /* {file_guard_token} */\n")


def apply_replacements(tokens, replacements):
    output = ""
    i = 0
    replacement_map = {i: (start, stop, text) for i, (start, stop, text) in enumerate(replacements)}

    while i < len(tokens):
        # Check if token index is start of a replacement
        for _, (start, stop, text) in replacement_map.items():
            if tokens[i].tokenIndex == start:
                output += text
                # Skip tokens from start to stop
                i += (stop - start + 1)
                break
        else:
            output += tokens[i].text
            i += 1

    return output


def replace_method_calls(tokens, skip_indices: set[int], replacements):
    transformed_code = []
    n_skips = 0
    for i, token in enumerate(tokens):
        skip = False
        if n_skips > 0:
            n_skips -= 1
            skip = True
        if skip:
            continue

        if i in skip_indices or token.type == Token.EOF or token.text.startswith("#"):
            continue

        if token.text == "superstruct":
            transformed_code.append("struct")
            continue

        # Check for replacements (method calls transformed)
        replaced = False
        for start_idx, end_idx, new_call in replacements:
            if start_idx <= i <= end_idx:
                n_skips = end_idx - start_idx
                # print(f"replacing '{token.text}' @ {i} + {n_skips} for {new_call}")
                transformed_code.append(new_call)
                replaced = True
                break
        if replaced:
            continue

        # If no replacement was found, just add the original token text
        transformed_code.append(token.text)

    return transformed_code


def main():
    input_stream = FileStream(FILE_NAME, encoding="UTF-8")
    lexer = CLexer(input_stream)
    token_stream = CommonTokenStream(lexer)
    parser = CParser(token_stream)
    tree = parser.compilationUnit()

    visitor = SuperCVisitor(token_stream)

    visitor.visit(tree)

    token_stream.fill()
    tokens = token_stream.tokens

    # replaced_calls = apply_replacements(tokens, visitor.replacements)
    # print(replaced_calls)

    # Flatten out the skip intervals into a set of token indices
    skip_indices: set[int] = set()
    for start, end in visitor.skip_intervals:
        skip_indices.update(range(start, end + 1))

    transformed_code = replace_method_calls(tokens, skip_indices, visitor.replacements)

    # main C
    with open(FILE_NAME + ".c", "w") as f:
        # header with all directives and structs
        f.write(f"#include \"{FILE_NAME}.h\"\n")

        # Original non-superstruct code
        f.write("".join(transformed_code) + "\n")

    directives: list[str] = extract_preprocessor_directives(token_stream)
    create_header_file(visitor.superstructs, directives)


if __name__ == '__main__':
    main()
