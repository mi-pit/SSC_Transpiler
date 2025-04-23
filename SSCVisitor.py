# Generated from SSC.g4 by ANTLR 4.13.2
from antlr4 import *
if "." in __name__:
    from .SSCParser import SSCParser
else:
    from SSCParser import SSCParser

# This class defines a complete generic visitor for a parse tree produced by SSCParser.

class SSCVisitor(ParseTreeVisitor):

    # Visit a parse tree produced by SSCParser#macro.
    def visitMacro(self, ctx:SSCParser.MacroContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#stringLiteral.
    def visitStringLiteral(self, ctx:SSCParser.StringLiteralContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#compoundStringLiteral.
    def visitCompoundStringLiteral(self, ctx:SSCParser.CompoundStringLiteralContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#primaryExpression.
    def visitPrimaryExpression(self, ctx:SSCParser.PrimaryExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#genericSelection.
    def visitGenericSelection(self, ctx:SSCParser.GenericSelectionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#genericAssocList.
    def visitGenericAssocList(self, ctx:SSCParser.GenericAssocListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#genericAssociation.
    def visitGenericAssociation(self, ctx:SSCParser.GenericAssociationContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#postfixExpression.
    def visitPostfixExpression(self, ctx:SSCParser.PostfixExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#argumentExpressionList.
    def visitArgumentExpressionList(self, ctx:SSCParser.ArgumentExpressionListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#unaryExpression.
    def visitUnaryExpression(self, ctx:SSCParser.UnaryExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#unaryOperator.
    def visitUnaryOperator(self, ctx:SSCParser.UnaryOperatorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#castExpression.
    def visitCastExpression(self, ctx:SSCParser.CastExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#multiplicativeExpression.
    def visitMultiplicativeExpression(self, ctx:SSCParser.MultiplicativeExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#additiveExpression.
    def visitAdditiveExpression(self, ctx:SSCParser.AdditiveExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#shiftExpression.
    def visitShiftExpression(self, ctx:SSCParser.ShiftExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#relationalExpression.
    def visitRelationalExpression(self, ctx:SSCParser.RelationalExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#equalityExpression.
    def visitEqualityExpression(self, ctx:SSCParser.EqualityExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#andExpression.
    def visitAndExpression(self, ctx:SSCParser.AndExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#exclusiveOrExpression.
    def visitExclusiveOrExpression(self, ctx:SSCParser.ExclusiveOrExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#inclusiveOrExpression.
    def visitInclusiveOrExpression(self, ctx:SSCParser.InclusiveOrExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#logicalAndExpression.
    def visitLogicalAndExpression(self, ctx:SSCParser.LogicalAndExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#logicalOrExpression.
    def visitLogicalOrExpression(self, ctx:SSCParser.LogicalOrExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#conditionalExpression.
    def visitConditionalExpression(self, ctx:SSCParser.ConditionalExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#assignmentExpression.
    def visitAssignmentExpression(self, ctx:SSCParser.AssignmentExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#assignmentOperator.
    def visitAssignmentOperator(self, ctx:SSCParser.AssignmentOperatorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#expression.
    def visitExpression(self, ctx:SSCParser.ExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#constantExpression.
    def visitConstantExpression(self, ctx:SSCParser.ConstantExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#declaration.
    def visitDeclaration(self, ctx:SSCParser.DeclarationContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#declarationSpecifiers.
    def visitDeclarationSpecifiers(self, ctx:SSCParser.DeclarationSpecifiersContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#declarationSpecifiers2.
    def visitDeclarationSpecifiers2(self, ctx:SSCParser.DeclarationSpecifiers2Context):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#declarationSpecifier.
    def visitDeclarationSpecifier(self, ctx:SSCParser.DeclarationSpecifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#initDeclaratorList.
    def visitInitDeclaratorList(self, ctx:SSCParser.InitDeclaratorListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#initDeclarator.
    def visitInitDeclarator(self, ctx:SSCParser.InitDeclaratorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#storageClassSpecifier.
    def visitStorageClassSpecifier(self, ctx:SSCParser.StorageClassSpecifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#typeSpecifier.
    def visitTypeSpecifier(self, ctx:SSCParser.TypeSpecifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#superStructSpecifier.
    def visitSuperStructSpecifier(self, ctx:SSCParser.SuperStructSpecifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#superStructBody.
    def visitSuperStructBody(self, ctx:SSCParser.SuperStructBodyContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#superStructMember.
    def visitSuperStructMember(self, ctx:SSCParser.SuperStructMemberContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#superStructDeclaration.
    def visitSuperStructDeclaration(self, ctx:SSCParser.SuperStructDeclarationContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#structOrUnionSpecifier.
    def visitStructOrUnionSpecifier(self, ctx:SSCParser.StructOrUnionSpecifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#structOrUnion.
    def visitStructOrUnion(self, ctx:SSCParser.StructOrUnionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#structDeclarationList.
    def visitStructDeclarationList(self, ctx:SSCParser.StructDeclarationListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#structDeclaration.
    def visitStructDeclaration(self, ctx:SSCParser.StructDeclarationContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#specifierQualifierList.
    def visitSpecifierQualifierList(self, ctx:SSCParser.SpecifierQualifierListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#structDeclaratorList.
    def visitStructDeclaratorList(self, ctx:SSCParser.StructDeclaratorListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#structDeclarator.
    def visitStructDeclarator(self, ctx:SSCParser.StructDeclaratorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#enumSpecifier.
    def visitEnumSpecifier(self, ctx:SSCParser.EnumSpecifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#enumeratorList.
    def visitEnumeratorList(self, ctx:SSCParser.EnumeratorListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#enumerator.
    def visitEnumerator(self, ctx:SSCParser.EnumeratorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#enumerationConstant.
    def visitEnumerationConstant(self, ctx:SSCParser.EnumerationConstantContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#atomicTypeSpecifier.
    def visitAtomicTypeSpecifier(self, ctx:SSCParser.AtomicTypeSpecifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#typeQualifier.
    def visitTypeQualifier(self, ctx:SSCParser.TypeQualifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#functionSpecifier.
    def visitFunctionSpecifier(self, ctx:SSCParser.FunctionSpecifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#alignmentSpecifier.
    def visitAlignmentSpecifier(self, ctx:SSCParser.AlignmentSpecifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#declarator.
    def visitDeclarator(self, ctx:SSCParser.DeclaratorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#directDeclarator.
    def visitDirectDeclarator(self, ctx:SSCParser.DirectDeclaratorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#vcSpecificModifer.
    def visitVcSpecificModifer(self, ctx:SSCParser.VcSpecificModiferContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#gccDeclaratorExtension.
    def visitGccDeclaratorExtension(self, ctx:SSCParser.GccDeclaratorExtensionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#gccAttributeSpecifier.
    def visitGccAttributeSpecifier(self, ctx:SSCParser.GccAttributeSpecifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#gccAttributeList.
    def visitGccAttributeList(self, ctx:SSCParser.GccAttributeListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#gccAttribute.
    def visitGccAttribute(self, ctx:SSCParser.GccAttributeContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#nestedParenthesesBlock.
    def visitNestedParenthesesBlock(self, ctx:SSCParser.NestedParenthesesBlockContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#pointer.
    def visitPointer(self, ctx:SSCParser.PointerContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#typeQualifierList.
    def visitTypeQualifierList(self, ctx:SSCParser.TypeQualifierListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#parameterTypeList.
    def visitParameterTypeList(self, ctx:SSCParser.ParameterTypeListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#parameterList.
    def visitParameterList(self, ctx:SSCParser.ParameterListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#parameterDeclaration.
    def visitParameterDeclaration(self, ctx:SSCParser.ParameterDeclarationContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#identifierList.
    def visitIdentifierList(self, ctx:SSCParser.IdentifierListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#typeName.
    def visitTypeName(self, ctx:SSCParser.TypeNameContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#abstractDeclarator.
    def visitAbstractDeclarator(self, ctx:SSCParser.AbstractDeclaratorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#directAbstractDeclarator.
    def visitDirectAbstractDeclarator(self, ctx:SSCParser.DirectAbstractDeclaratorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#typedefName.
    def visitTypedefName(self, ctx:SSCParser.TypedefNameContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#initializer.
    def visitInitializer(self, ctx:SSCParser.InitializerContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#initializerList.
    def visitInitializerList(self, ctx:SSCParser.InitializerListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#designation.
    def visitDesignation(self, ctx:SSCParser.DesignationContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#designatorList.
    def visitDesignatorList(self, ctx:SSCParser.DesignatorListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#designator.
    def visitDesignator(self, ctx:SSCParser.DesignatorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#staticAssertDeclaration.
    def visitStaticAssertDeclaration(self, ctx:SSCParser.StaticAssertDeclarationContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#statement.
    def visitStatement(self, ctx:SSCParser.StatementContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#labeledStatement.
    def visitLabeledStatement(self, ctx:SSCParser.LabeledStatementContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#compoundStatement.
    def visitCompoundStatement(self, ctx:SSCParser.CompoundStatementContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#blockItemList.
    def visitBlockItemList(self, ctx:SSCParser.BlockItemListContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#blockItem.
    def visitBlockItem(self, ctx:SSCParser.BlockItemContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#expressionStatement.
    def visitExpressionStatement(self, ctx:SSCParser.ExpressionStatementContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#selectionStatement.
    def visitSelectionStatement(self, ctx:SSCParser.SelectionStatementContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#iterationStatement.
    def visitIterationStatement(self, ctx:SSCParser.IterationStatementContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#forCondition.
    def visitForCondition(self, ctx:SSCParser.ForConditionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#forDeclaration.
    def visitForDeclaration(self, ctx:SSCParser.ForDeclarationContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#forExpression.
    def visitForExpression(self, ctx:SSCParser.ForExpressionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#jumpStatement.
    def visitJumpStatement(self, ctx:SSCParser.JumpStatementContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#compilationUnit.
    def visitCompilationUnit(self, ctx:SSCParser.CompilationUnitContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#translationUnit.
    def visitTranslationUnit(self, ctx:SSCParser.TranslationUnitContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#directive.
    def visitDirective(self, ctx:SSCParser.DirectiveContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#externalDeclaration.
    def visitExternalDeclaration(self, ctx:SSCParser.ExternalDeclarationContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#functionDefinition.
    def visitFunctionDefinition(self, ctx:SSCParser.FunctionDefinitionContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SSCParser#declarationList.
    def visitDeclarationList(self, ctx:SSCParser.DeclarationListContext):
        return self.visitChildren(ctx)



del SSCParser