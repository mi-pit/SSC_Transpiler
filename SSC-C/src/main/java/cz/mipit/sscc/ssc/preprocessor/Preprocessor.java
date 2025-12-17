package cz.mipit.sscc.ssc.preprocessor;

import cz.mipit.sscc.VisitorData;
import cz.mipit.sscc.file.InputFile;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static cz.mipit.sscc.VisitorData.getSSCPPVisitorData;

public final class Preprocessor {
    private Preprocessor() {
    }

    public static boolean preprocessSSC(final InputFile inputFile, final Path outputFile)
            throws IOException {
        System.out.println("Preprocessing file '" + inputFile.getFullName() + "'");

        final VisitorData data = getSSCPPVisitorData(inputFile.toAbsolutePath());

        final SSCPPConvertorVisitor visitor = new DirectiveVisitor(data.tokens(), inputFile.dir());
        final String result = visitor.visit(data.tree());

        if (!visitor.hasNoErrors()) {
            System.out.println("Has errors");
            return false;
        }

        System.out.println("got: " + result);

        System.out.println("Would output to " + outputFile);
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
        return true;
    }
}
