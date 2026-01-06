package cz.mipit.sscc.ssc.compiler.data.macro;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Macro {
    private final String identifier;
    private final List<String> fields;

    private final List<MacroBodyMember> replacement;

    private Macro(String identifier, List<String> fields, List<MacroBodyMember> replacement) {
        this.identifier = Objects.requireNonNull(identifier);
        this.replacement = replacement;
        this.fields = fields;
    }

    public static Macro fromContext(SSCParser.MacroDefinitionContext ctx,
                                    CommonTokenStream tokens, InputFile file) {
        final String identifier = ctx.Identifier().getText();

        final List<String> arguments = ctx.LeftParen() == null
                ? null
                : new ArrayList<>();
        if (ctx.LeftParen() != null) {
            final var argsCtx = ctx.macroArgs();
            if (argsCtx == null) {
                throw new SSCSyntaxException("Macro has no arguments", ctx, tokens, file);
            }
            for (var field : argsCtx.Identifier()) {
                arguments.add(field.getText());
            }
        }

        final var body = ctx.macroBody() == null
                ? null
                : MacroBodyMember.fromContext(ctx.macroBody(), arguments, tokens, file);
        return new Macro(identifier, arguments, body);
    }


    public String replace(List<String> arguments) {
        System.out.println("Macro " + identifier + " invoked with arguments: " + arguments);
        return "";
    }


    public boolean hasArguments() {
        return !fields.isEmpty();
    }

    public String identifier() {
        return identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Macro snd) {
            return identifier.equals(snd.identifier);
        }
        return false;
    }
}
