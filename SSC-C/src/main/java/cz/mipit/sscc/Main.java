package cz.mipit.sscc;

import cz.mipit.sscc.args.ArgumentParser;
import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.ssc.compiler.Compiler;

import java.io.IOException;

public final class Main {
    /**
     * Acts as argv[0]
     */
    public static final String SSCC_NAME = "SSC Compiler";

    private Main() {
    }

    public static final Logger logger = new Logger();

    public static void main(String[] args) throws IOException, InterruptedException {
        final SSCCOptions options = ArgumentParser.parse(args);
        logger.setOptions(options);
        final Compiler compiler = new Compiler(options);
        System.exit(compiler.run().ordinal());
    }
}
