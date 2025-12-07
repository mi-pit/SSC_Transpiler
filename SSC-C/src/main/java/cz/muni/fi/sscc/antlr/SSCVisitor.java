package cz.muni.fi.sscc.antlr; /* added to project in `./setup.sh` */

// Generated from SSC.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SSCParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SSCVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SSCParser#compoundStringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompoundStringLiteral(SSCParser.CompoundStringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpression(SSCParser.PrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#genericSelection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericSelection(SSCParser.GenericSelectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#genericAssocList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericAssocList(SSCParser.GenericAssocListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#genericAssociation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericAssociation(SSCParser.GenericAssociationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#postfixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixExpression(SSCParser.PostfixExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#argumentExpressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentExpressionList(SSCParser.ArgumentExpressionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpression(SSCParser.UnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#unaryOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOperator(SSCParser.UnaryOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#castExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCastExpression(SSCParser.CastExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(SSCParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(SSCParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#shiftExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShiftExpression(SSCParser.ShiftExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpression(SSCParser.RelationalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#equalityExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpression(SSCParser.EqualityExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#andExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpression(SSCParser.AndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#exclusiveOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExclusiveOrExpression(SSCParser.ExclusiveOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#inclusiveOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInclusiveOrExpression(SSCParser.InclusiveOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalAndExpression(SSCParser.LogicalAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOrExpression(SSCParser.LogicalOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#conditionalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionalExpression(SSCParser.ConditionalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#assignmentExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentExpression(SSCParser.AssignmentExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#assignmentOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentOperator(SSCParser.AssignmentOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(SSCParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#constantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantExpression(SSCParser.ConstantExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration(SSCParser.DeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#declarationSpecifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarationSpecifiers(SSCParser.DeclarationSpecifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#declarationSpecifiers2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarationSpecifiers2(SSCParser.DeclarationSpecifiers2Context ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#declarationSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarationSpecifier(SSCParser.DeclarationSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#initDeclaratorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitDeclaratorList(SSCParser.InitDeclaratorListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#initDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitDeclarator(SSCParser.InitDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#storageClassSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorageClassSpecifier(SSCParser.StorageClassSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#typeSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeSpecifier(SSCParser.TypeSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#superStructSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#superStructBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperStructBody(SSCParser.SuperStructBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#superStructMember}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperStructMember(SSCParser.SuperStructMemberContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#structOrUnionSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructOrUnionSpecifier(SSCParser.StructOrUnionSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#structOrUnion}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructOrUnion(SSCParser.StructOrUnionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#structDeclarationList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructDeclarationList(SSCParser.StructDeclarationListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#structDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructDeclaration(SSCParser.StructDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#specifierQualifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecifierQualifierList(SSCParser.SpecifierQualifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#structDeclaratorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructDeclaratorList(SSCParser.StructDeclaratorListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#structDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructDeclarator(SSCParser.StructDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#enumSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumSpecifier(SSCParser.EnumSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#enumeratorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumeratorList(SSCParser.EnumeratorListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#enumerator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumerator(SSCParser.EnumeratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#enumerationConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumerationConstant(SSCParser.EnumerationConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#atomicTypeSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtomicTypeSpecifier(SSCParser.AtomicTypeSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#typeQualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeQualifier(SSCParser.TypeQualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#functionSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionSpecifier(SSCParser.FunctionSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#alignmentSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlignmentSpecifier(SSCParser.AlignmentSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarator(SSCParser.DeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#directDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectDeclarator(SSCParser.DirectDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#vcSpecificModifer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVcSpecificModifer(SSCParser.VcSpecificModiferContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#gccDeclaratorExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGccDeclaratorExtension(SSCParser.GccDeclaratorExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#gccAttributeSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGccAttributeSpecifier(SSCParser.GccAttributeSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#gccAttributeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGccAttributeList(SSCParser.GccAttributeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#gccAttribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGccAttribute(SSCParser.GccAttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#nestedParenthesesBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNestedParenthesesBlock(SSCParser.NestedParenthesesBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#pointer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer(SSCParser.PointerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#typeQualifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeQualifierList(SSCParser.TypeQualifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#parameterTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterTypeList(SSCParser.ParameterTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#parameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterList(SSCParser.ParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#parameterDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterDeclaration(SSCParser.ParameterDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#identifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierList(SSCParser.IdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(SSCParser.TypeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#abstractDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAbstractDeclarator(SSCParser.AbstractDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#directAbstractDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectAbstractDeclarator(SSCParser.DirectAbstractDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#typedefName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypedefName(SSCParser.TypedefNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializer(SSCParser.InitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#initializerList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializerList(SSCParser.InitializerListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#designation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDesignation(SSCParser.DesignationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#designatorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDesignatorList(SSCParser.DesignatorListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#designator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDesignator(SSCParser.DesignatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#staticAssertDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStaticAssertDeclaration(SSCParser.StaticAssertDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(SSCParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#labeledStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabeledStatement(SSCParser.LabeledStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#compoundStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompoundStatement(SSCParser.CompoundStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#blockItemList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockItemList(SSCParser.BlockItemListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#blockItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockItem(SSCParser.BlockItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#expressionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(SSCParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#selectionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectionStatement(SSCParser.SelectionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#iterationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIterationStatement(SSCParser.IterationStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#forCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForCondition(SSCParser.ForConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#forDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForDeclaration(SSCParser.ForDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#forExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForExpression(SSCParser.ForExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#jumpStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJumpStatement(SSCParser.JumpStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#compilationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilationUnit(SSCParser.CompilationUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#translationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTranslationUnit(SSCParser.TranslationUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#externalDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternalDeclaration(SSCParser.ExternalDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#functionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDefinition(SSCParser.FunctionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SSCParser#declarationList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarationList(SSCParser.DeclarationListContext ctx);
}