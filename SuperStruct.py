import re

from SSCLexer import SSCLexer


def get_text_separated(token):
    if not hasattr(token, "getChildren") or not token.getChildren():
        return token.getText()
    return " ".join([get_text_separated(child) for child in token.getChildren()])


def get_keyword_replaced(word: str, keyword_dict: dict[str, str]) -> str:
    return keyword_dict.get(word, word)


class SuperStruct:
    def __init__(self, name, token_stream):
        self.token_stream = token_stream

        self.name: str = name
        self.fields: list[str] = []
        self.is_private: bool = False

        self.methods: list[tuple] = []

        self.static_methods: set[str] = set()

    def replace_tokens(self, ctx, keyword_dict: dict[str, str]) -> str:
        """
        Replaces keywords in keyword_dict::keys with their corresponding values.
        Also replaces method calls
        example: obj->methodname() => Classname_methodname(obj)
        """
        start_idx, stop_idx = ctx.getSourceInterval()
        tokens = self.token_stream.getTokens(start_idx, stop_idx + 1)

        result: str = ""
        skips: set[int] = set()
        for i in range(len(tokens)):
            if i in skips:
                continue
            tok_text: str = tokens[i].text

            method_call_next = i + 3 < len(tokens) and \
                               tokens[i + 1].text in (".", "->") and \
                               tokens[i + 3].text == "("

            if method_call_next:
                next_tok, res = self.replace_method_calls(tokens, i, keyword_dict)
                skips.update(range(i, next_tok))
                result += res
                continue

            result += get_keyword_replaced(tok_text, keyword_dict)

        return result

    def replace_method_calls(self, tokens, i: int, keyword_dict: dict[str, str]):
        """
        Replaces a method call like obj.method(...) or obj->method(...)
        with Class__method(local__obj, ...)
        Supports nested calls.
        Returns: (next_index_to_continue_from, rewritten_string)
        """
        object_name = get_keyword_replaced(tokens[i].text, keyword_dict)  # not necessarily 'this'
        operator = tokens[i + 1].text
        method_name = tokens[i + 2].text
        open_paren = tokens[i + 3].text
        assert open_paren == "("

        class_name = self.name
        if tokens[i].text == self.name:
            new_call = f"{class_name}__{method_name}("
        else:
            local_obj = f"&{object_name}" if operator == "." else object_name
            new_call = f"{class_name}__{method_name}({local_obj}"

        # recursively handle nested method calls
        args_tokens = []
        j = i + 4
        paren_count = 1

        while j < len(tokens) and paren_count > 0:
            tok = tokens[j]

            if tok.text == "(":
                paren_count += 1
            elif tok.text == ")":
                paren_count -= 1

            # Check for method call start in nested args
            if (j + 3 < len(tokens) and
                    re.search(r"(\.|->)", tokens[j + 1].text) and
                    tokens[j + 3].text == "(" and
                    paren_count > 0):
                # Recursively replace
                nested_end, nested_str = self.replace_method_calls(tokens, j, keyword_dict)
                args_tokens.append(nested_str)
                j = nested_end
                continue

            if paren_count > 0:
                args_tokens.append(get_keyword_replaced(tok.text, keyword_dict))

            j += 1

        args_str = "".join(args_tokens).strip()
        if args_str:
            if tokens[i].text != self.name:
                new_call += ", "
            new_call += f"{args_str}"
        new_call += ")"

        return j, new_call

    def to_c_code(self) -> tuple[str, list[str]]:
        header_code: list[str] = []

        struct_definition = f"\n/* superstruct {self.name} */\n"
        struct_definition += f"struct {self.name} {{"
        for field in self.fields:
            replaced = field.replace("superstruct", "struct")
            struct_definition += f"{replaced}"
        struct_definition += "};\n"

        if self.is_private:
            header_code.append(f"struct {self.name};")
            out_str = struct_definition
        else:
            header_code.append(struct_definition)
            out_str = ""

        for method_tuple in self.methods:
            curr_method_str = ""
            curr_method_is_private = False
            curr_method_is_static = False
            curr_method_is_pure = False
            specifiers, declarator, decl_list, compound_statement = method_tuple

            this_object_name = "local__" + self.name
            ss_struct_specifier_str = "struct " + self.name

            if specifiers:
                for spec in specifiers.declarationSpecifier():
                    spec_str = ""
                    spec_text = get_text_separated(spec)
                    if spec.typeSpecifier() and spec.typeSpecifier().superStructSpecifier():
                        ss_spec = spec.typeSpecifier().superStructSpecifier()
                        spec_str += "struct " + ss_spec.Identifier().getText()
                    elif spec_text == "private":
                        curr_method_is_private = True
                        curr_method_str += "static"
                    elif spec_text == "static":
                        curr_method_is_static = True  # don't paste
                    elif spec_text == "pure":
                        curr_method_is_pure = True
                    else:
                        spec_str += spec_text
                    curr_method_str += spec_str + " "

            if declarator.pointer():
                curr_method_str += "*"

            assert declarator.directDeclarator()
            # original function name, params list
            direct_decl = declarator.directDeclarator()
            function_name = self.name + "__" + direct_decl.directDeclarator().getText()
            curr_method_str += function_name + "("
            if curr_method_is_static:
                self.static_methods.add(function_name)
            elif curr_method_is_pure:
                curr_method_str += "const " + ss_struct_specifier_str + "*" + this_object_name
            else:
                curr_method_str += ss_struct_specifier_str + "*" + this_object_name

            if direct_decl.parameterTypeList():
                if not curr_method_is_static:
                    curr_method_str += ", "
                curr_method_str += get_text_separated(direct_decl.parameterTypeList()).replace("superstruct", "struct")

            curr_method_str += ")"

            if decl_list:
                # don't know what this is
                print("ATTENTION: found method decl_list", decl_list.getText())

            if not curr_method_is_private:
                header_code.append(curr_method_str + ";")

            out_str += curr_method_str
            keyword_dict = {"this": this_object_name, "superstruct": "struct"}
            out_str += self.replace_tokens(compound_statement, keyword_dict)

            out_str += "\n\n"  # one empty line between

        return out_str, header_code
