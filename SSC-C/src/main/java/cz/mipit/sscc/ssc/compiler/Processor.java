package cz.mipit.sscc.ssc.compiler;

import cz.mipit.sscc.util.ExitValue;

import java.io.IOException;

public interface Processor {
    ExitValue run() throws IOException, InterruptedException;
}
