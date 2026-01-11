package cz.mipit.sscc.util;

import antlr.ssc.SSCLexer;
import antlr.ssc.SSCParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.SSCErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;

public record VisitorData(CommonTokenStream tokens, ParseTree tree) {

    public static VisitorData fromFile(final InputFile inputFile) throws IOException {
        final SSCLexer lexer = new SSCLexer(CharStreams.fromString(Files.readString(inputFile.toPath())));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final SSCParser parser = new SSCParser(tokens);

        final SSCErrorListener listener = new SSCErrorListener(inputFile);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);

        final ParseTree tree = parser.compilationUnit();
        return new VisitorData(tokens, tree);
    }
}
