import re
import sys
from antlr4 import *
from CLexer import CLexer
from CParser import CParser
from SuperStruct import SuperStruct
from Visitors import CBaseVisitor, SuperCVisitor

# TODO:
#   superstruct Classname variable;
#     => struct Classname variable;
#   variable->function( args )
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
#   ss return type conversion
#   "forward" declaration

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


def main():
    input_stream = FileStream(FILE_NAME, encoding="UTF-8")
    lexer = CLexer(input_stream)
    token_stream = CommonTokenStream(lexer)
    parser = CParser(token_stream)
    tree = parser.compilationUnit()

    visitor = SuperCVisitor(token_stream)

    # Traverse the tree and collect all superstructs
    for ext_decl in tree.translationUnit().externalDeclaration():
        visitor.visit(ext_decl)

    token_stream.fill()
    tokens = token_stream.tokens

    # Flatten out the skip intervals into a set of token indices
    skip_indices = set()
    for start, end in visitor.skip_intervals:
        skip_indices.update(range(start, end + 1))

    non_ss_code = ""
    for i, token in enumerate(tokens):
        if i not in skip_indices and token.type != Token.EOF and not token.text.startswith("#"):
            if token.text == "superstruct":
                non_ss_code += "struct"
            else:
                non_ss_code += token.text

    # main C
    with open(FILE_NAME + ".c", "w") as f:
        # header with all directives and structs
        f.write(f"#include \"{FILE_NAME}.h\"\n")

        # Original non-superstruct code
        f.write(non_ss_code + "\n")

    directives: list[str] = extract_preprocessor_directives(token_stream)
    create_header_file(visitor.superstructs, directives)


if __name__ == '__main__':
    main()
