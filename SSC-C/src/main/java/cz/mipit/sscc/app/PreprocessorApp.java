package cz.mipit.sscc.app;

import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.preprocessor.Preprocessor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class PreprocessorApp {
    private PreprocessorApp() {
    }

    /* TODO */
    public static void main(String[] args) throws IOException {
        final List<InputFile> files = new ArrayList<>();
        for (String arg : args) {
            final Path path = Paths.get(arg);
            final InputFile inFile = InputFile.fromAbsolutePath(path.toAbsolutePath());
            files.add(inFile);
        }

        for (InputFile file : files) {
            Preprocessor.preprocessSSC(file, file.getChangedSuffix("preprocessed").toAbsolutePath());
        }
    }
}
