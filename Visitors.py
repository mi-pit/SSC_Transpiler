from CBaseVisitor import CBaseVisitor
from CLexer import CLexer
from SuperStruct import SuperStruct, get_text_separated


class SuperCVisitor(CBaseVisitor):
    def __init__(self, tokens):
        self.token_stream = tokens
        self.superstructs: list[SuperStruct] = []
        self.skip_intervals = []
        self.superstruct_names: set[str] = set()
        self.var_types: dict = {}

    def lookup_variable_type(self, varname):
        return self.var_types.get(varname, None)

    def get_original_text(self, ctx):
        start, stop = ctx.getSourceInterval()
        return self.token_stream.getText(start, stop)

    def show_in_context(self, token):
        if isinstance(token, int):
            tok = self.token_stream.get(token)
        else:
            tok = token

        # Get the input text
        text = self.token_stream.tokenSource.inputStream.strdata
        lines = text.splitlines()

        # Token line number is 1-based
        line_num = tok.line
        start_line = max(0, line_num - 3)
        end_line = min(len(lines), line_num + 2)

        print(f"--- Context around line {line_num} ---")
        for i in range(start_line, end_line):
            prefix = ">> " if (i + 1) == line_num else "   "
            print(f"{prefix}{i + 1:4}: {lines[i]}")
        print("--- End context ---\n")

    def visitDeclaration(self, ctx):
        decl_specs = get_text_separated(ctx.declarationSpecifiers())
        if "superstruct" in decl_specs:
            struct_type = decl_specs.replace("superstruct", "struct").strip()

            for init_decl in ctx.initDeclaratorList().initDeclarator():
                var_name = init_decl.declarator().getText().split('=')[0].strip()  # fixme
                self.var_types[var_name] = struct_type

    def visitPostfixExpression(self, ctx):
        if ctx.getChildCount() >= 4:
            obj_expr = get_text_separated(ctx.getChild(0))
            operator = get_text_separated(ctx.getChild(1))
            method_name = get_text_separated(ctx.getChild(2))
            arg_exprs = get_text_separated(ctx.getChild(3))

            if operator in [".", "->"]:
                print(f"\n✅ Found method call: {obj_expr}{operator}{method_name}{arg_exprs}")
                self.show_in_context(ctx.start)
                # You need to know the object type here; assume a lookup function
                obj_type = self.lookup_variable_type(obj_expr)
                if obj_type and obj_type in self.superstruct_names:
                    new_func = f"{obj_type}__{method_name}"

                    if operator == ".":
                        obj_expr = f"&{obj_expr}"  # Pass address of object

                    new_call = f"{new_func}({obj_expr}"
                    if arg_exprs != "()":
                        new_call += ", " + arg_exprs[1:-1]  # remove parens
                    new_call += ")"

                    print(f"Transformed call: {ctx.getText()} → {new_call}")

                    # Optional: replace in token stream if doing string replacement
                    return new_call

        return self.visitChildren(ctx)

    def visitSuperStructSpecifier(self, ctx):
        name = ctx.Identifier().getText()
        ss = SuperStruct(name, self.token_stream)

        # print(ctx.getText())

        if not ctx.superStructBody():
            # not an ss definition, but a declaration; TODO
            return

        # Track this superstruct's token interval to exclude from raw C output
        self.skip_intervals.append(ctx.getSourceInterval())

        for member_ctx in ctx.superStructBody().superStructMember():
            decl = member_ctx.declaration()
            func = member_ctx.functionDefinition()

            if decl:
                field_text = self.get_original_text(member_ctx.declaration())
                ss.fields.append(field_text)
            elif func:
                # Collect method definitions
                specifiers = func.declarationSpecifiers()
                declarator = func.declarator()
                decl_list = func.declarationList()
                compound_statement = func.compoundStatement()

                ss.methods.append((specifiers, declarator, decl_list, compound_statement))

        self.superstructs.append(ss)
        self.superstruct_names.update(ss.name)
