from typing import Optional

from SSCBaseVisitor import SSCBaseVisitor
from SuperStruct import SuperStruct
from Variable import Variable
from text_manipulation import get_text_separated


class SuperCVisitor(SSCBaseVisitor):
    def __init__(self, tokens, superstruct_names: set[str]):
        self.token_stream = tokens
        self.replacements: dict[tuple[int, int], str] = {}
        self.skip_intervals = []

        self.var_types: dict[str, Variable] = {}
        self.superstructs: list[SuperStruct] = []
        self.superstruct_names: set[str] = superstruct_names

        self.functions: list[str] = []
        self.directives: list[str] = []

    def lookup_variable(self, varname: str) -> Optional[Variable]:
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

        if line_num == 131:
            print(line_num)
            pass

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
        if func_name == "main":
            return self.visitChildren(ctx)

        ctx_text: str = f"{specs} {func_name}"
        prototype: str = ctx_text.split(";")[-1].replace("superstruct", "struct").strip() + ";"
        self.functions.append(prototype)
        # should this save the main function?
        return self.visitChildren(ctx)

    def visitDeclaration(self, ctx):  # fix: variables probably shouldn't be global
        type_name: list[str] = []
        specs_ls = []
        for spec in ctx.declarationSpecifiers().declarationSpecifier():
            if spec.typeSpecifier():
                type_name.append(get_text_separated(spec))
            else:
                specs_ls.append(get_text_separated(spec))

        if not type_name or "superstruct" not in type_name[0]:
            return self.visitChildren(ctx)

        decl_ls = ctx.initDeclaratorList()
        if not decl_ls:
            return self.visitChildren(ctx)

        for init_decl in decl_ls.initDeclarator():
            declarator = init_decl.declarator()  # pointer? directDeclarator
            if not declarator or not declarator.directDeclarator() or not declarator.directDeclarator().Identifier():
                continue

            variable = Variable(" ".join(specs_ls), " ".join(type_name),
                                declarator.pointer() is not None,
                                declarator.directDeclarator().Identifier().getText())
            self.var_types[variable.name] = variable

        return self.visitChildren(ctx)

    def visitPostfixExpression(self, ctx) -> Optional[str]:
        # print(f'Postfix Expression: "{get_text_separated(ctx)}"')
        self.visitChildren(ctx)

        if ctx.getChildCount() < 4:
            return None

        operator = get_text_separated(ctx.getChild(1))
        if operator not in [".", "->"]:
            return None

        obj_expr = get_text_separated(ctx.getChild(0))
        method_name = get_text_separated(ctx.getChild(2))
        if ctx.getChildCount() < 4 or ctx.getChild(3).getText() != "(":
            return None

        # Identify the start and end indices of the expression (the range we need to replace)
        start_idx = ctx.start.tokenIndex
        end_idx = ctx.stop.tokenIndex

        # Check if any existing interval (r0,r1) is contained in [start_idx, end_idx]
        contains_child = any(start_idx <= r0 and r1 <= end_idx
                             for (r0, r1) in self.replacements.keys())
        if contains_child:
            # [start_idx,end_idx] contains an already-processed child interval
            # Handle accordingly (e.g., skip replacing the outer interval).
            pass

        if self.replacements.get((start_idx, end_idx)):
            args_str = self.replacements[start_idx, end_idx]
        else:
            arguments_ls = [get_text_separated(ctx.getChild(i)) for i in range(3, ctx.getChildCount())]
            args_str = "".join(arguments_ls[1:-1])

        # print(f"> Found method call: {obj_expr}{operator}{method_name}( {args_str} )")
        # print(f">>>>>>>> Orig. text: {ctx.getText()}")
        # self.show_in_context(ctx.start)

        variable: Variable = self.lookup_variable(obj_expr)
        is_static_call = obj_expr in self.superstruct_names
        if not variable and not is_static_call:
            return None

        if is_static_call:
            new_call = f"{obj_expr}__{method_name}({args_str})"
        else:
            ss_name = variable.var_type.split()[-1]
            if ss_name not in self.superstruct_names:
                return None

            new_func = f"{ss_name}__{method_name}"

            if operator == ".":
                obj_expr = "&" + obj_expr

            new_call = f"{new_func}({obj_expr}"
            if args_str != "":
                new_call += ", " + args_str
            new_call += ")"

        # print(f"Transformed call: {ctx.getText()} → {new_call}")

        # Store the replacement with the token range and the new call
        self.replacements[start_idx, end_idx] = new_call

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
