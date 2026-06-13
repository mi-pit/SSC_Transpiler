package cz.mipit.sscc.ssc.exceptions.children;

import cz.mipit.sscc.file.File;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;


public class SSCCallbackException extends SSCTranspilerException {
    public SSCCallbackException(
            String message, List<ParseTree> offenders,
            CommonTokenStream tokens, File currentFile
    ) {
        super(
                Type.Language,
                message,
                offenders,
                tokens
        );
    }
}
