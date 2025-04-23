# Generated from SSC.g4 by ANTLR 4.13.2
from antlr4 import *
if "." in __name__:
    from .SSCParser import SSCParser
else:
    from SSCParser import SSCParser

# This class defines a complete listener for a parse tree produced by SSCParser.
class SSCListener(ParseTreeListener):

    # Enter a parse tree produced by SSCParser#macro.
    def enterMacro(self, ctx:SSCParser.MacroContext):
        pass

    # Exit a parse tree produced by SSCParser#macro.
    def exitMacro(self, ctx:SSCParser.MacroContext):
        pass


    # Enter a parse tree produced by SSCParser#stringLiteral.
    def enterStringLiteral(self, ctx:SSCParser.StringLiteralContext):
        pass

    # Exit a parse tree produced by SSCParser#stringLiteral.
    def exitStringLiteral(self, ctx:SSCParser.StringLiteralContext):
        pass


    # Enter a parse tree produced by SSCParser#compoundStringLiteral.
    def enterCompoundStringLiteral(self, ctx:SSCParser.CompoundStringLiteralContext):
        pass

    # Exit a parse tree produced by SSCParser#compoundStringLiteral.
    def exitCompoundStringLiteral(self, ctx:SSCParser.CompoundStringLiteralContext):
        pass


    # Enter a parse tree produced by SSCParser#primaryExpression.
    def enterPrimaryExpression(self, ctx:SSCParser.PrimaryExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#primaryExpression.
    def exitPrimaryExpression(self, ctx:SSCParser.PrimaryExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#genericSelection.
    def enterGenericSelection(self, ctx:SSCParser.GenericSelectionContext):
        pass

    # Exit a parse tree produced by SSCParser#genericSelection.
    def exitGenericSelection(self, ctx:SSCParser.GenericSelectionContext):
        pass


    # Enter a parse tree produced by SSCParser#genericAssocList.
    def enterGenericAssocList(self, ctx:SSCParser.GenericAssocListContext):
        pass

    # Exit a parse tree produced by SSCParser#genericAssocList.
    def exitGenericAssocList(self, ctx:SSCParser.GenericAssocListContext):
        pass


    # Enter a parse tree produced by SSCParser#genericAssociation.
    def enterGenericAssociation(self, ctx:SSCParser.GenericAssociationContext):
        pass

    # Exit a parse tree produced by SSCParser#genericAssociation.
    def exitGenericAssociation(self, ctx:SSCParser.GenericAssociationContext):
        pass


    # Enter a parse tree produced by SSCParser#postfixExpression.
    def enterPostfixExpression(self, ctx:SSCParser.PostfixExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#postfixExpression.
    def exitPostfixExpression(self, ctx:SSCParser.PostfixExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#argumentExpressionList.
    def enterArgumentExpressionList(self, ctx:SSCParser.ArgumentExpressionListContext):
        pass

    # Exit a parse tree produced by SSCParser#argumentExpressionList.
    def exitArgumentExpressionList(self, ctx:SSCParser.ArgumentExpressionListContext):
        pass


    # Enter a parse tree produced by SSCParser#unaryExpression.
    def enterUnaryExpression(self, ctx:SSCParser.UnaryExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#unaryExpression.
    def exitUnaryExpression(self, ctx:SSCParser.UnaryExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#unaryOperator.
    def enterUnaryOperator(self, ctx:SSCParser.UnaryOperatorContext):
        pass

    # Exit a parse tree produced by SSCParser#unaryOperator.
    def exitUnaryOperator(self, ctx:SSCParser.UnaryOperatorContext):
        pass


    # Enter a parse tree produced by SSCParser#castExpression.
    def enterCastExpression(self, ctx:SSCParser.CastExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#castExpression.
    def exitCastExpression(self, ctx:SSCParser.CastExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#multiplicativeExpression.
    def enterMultiplicativeExpression(self, ctx:SSCParser.MultiplicativeExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#multiplicativeExpression.
    def exitMultiplicativeExpression(self, ctx:SSCParser.MultiplicativeExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#additiveExpression.
    def enterAdditiveExpression(self, ctx:SSCParser.AdditiveExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#additiveExpression.
    def exitAdditiveExpression(self, ctx:SSCParser.AdditiveExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#shiftExpression.
    def enterShiftExpression(self, ctx:SSCParser.ShiftExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#shiftExpression.
    def exitShiftExpression(self, ctx:SSCParser.ShiftExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#relationalExpression.
    def enterRelationalExpression(self, ctx:SSCParser.RelationalExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#relationalExpression.
    def exitRelationalExpression(self, ctx:SSCParser.RelationalExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#equalityExpression.
    def enterEqualityExpression(self, ctx:SSCParser.EqualityExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#equalityExpression.
    def exitEqualityExpression(self, ctx:SSCParser.EqualityExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#andExpression.
    def enterAndExpression(self, ctx:SSCParser.AndExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#andExpression.
    def exitAndExpression(self, ctx:SSCParser.AndExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#exclusiveOrExpression.
    def enterExclusiveOrExpression(self, ctx:SSCParser.ExclusiveOrExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#exclusiveOrExpression.
    def exitExclusiveOrExpression(self, ctx:SSCParser.ExclusiveOrExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#inclusiveOrExpression.
    def enterInclusiveOrExpression(self, ctx:SSCParser.InclusiveOrExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#inclusiveOrExpression.
    def exitInclusiveOrExpression(self, ctx:SSCParser.InclusiveOrExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#logicalAndExpression.
    def enterLogicalAndExpression(self, ctx:SSCParser.LogicalAndExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#logicalAndExpression.
    def exitLogicalAndExpression(self, ctx:SSCParser.LogicalAndExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#logicalOrExpression.
    def enterLogicalOrExpression(self, ctx:SSCParser.LogicalOrExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#logicalOrExpression.
    def exitLogicalOrExpression(self, ctx:SSCParser.LogicalOrExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#conditionalExpression.
    def enterConditionalExpression(self, ctx:SSCParser.ConditionalExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#conditionalExpression.
    def exitConditionalExpression(self, ctx:SSCParser.ConditionalExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#assignmentExpression.
    def enterAssignmentExpression(self, ctx:SSCParser.AssignmentExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#assignmentExpression.
    def exitAssignmentExpression(self, ctx:SSCParser.AssignmentExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#assignmentOperator.
    def enterAssignmentOperator(self, ctx:SSCParser.AssignmentOperatorContext):
        pass

    # Exit a parse tree produced by SSCParser#assignmentOperator.
    def exitAssignmentOperator(self, ctx:SSCParser.AssignmentOperatorContext):
        pass


    # Enter a parse tree produced by SSCParser#expression.
    def enterExpression(self, ctx:SSCParser.ExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#expression.
    def exitExpression(self, ctx:SSCParser.ExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#constantExpression.
    def enterConstantExpression(self, ctx:SSCParser.ConstantExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#constantExpression.
    def exitConstantExpression(self, ctx:SSCParser.ConstantExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#declaration.
    def enterDeclaration(self, ctx:SSCParser.DeclarationContext):
        pass

    # Exit a parse tree produced by SSCParser#declaration.
    def exitDeclaration(self, ctx:SSCParser.DeclarationContext):
        pass


    # Enter a parse tree produced by SSCParser#declarationSpecifiers.
    def enterDeclarationSpecifiers(self, ctx:SSCParser.DeclarationSpecifiersContext):
        pass

    # Exit a parse tree produced by SSCParser#declarationSpecifiers.
    def exitDeclarationSpecifiers(self, ctx:SSCParser.DeclarationSpecifiersContext):
        pass


    # Enter a parse tree produced by SSCParser#declarationSpecifiers2.
    def enterDeclarationSpecifiers2(self, ctx:SSCParser.DeclarationSpecifiers2Context):
        pass

    # Exit a parse tree produced by SSCParser#declarationSpecifiers2.
    def exitDeclarationSpecifiers2(self, ctx:SSCParser.DeclarationSpecifiers2Context):
        pass


    # Enter a parse tree produced by SSCParser#declarationSpecifier.
    def enterDeclarationSpecifier(self, ctx:SSCParser.DeclarationSpecifierContext):
        pass

    # Exit a parse tree produced by SSCParser#declarationSpecifier.
    def exitDeclarationSpecifier(self, ctx:SSCParser.DeclarationSpecifierContext):
        pass


    # Enter a parse tree produced by SSCParser#initDeclaratorList.
    def enterInitDeclaratorList(self, ctx:SSCParser.InitDeclaratorListContext):
        pass

    # Exit a parse tree produced by SSCParser#initDeclaratorList.
    def exitInitDeclaratorList(self, ctx:SSCParser.InitDeclaratorListContext):
        pass


    # Enter a parse tree produced by SSCParser#initDeclarator.
    def enterInitDeclarator(self, ctx:SSCParser.InitDeclaratorContext):
        pass

    # Exit a parse tree produced by SSCParser#initDeclarator.
    def exitInitDeclarator(self, ctx:SSCParser.InitDeclaratorContext):
        pass


    # Enter a parse tree produced by SSCParser#storageClassSpecifier.
    def enterStorageClassSpecifier(self, ctx:SSCParser.StorageClassSpecifierContext):
        pass

    # Exit a parse tree produced by SSCParser#storageClassSpecifier.
    def exitStorageClassSpecifier(self, ctx:SSCParser.StorageClassSpecifierContext):
        pass


    # Enter a parse tree produced by SSCParser#typeSpecifier.
    def enterTypeSpecifier(self, ctx:SSCParser.TypeSpecifierContext):
        pass

    # Exit a parse tree produced by SSCParser#typeSpecifier.
    def exitTypeSpecifier(self, ctx:SSCParser.TypeSpecifierContext):
        pass


    # Enter a parse tree produced by SSCParser#superStructSpecifier.
    def enterSuperStructSpecifier(self, ctx:SSCParser.SuperStructSpecifierContext):
        pass

    # Exit a parse tree produced by SSCParser#superStructSpecifier.
    def exitSuperStructSpecifier(self, ctx:SSCParser.SuperStructSpecifierContext):
        pass


    # Enter a parse tree produced by SSCParser#superStructBody.
    def enterSuperStructBody(self, ctx:SSCParser.SuperStructBodyContext):
        pass

    # Exit a parse tree produced by SSCParser#superStructBody.
    def exitSuperStructBody(self, ctx:SSCParser.SuperStructBodyContext):
        pass


    # Enter a parse tree produced by SSCParser#superStructMember.
    def enterSuperStructMember(self, ctx:SSCParser.SuperStructMemberContext):
        pass

    # Exit a parse tree produced by SSCParser#superStructMember.
    def exitSuperStructMember(self, ctx:SSCParser.SuperStructMemberContext):
        pass


    # Enter a parse tree produced by SSCParser#superStructDeclaration.
    def enterSuperStructDeclaration(self, ctx:SSCParser.SuperStructDeclarationContext):
        pass

    # Exit a parse tree produced by SSCParser#superStructDeclaration.
    def exitSuperStructDeclaration(self, ctx:SSCParser.SuperStructDeclarationContext):
        pass


    # Enter a parse tree produced by SSCParser#structOrUnionSpecifier.
    def enterStructOrUnionSpecifier(self, ctx:SSCParser.StructOrUnionSpecifierContext):
        pass

    # Exit a parse tree produced by SSCParser#structOrUnionSpecifier.
    def exitStructOrUnionSpecifier(self, ctx:SSCParser.StructOrUnionSpecifierContext):
        pass


    # Enter a parse tree produced by SSCParser#structOrUnion.
    def enterStructOrUnion(self, ctx:SSCParser.StructOrUnionContext):
        pass

    # Exit a parse tree produced by SSCParser#structOrUnion.
    def exitStructOrUnion(self, ctx:SSCParser.StructOrUnionContext):
        pass


    # Enter a parse tree produced by SSCParser#structDeclarationList.
    def enterStructDeclarationList(self, ctx:SSCParser.StructDeclarationListContext):
        pass

    # Exit a parse tree produced by SSCParser#structDeclarationList.
    def exitStructDeclarationList(self, ctx:SSCParser.StructDeclarationListContext):
        pass


    # Enter a parse tree produced by SSCParser#structDeclaration.
    def enterStructDeclaration(self, ctx:SSCParser.StructDeclarationContext):
        pass

    # Exit a parse tree produced by SSCParser#structDeclaration.
    def exitStructDeclaration(self, ctx:SSCParser.StructDeclarationContext):
        pass


    # Enter a parse tree produced by SSCParser#specifierQualifierList.
    def enterSpecifierQualifierList(self, ctx:SSCParser.SpecifierQualifierListContext):
        pass

    # Exit a parse tree produced by SSCParser#specifierQualifierList.
    def exitSpecifierQualifierList(self, ctx:SSCParser.SpecifierQualifierListContext):
        pass


    # Enter a parse tree produced by SSCParser#structDeclaratorList.
    def enterStructDeclaratorList(self, ctx:SSCParser.StructDeclaratorListContext):
        pass

    # Exit a parse tree produced by SSCParser#structDeclaratorList.
    def exitStructDeclaratorList(self, ctx:SSCParser.StructDeclaratorListContext):
        pass


    # Enter a parse tree produced by SSCParser#structDeclarator.
    def enterStructDeclarator(self, ctx:SSCParser.StructDeclaratorContext):
        pass

    # Exit a parse tree produced by SSCParser#structDeclarator.
    def exitStructDeclarator(self, ctx:SSCParser.StructDeclaratorContext):
        pass


    # Enter a parse tree produced by SSCParser#enumSpecifier.
    def enterEnumSpecifier(self, ctx:SSCParser.EnumSpecifierContext):
        pass

    # Exit a parse tree produced by SSCParser#enumSpecifier.
    def exitEnumSpecifier(self, ctx:SSCParser.EnumSpecifierContext):
        pass


    # Enter a parse tree produced by SSCParser#enumeratorList.
    def enterEnumeratorList(self, ctx:SSCParser.EnumeratorListContext):
        pass

    # Exit a parse tree produced by SSCParser#enumeratorList.
    def exitEnumeratorList(self, ctx:SSCParser.EnumeratorListContext):
        pass


    # Enter a parse tree produced by SSCParser#enumerator.
    def enterEnumerator(self, ctx:SSCParser.EnumeratorContext):
        pass

    # Exit a parse tree produced by SSCParser#enumerator.
    def exitEnumerator(self, ctx:SSCParser.EnumeratorContext):
        pass


    # Enter a parse tree produced by SSCParser#enumerationConstant.
    def enterEnumerationConstant(self, ctx:SSCParser.EnumerationConstantContext):
        pass

    # Exit a parse tree produced by SSCParser#enumerationConstant.
    def exitEnumerationConstant(self, ctx:SSCParser.EnumerationConstantContext):
        pass


    # Enter a parse tree produced by SSCParser#atomicTypeSpecifier.
    def enterAtomicTypeSpecifier(self, ctx:SSCParser.AtomicTypeSpecifierContext):
        pass

    # Exit a parse tree produced by SSCParser#atomicTypeSpecifier.
    def exitAtomicTypeSpecifier(self, ctx:SSCParser.AtomicTypeSpecifierContext):
        pass


    # Enter a parse tree produced by SSCParser#typeQualifier.
    def enterTypeQualifier(self, ctx:SSCParser.TypeQualifierContext):
        pass

    # Exit a parse tree produced by SSCParser#typeQualifier.
    def exitTypeQualifier(self, ctx:SSCParser.TypeQualifierContext):
        pass


    # Enter a parse tree produced by SSCParser#functionSpecifier.
    def enterFunctionSpecifier(self, ctx:SSCParser.FunctionSpecifierContext):
        pass

    # Exit a parse tree produced by SSCParser#functionSpecifier.
    def exitFunctionSpecifier(self, ctx:SSCParser.FunctionSpecifierContext):
        pass


    # Enter a parse tree produced by SSCParser#alignmentSpecifier.
    def enterAlignmentSpecifier(self, ctx:SSCParser.AlignmentSpecifierContext):
        pass

    # Exit a parse tree produced by SSCParser#alignmentSpecifier.
    def exitAlignmentSpecifier(self, ctx:SSCParser.AlignmentSpecifierContext):
        pass


    # Enter a parse tree produced by SSCParser#declarator.
    def enterDeclarator(self, ctx:SSCParser.DeclaratorContext):
        pass

    # Exit a parse tree produced by SSCParser#declarator.
    def exitDeclarator(self, ctx:SSCParser.DeclaratorContext):
        pass


    # Enter a parse tree produced by SSCParser#directDeclarator.
    def enterDirectDeclarator(self, ctx:SSCParser.DirectDeclaratorContext):
        pass

    # Exit a parse tree produced by SSCParser#directDeclarator.
    def exitDirectDeclarator(self, ctx:SSCParser.DirectDeclaratorContext):
        pass


    # Enter a parse tree produced by SSCParser#vcSpecificModifer.
    def enterVcSpecificModifer(self, ctx:SSCParser.VcSpecificModiferContext):
        pass

    # Exit a parse tree produced by SSCParser#vcSpecificModifer.
    def exitVcSpecificModifer(self, ctx:SSCParser.VcSpecificModiferContext):
        pass


    # Enter a parse tree produced by SSCParser#gccDeclaratorExtension.
    def enterGccDeclaratorExtension(self, ctx:SSCParser.GccDeclaratorExtensionContext):
        pass

    # Exit a parse tree produced by SSCParser#gccDeclaratorExtension.
    def exitGccDeclaratorExtension(self, ctx:SSCParser.GccDeclaratorExtensionContext):
        pass


    # Enter a parse tree produced by SSCParser#gccAttributeSpecifier.
    def enterGccAttributeSpecifier(self, ctx:SSCParser.GccAttributeSpecifierContext):
        pass

    # Exit a parse tree produced by SSCParser#gccAttributeSpecifier.
    def exitGccAttributeSpecifier(self, ctx:SSCParser.GccAttributeSpecifierContext):
        pass


    # Enter a parse tree produced by SSCParser#gccAttributeList.
    def enterGccAttributeList(self, ctx:SSCParser.GccAttributeListContext):
        pass

    # Exit a parse tree produced by SSCParser#gccAttributeList.
    def exitGccAttributeList(self, ctx:SSCParser.GccAttributeListContext):
        pass


    # Enter a parse tree produced by SSCParser#gccAttribute.
    def enterGccAttribute(self, ctx:SSCParser.GccAttributeContext):
        pass

    # Exit a parse tree produced by SSCParser#gccAttribute.
    def exitGccAttribute(self, ctx:SSCParser.GccAttributeContext):
        pass


    # Enter a parse tree produced by SSCParser#nestedParenthesesBlock.
    def enterNestedParenthesesBlock(self, ctx:SSCParser.NestedParenthesesBlockContext):
        pass

    # Exit a parse tree produced by SSCParser#nestedParenthesesBlock.
    def exitNestedParenthesesBlock(self, ctx:SSCParser.NestedParenthesesBlockContext):
        pass


    # Enter a parse tree produced by SSCParser#pointer.
    def enterPointer(self, ctx:SSCParser.PointerContext):
        pass

    # Exit a parse tree produced by SSCParser#pointer.
    def exitPointer(self, ctx:SSCParser.PointerContext):
        pass


    # Enter a parse tree produced by SSCParser#typeQualifierList.
    def enterTypeQualifierList(self, ctx:SSCParser.TypeQualifierListContext):
        pass

    # Exit a parse tree produced by SSCParser#typeQualifierList.
    def exitTypeQualifierList(self, ctx:SSCParser.TypeQualifierListContext):
        pass


    # Enter a parse tree produced by SSCParser#parameterTypeList.
    def enterParameterTypeList(self, ctx:SSCParser.ParameterTypeListContext):
        pass

    # Exit a parse tree produced by SSCParser#parameterTypeList.
    def exitParameterTypeList(self, ctx:SSCParser.ParameterTypeListContext):
        pass


    # Enter a parse tree produced by SSCParser#parameterList.
    def enterParameterList(self, ctx:SSCParser.ParameterListContext):
        pass

    # Exit a parse tree produced by SSCParser#parameterList.
    def exitParameterList(self, ctx:SSCParser.ParameterListContext):
        pass


    # Enter a parse tree produced by SSCParser#parameterDeclaration.
    def enterParameterDeclaration(self, ctx:SSCParser.ParameterDeclarationContext):
        pass

    # Exit a parse tree produced by SSCParser#parameterDeclaration.
    def exitParameterDeclaration(self, ctx:SSCParser.ParameterDeclarationContext):
        pass


    # Enter a parse tree produced by SSCParser#identifierList.
    def enterIdentifierList(self, ctx:SSCParser.IdentifierListContext):
        pass

    # Exit a parse tree produced by SSCParser#identifierList.
    def exitIdentifierList(self, ctx:SSCParser.IdentifierListContext):
        pass


    # Enter a parse tree produced by SSCParser#typeName.
    def enterTypeName(self, ctx:SSCParser.TypeNameContext):
        pass

    # Exit a parse tree produced by SSCParser#typeName.
    def exitTypeName(self, ctx:SSCParser.TypeNameContext):
        pass


    # Enter a parse tree produced by SSCParser#abstractDeclarator.
    def enterAbstractDeclarator(self, ctx:SSCParser.AbstractDeclaratorContext):
        pass

    # Exit a parse tree produced by SSCParser#abstractDeclarator.
    def exitAbstractDeclarator(self, ctx:SSCParser.AbstractDeclaratorContext):
        pass


    # Enter a parse tree produced by SSCParser#directAbstractDeclarator.
    def enterDirectAbstractDeclarator(self, ctx:SSCParser.DirectAbstractDeclaratorContext):
        pass

    # Exit a parse tree produced by SSCParser#directAbstractDeclarator.
    def exitDirectAbstractDeclarator(self, ctx:SSCParser.DirectAbstractDeclaratorContext):
        pass


    # Enter a parse tree produced by SSCParser#typedefName.
    def enterTypedefName(self, ctx:SSCParser.TypedefNameContext):
        pass

    # Exit a parse tree produced by SSCParser#typedefName.
    def exitTypedefName(self, ctx:SSCParser.TypedefNameContext):
        pass


    # Enter a parse tree produced by SSCParser#initializer.
    def enterInitializer(self, ctx:SSCParser.InitializerContext):
        pass

    # Exit a parse tree produced by SSCParser#initializer.
    def exitInitializer(self, ctx:SSCParser.InitializerContext):
        pass


    # Enter a parse tree produced by SSCParser#initializerList.
    def enterInitializerList(self, ctx:SSCParser.InitializerListContext):
        pass

    # Exit a parse tree produced by SSCParser#initializerList.
    def exitInitializerList(self, ctx:SSCParser.InitializerListContext):
        pass


    # Enter a parse tree produced by SSCParser#designation.
    def enterDesignation(self, ctx:SSCParser.DesignationContext):
        pass

    # Exit a parse tree produced by SSCParser#designation.
    def exitDesignation(self, ctx:SSCParser.DesignationContext):
        pass


    # Enter a parse tree produced by SSCParser#designatorList.
    def enterDesignatorList(self, ctx:SSCParser.DesignatorListContext):
        pass

    # Exit a parse tree produced by SSCParser#designatorList.
    def exitDesignatorList(self, ctx:SSCParser.DesignatorListContext):
        pass


    # Enter a parse tree produced by SSCParser#designator.
    def enterDesignator(self, ctx:SSCParser.DesignatorContext):
        pass

    # Exit a parse tree produced by SSCParser#designator.
    def exitDesignator(self, ctx:SSCParser.DesignatorContext):
        pass


    # Enter a parse tree produced by SSCParser#staticAssertDeclaration.
    def enterStaticAssertDeclaration(self, ctx:SSCParser.StaticAssertDeclarationContext):
        pass

    # Exit a parse tree produced by SSCParser#staticAssertDeclaration.
    def exitStaticAssertDeclaration(self, ctx:SSCParser.StaticAssertDeclarationContext):
        pass


    # Enter a parse tree produced by SSCParser#statement.
    def enterStatement(self, ctx:SSCParser.StatementContext):
        pass

    # Exit a parse tree produced by SSCParser#statement.
    def exitStatement(self, ctx:SSCParser.StatementContext):
        pass


    # Enter a parse tree produced by SSCParser#labeledStatement.
    def enterLabeledStatement(self, ctx:SSCParser.LabeledStatementContext):
        pass

    # Exit a parse tree produced by SSCParser#labeledStatement.
    def exitLabeledStatement(self, ctx:SSCParser.LabeledStatementContext):
        pass


    # Enter a parse tree produced by SSCParser#compoundStatement.
    def enterCompoundStatement(self, ctx:SSCParser.CompoundStatementContext):
        pass

    # Exit a parse tree produced by SSCParser#compoundStatement.
    def exitCompoundStatement(self, ctx:SSCParser.CompoundStatementContext):
        pass


    # Enter a parse tree produced by SSCParser#blockItemList.
    def enterBlockItemList(self, ctx:SSCParser.BlockItemListContext):
        pass

    # Exit a parse tree produced by SSCParser#blockItemList.
    def exitBlockItemList(self, ctx:SSCParser.BlockItemListContext):
        pass


    # Enter a parse tree produced by SSCParser#blockItem.
    def enterBlockItem(self, ctx:SSCParser.BlockItemContext):
        pass

    # Exit a parse tree produced by SSCParser#blockItem.
    def exitBlockItem(self, ctx:SSCParser.BlockItemContext):
        pass


    # Enter a parse tree produced by SSCParser#expressionStatement.
    def enterExpressionStatement(self, ctx:SSCParser.ExpressionStatementContext):
        pass

    # Exit a parse tree produced by SSCParser#expressionStatement.
    def exitExpressionStatement(self, ctx:SSCParser.ExpressionStatementContext):
        pass


    # Enter a parse tree produced by SSCParser#selectionStatement.
    def enterSelectionStatement(self, ctx:SSCParser.SelectionStatementContext):
        pass

    # Exit a parse tree produced by SSCParser#selectionStatement.
    def exitSelectionStatement(self, ctx:SSCParser.SelectionStatementContext):
        pass


    # Enter a parse tree produced by SSCParser#iterationStatement.
    def enterIterationStatement(self, ctx:SSCParser.IterationStatementContext):
        pass

    # Exit a parse tree produced by SSCParser#iterationStatement.
    def exitIterationStatement(self, ctx:SSCParser.IterationStatementContext):
        pass


    # Enter a parse tree produced by SSCParser#forCondition.
    def enterForCondition(self, ctx:SSCParser.ForConditionContext):
        pass

    # Exit a parse tree produced by SSCParser#forCondition.
    def exitForCondition(self, ctx:SSCParser.ForConditionContext):
        pass


    # Enter a parse tree produced by SSCParser#forDeclaration.
    def enterForDeclaration(self, ctx:SSCParser.ForDeclarationContext):
        pass

    # Exit a parse tree produced by SSCParser#forDeclaration.
    def exitForDeclaration(self, ctx:SSCParser.ForDeclarationContext):
        pass


    # Enter a parse tree produced by SSCParser#forExpression.
    def enterForExpression(self, ctx:SSCParser.ForExpressionContext):
        pass

    # Exit a parse tree produced by SSCParser#forExpression.
    def exitForExpression(self, ctx:SSCParser.ForExpressionContext):
        pass


    # Enter a parse tree produced by SSCParser#jumpStatement.
    def enterJumpStatement(self, ctx:SSCParser.JumpStatementContext):
        pass

    # Exit a parse tree produced by SSCParser#jumpStatement.
    def exitJumpStatement(self, ctx:SSCParser.JumpStatementContext):
        pass


    # Enter a parse tree produced by SSCParser#compilationUnit.
    def enterCompilationUnit(self, ctx:SSCParser.CompilationUnitContext):
        pass

    # Exit a parse tree produced by SSCParser#compilationUnit.
    def exitCompilationUnit(self, ctx:SSCParser.CompilationUnitContext):
        pass


    # Enter a parse tree produced by SSCParser#translationUnit.
    def enterTranslationUnit(self, ctx:SSCParser.TranslationUnitContext):
        pass

    # Exit a parse tree produced by SSCParser#translationUnit.
    def exitTranslationUnit(self, ctx:SSCParser.TranslationUnitContext):
        pass


    # Enter a parse tree produced by SSCParser#directive.
    def enterDirective(self, ctx:SSCParser.DirectiveContext):
        pass

    # Exit a parse tree produced by SSCParser#directive.
    def exitDirective(self, ctx:SSCParser.DirectiveContext):
        pass


    # Enter a parse tree produced by SSCParser#externalDeclaration.
    def enterExternalDeclaration(self, ctx:SSCParser.ExternalDeclarationContext):
        pass

    # Exit a parse tree produced by SSCParser#externalDeclaration.
    def exitExternalDeclaration(self, ctx:SSCParser.ExternalDeclarationContext):
        pass


    # Enter a parse tree produced by SSCParser#functionDefinition.
    def enterFunctionDefinition(self, ctx:SSCParser.FunctionDefinitionContext):
        pass

    # Exit a parse tree produced by SSCParser#functionDefinition.
    def exitFunctionDefinition(self, ctx:SSCParser.FunctionDefinitionContext):
        pass


    # Enter a parse tree produced by SSCParser#declarationList.
    def enterDeclarationList(self, ctx:SSCParser.DeclarationListContext):
        pass

    # Exit a parse tree produced by SSCParser#declarationList.
    def exitDeclarationList(self, ctx:SSCParser.DeclarationListContext):
        pass



del SSCParser