package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.SSCBaseVisitor;
import antlr.SSCParser;
import cz.mipit.sscc.ssc.compiler.exceptions.AntlrException;
import cz.mipit.sscc.ssc.compiler.exceptions.SSCSyntaxException;
import cz.mipit.sscc.ssc.compiler.exceptions.SSCTranspilerException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.function.Supplier;

public abstract class ConvertorVisitor extends SSCBaseVisitor<String> {
    protected final CommonTokenStream tokens;
    private boolean hasErrors = false;

    protected ConvertorVisitor(CommonTokenStream tokens) {
        this.tokens = tokens;
    }

    @Override
    public String visitChildren(RuleNode node) {
        StringBuilder sb = new StringBuilder();
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            try {
                sb.append(node.getChild(i).accept(this));
            } catch (SSCSyntaxException e) {
                printErrorMessage(() -> e);
            }
        }
        return sb.toString();
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == Token.EOF) {
            return "";
        }

        // For better error messages between Extracting SSs & Replacing refs
        return switch (node.getSymbol().getType()) {
            case SSCParser.LeftBrace -> "{\n";
            case SSCParser.RightBrace -> "}\n";
            case SSCParser.Semi -> ";\n";

            default -> node.getText() + " ";
        };
    }

    @Override
    protected String defaultResult() {
        return "";
    }

    @Override
    public String visitErrorNode(ErrorNode node) {
        hasErrors = true;

        // Don't throw! Let the user see the rest of the error nodes!
        printErrorMessage(() -> new AntlrException(node.getSymbol(), tokens));

        return super.visitErrorNode(node);
    }

    private static void printErrorMessage(final Supplier<SSCTranspilerException> supplier) {
        final SSCTranspilerException e = supplier.get();
        System.err.println(e.getMessage());
    }

    public boolean hasNoErrors() {
        return !hasErrors;
    }
}
