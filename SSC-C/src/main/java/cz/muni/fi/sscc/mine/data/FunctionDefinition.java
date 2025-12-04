package cz.muni.fi.sscc.mine.data;

import cz.muni.fi.sscc.SSCSyntaxException;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.util.Util;
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
                parseFunctionSpecs(ctx.declarationSpecifiers()),
                parseType(ctx.declarationSpecifiers(), ctx.declarator(), tokens),
                parseName(ctx.declarator(), tokens),
                parseFunctionArgs(ctx.declarator(), tokens),
                parseFunctionBody(ctx.compoundStatement(), tokens),
                ssName
        );
    }

    public static List<String> parseFunctionSpecs(SSCParser.DeclarationSpecifiersContext ctx) {
        return ctx.declarationSpecifier().stream()
                .filter(declSpec -> declSpec.typeSpecifier() == null) /* filter out type names */
                .map(RuleContext::getText).collect(Collectors.toList());
    }

    public static String parseType(SSCParser.DeclarationSpecifiersContext declSpecs,
                                   SSCParser.DeclaratorContext decl,
                                   CommonTokenStream tokens) {
        List<String> builder = new ArrayList<>();

        for (var spec : declSpecs.declarationSpecifier()) {
            if (spec.typeSpecifier() != null) {
                builder.add(Util.getContextText(spec.typeSpecifier(), tokens));
            }
        }

        if (decl.pointer() != null) {
            builder.add(Util.getContextText(decl.pointer(), tokens));
        }

        return String.join(" ", builder);
    }

    public static String parseName(SSCParser.DeclaratorContext ctx, CommonTokenStream tokens) {
        var directDecl = ctx.directDeclarator();
        assert directDecl != null;

        if (directDecl.Identifier() == null && directDecl.LeftParen() == null || directDecl.RightParen() == null) {
            return Util.getContextText(ctx, tokens);
        }

        if (directDecl.Identifier() != null) {
            return directDecl.Identifier().getText();
        }
        if (directDecl.LeftParen() != null) {
            return Util.getContextText(directDecl.directDeclarator(), tokens);
        }

        throw new SSCSyntaxException("Unknown problem", ctx, tokens);
    }

    public static List<String> parseFunctionArgs(SSCParser.DeclaratorContext ctx, CommonTokenStream tokens) {
        List<String> args = new ArrayList<>();
        if (ctx.directDeclarator().parameterTypeList() == null) {
            /* function declaration without a prototype -- let cc deal with it */
            return args;
        }
        for (var param : ctx.directDeclarator().parameterTypeList().parameterList().parameterDeclaration()) {
            args.add(Util.getContextText(param, tokens));
        }
        return args;
    }

    public static List<String> parseFunctionBody(SSCParser.CompoundStatementContext ctx, CommonTokenStream tokens) {
        List<String> statements = new ArrayList<>();
        for (var statement : ctx.blockItemList().blockItem()) {
            statements.add(Util.getContextText(statement, tokens));
        }
        return statements;
    }

    public String getText() {
        final boolean isStatic = specs.stream().anyMatch("static"::equals);
        final boolean isPure = specs.stream().anyMatch("pure"::equals);

        if (!isStatic && args.size() == 1 && args.get(0).equals("void")) {
            args.remove(0);
        }

        if (isStatic && isPure) {
            throw new SSCSyntaxException("Method may not be both `static` and `pure`", null, null);
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
                + " {\n\t" + String.join("\n\t", statements) + "\n}\n";
    }
}
