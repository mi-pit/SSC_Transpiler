package cz.muni.fi.sscc.data;

import antlr.SSCParser;
import cz.muni.fi.sscc.exceptions.SSCSyntaxException;
import cz.muni.fi.sscc.exceptions.UnknownTranspilationException;
import cz.muni.fi.sscc.util.Strings;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record FunctionDefinition(List<String> specs,
                                 String type,
                                 String name,
                                 List<String> args,
                                 List<String> statements,
                                 String superstructMemberOf) {
    public FunctionDefinition(String ssName, SSCParser.FunctionDefinitionContext ctx, CommonTokenStream tokens) {
        this(
                parseFunctionSpecs(ctx.declarationSpecifiers(), tokens),
                parseType(ctx.declarationSpecifiers(), ctx.declarator(), tokens),
                parseName(ctx.declarator(), tokens),
                parseFunctionArgs(ctx.declarator(), tokens),
                parseFunctionBody(ctx.compoundStatement(), tokens),
                ssName
        );
    }

    public static List<String> parseFunctionSpecs(final SSCParser.DeclarationSpecifiersContext ctx,
                                                  final CommonTokenStream tokens) {
        final List<String> specs = ctx
                .declarationSpecifier()
                .stream()
                .filter(declSpec -> declSpec.typeSpecifier() == null) /* filter out type names */
                .map(RuleContext::getText)
                .toList();

        final boolean isStatic = specs.stream().anyMatch("static"::equals);
        final boolean isPure = specs.stream().anyMatch("pure"::equals);

        if (isStatic && isPure) {
            throw new SSCSyntaxException("Method may not be both `static` and `pure`", ctx, tokens);
        }

        return specs;
    }

    public static String parseType(SSCParser.DeclarationSpecifiersContext declSpecs,
                                   SSCParser.DeclaratorContext decl,
                                   CommonTokenStream tokens) {
        List<String> builder = new ArrayList<>();

        for (var spec : declSpecs.declarationSpecifier()) {
            if (spec.typeSpecifier() != null) {
                builder.add(Strings.getContextText(spec.typeSpecifier(), tokens));
            }
        }

        if (decl.pointer() != null) {
            builder.add(Strings.getContextText(decl.pointer(), tokens));
        }

        return String.join(" ", builder);
    }

    public static String parseName(SSCParser.DeclaratorContext ctx, CommonTokenStream tokens) {
        var directDecl = ctx.directDeclarator();
        if (directDecl == null) {
            throw new SSCSyntaxException("Direct declarator is null", ctx, tokens);
        }

        if (directDecl.Identifier() == null && (directDecl.LeftParen() == null || directDecl.RightParen() == null)) {
            return Strings.getContextText(ctx, tokens);
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

        return Strings.getContextText(directDecl.directDeclarator(), tokens);
    }

    public static List<String> parseFunctionArgs(final SSCParser.DeclaratorContext ctx,
                                                 final CommonTokenStream tokens) {
        final List<String> args = new ArrayList<>();
        if (ctx.directDeclarator().parameterTypeList() == null) {
            /* function declaration without a prototype -- let cc deal with it */
            return args;
        }
        for (var param : ctx.directDeclarator().parameterTypeList().parameterList().parameterDeclaration()) {
            args.add(Strings.getContextText(param, tokens));
        }

        if (ctx.directDeclarator().parameterTypeList().Ellipsis() != null) {
            args.add("...");
        }

        return args;
    }

    public static List<String> parseFunctionBody(SSCParser.CompoundStatementContext ctx, CommonTokenStream tokens) {
        List<String> statements = new ArrayList<>();
        for (var statement : ctx.blockItemList().blockItem()) {
            statements.add(Strings.getContextText(statement, tokens));
        }
        return statements;
    }

    public String getText() {
        final boolean isStatic = specs.stream().anyMatch("static"::equals);
        final boolean isPure = specs.stream().anyMatch("pure"::equals);
        assert !isStatic || !isPure;

        if (!isStatic && args.size() == 1 && args.get(0).equals("void")) {
            args.remove(0);
        }

        final StringBuilder selfRef = new StringBuilder();
        if (!isStatic) {
            if (isPure) {
                selfRef.append("const ");
            }
            selfRef.append("superstruct ");
            selfRef.append(superstructMemberOf);
            selfRef.append(" *this");

            if (!args.isEmpty()) {
                selfRef.append(", ");
            }
        }

        final String specsString = specs.stream().filter(spec -> !"pure".equals(spec) && !"static".equals(spec))
                .collect(Collectors.joining(" "));

        return specsString
                + " " + type
                + " " + superstructMemberOf + "__" + name
                + "("
                + selfRef
                + String.join(", ", args) + ")"
                + " {\n    " + String.join("\n    ", statements) + "\n}\n";
    }
}
