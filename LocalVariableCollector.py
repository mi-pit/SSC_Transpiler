from SSCVisitor import SSCVisitor
from SSCParser import SSCParser

from Variable import Variable

from text_manipulation import get_text_separated


class LocalVariableCollector(SSCVisitor):
    def __init__(self):
        super().__init__()
        self.local_variables: dict[str, Variable] = {}

    def visitDeclaration(self, ctx: SSCParser.DeclarationContext):
        type_name = []
        specs_ls = []
        for spec in ctx.declarationSpecifiers().declarationSpecifier():
            if spec.typeSpecifier():
                type_name.append(get_text_separated(spec))
            else:
                specs_ls.append(get_text_separated(spec))

        init_decl_list = ctx.initDeclaratorList()
        if not init_decl_list:
            return self.visitChildren(ctx)

        for init_decl in init_decl_list.initDeclarator():
            declarator = init_decl.declarator()

            if not declarator or not declarator.directDeclarator() or not declarator.directDeclarator().Identifier():
                print(f"declarator: {get_text_separated(declarator)}")
                continue

            pointer = declarator.pointer() is not None
            var_name = declarator.directDeclarator().getText()

            var_obj = Variable(specs=" ".join(specs_ls), var_type=" ".join(type_name), pointer=pointer,
                               name=var_name)
            self.local_variables[var_name] = var_obj

        return self.visitChildren(ctx)
