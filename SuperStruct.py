import re

from CLexer import CLexer


def get_text_separated(token):
    if not hasattr(token, "getChildren") or not token.getChildren():
        return token.getText()
    return " ".join([get_text_separated(child) for child in token.getChildren()])


def get_keyword_replaced(word: str, keyword_dict: dict[str, str]) -> str:
    return keyword_dict.get(word, word)


class SuperStruct:
    def __init__(self, name, token_stream):
        self.name: str = name
        self.fields: list[str] = []
        self.methods = []
        self.token_stream = token_stream

    def replace_keywords_in_tokens(self, ctx, keyword_dict):
        """
        Replaces keywords in keyword_dict::keys with their corresponding values.
        Also replaces method calls
        example: obj->methodname() => Classname_methodname(obj)
        """
        start_idx, stop_idx = ctx.getSourceInterval()
        tokens = self.token_stream.getTokens(start_idx, stop_idx + 1)

        result = ""
        skips = set()
        for i in range(len(tokens)):
            if i in skips:
                continue
            tok_text: str = tokens[i].text

            method_call_next = i + 3 < len(tokens) and \
                               re.search("(\\.|->)", tokens[i + 1].text) and \
                               tokens[i + 3].text == "("

            if method_call_next:
                next_tok, res = self.replace_method_calls(tokens, i, keyword_dict)
                skips.update(list(range(i, next_tok)))
                result += res
                continue

            result += get_keyword_replaced(tok_text, keyword_dict)

        return result

    def replace_method_calls(self, tokens, i, keyword_dict):
        """
        Replaces a method call like obj.method(...) or obj->method(...)
        with Class__method(local__obj, ...)
        Supports nested calls.
        Returns: (next_index_to_continue_from, rewritten_string)
        """
        object_name = get_keyword_replaced(tokens[i].text, keyword_dict)
        operator = tokens[i + 1].text
        method_name = tokens[i + 2].text
        open_paren = tokens[i + 3].text
        assert open_paren == "("

        class_name = self.name
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
            new_call += f", {args_str}"
        new_call += ")"

        return j, new_call

    def to_c_code(self) -> tuple[str, list[str]]:
        header_code: list[str] = []

        out_str = f"\n/* superstruct {self.name} */\n"
        out_str += f"struct {self.name} {{"
        for field in self.fields:
            # FIXME?
            replaced = field.replace("superstruct", "struct")
            out_str += f"{replaced}"
        out_str += "};\n\n"

        header_code.append(f"struct {self.name};")

        for method_tuple in self.methods:
            curr_method_str = ""
            curr_method_is_private = False
            specifiers, declarator, decl_list, compound_statement = method_tuple

            this_object_name = "local__" + self.name
            ss_struct_specifier_str = "struct " + self.name

            if specifiers:
                for spec in specifiers.declarationSpecifier():
                    spec_str = ""
                    if spec.typeSpecifier() and spec.typeSpecifier().superStructSpecifier():
                        ss_spec = spec.typeSpecifier().superStructSpecifier()
                        spec_str += "struct " + ss_spec.Identifier().getText()
                    elif get_text_separated(spec) == "private":
                        curr_method_is_private = True
                    else:
                        spec_str += get_text_separated(spec)
                    curr_method_str += spec_str + " "

            if declarator.pointer():
                curr_method_str += get_text_separated(declarator.pointer())

            assert declarator.directDeclarator()
            # original function name, params list
            direct_decl = declarator.directDeclarator()
            function_name = self.name + "__" + direct_decl.directDeclarator().getText()
            curr_method_str += function_name + "(" + ss_struct_specifier_str + " *" + this_object_name
            if direct_decl.parameterTypeList():
                curr_method_str += ", "
                curr_method_str += get_text_separated(direct_decl.parameterTypeList()).replace("superstruct", "struct")

            curr_method_str += ")"

            if decl_list:
                # don't know what this is
                print(decl_list.getText())

            if not curr_method_is_private:
                header_code.append(curr_method_str + ";")

            out_str += curr_method_str
            keyword_dict = {"this": this_object_name, "superstruct": "struct"}
            out_str += self.replace_keywords_in_tokens(compound_statement, keyword_dict)

            out_str += "\n\n"  # one empty line between

        return out_str, header_code
