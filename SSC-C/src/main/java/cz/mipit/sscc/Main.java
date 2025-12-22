package cz.mipit.sscc;

public final class Main {
    private Main() {
    }

    public static final Logger logger = new Logger();

    public static void main(String[] args) {
        try {
            final Application app = new Application(args);
            logger.setOptions(app.getOptions());
            app.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
