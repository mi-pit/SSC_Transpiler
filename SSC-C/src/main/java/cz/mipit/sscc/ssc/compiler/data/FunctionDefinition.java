package cz.mipit.sscc.ssc.compiler.data;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.exceptions.SSCSyntaxException;
import cz.mipit.sscc.ssc.exceptions.UnknownTranspilationException;
import cz.mipit.sscc.util.ContextText;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.List;

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

    private FunctionDefinition(List<String> specs,
                               boolean isStatic,
                               boolean isPure,
                               boolean isPrivate,
                               String type,
                               String name,
                               List<String> args,
                               List<String> statements,
                               String superstructMemberOfName) {
        this.specs = specs;
        this.isStatic = isStatic;
        this.isPure = isPure;
        this.isPrivate = isPrivate;
        this.type = type;
        this.name = name;
        this.args = args;
        this.statements = statements;
        this.superstructMemberOfName = superstructMemberOfName;
    }

    public static FunctionDefinition fromSemiParsedContext(final boolean isStatic,
                                                           final boolean isPure,
                                                           final boolean isPrivate,
                                                           final List<String> specsWithoutCustom,
                                                           final SSCParser.FunctionDefinitionContext ctx,
                                                           final CommonTokenStream tokens,
                                                           final String superstructMemberOfName) {
        if (isStatic && isPure) {
            throw new SSCSyntaxException("Method may not be both `static` and `pure`", ctx, tokens);
        }

        return new FunctionDefinition(
                specsWithoutCustom, isStatic, isPure, isPrivate,
                parseType(ctx.declarationSpecifiers(), ctx.declarator(), tokens),
                parseName(ctx.declarator(), tokens),
                parseFunctionArgs(ctx.declarator(), tokens),
                parseFunctionBody(ctx.compoundStatement(), tokens),
                superstructMemberOfName
        );
    }

    public static String parseType(SSCParser.DeclarationSpecifiersContext declSpecs,
                                   SSCParser.DeclaratorContext decl,
                                   CommonTokenStream tokens) {
        List<String> builder = new ArrayList<>();

        for (var spec : declSpecs.declarationSpecifier()) {
            if (spec.typeSpecifier() != null) {
                builder.add(ContextText.getLiteral(spec.typeSpecifier(), tokens));
            }
        }

        if (decl.pointer() != null) {
            builder.add(ContextText.getLiteral(decl.pointer(), tokens));
        }

        return String.join(" ", builder);
    }

    public static String parseName(SSCParser.DeclaratorContext ctx, CommonTokenStream tokens) {
        var directDecl = ctx.directDeclarator();
        if (directDecl == null) {
            throw new SSCSyntaxException("Direct declarator is null", ctx, tokens);
        }

        if (directDecl.Identifier() == null && (directDecl.LeftParen() == null || directDecl.RightParen() == null)) {
            return ContextText.getLiteral(ctx, tokens);
        }

        if (directDecl.Identifier() != null) {
            return directDecl.Identifier().getText();
        }

        if (directDecl.LeftParen() == null) {
            // How could this be parsed as a function definition?
            throw new UnknownTranspilationException("Parser \"found\" function definition without parentheses", ctx, tokens);
        }

        if (directDecl.RightParen() == null) {
            throw new SSCSyntaxException("Direct declarator has left parenthesis, but not a matching right one", ctx, tokens);
        }
        if (directDecl.directDeclarator() == null) {
            throw new SSCSyntaxException("Missing direct declarator (perhaps missing a variable name?)", directDecl, tokens);
        }

        return ContextText.getLiteral(directDecl.directDeclarator(), tokens);
    }

    private static List<String> parseFunctionArgs(final SSCParser.DeclaratorContext ctx,
                                                  final CommonTokenStream tokens) {
        final List<String> args = new ArrayList<>();
        if (ctx.directDeclarator().parameterTypeList() == null) {
            /* function declaration without a prototype -- let cc deal with it */
            return args;
        }
        for (var param : ctx.directDeclarator().parameterTypeList().parameterList().parameterDeclaration()) {
            args.add(ContextText.getLiteral(param, tokens));
        }

        if (ctx.directDeclarator().parameterTypeList().Ellipsis() != null) {
            args.add("...");
        }

        return args;
    }

    private static List<String> parseFunctionBody(SSCParser.CompoundStatementContext ctx, CommonTokenStream tokens) {
        List<String> statements = new ArrayList<>();
        for (var statement : ctx.blockItemList().blockItem()) {
            statements.add(ContextText.getLiteral(statement, tokens));
        }
        return statements;
    }

    public String getDeclaration() {
        return getDeclaration(false) + ";\n";
    }

    public String getDefinition() {
        return getDeclaration(true) + " " + getBody();
    }

    private String getDeclaration(boolean willHaveBody) {
        assert !isStatic || !isPure;

        if (!isStatic && args.size() == 1 && args.get(0).equals("void")) {
            args.remove(0);
        }

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
                selfRef.append("const ");
            }
            selfRef.append("this");

            if (!args.isEmpty()) {
                selfRef.append(", ");
            }
        }

        final String specsString = String.join(" ", specs);

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
}
