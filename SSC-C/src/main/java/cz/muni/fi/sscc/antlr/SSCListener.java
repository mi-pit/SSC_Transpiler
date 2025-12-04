package cz.muni.fi.sscc.antlr;
// Generated from SSC.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SSCParser}.
 */
public interface SSCListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SSCParser#compoundStringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterCompoundStringLiteral(SSCParser.CompoundStringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#compoundStringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitCompoundStringLiteral(SSCParser.CompoundStringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpression(SSCParser.PrimaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpression(SSCParser.PrimaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#genericSelection}.
	 * @param ctx the parse tree
	 */
	void enterGenericSelection(SSCParser.GenericSelectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#genericSelection}.
	 * @param ctx the parse tree
	 */
	void exitGenericSelection(SSCParser.GenericSelectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#genericAssocList}.
	 * @param ctx the parse tree
	 */
	void enterGenericAssocList(SSCParser.GenericAssocListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#genericAssocList}.
	 * @param ctx the parse tree
	 */
	void exitGenericAssocList(SSCParser.GenericAssocListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#genericAssociation}.
	 * @param ctx the parse tree
	 */
	void enterGenericAssociation(SSCParser.GenericAssociationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#genericAssociation}.
	 * @param ctx the parse tree
	 */
	void exitGenericAssociation(SSCParser.GenericAssociationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostfixExpression(SSCParser.PostfixExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostfixExpression(SSCParser.PostfixExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#argumentExpressionList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentExpressionList(SSCParser.ArgumentExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#argumentExpressionList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentExpressionList(SSCParser.ArgumentExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(SSCParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(SSCParser.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#unaryOperator}.
	 * @param ctx the parse tree
	 */
	void enterUnaryOperator(SSCParser.UnaryOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#unaryOperator}.
	 * @param ctx the parse tree
	 */
	void exitUnaryOperator(SSCParser.UnaryOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#castExpression}.
	 * @param ctx the parse tree
	 */
	void enterCastExpression(SSCParser.CastExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#castExpression}.
	 * @param ctx the parse tree
	 */
	void exitCastExpression(SSCParser.CastExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(SSCParser.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(SSCParser.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(SSCParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(SSCParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#shiftExpression}.
	 * @param ctx the parse tree
	 */
	void enterShiftExpression(SSCParser.ShiftExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#shiftExpression}.
	 * @param ctx the parse tree
	 */
	void exitShiftExpression(SSCParser.ShiftExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpression(SSCParser.RelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpression(SSCParser.RelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpression(SSCParser.EqualityExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpression(SSCParser.EqualityExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#andExpression}.
	 * @param ctx the parse tree
	 */
	void enterAndExpression(SSCParser.AndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#andExpression}.
	 * @param ctx the parse tree
	 */
	void exitAndExpression(SSCParser.AndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#exclusiveOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterExclusiveOrExpression(SSCParser.ExclusiveOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#exclusiveOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitExclusiveOrExpression(SSCParser.ExclusiveOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#inclusiveOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterInclusiveOrExpression(SSCParser.InclusiveOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#inclusiveOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitInclusiveOrExpression(SSCParser.InclusiveOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalAndExpression(SSCParser.LogicalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalAndExpression(SSCParser.LogicalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOrExpression(SSCParser.LogicalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOrExpression(SSCParser.LogicalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#conditionalExpression}.
	 * @param ctx the parse tree
	 */
	void enterConditionalExpression(SSCParser.ConditionalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#conditionalExpression}.
	 * @param ctx the parse tree
	 */
	void exitConditionalExpression(SSCParser.ConditionalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#assignmentExpression}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentExpression(SSCParser.AssignmentExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#assignmentExpression}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentExpression(SSCParser.AssignmentExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentOperator(SSCParser.AssignmentOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentOperator(SSCParser.AssignmentOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(SSCParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(SSCParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpression(SSCParser.ConstantExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpression(SSCParser.ConstantExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#declaration}.
	 * @param ctx the parse tree
	 */
	void enterDeclaration(SSCParser.DeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#declaration}.
	 * @param ctx the parse tree
	 */
	void exitDeclaration(SSCParser.DeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#declarationSpecifiers}.
	 * @param ctx the parse tree
	 */
	void enterDeclarationSpecifiers(SSCParser.DeclarationSpecifiersContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#declarationSpecifiers}.
	 * @param ctx the parse tree
	 */
	void exitDeclarationSpecifiers(SSCParser.DeclarationSpecifiersContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#declarationSpecifiers2}.
	 * @param ctx the parse tree
	 */
	void enterDeclarationSpecifiers2(SSCParser.DeclarationSpecifiers2Context ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#declarationSpecifiers2}.
	 * @param ctx the parse tree
	 */
	void exitDeclarationSpecifiers2(SSCParser.DeclarationSpecifiers2Context ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#declarationSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterDeclarationSpecifier(SSCParser.DeclarationSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#declarationSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitDeclarationSpecifier(SSCParser.DeclarationSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#initDeclaratorList}.
	 * @param ctx the parse tree
	 */
	void enterInitDeclaratorList(SSCParser.InitDeclaratorListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#initDeclaratorList}.
	 * @param ctx the parse tree
	 */
	void exitInitDeclaratorList(SSCParser.InitDeclaratorListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#initDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterInitDeclarator(SSCParser.InitDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#initDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitInitDeclarator(SSCParser.InitDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#storageClassSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterStorageClassSpecifier(SSCParser.StorageClassSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#storageClassSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitStorageClassSpecifier(SSCParser.StorageClassSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#typeSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterTypeSpecifier(SSCParser.TypeSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#typeSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitTypeSpecifier(SSCParser.TypeSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#superStructSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#superStructSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#superStructBody}.
	 * @param ctx the parse tree
	 */
	void enterSuperStructBody(SSCParser.SuperStructBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#superStructBody}.
	 * @param ctx the parse tree
	 */
	void exitSuperStructBody(SSCParser.SuperStructBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#superStructMember}.
	 * @param ctx the parse tree
	 */
	void enterSuperStructMember(SSCParser.SuperStructMemberContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#superStructMember}.
	 * @param ctx the parse tree
	 */
	void exitSuperStructMember(SSCParser.SuperStructMemberContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#structOrUnionSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterStructOrUnionSpecifier(SSCParser.StructOrUnionSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#structOrUnionSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitStructOrUnionSpecifier(SSCParser.StructOrUnionSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#structOrUnion}.
	 * @param ctx the parse tree
	 */
	void enterStructOrUnion(SSCParser.StructOrUnionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#structOrUnion}.
	 * @param ctx the parse tree
	 */
	void exitStructOrUnion(SSCParser.StructOrUnionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#structDeclarationList}.
	 * @param ctx the parse tree
	 */
	void enterStructDeclarationList(SSCParser.StructDeclarationListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#structDeclarationList}.
	 * @param ctx the parse tree
	 */
	void exitStructDeclarationList(SSCParser.StructDeclarationListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#structDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterStructDeclaration(SSCParser.StructDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#structDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitStructDeclaration(SSCParser.StructDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#specifierQualifierList}.
	 * @param ctx the parse tree
	 */
	void enterSpecifierQualifierList(SSCParser.SpecifierQualifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#specifierQualifierList}.
	 * @param ctx the parse tree
	 */
	void exitSpecifierQualifierList(SSCParser.SpecifierQualifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#structDeclaratorList}.
	 * @param ctx the parse tree
	 */
	void enterStructDeclaratorList(SSCParser.StructDeclaratorListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#structDeclaratorList}.
	 * @param ctx the parse tree
	 */
	void exitStructDeclaratorList(SSCParser.StructDeclaratorListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#structDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterStructDeclarator(SSCParser.StructDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#structDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitStructDeclarator(SSCParser.StructDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#enumSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterEnumSpecifier(SSCParser.EnumSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#enumSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitEnumSpecifier(SSCParser.EnumSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#enumeratorList}.
	 * @param ctx the parse tree
	 */
	void enterEnumeratorList(SSCParser.EnumeratorListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#enumeratorList}.
	 * @param ctx the parse tree
	 */
	void exitEnumeratorList(SSCParser.EnumeratorListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#enumerator}.
	 * @param ctx the parse tree
	 */
	void enterEnumerator(SSCParser.EnumeratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#enumerator}.
	 * @param ctx the parse tree
	 */
	void exitEnumerator(SSCParser.EnumeratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#enumerationConstant}.
	 * @param ctx the parse tree
	 */
	void enterEnumerationConstant(SSCParser.EnumerationConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#enumerationConstant}.
	 * @param ctx the parse tree
	 */
	void exitEnumerationConstant(SSCParser.EnumerationConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#atomicTypeSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterAtomicTypeSpecifier(SSCParser.AtomicTypeSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#atomicTypeSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitAtomicTypeSpecifier(SSCParser.AtomicTypeSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#typeQualifier}.
	 * @param ctx the parse tree
	 */
	void enterTypeQualifier(SSCParser.TypeQualifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#typeQualifier}.
	 * @param ctx the parse tree
	 */
	void exitTypeQualifier(SSCParser.TypeQualifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#functionSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterFunctionSpecifier(SSCParser.FunctionSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#functionSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitFunctionSpecifier(SSCParser.FunctionSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#alignmentSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterAlignmentSpecifier(SSCParser.AlignmentSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#alignmentSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitAlignmentSpecifier(SSCParser.AlignmentSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#declarator}.
	 * @param ctx the parse tree
	 */
	void enterDeclarator(SSCParser.DeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#declarator}.
	 * @param ctx the parse tree
	 */
	void exitDeclarator(SSCParser.DeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#directDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterDirectDeclarator(SSCParser.DirectDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#directDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitDirectDeclarator(SSCParser.DirectDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#vcSpecificModifer}.
	 * @param ctx the parse tree
	 */
	void enterVcSpecificModifer(SSCParser.VcSpecificModiferContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#vcSpecificModifer}.
	 * @param ctx the parse tree
	 */
	void exitVcSpecificModifer(SSCParser.VcSpecificModiferContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#gccDeclaratorExtension}.
	 * @param ctx the parse tree
	 */
	void enterGccDeclaratorExtension(SSCParser.GccDeclaratorExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#gccDeclaratorExtension}.
	 * @param ctx the parse tree
	 */
	void exitGccDeclaratorExtension(SSCParser.GccDeclaratorExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#gccAttributeSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterGccAttributeSpecifier(SSCParser.GccAttributeSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#gccAttributeSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitGccAttributeSpecifier(SSCParser.GccAttributeSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#gccAttributeList}.
	 * @param ctx the parse tree
	 */
	void enterGccAttributeList(SSCParser.GccAttributeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#gccAttributeList}.
	 * @param ctx the parse tree
	 */
	void exitGccAttributeList(SSCParser.GccAttributeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#gccAttribute}.
	 * @param ctx the parse tree
	 */
	void enterGccAttribute(SSCParser.GccAttributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#gccAttribute}.
	 * @param ctx the parse tree
	 */
	void exitGccAttribute(SSCParser.GccAttributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#nestedParenthesesBlock}.
	 * @param ctx the parse tree
	 */
	void enterNestedParenthesesBlock(SSCParser.NestedParenthesesBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#nestedParenthesesBlock}.
	 * @param ctx the parse tree
	 */
	void exitNestedParenthesesBlock(SSCParser.NestedParenthesesBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#pointer}.
	 * @param ctx the parse tree
	 */
	void enterPointer(SSCParser.PointerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#pointer}.
	 * @param ctx the parse tree
	 */
	void exitPointer(SSCParser.PointerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#typeQualifierList}.
	 * @param ctx the parse tree
	 */
	void enterTypeQualifierList(SSCParser.TypeQualifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#typeQualifierList}.
	 * @param ctx the parse tree
	 */
	void exitTypeQualifierList(SSCParser.TypeQualifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#parameterTypeList}.
	 * @param ctx the parse tree
	 */
	void enterParameterTypeList(SSCParser.ParameterTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#parameterTypeList}.
	 * @param ctx the parse tree
	 */
	void exitParameterTypeList(SSCParser.ParameterTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void enterParameterList(SSCParser.ParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void exitParameterList(SSCParser.ParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#parameterDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterParameterDeclaration(SSCParser.ParameterDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#parameterDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitParameterDeclaration(SSCParser.ParameterDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierList(SSCParser.IdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierList(SSCParser.IdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterTypeName(SSCParser.TypeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitTypeName(SSCParser.TypeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#abstractDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterAbstractDeclarator(SSCParser.AbstractDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#abstractDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitAbstractDeclarator(SSCParser.AbstractDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#directAbstractDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterDirectAbstractDeclarator(SSCParser.DirectAbstractDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#directAbstractDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitDirectAbstractDeclarator(SSCParser.DirectAbstractDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#typedefName}.
	 * @param ctx the parse tree
	 */
	void enterTypedefName(SSCParser.TypedefNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#typedefName}.
	 * @param ctx the parse tree
	 */
	void exitTypedefName(SSCParser.TypedefNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#initializer}.
	 * @param ctx the parse tree
	 */
	void enterInitializer(SSCParser.InitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#initializer}.
	 * @param ctx the parse tree
	 */
	void exitInitializer(SSCParser.InitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#initializerList}.
	 * @param ctx the parse tree
	 */
	void enterInitializerList(SSCParser.InitializerListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#initializerList}.
	 * @param ctx the parse tree
	 */
	void exitInitializerList(SSCParser.InitializerListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#designation}.
	 * @param ctx the parse tree
	 */
	void enterDesignation(SSCParser.DesignationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#designation}.
	 * @param ctx the parse tree
	 */
	void exitDesignation(SSCParser.DesignationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#designatorList}.
	 * @param ctx the parse tree
	 */
	void enterDesignatorList(SSCParser.DesignatorListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#designatorList}.
	 * @param ctx the parse tree
	 */
	void exitDesignatorList(SSCParser.DesignatorListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#designator}.
	 * @param ctx the parse tree
	 */
	void enterDesignator(SSCParser.DesignatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#designator}.
	 * @param ctx the parse tree
	 */
	void exitDesignator(SSCParser.DesignatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#staticAssertDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterStaticAssertDeclaration(SSCParser.StaticAssertDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#staticAssertDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitStaticAssertDeclaration(SSCParser.StaticAssertDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(SSCParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(SSCParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#labeledStatement}.
	 * @param ctx the parse tree
	 */
	void enterLabeledStatement(SSCParser.LabeledStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#labeledStatement}.
	 * @param ctx the parse tree
	 */
	void exitLabeledStatement(SSCParser.LabeledStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#compoundStatement}.
	 * @param ctx the parse tree
	 */
	void enterCompoundStatement(SSCParser.CompoundStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#compoundStatement}.
	 * @param ctx the parse tree
	 */
	void exitCompoundStatement(SSCParser.CompoundStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#blockItemList}.
	 * @param ctx the parse tree
	 */
	void enterBlockItemList(SSCParser.BlockItemListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#blockItemList}.
	 * @param ctx the parse tree
	 */
	void exitBlockItemList(SSCParser.BlockItemListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#blockItem}.
	 * @param ctx the parse tree
	 */
	void enterBlockItem(SSCParser.BlockItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#blockItem}.
	 * @param ctx the parse tree
	 */
	void exitBlockItem(SSCParser.BlockItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionStatement(SSCParser.ExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionStatement(SSCParser.ExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#selectionStatement}.
	 * @param ctx the parse tree
	 */
	void enterSelectionStatement(SSCParser.SelectionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#selectionStatement}.
	 * @param ctx the parse tree
	 */
	void exitSelectionStatement(SSCParser.SelectionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void enterIterationStatement(SSCParser.IterationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void exitIterationStatement(SSCParser.IterationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#forCondition}.
	 * @param ctx the parse tree
	 */
	void enterForCondition(SSCParser.ForConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#forCondition}.
	 * @param ctx the parse tree
	 */
	void exitForCondition(SSCParser.ForConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#forDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterForDeclaration(SSCParser.ForDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#forDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitForDeclaration(SSCParser.ForDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#forExpression}.
	 * @param ctx the parse tree
	 */
	void enterForExpression(SSCParser.ForExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#forExpression}.
	 * @param ctx the parse tree
	 */
	void exitForExpression(SSCParser.ForExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#jumpStatement}.
	 * @param ctx the parse tree
	 */
	void enterJumpStatement(SSCParser.JumpStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#jumpStatement}.
	 * @param ctx the parse tree
	 */
	void exitJumpStatement(SSCParser.JumpStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompilationUnit(SSCParser.CompilationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompilationUnit(SSCParser.CompilationUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#translationUnit}.
	 * @param ctx the parse tree
	 */
	void enterTranslationUnit(SSCParser.TranslationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#translationUnit}.
	 * @param ctx the parse tree
	 */
	void exitTranslationUnit(SSCParser.TranslationUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#externalDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterExternalDeclaration(SSCParser.ExternalDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#externalDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitExternalDeclaration(SSCParser.ExternalDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDefinition(SSCParser.FunctionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDefinition(SSCParser.FunctionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SSCParser#declarationList}.
	 * @param ctx the parse tree
	 */
	void enterDeclarationList(SSCParser.DeclarationListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SSCParser#declarationList}.
	 * @param ctx the parse tree
	 */
	void exitDeclarationList(SSCParser.DeclarationListContext ctx);
}