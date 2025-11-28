package cz.muni.fi.sscc.util;

import cz.muni.fi.sscc.Main;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Util {

    /**
     * Returns the exact text corresponding to a ParserRuleContext.
     * Works for any context.
     */
    public static String getContextText(ParserRuleContext ctx, CommonTokenStream tokens) {
        if (ctx == null || tokens == null)
            return "";
        int start = ctx.getStart().getTokenIndex();
        int stop = ctx.getStop().getTokenIndex();
        return tokens.getText(new Interval(start, stop));
    }

    public static void printDebug(String string) {
        printDebug("%s\n", string);
    }

    public static void printDebug(String fmt, Object... objects) {
        if (Main.doPrintDebug) {
            System.out.print("[DEBUG] ");
            System.out.printf(fmt, objects);
        }
    }

    public static void printVerbose(String string, Object... objects) {
        if (Main.verbose) {
            System.out.printf(string, objects);
            System.out.println();
        }
    }

    public static void printVerbose(String string) {
        printVerbose("%s", string);
    }

    public static void writeToFile(Path outputFile, String string)
            throws IOException {
        writeToFile(outputFile, "%s", string);
    }

    public static void writeToFile(Path outputFile, String fmt, Object... objects)
            throws IOException {
        printDebug(fmt, objects);
        Files.writeString(outputFile, String.format(fmt, objects), StandardOpenOption.APPEND);
    }
}
