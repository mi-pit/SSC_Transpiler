package cz.mipit.sscc.ssc.preprocessor;

import cz.mipit.sscc.Main;

import java.io.IOException;
import java.nio.file.Path;

public final class Preprocessor {
    private Preprocessor() {
    }

    public static void preprocessSSC(final Path file) throws IOException {
        Main.logger.printVerbose("Preprocessing SSC file: '" + file.toString() + "'");

        // final String text = Files.readString(file);
    }
}
