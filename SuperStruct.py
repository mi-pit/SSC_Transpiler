import re

from CLexer import CLexer


def get_text_separated(token):
    if not hasattr(token, "getChildren") or not token.getChildren():
        return token.getText()
    return " ".join([get_text_separated(child) for child in token.getChildren()])


class SuperStruct:
    def __init__(self, name, token_stream):
        self.name: str = name
        self.fields: list[str] = []
        self.methods = []
        self.token_stream = token_stream

    def replace_keywords_in_tokens(self, ctx, keyword_dict):
        start_idx, stop_idx = ctx.getSourceInterval()
        tokens = self.token_stream.getTokens(start_idx, stop_idx + 1)

        result = ""
        for i in range(len(tokens)):
            tok_text = tokens[i].text

            if tok_text in keyword_dict:
                result += keyword_dict.get(tok_text)
            else:
                result += tok_text

        return result

    def to_c_code(self) -> str:
        out_str = f"/* superstruct {self.name} */\n"
        out_str += f"struct {self.name} {{"
        for field in self.fields:
            # FIXME?!
            replaced = field.replace("superstruct", "struct")
            out_str += f"{replaced}"
        out_str += "};\n\n\n"

        for method_tuple in self.methods:
            specifiers, declarator, decl_list, compound_statement = method_tuple

            this_object_name = "local__" + self.name
            ss_struct_specifier_str = "struct " + self.name

            if specifiers:
                for spec in specifiers.declarationSpecifier():
                    spec_str = ""
                    if spec.typeSpecifier() and spec.typeSpecifier().superStructSpecifier():
                        ss_spec = spec.typeSpecifier().superStructSpecifier()
                        spec_str += "struct " + ss_spec.Identifier().getText()
                    else:
                        spec_str += get_text_separated(spec)
                    out_str += spec_str + " "

            if declarator.pointer():
                out_str += declarator.pointer().getText()

            assert declarator.directDeclarator()
            # original function name, params list
            direct_decl = declarator.directDeclarator()
            function_name = self.name + "__" + direct_decl.directDeclarator().getText()
            out_str += function_name + "(" + ss_struct_specifier_str + " *" + this_object_name
            if direct_decl.parameterTypeList():
                params_ls = direct_decl.parameterTypeList().parameterList().parameterDeclaration()

                for param in params_ls:
                    param_spec = [spec.getText() for spec in param.declarationSpecifiers().declarationSpecifier()]
                    out_str += ", " + " ".join(param_spec) + " " + param.declarator().getText()

            out_str += ") "

            if decl_list:
                # don't know what this is
                print(decl_list.getText())

            keyword_dict = {"this": this_object_name, "superstruct": "struct"}
            out_str += self.replace_keywords_in_tokens(compound_statement, keyword_dict)

            out_str += "\n\n"  # one empty line between

        return out_str
