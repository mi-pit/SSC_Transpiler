package cz.mipit.sscc;

import cz.mipit.sscc.ssc.compiler.Application;

import java.io.IOException;

public final class Main {
    private Main() {
    }

    public static final Logger logger = new Logger();

    public static void main(String[] args) throws IOException, InterruptedException {
        final Application app = new Application(args);
        logger.setOptions(app.getOptions());
        app.run();
    }
}
