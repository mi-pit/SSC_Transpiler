package cz.mipit.sscc;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public record VisitorData(CommonTokenStream tokens, ParseTree tree) {
}
