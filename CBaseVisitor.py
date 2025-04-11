from CVisitor import CVisitor


class CBaseVisitor(CVisitor):
    def visitChildren(self, node):
        result = None
        for i in range(node.getChildCount()):
            child = node.getChild(i)
            result = child.accept(self)
        return result
