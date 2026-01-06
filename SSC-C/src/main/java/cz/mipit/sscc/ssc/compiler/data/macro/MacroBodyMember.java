package cz.mipit.sscc.ssc.compiler.data.macro;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MacroBodyMember {
    private final String data;
    private final boolean isArg;

    private MacroBodyMember(boolean isArg, String data) {
        this.isArg = isArg;
        this.data = Objects.requireNonNull(data);
    }

    public static List<MacroBodyMember> fromContext(
            final SSCParser.MacroBodyContext ctx,
            final List<String> args,
            final CommonTokenStream tokens,
            final InputFile file
    ) {
        if (args == null || args.isEmpty()) {
            // todo: split by whitespace
            return Arrays.stream(SSCCUtil.Text.getLiteral(ctx, tokens).split(""))
                    .map(s -> new MacroBodyMember(false, s))
                    .toList();// fixme
        }
        final Set<String> argsSet = Set.copyOf(args);

        final List<MacroBodyMember> members = new ArrayList<>();
        recursive(ctx, argsSet, members);
        return members;
    }

    private static void recursive(final ParseTree tree, final Set<String> args,
                                  final List<MacroBodyMember> members) {
        for (int i = 0; i < tree.getChildCount(); i++) {
            final ParseTree child = tree.getChild(i);
            if (child instanceof TerminalNode terminal) {
                final String text = terminal.getText();
                if (args.contains(text)) {
                    members.add(new MacroBodyMember(true, text));
                }
            } else {
                recursive(child, args, members);
            }
        }
    }

    public String data() {
        return data;
    }

    public boolean isArgument() {
        return isArg;
    }
}
