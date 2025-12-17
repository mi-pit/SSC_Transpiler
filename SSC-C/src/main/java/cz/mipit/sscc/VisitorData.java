package cz.mipit.sscc;

import antlr.ssc.SSCLexer;
import antlr.ssc.SSCParser;
import antlr.sscpp.SSCPPLexer;
import antlr.sscpp.SSCPPParser;
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

    public static VisitorData getSSCPPVisitorData(final Path file) throws IOException {
        final SSCPPLexer lexer = new SSCPPLexer(CharStreams.fromString(Files.readString(file)));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final SSCPPParser parser = new SSCPPParser(tokens);

        parser.removeErrorListeners();

        final ParseTree tree = parser.compilationUnit();
        return new VisitorData(tokens, tree);
    }
}
