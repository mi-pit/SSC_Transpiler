package cz.mipit.sscc.ssc;

import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.util.ExitValue;

import java.io.IOException;

public interface Compiler {
    ExitValue run() throws IOException, InterruptedException, SSCTranspilerException;
}
