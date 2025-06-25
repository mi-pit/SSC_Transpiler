import os.path
import re
import sys

from antlr4 import *

from ArgsProcessing import CommandLineArgs, process_argv
from SSCLexer import SSCLexer
from SSCParser import SSCParser
from Visitors import SuperCVisitor


def remove_static_functions(functions: list[str]) -> list[str]:
    picked_functions: list[str] = []
    for func in functions:
        if "static" not in func:
            picked_functions.append(func)

    return picked_functions


def get_include_string(file_name: str) -> str:
    return f'#include "{os.path.basename(file_name)}"\n'


def create_ss_files(header_file_name: str, methods_file_name: str, visitor: SuperCVisitor) -> None:
    with open(header_file_name, "w") as header_file:
        file_guard_token = "SSC__" + re.sub("[^a-zA-Z0-9]", "_", os.path.basename(header_file_name).upper())
        header_file.write(f"#ifndef {file_guard_token} /* guard */\n"
                          f"#define {file_guard_token}\n")

        header_file.write("\n".join(visitor.directives) + "\n/* ---- END OF DIRECTIVES ---- */\n\n\n")
        header_file.write("\n".join(visitor.functions) + "\n")

        if len(visitor.superstructs) != 0:
            with open(methods_file_name, "w") as c_file:
                c_file.write(get_include_string(header_file_name) + '\n\n')
                # All transformed super-structs (structs and methods)
                for ss in visitor.superstructs:
                    code, headers = ss.to_c_code()
                    c_file.write(code + "\n\n")
                    header_file.write("\n".join(headers) + "\n\n")

        header_file.write(f"\n\n#endif /* {file_guard_token} */\n")


def show_tokens_in_interval(token_stream, start_idx, stop_idx):
    """
    Prints all tokens between start_idx and stop_idx (inclusive).
    Useful for debugging.
    """
    tokens = token_stream.getTokens(start_idx, stop_idx)
    for token in tokens:
        print(f"{token.text}", end="")
    print()


def replace_method_calls(tokens, skip_indices: set[int], replacements: dict[tuple[int, int], str]) -> list[str]:
    transformed_code: list[str] = []
    n_skips = 0
    for i, token in enumerate(tokens):
        if n_skips > 0:
            n_skips -= 1
            continue

        if i in skip_indices or token.type == Token.EOF or token.text.startswith("#"):
            continue

        if token.text == "superstruct":
            transformed_code.append("struct")
            continue

        # Check for replacements (method calls transformed)
        for (start_idx, end_idx), new_call in replacements.items():
            if start_idx <= i <= end_idx:
                n_skips = end_idx - start_idx
                transformed_code.append(new_call)
                break
        else:
            # If no replacement was found, just add the original token text
            transformed_code.append(token.text)

    return transformed_code


def remove_filename_extention(filename: str) -> str:
    return filename[:filename.rfind(".")]


def main(argv: 'CommandLineArgs') -> None:
    superstructs = argv.structs
    for filename in argv.files:
        name_of_files: str = remove_filename_extention(filename)

        header_file_name: str = name_of_files + ".h"
        methods_file_name: str = name_of_files + "-ss.c"
        c_code_file_name: str = name_of_files + ".c"

        # print([header_file_name, methods_file_name, c_code_file_name])

        try:
            input_stream = FileStream(filename, encoding="UTF-8")
        except:
            try:
                input_stream = FileStream(filename)
            except:
                print(f"Could not open '{filename}'", file=sys.stderr)
                exit(1)
        lexer = SSCLexer(input_stream)
        token_stream = CommonTokenStream(lexer)
        parser = SSCParser(token_stream)
        tree = parser.compilationUnit()
        # print(tree.toStringTree(recog=parser))

        visitor = SuperCVisitor(token_stream, superstructs)

        visitor.visit(tree)

        token_stream.fill()
        tokens: list = token_stream.tokens

        # Flatten out the skip intervals into a set of token indices
        skip_indices: set[int] = set()
        for start, end in visitor.skip_intervals:
            skip_indices.update(list(range(start, end + 1)))

        visitor.functions = remove_static_functions(visitor.functions)
        transformed_code: list[str] = replace_method_calls(tokens, skip_indices, visitor.replacements)

        main_c_code = "".join(transformed_code)
        if main_c_code and not main_c_code.isspace():
            # main C
            with open(c_code_file_name, "w") as f:
                # header with all directives and structs
                f.write(get_include_string(header_file_name) + "\n")

                # Original non-superstruct code
                f.write(main_c_code + "\n")

        create_ss_files(header_file_name, methods_file_name, visitor)


if __name__ == '__main__':
    args = process_argv()
    # print(args)
    main(args)
