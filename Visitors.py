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
        # First, recurse into children:
        # this will let inner method calls be replaced first
        children_rewrites = [self.visit(c) for c in ctx.children]

        # If this node isn’t a method call, bail out
        if not ctx.getChild(3) or ctx.getChild(3).getText() != "(":
            return None

        # print(self.get_original_text(ctx))
        # self.show_in_context(ctx.start)

        # Reconstruct object, operator, method, and args *from the rewritten children*
        obj_expr = children_rewrites[0] or get_text_separated(ctx.getChild(0))
        operator = children_rewrites[1] or ctx.getChild(1).getText()
        method_name = children_rewrites[2] or ctx.getChild(2).getText()

        # Gather argument expressions by visiting the sub-contexts
        args = []
        # children start at index 3: "(" , then interleaved expr and punctuation
        # so skip the "(" at 3 and last ")" at -1
        for i in range(4, ctx.getChildCount() - 1):
            child_ctx = ctx.getChild(i)
            rewritten = self.visit(child_ctx)
            if rewritten is not None:
                args.append(rewritten)
            else:
                args.append(get_text_separated(child_ctx))

        args_str = ", ".join(args)

        # now do your usual lookup to see if it's a static or instance call
        variable = self.lookup_variable(obj_expr)
        is_static_call = obj_expr in self.superstruct_names
        if not variable and not is_static_call:
            return None

        if is_static_call:
            new_call = f"{obj_expr}__{method_name}({args_str})"
        else:
            ss_name = variable.var_type.split()[-1]
            if ss_name not in self.superstruct_names:
                return None

            if operator == ".":
                obj_expr = "&" + obj_expr
            new_call = f"{ss_name}__{method_name}({obj_expr}"
            if args_str:
                new_call += ", " + args_str
            new_call += ")"

        # store it if you need the token mapping, or just return it
        start_idx = ctx.start.tokenIndex
        end_idx = ctx.stop.tokenIndex
        self.replacements[(start_idx, end_idx)] = new_call

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
