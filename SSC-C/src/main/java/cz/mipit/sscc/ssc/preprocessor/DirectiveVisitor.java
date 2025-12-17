package cz.mipit.sscc.ssc.preprocessor;

import antlr.sscpp.SSCPPParser;
import org.antlr.v4.runtime.CommonTokenStream;

import java.nio.file.Path;

public class DirectiveVisitor extends SSCPPConvertorVisitor {
    private final Path baseDirectory;
    private boolean hasDirectives;

    public DirectiveVisitor(CommonTokenStream tokens, Path baseDirectory) {
        super(tokens);

        this.baseDirectory = baseDirectory;
        hasDirectives = false;
    }

    @Override
    public String visitDirective(SSCPPParser.DirectiveContext ctx) {
        System.out.println("Found directive: " + ctx.getText());
        return "";
    }

    public boolean foundAnyDirectives() {
        return hasDirectives;
    }
}
