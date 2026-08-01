package cz.mipit.sscc;

import cz.mipit.sscc.args.ArgumentParser;
import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.ssc.Compiler;
import cz.mipit.sscc.ssc.compiler.SSCCompiler;
import cz.mipit.sscc.util.ExitValue;

public final class Main {
    /**
     * Acts as argv[0]
     */
    public static final String SSCC_NAME = "sscc";

    private Main() {
    }

    public static final Logger logger = new Logger();

    public static void main(String[] args) {
        final SSCCOptions options = ArgumentParser.parse(args);
        logger.setOptions(options);

        final Compiler compiler = new SSCCompiler(options);
        try {
            final ExitValue ev = compiler.run();
            System.exit(ev.ordinal());
        } catch (Throwable e) {
            logger.printException(e);
            System.exit(1);
        }
    }
}
