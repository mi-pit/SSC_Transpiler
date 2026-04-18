package cz.mipit.sscc.ssc.compiler.data.tmpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Template {
    private final String name;

    private final String fnAttributes;
    private final List<Token> returnType;
    private final List<List<Token>> parameters;
    private final List<Token> body;

    private final List<String> typeArgumentAliases;


    public Template(
            final String name,
            final String fnAttributes,
            final List<String> typeArgumentAliases,
            final List<String> returnType,
            final List<List<String>> parameters,
            final List<String> functionTokens
    ) {
        this.name = name;
        this.fnAttributes = fnAttributes;
        this.typeArgumentAliases = typeArgumentAliases;
        this.returnType = returnType.stream().map(s -> new Token(typeArgumentAliases.contains(s), false, s)).toList();
        this.parameters =
                parameters.stream().map(
                        strings -> strings.stream().map(
                                s -> new Token(typeArgumentAliases.contains(s), false, s)
                        ).toList()
                ).toList();

        this.body = new ArrayList<>();

        for (String raw : functionTokens) {
            if (raw == null) {
                body.add(new Token(false, true, name));
            }
            final Token parsed = new Token(
                    typeArgumentAliases.contains(
                            Objects.requireNonNull(raw)
                    ),
                    false,
                    raw);
            body.add(parsed);
        }
    }

    public String getName() {
        return name;
    }

    public String convert(List<String> calledTypeArguments, String resolvedName) {
        List<String> params = parameters
                .stream()
                .map(l -> l.stream().map(tok -> tok.convert(calledTypeArguments)).collect(Collectors.joining(" ")))
                .toList();

        return fnAttributes +
                " " +
                String.join(
                        " ",
                        returnType.stream().map(t -> t.convert(calledTypeArguments)).toList()) +
                " " + resolvedName +
                "(" +
                String.join(", ", params) +
                ")" +
                String.join(
                        " ",
                        body.stream().map(b -> b.convert(calledTypeArguments)).toList()
                );
    }

    private class Token {
        final boolean isTypeArgument;
        final boolean isName;
        final String token;

        public Token(boolean isTypeArgument, boolean isName, String token) {
            this.isTypeArgument = isTypeArgument;
            this.isName = isName;
            this.token = token;
        }

        public String convert(List<String> actualTypeArgs) {
            if (isTypeArgument) {
                for (int i = 0; i < typeArgumentAliases.size(); i++) {
                    final String typeArgumentAlias = typeArgumentAliases.get(i);
                    // if "A" == this
                    //    return "int"
                    if (typeArgumentAlias.equals(this.token)) {
                        return actualTypeArgs.get(i);
                    }
                }
            }
            if (isName) {
                return name;
            }

            return this.token;
        }
    }
}
