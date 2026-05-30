package cz.mipit.sscc.ssc.compiler.visitors.fmt;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.visitors.BaseConvertorVisitor;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FormattingConvertor extends BaseConvertorVisitor {
    @Override
    public void debugPrintDump() {
    }

    @Override
    public String visit(ParseTree node) {
        return visitDefault(node);
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
        return getIndent(level);
    }

    private String getIndent(int level) {
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
    public String visitErrorNode(ErrorNode node) {
        final String literal = getLiteral(node);

        Main.logger.printDebug("Error node: '" + literal + "'");
        return literal;
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
            return visit(ctx.staticAssertDeclaration());
        }
        if (ctx.attributeDeclaration() != null) {
            return visit(ctx.attributeDeclaration());
        }

        final String declSpecs = visit(ctx.declarationSpecifiers());
        if (ctx.initDeclaratorList() == null) {
            return getIndent() + declSpecs + ";\n";
        }

        final StringJoiner initDeclJoiner = new StringJoiner(", ", " ", "");
        for (final SSCParser.InitDeclaratorContext initDecl : ctx.initDeclaratorList().initDeclarator()) {
            initDeclJoiner.add(
                    visit(initDecl)
            );
        }

        return getIndent() + declSpecs + initDeclJoiner + ";\n";
    }

    // '|' '[' parameterTypeList ']' '|' '->' typeName lambdaAttributes? functionBody
    @Override
    public String visitLambdaFunction(SSCParser.LambdaFunctionContext ctx) {
        final String paramTypeLs = visit(ctx.parameterTypeList());
        final String typeName = visit(ctx.typeName());
        final String attrs = ctx.lambdaAttributes() != null ? " " + visit(ctx.lambdaAttributes()) : "";

        final SSCParser.FunctionBodyContext fnBodyCtx = ctx.functionBody();
        final String body = fnBodyCtx.compoundStatement().blockItemList() == null
                ? " " + visit(fnBodyCtx)
                : visit(fnBodyCtx);
        return String.format("|[ %s ]| -> %s%s%s", paramTypeLs, typeName, attrs, body);
    }

    // (Identifier | templateDispatch) '::' Identifier
    @Override
    public String visitPrimaryExpression(SSCParser.PrimaryExpressionContext ctx) {
        if (ctx.DoubleColon() == null) {
            return super.visitPrimaryExpression(ctx);
        }

        final String fstPart = visit(ctx.children.getFirst());
        final String lstPart = visit(ctx.children.getLast());

        return fstPart + visit(ctx.DoubleColon()) + lstPart;
    }

    @Override
    public String visitPostfixExpression(SSCParser.PostfixExpressionContext ctx) {
        if (ctx.primaryExpression() == null) {
            return super.visitPostfixExpression(ctx);
        }

        final String primary = visit(ctx.primaryExpression());

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
            final String converted = visit(tc);
            joiner.add(converted);
        }
        return joiner.toString();
    }

    @Override
    public String visitTemplateDispatch(SSCParser.TemplateDispatchContext ctx) {
        return visit(ctx.Identifier()) + visit(ctx.templateDispatchTypeArguments());
    }

    // Template '<' templateTypes '>'
    @Override
    public String visitTemplateHeader(SSCParser.TemplateHeaderContext ctx) {
        return "tmpl<" + visit(ctx.templateTypes()) + ">\n";
    }

    // Identifier (',' Identifier)*
    @Override
    public String visitTemplateTypes(SSCParser.TemplateTypesContext ctx) {
        return ctx.Identifier()
                .stream()
                .map(this::visit)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String visitTemplateDefinition(SSCParser.TemplateDefinitionContext ctx) {
        if (ctx.superStructSpecifier() == null) {
            return super.visitTemplateDefinition(ctx);
        }

        return visit(ctx.templateHeader()) + visit(ctx.superStructSpecifier()) + ";\n";
    }

    // Superstruct Identifier Interface '{' (functionHeader ';')+ '}'
    @Override
    public String visitSuperStructInterface(SSCParser.SuperStructInterfaceContext ctx) {
        final String ssTerminal = visit(ctx.Superstruct());
        final String ident = visit(ctx.Identifier());
        final String intfcTerminal = visit(ctx.Interface());
        return """
                %s %s %s
                %s
                """
                .formatted(
                        ssTerminal,
                        ident,
                        intfcTerminal,
                        getNamespaceBlock(() -> convertFunctionHeaderContexts(ctx.functionHeader()))
                );
    }

    // attributeSpecifierSequence? declarationSpecifiers? declarator
    private List<String> convertFunctionHeaderContexts(List<SSCParser.FunctionHeaderContext> functionHeaderContexts) {
        enum SSCSpecifier {
            NONE,
            PURE, PRIVATE, STATIC,
            ;

            static SSCSpecifier from(SSCParser.DeclarationSpecifierContext declSpec) {
                if (declSpec.superstructMemberDeclarationSpecifier() == null) {
                    return NONE;
                }

                final SSCParser.SuperstructMemberDeclarationSpecifierContext fnSpec =
                        declSpec.superstructMemberDeclarationSpecifier();
                if (fnSpec.StaticFunction() != null) {
                    return STATIC;
                }
                if (fnSpec.Pure() != null) {
                    return PURE;
                }
                if (fnSpec.Private() != null) {
                    return PRIVATE;
                }

                return NONE;
            }

            static SSCSpecifier from(SSCParser.FunctionHeaderContext functionHeaderContext) {
                if (functionHeaderContext.declarationSpecifiers() == null) {
                    return NONE;
                }
                final var declSpecsCtx = functionHeaderContext.declarationSpecifiers();
                // declarationSpecifier+
                if (declSpecsCtx.declarationSpecifier().isEmpty()) {
                    return NONE;
                }

                for (SSCParser.DeclarationSpecifierContext declSpecCtx : declSpecsCtx.declarationSpecifier()) {
                    final SSCSpecifier s = from(declSpecCtx);
                    if (s != NONE) {
                        return s;
                    }
                }
                return NONE;
            }
        }

        SSCSpecifier prevSpecifier = null;
        final List<String> res = new ArrayList<>();
        for (final SSCParser.FunctionHeaderContext fnHeader : functionHeaderContexts) {

            final SSCSpecifier curr = SSCSpecifier.from(fnHeader);
            if (prevSpecifier != null && curr != prevSpecifier) {
                res.add("\n");
            }
            prevSpecifier = curr;

            res.add(getIndent() + visit(fnHeader) + ";\n");
        }
        return res;
    }

    // Superstruct Identifier '{' superStructBody '}'
    @Override
    public String visitSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx) {
        final String ssTerminal = visit(ctx.Superstruct());
        final String spec = ssTerminal + " " + visit(ctx.Identifier());
        if (ctx.superStructBody() == null) {
            return spec;
        }
        return spec + "\n" + visit(ctx.superStructBody());
    }

    // superStructMember+
    @Override
    public String visitSuperStructBody(SSCParser.SuperStructBodyContext ctx) {
        return getNamespaceBlock(() -> ctx.superStructMember()
                .stream()
                .map(this::visit)
                .toList()
        );
    }

    @Override
    public String visitSuperStructMember(SSCParser.SuperStructMemberContext ctx) {
        if (ctx.functionDefinition() == null) {
            return super.visitSuperStructMember(ctx);
        }

        return "\n" + getIndent() + visit(ctx.functionDefinition());
    }

    @Override
    public String visitFunctionDefinition(SSCParser.FunctionDefinitionContext ctx) {
        return super.visitFunctionDefinition(ctx) + "\n";
    }

    // FlagsSet attributeSpecifierSequence? gnuAttributes? Identifier? '{' flagsInitializerList ','? '}'
    @Override
    public String visitFlagsSpecifier(SSCParser.FlagsSpecifierContext ctx) {
        if (ctx.flagsInitializerList() == null) {
            return super.visitFlagsSpecifier(ctx);
        }

        final String key = visit(ctx.FlagsSet());

        return key + " " + (ctx.Identifier() == null ? "" : visit(ctx.Identifier()) + " ")
               + getIndentedBlock(() -> visit(ctx.flagsInitializerList()));
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
            sBuilder.add(visit(ctx.attributeSpecifierSequence()));
        }
        if (ctx.gnuAttributes() != null) {
            sBuilder.add(visit(ctx.gnuAttributes()));
        }
        if (ctx.Identifier() != null) {
            sBuilder.add(visit(ctx.Identifier()));
        }
        if (ctx.Colon() != null) {
            assert ctx.typeName() != null;
            sBuilder.add(visit(ctx.Colon()));
            sBuilder.add(visit(ctx.typeName()));
        }
        if (ctx.enumTypeSpecifier() != null) {
            sBuilder.add(visit(ctx.enumTypeSpecifier()));
        }
        final String block = getIndentedBlock(() -> visit(ctx.enumeratorList()));

        return sBuilder + block;
    }

    // enumerator (',' enumerator)*
    @Override
    public String visitEnumeratorList(SSCParser.EnumeratorListContext ctx) {
        final StringBuilder sBuilder = new StringBuilder();
        for (final SSCParser.EnumeratorContext c : ctx.enumerator()) {
            sBuilder.append(getIndent())
                    .append(visit(c))
                    .append(",\n");
        }
        return sBuilder.toString();
    }

    // flagsInitializer (',' flagsInitializer)*
    @Override
    public String visitFlagsInitializerList(SSCParser.FlagsInitializerListContext ctx) {
        final StringBuilder builder = new StringBuilder();
        for (final SSCParser.FlagsInitializerContext fic : ctx.flagsInitializer()) {
            final String c = visit(fic);

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
            final String ident = visit(ctx.Identifier());

            if (ctx.Colon() != null) {
                // Identifier ':' DigitSequence
                fst = ident + " : " + visit(ctx.DigitSequence());
                lastChild = 3;
            } else if (ctx.vcSpecificModifer() != null) {
                // vcSpecificModifer Identifier
                fst = visit(ctx.vcSpecificModifer()) + " " + ident;
                lastChild = 2;
            } else {
                // Identifier attributeSpecifierSequence?
                final String att;
                if (ctx.children.size() > 1 && ctx.children.get(1) instanceof SSCParser.AttributeSpecifierSequenceContext ass) {
                    lastChild = 2;
                    att = visit(ass);
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
                sub = visit(ctx.vcSpecificModifer()) + visit(ctx.declarator());
                lastChild = 4;
            } else {
                // '(' declarator ')'
                sub = visit(ctx.declarator());
                lastChild = 3;
            }
            fst = "( " + sub + " )";
        } else {
            // gnuAttribute
            assert ctx.gnuAttribute() != null;
            fst = visit(ctx.gnuAttribute());
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
                visit(ctx.initializerList()) + (ctx.Comma() != null ? "," : "") + "\n"
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
                        .append(visit(designation))
                        .append(" ");
            } else if (child instanceof SSCParser.InitializerContext initializer) {
                builder.append(
                        visit(initializer)
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

        return "." + visit(ctx.Identifier());
    }

    // '_Static_assert' '(' constantExpression (',' StringLiteral)? ')' ';'
    @Override
    public String visitStaticAssertDeclaration(SSCParser.StaticAssertDeclarationContext ctx) {
        final String constantExpression = visit(ctx.constantExpression());
        final String strLiteral = ctx.StringLiteral() == null ? ", " + visit(ctx.StringLiteral()) : "";
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
            return s;
        }

        final StringBuilder builder = new StringBuilder(s);
        builder.deleteCharAt(builder.length() - 3);

        return builder.toString();
    }

    @Override
    public String visitCompoundStatement(SSCParser.CompoundStatementContext ctx) {
        if (ctx.blockItemList() == null) {
            return visit(ctx.LeftBrace()) + visit(ctx.RightBrace());
        }

        return getIndentedBlock(() -> visit(ctx.blockItemList()));
    }

    // TODO: make configurable
    private static final int LIMIT_LINES = 4;
    private static final int LIMIT_CHARS = 80;

    // blockItem+
    @Override
    public String visitBlockItemList(SSCParser.BlockItemListContext ctx) {
        int prevLines = 0;
        int prevChars = 0;
        boolean justLinebroke = false;

        final StringBuilder builder = new StringBuilder();

        final List<SSCParser.BlockItemContext> blockItems = ctx.blockItem();
        for (int i = 0; i < blockItems.size(); i++) {
            final SSCParser.BlockItemContext blockItemContext = blockItems.get(i);

            final String blockItemString = visit(blockItemContext);
            final int nLines = (int) blockItemString.lines().count();
            final int nChars = blockItemString.length();

            final boolean linebreakBefore = i != 0
                                            && !justLinebroke
                                            && (lineTooLongOrTooManyLines(prevLines, prevChars)
                                                || lineTooLongOrTooManyLines(nLines, nChars));
            final boolean linebreakAfter_ = i != blockItems.size() - 1
                                            && lineTooLongOrTooManyLines(nLines, nChars);

            if (linebreakBefore) {
                builder.append("\n");
            }
            builder.append(blockItemString);
            if (linebreakAfter_) {
                builder.append("\n");
            }

            justLinebroke = linebreakAfter_;
            prevLines = nLines;
            prevChars = nChars;
        }

        return builder.toString();
    }

    private static boolean lineTooLongOrTooManyLines(final int nLines, final int nChars) {
        assert nLines >= 0;
        if (nLines == 0)
            return false;

        return nChars / nLines > LIMIT_CHARS || nLines > LIMIT_LINES;
    }

    @Override
    public String visitStructOrUnionSpecifier(SSCParser.StructOrUnionSpecifierContext ctx) {
        if (ctx.memberDeclarationList() == null) {
            return super.visitStructOrUnionSpecifier(ctx);
        }

        final String structOrUnion = visit(ctx.structOrUnion());
        final String attributes = ctx.attributeSpecifierSequence() != null
                ? " " + visit(ctx.attributeSpecifierSequence())
                : "";
        final String gnuAttributes = ctx.gnuAttributes() != null
                ? " " + visit(ctx.gnuAttributes())
                : "";
        final String identifier = ctx.Identifier() != null
                ? " " + visit(ctx.Identifier())
                : "";

        final String block = getIndentedBlock(() -> visit(ctx.memberDeclarationList()));

        return structOrUnion + attributes + gnuAttributes + identifier + block;
    }

    @Override
    public String visitMemberDeclaration(SSCParser.MemberDeclarationContext ctx) {
        if (ctx.staticAssertDeclaration() != null) {
            return visit(ctx.staticAssertDeclaration());
        }

        if (ctx.memberDeclaration() != null) {
            return visit(ctx.KW__extension__()) + " " + visit(ctx.memberDeclaration());
        }

        final String attributes = ctx.attributeSpecifierSequence() != null
                ? visit(ctx.attributeSpecifierSequence()) + " "
                : "";
        final String specifierQualifierList = visit(ctx.specifierQualifierList());
        final String memberDeclList = ctx.memberDeclaratorList() != null
                ? " " + visit(ctx.memberDeclaratorList())
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
        final String paramList = visit(ctx.parameterList());
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
            joiner.add(visit(p));
        }

        return joiner.toString();
    }

    // assignmentExpression (',' assignmentExpression)*
    @Override
    public String visitArgumentExpressionList(SSCParser.ArgumentExpressionListContext ctx) {
        final StringJoiner joiner = new StringJoiner(", ");

        for (SSCParser.AssignmentExpressionContext a : ctx.assignmentExpression()) {
            joiner.add(visit(a));
        }

        return joiner.toString();
    }

    private boolean EXT_justLinebroke = false;

    @Override
    public String visitExternalDeclaration(SSCParser.ExternalDeclarationContext ctx) {
        final String s = super.visitExternalDeclaration(ctx);

        final int nLines = (int) s.lines().count();

        if (nLines > LIMIT_LINES) {
            final String linebreakBefore = EXT_justLinebroke ? "" : "\n";

            EXT_justLinebroke = true;
            return linebreakBefore + s + "\n";
        }

        EXT_justLinebroke = false;
        return s;
    }

    // Attribute '(' '(' gnuAttributeList ')' ')'
    @Override
    public String visitGnuAttribute(SSCParser.GnuAttributeContext ctx) {
        final String attr = visit(ctx.Attribute());
        final String list = visit(ctx.gnuAttributeList());

        return "%s((%s))".formatted(attr, list);
    }

    @Override
    public String visitExpressionStatement(SSCParser.ExpressionStatementContext ctx) {
        return getIndent() + super.visitExpressionStatement(ctx) + "\n";
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
        return getIndent() + visitSelectionStatement_(ctx) + "\n";
    }

    private String visitSelectionStatement_(SSCParser.SelectionStatementContext ctx) {
        if (ctx.statement().getFirst().compoundStatement() != null) {
            return super.visitSelectionStatement(ctx);
        }

        if (!(ctx.children.getFirst() instanceof TerminalNode t)) {
            throw new AssertionError();
        }

        final String selectionKeyword = visit(t);
        final String expression = visit(ctx.expression());
        level++;
        final String statement = visit(ctx.statement().getFirst());
        level--;

        if (ctx.Else() == null) {
            return selectionKeyword + " ( " + expression + " )\n" + statement;
        }

        assert ctx.statement().size() == 2;

        final String elseKeyword = visit(ctx.Else());

        final SSCParser.StatementContext elseStatementCtx = ctx.statement().getLast();

        final boolean elif = elseStatementCtx.selectionStatement() != null && elseStatementCtx.selectionStatement().If() != null;
        final boolean compound = elseStatementCtx.compoundStatement() != null;
        final boolean shouldIndent = !elif && !compound;
        if (shouldIndent)
            level++;
        final String linebreak = !shouldIndent ? "" : "\n";

        String elseStatement = visit(elseStatementCtx);
        if (elif) {
            elseStatement = " " + elseStatement.strip();
        }

        if (shouldIndent)
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
        return getIndent() + super.visitJumpStatement(ctx) + "\n";
    }

    //labeledStatement
    //    : Identifier ':' statement?
    //    | Label Identifier ';'
    //    | 'case' constantExpression ':' statement
    //    | 'default' ':' statement
    //    ;
    @Override
    public String visitLabeledStatement(SSCParser.LabeledStatementContext ctx) {
        assert level > 0;

        level--;
        final String s = getIndent() + visitLabeledStatement_(ctx);
        level++;

        return s;
    }

    private String visitLabeledStatement_(SSCParser.LabeledStatementContext ctx) {
        final TerminalNode colonNode = ctx.Colon();
        if (colonNode == null) {
            // Label Identifier ';'
            return visit(ctx.Label()) + " " + visit(ctx.Identifier()) + ";";
        }

        final String colon = visit(colonNode);

        final SSCParser.StatementContext statementCtx = ctx.statement();

        final String statementFormatted;
        if (statementCtx != null) {
            level++;
            final String statementLiteral = visit(statementCtx);
            statementFormatted = statementCtx.compoundStatement() != null
                    ? " " + statementLiteral
                    : "\n" + statementLiteral;
            level--;
        } else {
            statementFormatted = "";
        }

        if (ctx.Identifier() != null) {
            // Identifier ':' statement?
            final String identifier = visit(ctx.Identifier());
            return identifier + colon + statementFormatted;
        }

        if (ctx.Case() != null) {
            // 'case' constantExpression ':' statement
            final String constantExpression = visit(ctx.constantExpression());
            return visit(ctx.Case()) + " " + constantExpression + colon + statementFormatted;
        }

        // 'default' ':' statement
        final String default_ = visit(ctx.Default());

        return default_ + colon + statementFormatted;
    }

    // logicalOrExpression (Question expression Colon conditionalExpression)?
    // 'if' logicalOrExpression 'then' expression 'else' conditionalExpression
    @Override
    public String visitConditionalExpression(SSCParser.ConditionalExpressionContext ctx) {
        return condExprHelper(ctx);
    }

    private String condExprHelper(final SSCParser.ConditionalExpressionContext orig) {
        final List<String> ifs = new ArrayList<>();
        final List<String> thens = new ArrayList<>();
        final List<String> elses = new ArrayList<>();
        final List<String> conds = new ArrayList<>();
        final List<String> exprs = new ArrayList<>();
        String last = null;

        for (SSCParser.ConditionalExpressionContext ctx = orig; ctx != null; ctx = ctx.conditionalExpression()) {
            if (ctx.conditionalExpression() == null) {
                last = visit(ctx.logicalOrExpression());
                continue;
            }

            ifs.add(
                    ctx.If() != null ? visit(ctx.If()) : ""
            );
            thens.add(
                    ctx.Then() != null ? visit(ctx.Then()) : visit(ctx.Question())
            );
            elses.add(
                    ctx.Else() != null ? visit(ctx.Else()) : visit(ctx.Colon())
            );

            conds.add(
                    visit(ctx.logicalOrExpression())
            );
            exprs.add(
                    visit(ctx.expression())
            );
        }
        assert conds.size() == exprs.size();
        assert last != null;

        if (conds.isEmpty()) {
            // logicalOrExpression
            return last;
        }
        if (conds.size() == 1) {
            return String.format("%s %s %s %s %s %s",
                    ifs.getFirst(), conds.getFirst(),
                    thens.getFirst(), exprs.getFirst(),
                    elses.getFirst(), last
            );
        }

        assert !conds.isEmpty();
        final boolean padIfThenElses = ifs.stream().anyMatch(s -> !s.isBlank());

        final int maxCondLength = conds.stream().mapToInt(String::length).max().getAsInt();

        final StringBuilder builder = new StringBuilder(
                "\n" + getIndent(level + 1) + " "
        );
        for (int i = 0; i < conds.size(); i++) {
            final boolean isFirst = i == 0;
            final boolean isLast = i == conds.size() - 1;

            String if_ = ifs.get(i);
            if (padIfThenElses && if_.isEmpty()) {
                if_ = "  ";
            }

            String then = thens.get(i);
            if (padIfThenElses && "?".equals(then)) {
                then += "   ";
            }

            String else_ = elses.get(i);
            if (padIfThenElses && ":".equals(else_)) {
                else_ += "   ";
            }

            final String fstPad = isFirst ? " ".repeat(else_.length()) : "";

            final String condition = conds.get(i);
            final String conditionPadding = " ".repeat(maxCondLength - condition.length());

            final String expression = exprs.get(i);

            final int constantStuffLength = (else_ + " " + if_ + "  ").length();

            final String elseIndent = getIndent(level + 1)
                                      + (!isLast ? "" : " ".repeat(maxCondLength + constantStuffLength));

            final String e = fstPad + if_ + " " + condition + conditionPadding + " "
                             + then + " " + expression + "\n"
                             + elseIndent + else_ + " ";
            builder.append(e);
        }
        builder.append(last);

        return builder.toString();
    }
}
