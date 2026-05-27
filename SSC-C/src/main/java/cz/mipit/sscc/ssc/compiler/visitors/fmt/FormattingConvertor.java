package cz.mipit.sscc.ssc.compiler.visitors.fmt;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.visitors.BaseConvertorVisitor;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FormattingConvertor extends BaseConvertorVisitor {
    @Override
    public void debugPrintDump() {
        Main.logger.printDebug("Formatting. Nothing to dump.");
    }

    /*
     * declarator
     * parameterTypeList
     * argumentExpressionList
     * enumSpecifier
     *
     * ifs/loops
     * labels
     */

    private int level;

    private String getIndent() {
        return SSCCUtil.Text.INDENT.repeat(level);
    }

    private String getIndentedBlock(Supplier<String> s) {
        level++;
        final String block = s.get();
        level--;

        return "\n" + getIndent() + "{\n" + block + getIndent() + "}";
    }

    private String getNamespaceBlock(Supplier<List<String>> s) {
        assert level == 0;

        level++;

        final List<String> block = s.get();
        final StringJoiner joiner = new StringJoiner(
                "",
                "{\n",
                "}"
        );
        block.forEach(joiner::add);

        level--;

        return joiner.toString();
    }

    public FormattingConvertor(
            CommonTokenStream tokens,
            InputFile currentFile
    ) {
        super(tokens, currentFile);

        level = 0;
    }

    @Override
    public String visitChildren(RuleNode node) {
        return visitChildrenSimple(node);
    }

    private String visitChildrenSimple(RuleNode node) {
        final StringBuilder appendix = new StringBuilder();
        for (int i = 0; i < node.getChildCount(); i++) {
            final ParseTree child = node.getChild(i);

            final String childText;
            try {
                childText = visit(child);
            } catch (final SSCTranspilerException e) {
                Main.logger.printException(e);
                continue;
            }

            if (!appendix.isEmpty() &&
                !Character.isWhitespace(appendix.charAt(appendix.length() - 1))) {
                appendix.append(" ");
            }
            appendix.append(childText);
        }

        return appendix.toString();
    }


    @Override
    public String visitDeclaration(SSCParser.DeclarationContext ctx) {
        if (ctx.staticAssertDeclaration() != null) {
            return visitStaticAssertDeclaration(ctx.staticAssertDeclaration());
        }
        if (ctx.attributeDeclaration() != null) {
            return visitAttributeDeclaration(ctx.attributeDeclaration());
        }

        final String declSpecs = visitDeclarationSpecifiers(ctx.declarationSpecifiers());
        if (ctx.initDeclaratorList() == null) {
            return getIndent() + declSpecs + ";\n";
        }

        final StringJoiner initDeclJoiner = new StringJoiner(", ", " ", "");
        for (final SSCParser.InitDeclaratorContext initDecl : ctx.initDeclaratorList().initDeclarator()) {
            initDeclJoiner.add(
                    visitInitDeclarator(initDecl)
            );
        }

        return getIndent() + declSpecs + initDeclJoiner + ";\n";
    }

    // '|' '[' parameterTypeList ']' '|' '->' typeName lambdaAttributes? functionBody
    @Override
    public String visitLambdaFunction(SSCParser.LambdaFunctionContext ctx) {
        final String paramTypeLs = visitParameterTypeList(ctx.parameterTypeList());
        final String typeName = visitTypeName(ctx.typeName());
        final String attrs = ctx.lambdaAttributes() != null ? " " + visitLambdaAttributes(ctx.lambdaAttributes()) : "";
        return String.format("|[ %s ]| -> %s%s%s", paramTypeLs, typeName, attrs, visitFunctionBody(ctx.functionBody()));
    }

    // (Identifier | templateDispatch) '::' Identifier
    @Override
    public String visitPrimaryExpression(SSCParser.PrimaryExpressionContext ctx) {
        if (ctx.DoubleColon() == null) {
            return super.visitPrimaryExpression(ctx);
        }

        final String fstPart = visit(ctx.children.getFirst());
        final String lstPart = visit(ctx.children.getLast());

        return fstPart + "::" + lstPart;
    }

    @Override
    public String visitPostfixExpression(SSCParser.PostfixExpressionContext ctx) {
        if (ctx.primaryExpression() == null) {
            return super.visitPostfixExpression(ctx);
        }

        final String primary = visitPrimaryExpression(ctx.primaryExpression());

        final StringBuilder res = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < ctx.children.size(); i++) {
            if (first) {
                first = false;
                continue;
            }
            final ParseTree child = ctx.children.get(i);
            assert !(child instanceof SSCParser.PrimaryExpressionContext);

            if (child instanceof TerminalNode t &&
                (t.getSymbol().getType() == SSCParser.RightParen ||
                 t.getSymbol().getType() == SSCParser.RightBracket ||
                 t.getSymbol().getType() == SSCParser.RightBrace)
            ) {
                final int index = i - 1;
                assert index > 0;
                if (!(ctx.children.get(index) instanceof TerminalNode prevT)
                    || prevT.getSymbol().getType() != SSCParser.LeftParen) {
                    res.append(' ');
                }
            }

            res.append(visit(child));

            if (child instanceof TerminalNode t &&
                (t.getSymbol().getType() == SSCParser.LeftParen ||
                 t.getSymbol().getType() == SSCParser.LeftBracket ||
                 t.getSymbol().getType() == SSCParser.LeftBrace)
            ) {
                final int index = i + 1;
                assert index <= ctx.children.size();
                if (!(ctx.children.get(index) instanceof TerminalNode nextT)
                    || nextT.getSymbol().getType() != SSCParser.RightParen) {
                    res.append(' ');
                }
            }
        }

        return primary + res;
    }

    @Override
    public String visitTemplateDispatchTypeArguments(SSCParser.TemplateDispatchTypeArgumentsContext ctx) {
        // '<' typeArgument (',' typeArgument)* '>'
        final StringJoiner joiner = new StringJoiner(", ", "<", ">");
        for (final SSCParser.TypeArgumentContext tc : ctx.typeArgument()) {
            final String converted = visitTypeArgument(tc);
            joiner.add(converted);
        }
        return joiner.toString();
    }

    @Override
    public String visitTemplateDispatch(SSCParser.TemplateDispatchContext ctx) {
        return visitTerminal(ctx.Identifier()) + visitTemplateDispatchTypeArguments(ctx.templateDispatchTypeArguments());
    }

    // Template '<' templateTypes '>'
    @Override
    public String visitTemplateHeader(SSCParser.TemplateHeaderContext ctx) {
        return "tmpl<" + visitTemplateTypes(ctx.templateTypes()) + ">\n";
    }

    // Identifier (',' Identifier)*
    @Override
    public String visitTemplateTypes(SSCParser.TemplateTypesContext ctx) {
        return ctx.Identifier()
                .stream()
                .map(this::visitTerminal)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String visitTemplateDefinition(SSCParser.TemplateDefinitionContext ctx) {
        if (ctx.superStructSpecifier() == null) {
            return super.visitTemplateDefinition(ctx);
        }

        return visitTemplateHeader(ctx.templateHeader()) + visitSuperStructSpecifier(ctx.superStructSpecifier()) + ";\n";
    }

    // Superstruct Identifier Interface '{' (functionHeader ';')+ '}'
    @Override
    public String visitSuperStructInterface(SSCParser.SuperStructInterfaceContext ctx) {
        final String ssTerminal = visitTerminal(ctx.Superstruct());
        final String ident = visitTerminal(ctx.Identifier());
        final String intfcTerminal = visitTerminal(ctx.Interface());
        return """
                %s %s %s
                %s
                """
                .formatted(
                        ssTerminal,
                        ident,
                        intfcTerminal,
                        getNamespaceBlock(() -> ctx
                                .functionHeader()
                                .stream().map(fnHeader -> getIndent() + visitFunctionHeader(fnHeader) + ";\n")
                                .toList()
                        )
                );
    }

    // Superstruct Identifier '{' superStructBody '}'
    @Override
    public String visitSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx) {
        final String ssTerminal = visitTerminal(ctx.Superstruct());
        final String spec = ssTerminal + " " + visitTerminal(ctx.Identifier());
        if (ctx.superStructBody() == null) {
            return spec;
        }
        return spec + "\n" + visitSuperStructBody(ctx.superStructBody());
    }

    // superStructMember+
    @Override
    public String visitSuperStructBody(SSCParser.SuperStructBodyContext ctx) {
        return getNamespaceBlock(() -> ctx.superStructMember()
                .stream()
                .map(this::visitSuperStructMember)
                .toList()
        );
    }

    @Override
    public String visitSuperStructMember(SSCParser.SuperStructMemberContext ctx) {
        if (ctx.functionDefinition() != null) {
            return "\n" + getIndent() + visitFunctionDefinition(ctx.functionDefinition()) + "\n";
        }
        return getIndent() + super.visitSuperStructMember(ctx);
    }

    // FlagsSet attributeSpecifierSequence? gnuAttributes? Identifier? '{' flagsInitializerList ','? '}'
    @Override
    public String visitFlagsSpecifier(SSCParser.FlagsSpecifierContext ctx) {
        if (ctx.flagsInitializerList() == null) {
            return super.visitFlagsSpecifier(ctx);
        }

        final String key = visitTerminal(ctx.FlagsSet());

        return key + " " + (ctx.Identifier() == null ? "" : visit(ctx.Identifier()) + " ")
               + getIndentedBlock(() -> visitFlagsInitializerList(ctx.flagsInitializerList()));
    }

    // 'enum' attributeSpecifierSequence? gnuAttributes? Identifier? (':' typeName)?
    // enumTypeSpecifier? '{' enumeratorList ','? '}'
    @Override
    public String visitEnumSpecifier(SSCParser.EnumSpecifierContext ctx) {
        if (ctx.enumeratorList() == null) {
            return super.visitEnumSpecifier(ctx);
        }

        final StringJoiner sBuilder = new StringJoiner(" ");

        sBuilder.add("enum");
        if (ctx.attributeSpecifierSequence() != null) {
            sBuilder.add(visitAttributeSpecifierSequence(ctx.attributeSpecifierSequence()));
        }
        if (ctx.gnuAttributes() != null) {
            sBuilder.add(visitGnuAttributes(ctx.gnuAttributes()));
        }
        if (ctx.Identifier() != null) {
            sBuilder.add(visitTerminal(ctx.Identifier()));
        }
        if (ctx.Colon() != null) {
            assert ctx.typeName() != null;
            sBuilder.add(visitTerminal(ctx.Colon()));
            sBuilder.add(visitTypeName(ctx.typeName()));
        }
        if (ctx.enumTypeSpecifier() != null) {
            sBuilder.add(visitEnumTypeSpecifier(ctx.enumTypeSpecifier()));
        }
        final String block = getIndentedBlock(() -> visitEnumeratorList(ctx.enumeratorList()));

        return sBuilder + block;
    }

    // enumerator (',' enumerator)*
    @Override
    public String visitEnumeratorList(SSCParser.EnumeratorListContext ctx) {
        final StringBuilder sBuilder = new StringBuilder();
        for (final SSCParser.EnumeratorContext c : ctx.enumerator()) {
            sBuilder.append(getIndent())
                    .append(visitEnumerator(c))
                    .append(",\n");
        }
        return sBuilder.toString();
    }

    // flagsInitializer (',' flagsInitializer)*
    @Override
    public String visitFlagsInitializerList(SSCParser.FlagsInitializerListContext ctx) {
        final StringBuilder builder = new StringBuilder();
        for (final SSCParser.FlagsInitializerContext fic : ctx.flagsInitializer()) {
            final String c = visitFlagsInitializer(fic);

            builder
                    .append(getIndent())
                    .append(c)
                    .append(",\n");
        }

        return builder.toString();
    }

    // (gnuAttribute? pointer declarationSpecifiers?)* (gnuAttribute* directDeclarator gccDeclaratorExtension*)
    @Override
    public String visitDeclarator(SSCParser.DeclaratorContext ctx) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ctx.children.size(); i++) {
            final ParseTree child = ctx.children.get(i);
            final boolean isLast = i == (ctx.children.size() - 1);

            builder.append(visit(child));

            if (!(child instanceof SSCParser.PointerContext) && !isLast) {
                builder.append(" ");
            }
        }

        return builder.toString();
    }

    // (('*' | '^') typeQualifierList?)+
    @Override
    public String visitPointer(SSCParser.PointerContext ctx) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ctx.children.size(); i++) {
            final ParseTree child = ctx.children.get(i);
            builder.append(visit(child));
            if (!(child instanceof TerminalNode)) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }

    // (
    //      Identifier attributeSpecifierSequence?
    //      | '(' declarator ')'
    //      | Identifier ':' DigitSequence         // bit field
    //      | vcSpecificModifer Identifier         // Visual C Extension
    //      | '(' vcSpecificModifer declarator ')' // Visual C Extension
    //      | gnuAttribute
    // )
    // (
    //      '[' typeQualifierList? assignmentExpression? ']' attributeSpecifierSequence?
    //      | '[' 'static' typeQualifierList? assignmentExpression ']' attributeSpecifierSequence?
    //      | '[' typeQualifierList 'static' assignmentExpression ']' attributeSpecifierSequence?
    //      | '[' typeQualifierList? '*' ']' attributeSpecifierSequence?
    //      | '(' parameterTypeList ')' attributeSpecifierSequence?
    // )*
    @Override
    public String visitDirectDeclarator(SSCParser.DirectDeclaratorContext ctx) {
        final String fst;
        final int lastChild;
        if (ctx.Identifier() != null) {
            final String ident = visitTerminal(ctx.Identifier());

            if (ctx.Colon() != null) {
                // Identifier ':' DigitSequence
                fst = ident + " : " + visitTerminal(ctx.DigitSequence());
                lastChild = 3;
            } else if (ctx.vcSpecificModifer() != null) {
                // vcSpecificModifer Identifier
                fst = visitVcSpecificModifer(ctx.vcSpecificModifer()) + " " + ident;
                lastChild = 2;
            } else {
                // Identifier attributeSpecifierSequence?
                final String att;
                if (ctx.children.size() > 1 && ctx.children.get(1) instanceof SSCParser.AttributeSpecifierSequenceContext ass) {
                    lastChild = 2;
                    att = visitAttributeSpecifierSequence(ass);
                } else {
                    lastChild = 1;
                    att = "";
                }
                fst = ident + att;
            }
        } else if (ctx.declarator() != null) {
            final String sub;
            if (ctx.vcSpecificModifer() != null) {
                // '(' vcSpecificModifer declarator ')'
                sub = visitVcSpecificModifer(ctx.vcSpecificModifer()) + visitDeclarator(ctx.declarator());
                lastChild = 4;
            } else {
                // '(' declarator ')'
                sub = visitDeclarator(ctx.declarator());
                lastChild = 3;
            }
            fst = "( " + sub + " )";
        } else {
            // gnuAttribute
            assert ctx.gnuAttribute() != null;
            fst = visitGnuAttribute(ctx.gnuAttribute());
            lastChild = 1;
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(fst);

        for (int i = lastChild; i < ctx.children.size(); i++) {
            final ParseTree child = ctx.children.get(i);
            if (child instanceof TerminalNode t) {
                if (
                        (t.getSymbol().getType() == SSCParser.RightBracket)
                        || (t.getSymbol().getType() == SSCParser.RightParen
                            && parameterTypeListIsNotEmpty((SSCParser.ParameterTypeListContext) ctx.children.get(i - 1)))
                ) {
                    builder.append(" ");
                }
            }

            builder.append(visit(child));

            if (child instanceof TerminalNode t) {
                if (
                        (t.getSymbol().getType() == SSCParser.LeftBracket)
                        || (t.getSymbol().getType() == SSCParser.LeftParen
                            && parameterTypeListIsNotEmpty((SSCParser.ParameterTypeListContext) ctx.children.get(i + 1)))
                ) {
                    builder.append(" ");
                }
            }
        }

        return builder.toString();
    }

    private static boolean parameterTypeListIsNotEmpty(SSCParser.ParameterTypeListContext ctx) {
        final List<SSCParser.ParameterDeclarationContext> paramsDecls = ctx.parameterList().parameterDeclaration();
        return paramsDecls.size() > 1 || paramsDecls.getFirst().declarationSpecifiers() != null;
    }

    @Override
    public String visitUnaryExpression(SSCParser.UnaryExpressionContext ctx) {
        int nSkip = 0;
        final StringBuilder fstBuilder = new StringBuilder();
        for (int i = 0; i < ctx.children.size(); i++) {
            final ParseTree child = ctx.children.get(i);
            if (!(child instanceof TerminalNode t) ||
                (t.getSymbol().getType() != SSCParser.PlusPlus &&
                 t.getSymbol().getType() != SSCParser.MinusMinus &&
                 t.getSymbol().getType() != SSCParser.Sizeof)
            ) {
                break;
            }

            fstBuilder.append(visit(child));
            fstBuilder.append(' ');
            nSkip++;
        }

        final String fst = fstBuilder.toString();

        final String rest = ctx.children
                .stream()
                .skip(nSkip)
                .map(this::visit)
                .collect(Collectors.joining());

        return fst + rest;
    }

    @Override
    public String visitInitializer(SSCParser.InitializerContext ctx) {
        if (ctx.LeftBrace() == null) {
            return super.visitInitializer(ctx);
        }
        if (ctx.initializerList() == null) {
            return "{}";
        }

        return getIndentedBlock(() ->
                visitInitializerList(ctx.initializerList()) + (ctx.Comma() != null ? "," : "") + "\n"
        );
    }

    // designation? initializer (',' designation? initializer)*
    @Override
    public String visitInitializerList(SSCParser.InitializerListContext ctx) {
        final StringBuilder builder = new StringBuilder(getIndent());

        for (int i = 0; i < ctx.getChildCount(); i++) {
            final ParseTree child = ctx.getChild(i);

            if (child instanceof SSCParser.DesignationContext designation) {
                builder
                        .append(visitDesignation(designation))
                        .append(" ");
            } else if (child instanceof SSCParser.InitializerContext initializer) {
                builder.append(
                        visitInitializer(initializer)
                );
            } else if (child instanceof TerminalNode terminal) {
                assert terminal.getSymbol().getType() == SSCParser.Comma;
                builder
                        .append(",\n")
                        .append(getIndent());
            }
        }

        return builder.toString();
    }

    @Override
    public String visitDesignator(SSCParser.DesignatorContext ctx) {
        if (ctx.gnuArrayDesignator() != null) {
            return super.visitDesignator(ctx);
        }

        return "." + visitTerminal(ctx.Identifier());
    }

    // '_Static_assert' '(' constantExpression (',' StringLiteral)? ')' ';'
    @Override
    public String visitStaticAssertDeclaration(SSCParser.StaticAssertDeclarationContext ctx) {
        final String constantExpression = visitConstantExpression(ctx.constantExpression());
        final String strLiteral = ctx.StringLiteral() == null ? ", " + visitTerminal(ctx.StringLiteral()) : "";
        return "_Static_assert( " + constantExpression + strLiteral + " );\n";
    }

    @Override
    public String visitStatement(SSCParser.StatementContext ctx) {
        final String s = super.visitStatement(ctx);

        if (s.length() <= 3 ||
            s.charAt(s.length() - 1) != '\n' ||
            s.charAt(s.length() - 2) != ';' ||
            s.charAt(s.length() - 3) != ' '
        ) {
            return getIndent() + s;
        }

        final StringBuilder builder = new StringBuilder(s);
        builder.deleteCharAt(builder.length() - 3);

        return getIndent() + builder;
    }

    @Override
    public String visitCompoundStatement(SSCParser.CompoundStatementContext ctx) {
        if (ctx.blockItemList() == null) {
            return super.visitCompoundStatement(ctx);
        }

        return getIndentedBlock(() -> visitBlockItemList(ctx.blockItemList()));
    }

    // blockItem+
    @Override
    public String visitBlockItemList(SSCParser.BlockItemListContext ctx) {
        // TODO: make configurable
        final int LIMIT_LINES = 5;
        final int LIMIT_CHARS = 200;

        int prevLines = 0;
        int prevChars = 0;

        final StringBuilder builder = new StringBuilder();
        for (SSCParser.BlockItemContext blockItemContext : ctx.blockItem()) {
            if (prevChars > LIMIT_CHARS || prevLines > LIMIT_LINES) {
                //builder.append("\n");
            }

            final String blockItemString = visitBlockItem(blockItemContext);
            builder.append(blockItemString);

            final long nLines = blockItemString.lines().count();
            assert nLines <= Integer.MAX_VALUE;
            prevLines = (int) nLines;
            prevChars = blockItemString.length();
        }

        return builder.toString();
    }

    @Override
    public String visitStructOrUnionSpecifier(SSCParser.StructOrUnionSpecifierContext ctx) {
        if (ctx.memberDeclarationList() == null) {
            return super.visitStructOrUnionSpecifier(ctx);
        }

        final String structOrUnion = visitStructOrUnion(ctx.structOrUnion());
        final String attributes = ctx.attributeSpecifierSequence() != null
                ? " " + visitAttributeSpecifierSequence(ctx.attributeSpecifierSequence())
                : "";
        final String gnuAttributes = ctx.gnuAttributes() != null
                ? " " + visitGnuAttributes(ctx.gnuAttributes())
                : "";
        final String identifier = ctx.Identifier() != null
                ? " " + visitTerminal(ctx.Identifier())
                : "";

        final String block = getIndentedBlock(() -> visitMemberDeclarationList(ctx.memberDeclarationList()));

        return structOrUnion + attributes + gnuAttributes + identifier + block;
    }

    @Override
    public String visitMemberDeclaration(SSCParser.MemberDeclarationContext ctx) {
        if (ctx.staticAssertDeclaration() != null) {
            return visitStaticAssertDeclaration(ctx.staticAssertDeclaration());
        }

        if (ctx.memberDeclaration() != null) {
            return visitTerminal(ctx.KW__extension__()) + " " + visitMemberDeclaration(ctx.memberDeclaration());
        }

        final String attributes = ctx.attributeSpecifierSequence() != null
                ? visitAttributeSpecifierSequence(ctx.attributeSpecifierSequence()) + " "
                : "";
        final String specifierQualifierList = visitSpecifierQualifierList(ctx.specifierQualifierList());
        final String memberDeclList = ctx.memberDeclaratorList() != null
                ? " " + visitMemberDeclaratorList(ctx.memberDeclaratorList())
                : "";

        return getIndent() + attributes + specifierQualifierList + memberDeclList + ";\n";
    }

    // parameterList (',' '...')?
    // '...'
    @Override
    public String visitParameterTypeList(SSCParser.ParameterTypeListContext ctx) {
        if (ctx.parameterList() == null) {
            return super.visitParameterTypeList(ctx);
        }

        final StringJoiner joiner = new StringJoiner(", ");
        final String paramList = visitParameterList(ctx.parameterList());
        joiner.add(paramList);

        if (ctx.Ellipsis() != null) {
            joiner.add("...");
        }

        return joiner.toString();
    }

    // parameterDeclaration (',' parameterDeclaration)*
    @Override
    public String visitParameterList(SSCParser.ParameterListContext ctx) {
        final StringJoiner joiner = new StringJoiner(", ");
        for (final SSCParser.ParameterDeclarationContext p : ctx.parameterDeclaration()) {
            joiner.add(visitParameterDeclaration(p));
        }

        return joiner.toString();
    }

    @Override
    public String visitExternalDeclaration(SSCParser.ExternalDeclarationContext ctx) {
        return super.visitExternalDeclaration(ctx) + "\n";
    }

    // Attribute '(' '(' gnuAttributeList ')' ')'
    @Override
    public String visitGnuAttribute(SSCParser.GnuAttributeContext ctx) {
        final String attr = visitTerminal(ctx.Attribute());
        final String list = visitGnuAttributeList(ctx.gnuAttributeList());

        return attr + "(( " + list + " ))";
    }

    @Override
    public String visitExpressionStatement(SSCParser.ExpressionStatementContext ctx) {
        return super.visitExpressionStatement(ctx) + "\n";
    }

//iterationStatement
    //    : While '(' expression ')' statement
    //    | Do statement While '(' expression ')' ';'
    //    | For '(' forCondition ')' statement
    //    ;

    //selectionStatement
    //    : 'if' '(' expression ')' statement ('else' statement)?
    //    | 'switch' '(' expression ')' statement
    //    ;
    @Override
    public String visitSelectionStatement(SSCParser.SelectionStatementContext ctx) {
        if (ctx.statement().getFirst().compoundStatement() != null) {
            return super.visitSelectionStatement(ctx);
        }

        if (!(ctx.children.getFirst() instanceof TerminalNode t)) {
            throw new AssertionError();
        }

        final String selectionKeyword = visitTerminal(t);
        final String expression = visitExpression(ctx.expression());
        level++;
        final String statement = visitStatement(ctx.statement().getFirst());
        level--;

        if (ctx.Else() == null) {
            return String.format(
                    """
                            %s ( %s )
                            %s
                            """,
                    selectionKeyword, expression, statement
            );
        }

        assert ctx.statement().size() == 2;

        final String elseKeyword = visitTerminal(ctx.Else());

        final SSCParser.StatementContext elseStatementCtx = ctx.statement().getLast();
        final boolean elif = elseStatementCtx.selectionStatement() != null && elseStatementCtx.selectionStatement().If() != null;
        final boolean compound = elseStatementCtx.compoundStatement() != null;
        final boolean indent = !elif && !compound;
        if (indent)
            level++;
        final String linebreak = !indent ? "" : "\n";

        String elseStatement = visitStatement(elseStatementCtx);
        if (elif) {
            elseStatement = " " + elseStatement.strip();
        }

        if (indent)
            level--;

        return selectionKeyword + " ( " + expression + " )\n"
               + statement + getIndent() + elseKeyword + linebreak
               + elseStatement;
    }

    //jumpStatement
    //    : (
    //        'goto' Identifier
    //        | 'continue'
    //        | 'break'
    //        | 'return' expression?
    //        | 'goto' unaryExpression // GCC extension
    //    ) ';'
    @Override
    public String visitJumpStatement(SSCParser.JumpStatementContext ctx) {
        return super.visitJumpStatement(ctx) + "\n";
    }

    //labeledStatement
    //    : Identifier ':' statement?
    //    | Label Identifier ';'
    //    | 'case' constantExpression ':' statement
    //    | 'default' ':' statement
    //    ;
    @Override
    public String visitLabeledStatement(SSCParser.LabeledStatementContext ctx) {
        if (ctx.Colon() == null) {
            // Label Identifier ';'
            return visitTerminal(ctx.Label()) + " " + visitTerminal(ctx.Identifier()) + ";";
        }
        if (ctx.Identifier() != null) {
            // Identifier ':' statement?
            final String identifier = visitTerminal(ctx.Identifier());
            final String statement = ctx.statement() != null ? " " + visitStatement(ctx.statement()) : "";
            return identifier + ":" + statement;
        }

        return super.visitLabeledStatement(ctx);
    }
}
