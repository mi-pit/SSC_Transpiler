package cz.mipit.sscc.util;

import antlr.ssc.SSCLexer;
import antlr.ssc.SSCParser;
import antlr.ssc.SymbolTable;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.SSCErrorListener;
import cz.mipit.sscc.ssc.exceptions.children.AntlrException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.util.SequencedCollection;

public record VisitorInput(
        CommonTokenStream tokens,
        ParseTree tree,
        InputFile inputFile,
        SymbolTable symbolTable
) {
    public static VisitorInput fromFile(final InputFile inputFile,
                                        final SequencedCollection<AntlrException> errors)
            throws IOException {
        final SSCLexer lexer = new SSCLexer(CharStreams.fromString(Files.readString(inputFile.toPath())));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);

        final SSCParser parser = new SSCParser(tokens);

        final SSCErrorListener listener = new SSCErrorListener(inputFile, errors);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);

        final ParseTree root = parser.compilationUnit();
        return new VisitorInput(tokens, root, inputFile, parser.getSymbolTable());
    }
}
