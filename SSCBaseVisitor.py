from SSCVisitor import SSCVisitor


class SSCBaseVisitor(SSCVisitor):
    def visitChildren(self, node):
        result = None
        for i in range(node.getChildCount()):
            child = node.getChild(i)
            result = child.accept(self)
        return result
