package cz.mipit.sscc.ssc.compiler.data.ss;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionDefinition {
    private final List<String> specs;
    private final boolean isStatic;
    private final boolean isPure;
    private final boolean isPrivate;
    private final String type;
    private final String name;
    private final List<String> args;
    private final List<String> statements; /* body */
    private final String superstructMemberOfName;

    private final InputFile inputFile;
    private final CommonTokenStream tokens;

    private FunctionDefinition(
            final boolean isStatic,
            final boolean isPure,
            final boolean isPrivate,
            final List<String> specsWithoutCustom,
            final SSCParser.FunctionDefinitionContext ctx,
            final CommonTokenStream tokens,
            final String superstructMemberOfName,
            final InputFile currentFile
    ) {
        this.inputFile = currentFile;
        this.tokens = tokens;

        this.specs = specsWithoutCustom;
        this.isStatic = isStatic;
        this.isPure = isPure;
        this.isPrivate = isPrivate;
        this.type = parseType(ctx.declarationSpecifiers(), ctx.declarator(), tokens);
        this.name = parseName(ctx.declarator(), tokens, currentFile);
        this.args = parseFunctionArgs(ctx.declarator(), tokens);
        this.statements = parseFunctionBody(ctx.functionBody(), tokens);
        this.superstructMemberOfName = superstructMemberOfName;

        if (!isStatic && args.size() == 1 && args.get(0).equals("void")) {
            this.args.remove(0);
        }
    }

    public static FunctionDefinition fromSemiParsedContext(final boolean isStatic,
                                                           final boolean isPure,
                                                           final boolean isPrivate,
                                                           final List<String> specsWithoutCustom,
                                                           final SSCParser.FunctionDefinitionContext ctx,
                                                           final CommonTokenStream tokens,
                                                           final String superstructMemberOfName,
                                                           final InputFile currentFile) {
        if (isStatic && isPure) {
            throw new SSCSyntaxException("Method may not be both `static` and `pure`", ctx, tokens, currentFile);
        }

        return new FunctionDefinition(
                isStatic, isPure, isPrivate, specsWithoutCustom, ctx, tokens, superstructMemberOfName, currentFile
        );
    }

    public String parseType(SSCParser.DeclarationSpecifiersContext declSpecs,
                            SSCParser.DeclaratorContext decl,
                            CommonTokenStream tokens) {
        final List<String> builder = new ArrayList<>();

        for (var spec : declSpecs.declarationSpecifier()) {
            if (spec.typeSpecifier() != null) {
                builder.add(SSCCUtil.Text.getLiteral(spec.typeSpecifier(), tokens));
            }
        }

        if (decl.pointer() != null) {
            builder.add(decl
                    .pointer()
                    .stream()
                    .map(ptrCtx -> SSCCUtil.Text.getLiteral(ptrCtx, tokens))
                    .collect(Collectors.joining(" "))
            );
        }

        return String.join(" ", builder);
    }

    public static String parseName(SSCParser.DeclaratorContext ctx,
                                   CommonTokenStream tokens,
                                   InputFile currentFile) {
        var directDecl = ctx.directDeclarator();
        if (directDecl == null) {
            throw new SSCSyntaxException("Direct declarator is null", ctx, tokens, currentFile);
        }

        if (directDecl.Identifier() == null && (directDecl.LeftParen() == null || directDecl.RightParen() == null)) {
            return SSCCUtil.Text.getLiteral(ctx, tokens);
        }

        if (directDecl.Identifier() != null) {
            return directDecl.Identifier().getText();
        }

        assert directDecl.LeftParen() != null : "Parser \"found\" function definition without parentheses";

        if (directDecl.RightParen() == null) {
            throw new SSCSyntaxException("Direct declarator has left parenthesis, but not a matching right one",
                    ctx, tokens, currentFile);
        }
        if (directDecl.Identifier() == null) {
            throw new SSCSyntaxException("Missing declarator identifier (perhaps missing a variable name?)",
                    directDecl, tokens, currentFile);
        }

        return directDecl.Identifier().getText();
    }

    private List<String> parseFunctionArgs(final SSCParser.DeclaratorContext ctx,
                                           final CommonTokenStream tokens) {
        final List<String> args = new ArrayList<>();
        final var directDecl = ctx.directDeclarator();
        final List<SSCParser.ParameterTypeListContext> paramTypeList = directDecl.parameterTypeList();
        if (paramTypeList.size() > 1) {
            throw getException("Parameter type list has more than one parameter type", directDecl);
        }

        final SSCParser.ParameterTypeListContext paramType = paramTypeList.get(0);
        for (var param : paramType.parameterList().parameterDeclaration()) {
            final String paramStr = SSCCUtil.Text.getLiteral(param, tokens);
            if (!paramStr.isBlank())
                args.add(paramStr);
        }
        if (paramType.Ellipsis() != null) {
            args.add("...");
        }

        if (args.isEmpty()) {
            throw getException("Function declaration without a prototype", directDecl);
        }

        return args;
    }

    private static List<String> parseFunctionBody(SSCParser.FunctionBodyContext fb, CommonTokenStream tokens) {
        final var compoundStatement = fb.compoundStatement();
        if (compoundStatement.blockItemList() == null) {
            return List.of();
        }
        final List<String> statements = new ArrayList<>();
        for (var statement : compoundStatement.blockItemList().blockItem()) {
            statements.add(SSCCUtil.Text.getLiteral(statement, tokens));
        }
        return statements;
    }

    public String getDeclaration() {
        return getDeclaration(false) + ";";
    }

    public String getDefinition() {
        return getDeclaration(true) + " " + getBody();
    }

    private String getDeclaration(boolean willHaveBody) {
        final StringBuilder selfRef = new StringBuilder();
        if (!isStatic) {
            if (isPure) {
                selfRef.append("const ");
            }
            selfRef
                    .append("superstruct ")
                    .append(superstructMemberOfName)
                    .append(" *");

            if (willHaveBody) {
                selfRef.append("const this");
            }

            if (!args.isEmpty()) {
                selfRef.append(", ");
            }
        }

        final String specsString = "static " + String.join(" ", specs);

        return specsString
                + (specsString.isBlank() ? "" : " ")
                + type
                + " " + superstructMemberOfName + "__" + name
                + "(" + selfRef + String.join(", ", args) + ")";
    }

    private String getBody() {
        return "{\n" +
                "    " + String.join("\n    ", statements) + "\n" +
                "}\n";
    }


    public String getName() {
        return name;
    }

    public boolean isPrivate() {
        return isPrivate;
    }


    @Override
    public String toString() {
        return "FunctionDefinition{" + getDeclaration(true) + "}";
    }

    private SSCSyntaxException getException(String message, ParserRuleContext ctx) {
        return new SSCSyntaxException(message, ctx, tokens, inputFile);
    }
}
