from CVisitor import CVisitor
from SuperStruct import SuperStruct


class CBaseVisitor(CVisitor):
    def visitChildren(self, node):
        result = None
        for i in range(node.getChildCount()):
            child = node.getChild(i)
            result = child.accept(self)
        return result


class SuperCVisitor(CBaseVisitor):
    def __init__(self, tokens):
        self.token_stream = tokens
        self.superstructs = []
        self.skip_intervals = []

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
