import re

from SSCBaseVisitor import SSCBaseVisitor
from SSCLexer import SSCLexer
from SuperStruct import SuperStruct, get_text_separated
from Variable import Variable


class SuperCVisitor(SSCBaseVisitor):
    def __init__(self, tokens, superstruct_names: set[str]):
        self.token_stream = tokens
        self.replacements: set = set()
        self.skip_intervals = []

        self.var_types: dict[str, Variable] = {}
        self.superstructs: list[SuperStruct] = []
        self.superstruct_names: set[str] = superstruct_names

        self.functions: list[str] = []
        self.directives: list[str] = []

    def lookup_variable(self, varname: str) -> Variable | None:
        return self.var_types.get(varname, None)

    def get_original_text(self, ctx) -> str:
        start, stop = ctx.getSourceInterval()
        return self.token_stream.getText(start, stop)

    def show_in_context(self, token) -> None:
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

    def visitDirective(self, ctx):
        self.directives.append(get_text_separated(ctx).strip())
        return self.visitChildren(ctx)

    def visitFunctionDefinition(self, ctx):
        if not ctx.declarationSpecifiers():
            return self.visitChildren(ctx)

        specs: str = get_text_separated(ctx.declarationSpecifiers())
        func_name: str = get_text_separated(ctx.declarator())
        ctx_text: str = f"{specs} {func_name}"
        prototype: str = ctx_text.split(";")[-1].replace("superstruct", "struct").strip() + ";"
        self.functions.append(prototype)
        # should this save the main function?
        return self.visitChildren(ctx)

    def visitDeclaration(self, ctx):
        decl_specs: str = get_text_separated(ctx.declarationSpecifiers())
        if "superstruct" not in decl_specs:
            return self.visitChildren(ctx)

        decl_ls = ctx.initDeclaratorList()
        if not decl_ls:
            return self.visitChildren(ctx)
        for init_decl in decl_ls.initDeclarator():
            declarator = init_decl.declarator()  # pointer? directDeclarator
            if not declarator or not declarator.directDeclarator() or not declarator.directDeclarator().Identifier():
                # print(f"declarator: {get_text_separated(declarator)}")
                # self.show_in_context(init_decl.start)
                continue

            variable = Variable(decl_specs,
                                bool(declarator.pointer()) if declarator.pointer() else None,
                                declarator.directDeclarator().Identifier().getText())
            self.var_types[variable.name] = variable

        return self.visitChildren(ctx)

    def visitPostfixExpression(self, ctx):
        # print(f'Postfix Expression: "{get_text_separated(ctx)}"')

        if ctx.getChildCount() < 4:
            return self.visitChildren(ctx)

        operator = get_text_separated(ctx.getChild(1))
        if operator not in [".", "->"]:
            return self.visitChildren(ctx)

        obj_expr = get_text_separated(ctx.getChild(0))
        method_name = get_text_separated(ctx.getChild(2))
        if ctx.getChildCount() < 4 or ctx.getChild(3).getText() != "(":
            return self.visitChildren(ctx)

        arguments_ls = [get_text_separated(ctx.getChild(i)) for i in range(3, ctx.getChildCount())]
        args_str = "".join(arguments_ls[1:-1])

        # print(f"> Found method call: {obj_expr}{operator}{method_name}({args_str})")
        # print(f">>>>>>>> Orig. text: {ctx.getText()}")
        # self.show_in_context(ctx.start)

        variable: Variable = self.lookup_variable(obj_expr)
        is_static_call = obj_expr in self.superstruct_names
        if not variable and not is_static_call:
            return self.visitChildren(ctx)

        if is_static_call:
            new_call = f"{obj_expr}__{method_name}({args_str})"
        else:
            ss_name = variable.var_type.split()[-1]
            if ss_name not in self.superstruct_names:
                return self.visitChildren(ctx)

            new_func = f"{ss_name}__{method_name}"

            if operator == ".":
                obj_expr = "&" + obj_expr

            new_call = f"{new_func}({obj_expr}"
            if args_str != "":
                new_call += ", " + args_str
            new_call += ")"

        # print(f"Transformed call: {ctx.getText()} → {new_call}")

        # Identify the start and end indices of the expression (the range we need to replace)
        start_idx = ctx.start.tokenIndex
        end_idx = ctx.stop.tokenIndex

        # Store the replacement with the token range and the new call
        self.replacements.add((start_idx, end_idx, new_call))

        self.visitChildren(ctx)
        return new_call

    def visitSuperStructSpecifier(self, ctx) -> None:
        # don't visit children
        name = ctx.Identifier().getText()
        ss = SuperStruct(name, self.token_stream)

        if not ctx.superStructBody():
            self.superstruct_names.add(ctx.Identifier().getText())
            # not an ss definition, but a declaration
            return

        ss.is_private = ctx.getChild(0).getText() == "private" if ctx.getChildCount() > 0 else False

        # Track this superstruct's token interval to exclude from raw C output
        start, stop = ctx.getSourceInterval()
        self.skip_intervals.append((start, stop + 2))  # + 2 to include "};"

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
        self.superstruct_names.add(ss.name)
