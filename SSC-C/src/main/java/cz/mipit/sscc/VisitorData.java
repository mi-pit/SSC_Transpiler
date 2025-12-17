package cz.mipit.sscc;

import antlr.ssc.SSCLexer;
import antlr.ssc.SSCParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record VisitorData(CommonTokenStream tokens, ParseTree tree) {
    public static VisitorData getSSCVisitorData(final Path file) throws IOException {
        final SSCLexer lexer = new SSCLexer(CharStreams.fromString(Files.readString(file)));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final SSCParser parser = new SSCParser(tokens);

        parser.removeErrorListeners();

        final ParseTree tree = parser.compilationUnit();
        return new VisitorData(tokens, tree);
    }
}
