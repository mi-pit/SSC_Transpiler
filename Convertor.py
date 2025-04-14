import re
import sys
from antlr4 import *
from CLexer import CLexer
from CParser import CParser
from SuperStruct import SuperStruct
from Visitors import CBaseVisitor, SuperCVisitor

FILE_NAME: str = sys.argv[1]
HEADER_FILE_NAME: str = FILE_NAME + "--header.h"
METHODS_FILE_NAME: str = FILE_NAME + "--code.c"


def extract_preprocessor_directives(token_stream):
    directives: list[str] = []
    for token in token_stream.tokens:
        if token.text.startswith("#") and not token.text.startswith("#undef"):
            directives.append(token.text)
    return directives


def create_header_file(visitor, directives: list[str]):
    superstructs: list[SuperStruct] = visitor.superstructs
    static_headers = visitor.functions

    with open(METHODS_FILE_NAME, "w") as c_file, \
            open(HEADER_FILE_NAME, "w") as header_file:
        c_file.write(f'#include "{HEADER_FILE_NAME}"\n\n')

        file_guard_token = re.sub("[^a-zA-Z]", "_", HEADER_FILE_NAME.upper()) + "_H"
        header_file.write(f"#ifndef {file_guard_token} /* guard */\n"
                          f"#define {file_guard_token}\n")

        header_file.write("\n".join(directives) + "\n/* ---- END OF DIRECTIVES ---- */\n\n\n")

        header_file.write("\n".join(static_headers) + "\n")

        # All transformed super-structs (structs and methods)
        for ss in superstructs:
            code, ls = ss.to_c_code()
            c_file.write(code + "\n")
            header_file.write("\n".join(ls) + "\n")

        header_file.write(f"\n\n#endif /* {file_guard_token} */\n")


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
                transformed_code.append(new_call)
                replaced = True
                break
        if replaced:
            continue

        # If no replacement was found, just add the original token text
        transformed_code.append(token.text)

    return transformed_code


def main() -> None:
    try:
        input_stream = FileStream(FILE_NAME, encoding="UTF-8")
    except EncodingWarning:
        try:
            input_stream = FileStream(FILE_NAME)
        except EncodingWarning:
            raise EncodingWarning()
    lexer = CLexer(input_stream)
    token_stream = CommonTokenStream(lexer)
    parser = CParser(token_stream)
    tree = parser.compilationUnit()

    visitor = SuperCVisitor(token_stream)

    visitor.visit(tree)

    token_stream.fill()
    tokens: list = token_stream.tokens

    # Flatten out the skip intervals into a set of token indices
    skip_indices: set[int] = set()
    for start, end in visitor.skip_intervals:
        skip_indices.update(list(range(start, end + 1)))

    transformed_code: list[str] = replace_method_calls(tokens, skip_indices, visitor.replacements)

    # main C
    with open(FILE_NAME + ".c", "w") as f:
        # header with all directives and structs
        f.write(f"#include \"{HEADER_FILE_NAME}\"\n")

        # Original non-superstruct code
        f.write("".join(transformed_code) + "\n")

    directives: list[str] = extract_preprocessor_directives(token_stream)
    create_header_file(visitor, directives)


if __name__ == '__main__':
    main()
