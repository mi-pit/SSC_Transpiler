package cz.mipit.sscc.ssc.exceptions.children;

import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public class SSCLanguageException extends SSCTranspilerException {
    public SSCLanguageException(String message,
                                ParserRuleContext ctx,
                                CommonTokenStream tokens,
                                InputFile currentFile) {
        super(Type.Language, message, ctx, tokens, currentFile);
    }

    public SSCLanguageException(String message,
                                TerminalNode node,
                                CommonTokenStream tokens,
                                InputFile currentFile) {
        super(Type.Language, message, node, tokens, currentFile);
    }
}
