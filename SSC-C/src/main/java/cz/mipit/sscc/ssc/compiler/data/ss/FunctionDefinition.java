package cz.mipit.sscc.ssc.compiler.data.ss;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.visitors.SuperstructConvertorVisitor;
import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FunctionDefinition {
    private final SuperstructConvertorVisitor convertor;
    private final Map<String, Set<SuperstructVariable>> functionVariables;

    private final List<String> specs;
    private final boolean isStatic;
    private final boolean isPure;
    private final boolean isPrivate;
    private final String type;
    private final String name;
    private final List<String> args;
    private final String statements; /* body */
    private final String superstructMemberOfName;

    private final InputFile inputFile;
    private final CommonTokenStream tokens;

    private FunctionDefinition(
            final SuperstructConvertorVisitor convertor,
            final Map<String, Set<SuperstructVariable>> functionVariables,
            final boolean isStatic,
            final boolean isPure,
            final boolean isPrivate,
            final List<String> specsWithoutCustom,
            final SSCParser.FunctionDefinitionContext ctx,
            final CommonTokenStream tokens,
            final String superstructMemberOfName,
            final InputFile currentFile
    ) {
        this.convertor = convertor;
        this.functionVariables = functionVariables;

        this.inputFile = currentFile;
        this.tokens = tokens;

        this.specs = specsWithoutCustom;
        this.isStatic = isStatic;
        this.isPure = isPure;
        this.isPrivate = isPrivate;
        this.type = parseType(ctx.declarationSpecifiers(), ctx.declarator(), tokens);

        this.name = parseName(ctx.declarator(), tokens, currentFile);
        functionVariables.put(name, new HashSet<>());
        if (!isStatic) {
            functionVariables.get(name).add(new SuperstructVariable(convertor.getCurrentSSName(), 1, "this"));
        }

        this.args = parseFunctionArgs(ctx.declarator(), tokens);
        this.statements = parseFunctionBody(ctx.functionBody());
        this.superstructMemberOfName = superstructMemberOfName;

        if (!isStatic && args.size() == 1 && args.getFirst().equals("void")) {
            this.args.removeFirst();
        }
    }

    public static FunctionDefinition fromSemiParsedContext(final SuperstructConvertorVisitor convertor,
                                                           final Map<String, Set<SuperstructVariable>> functionVariables,
                                                           final boolean isStatic,
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
                convertor, functionVariables,
                isStatic, isPure, isPrivate, specsWithoutCustom, ctx,
                tokens, superstructMemberOfName, currentFile
        );
    }

    public String parseType(SSCParser.DeclarationSpecifiersContext declSpecs,
                            SSCParser.DeclaratorContext decl,
                            CommonTokenStream tokens) {
        final List<String> builder = new ArrayList<>();

        for (var spec : declSpecs.declarationSpecifier()) {
            if (spec.typeSpecifier() == null) {
                continue;
            }
            final SSCParser.TypeSpecifierContext typeSpec = spec.typeSpecifier();
            if (typeSpec.superStructSpecifier() == null) {
                builder.add(SSCCUtil.Text.getLiteral(spec.typeSpecifier(), tokens));
                continue;
            }
            final SSCParser.SuperStructSpecifierContext superStructSpec = typeSpec.superStructSpecifier();
            builder.add("struct " + superStructSpec.Identifier().getText());
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

        final SSCParser.ParameterTypeListContext paramType = paramTypeList.getFirst();
        for (var param : paramType.parameterList().parameterDeclaration()) {
            @Nullable String ssName = null;

            final List<String> curr = new ArrayList<>();
            for (var declSpec : param.declarationSpecifiers().declarationSpecifier()) {
                if (declSpec.typeSpecifier() == null) {
                    curr.add(SSCCUtil.Text.getLiteral(declSpec, tokens));
                    continue;
                }
                final var typeSpecCtx = declSpec.typeSpecifier();
                if (typeSpecCtx.superStructSpecifier() == null) {
                    curr.add(SSCCUtil.Text.getLiteral(typeSpecCtx, tokens));
                    continue;
                }
                final var superStructSpecCtx = typeSpecCtx.superStructSpecifier();
                ssName = superStructSpecCtx.Identifier().getText();
                curr.add("struct " + ssName);
            }
            final var declarator = param.declarator();
            if (declarator != null) {
                final int pointer = SSCCUtil.getPointerLevel(declarator);
                if (declarator.directDeclarator().Identifier() == null) {
                    continue;
                }

                final String varName = declarator.directDeclarator().Identifier().getText();
                functionVariables.get(this.name).add(new SuperstructVariable(convertor.getCurrentSSName(), pointer, varName));
                curr.add("*".repeat(pointer) + varName);
            }

            final String paramStr = String.join(" ", curr);
            if (!paramStr.isBlank()) {
                args.add(paramStr);
            }
        }
        if (args.isEmpty()) {
            throw getException("Function declaration without a prototype", directDecl);
        }

        if (paramType.Ellipsis() != null) {
            args.add("...");
        }

        return args;
    }

    private String parseFunctionBody(SSCParser.FunctionBodyContext fb) {
        return convertor.visitFunctionBody(fb);
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
                    .append("struct ")
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
                statements +
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
