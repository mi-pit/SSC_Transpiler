package cz.mipit.sscc.ssc.compiler.cc;

import cz.mipit.sscc.Logger;

public final class CCStandard {
    public static final CCStandard
            C99 = new CCStandard("c99"),
            C11 = new CCStandard("c11"),
            C17 = new CCStandard("c17"),
            C23 = new CCStandard("c2x");

    private final String repre;

    private CCStandard(String repre) {
        this.repre = repre;
    }

    public String ccOptionString() {
        return "--std=" + repre;
    }

    public static CCStandard fromString(String repre) {
        return switch (repre) {
            case "c99" -> C99;
            case "c11" -> C11;
            case "c17" -> C17;
            case "c23" -> C23;

            default -> {
                Logger.warn("Unknown standard: '%s'", repre);
                yield new CCStandard(repre);
            }
        };
    }
}
