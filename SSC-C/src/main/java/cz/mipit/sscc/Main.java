package cz.mipit.sscc;

import cz.mipit.sscc.args.ArgumentParser;
import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.ssc.Compiler;
import cz.mipit.sscc.ssc.compiler.SSCCompiler;

import java.io.IOException;

public final class Main {
    /**
     * Acts as argv[0]
     */
    public static final String SSCC_NAME = "SSC Compiler";

    private Main() {
    }

    public static final Logger logger = new Logger();

    public static void main(String[] args) {
        final SSCCOptions options = ArgumentParser.parse(args);
        logger.setOptions(options);

        final Compiler compiler = new SSCCompiler(options);
        try {
            final int rv = compiler.run().ordinal();
            System.exit(rv);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
