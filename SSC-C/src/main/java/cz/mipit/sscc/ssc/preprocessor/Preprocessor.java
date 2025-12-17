package cz.mipit.sscc.ssc.preprocessor;

import cz.mipit.sscc.VisitorData;
import cz.mipit.sscc.ssc.compiler.visitors.ConvertorVisitor;

import java.io.IOException;
import java.util.regex.Pattern;

public final class Preprocessor {
    private Preprocessor() {
    }

    private static final String INCLUDE_LOCAL = "$\\s*#\\s*include\\s*\"[^\"]+\".*";
    private static final Pattern INCLUDE_LOCAL_PATTERN = Pattern.compile(INCLUDE_LOCAL);

    public static void preprocessSSC(VisitorData data) throws IOException {
        ConvertorVisitor visitor = new DirectiveVisitor();
    }
}
