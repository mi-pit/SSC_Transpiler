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

    private final String originalName;
    private final String qualifiedName;

    private final List<String> args;
    private final String statements; /* body */
    private final String superstructMemberOfName;

    private final InputFile inputFile;
    private final CommonTokenStream tokens;

    /* TODO: parse in visitor */
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
        this.type = parseType(ctx.declarationSpecifiers(), ctx.declarator());

        this.originalName = parseName(ctx.declarator());
        this.qualifiedName = convertor.getCurrentSSName() + "__" + originalName;

        convertor.currentFunctionName = qualifiedName;
        functionVariables.put(qualifiedName, new HashSet<>());
        if (!isStatic) {
            functionVariables.get(qualifiedName).add(new SuperstructVariable(convertor.getCurrentSSName(), 1, "this"));
        }

        this.args = parseFunctionArgs(ctx.declarator());
        this.statements = convertor.visitFunctionBody(ctx.functionBody());
        this.superstructMemberOfName = superstructMemberOfName;

        if (!isStatic && args.size() == 1 && args.getFirst().equals("void")) {
            this.args.removeFirst();
        }

        convertor.currentFunctionName = null;
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
                            SSCParser.DeclaratorContext decl) {
        final List<String> builder = new ArrayList<>();

        for (var spec : declSpecs.declarationSpecifier()) {
            if (spec.typeSpecifier() == null) {
                continue;
            }
            final SSCParser.TypeSpecifierContext typeSpec = spec.typeSpecifier();
            if (typeSpec.superStructSpecifier() == null) {
                builder.add(convertor.visitTypeSpecifier(spec.typeSpecifier()));
                continue;
            }
            final SSCParser.SuperStructSpecifierContext superStructSpec = typeSpec.superStructSpecifier();
            builder.add("struct " + superStructSpec.Identifier().getText());
        }

        if (decl.pointer() != null) {
            builder.add(decl
                    .pointer()
                    .stream()
                    .map(convertor::visitPointer)
                    .collect(Collectors.joining(" "))
            );
        }

        return String.join(" ", builder);
    }

    public String parseName(SSCParser.DeclaratorContext ctx) {
        var directDecl = ctx.directDeclarator();
        if (directDecl == null) {
            throw getException("Direct declarator is null", ctx);
        }

        if (directDecl.Identifier() == null && (directDecl.LeftParen() == null || directDecl.RightParen() == null)) {
            return convertor.visitDeclarator(ctx);
        }

        if (directDecl.Identifier() != null) {
            return directDecl.Identifier().getText();
        }

        assert directDecl.LeftParen() != null : "Parser \"found\" function definition without parentheses";

        if (directDecl.RightParen() == null) {
            throw getException("Direct declarator has left parenthesis, but not a matching right one", ctx);
        }
        if (directDecl.Identifier() == null) {
            throw getException("Missing declarator identifier (perhaps missing a variable name?)", directDecl);
        }

        return directDecl.Identifier().getText();
    }

    private List<String> parseFunctionArgs(final SSCParser.DeclaratorContext ctx) {
        final List<String> args = new ArrayList<>();
        final var directDecl = ctx.directDeclarator();
        final List<SSCParser.ParameterTypeListContext> paramTypeList = directDecl.parameterTypeList();
        if (paramTypeList.size() > 1) {
            throw getException("Parameter type list has more than one parameter type", directDecl);
        }

        final SSCParser.ParameterTypeListContext paramType = paramTypeList.getFirst();
        for (final var param : paramType.parameterList().parameterDeclaration()) {
            @Nullable String ssName = null;

            final List<String> curr = new ArrayList<>();
            for (final var declSpec : param.declarationSpecifiers().declarationSpecifier()) {
                if (declSpec.typeSpecifier() == null) {
                    curr.add(convertor.visitDeclarationSpecifier(declSpec));
                    continue;
                }
                final var typeSpecCtx = declSpec.typeSpecifier();
                if (typeSpecCtx.superStructSpecifier() == null) {
                    curr.add(convertor.visitTypeSpecifier(typeSpecCtx));
                    continue;
                }

                if (ssName != null) {
                    throw getException("Duplicate super struct specifier", declSpec);
                }
                final var superStructSpecCtx = typeSpecCtx.superStructSpecifier();
                ssName = superStructSpecCtx.Identifier().getText();
                curr.add("struct " + ssName);
            }
            final var declarator = param.declarator();
            if (declarator != null) {
                final int pointer = SSCCUtil.getPointerLevel(declarator);

                if (declarator.directDeclarator().Identifier() != null) {
                    final String varName = declarator.directDeclarator().Identifier().getText();
                    if (ssName != null) {
                        functionVariables.get(qualifiedName).add(new SuperstructVariable(ssName, pointer, varName));
                    }
                }

                curr.add(convertor.visitDeclarator(declarator));
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
                + " " + superstructMemberOfName + "__" + originalName
                + "(" + selfRef + String.join(", ", args) + ")";
    }

    private String getBody() {
        return statements;
    }

    public String getOriginalName() {
        return originalName;
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
