package cz.muni.fi.sscc.mine;

import cz.muni.fi.sscc.antlr.SSCBaseVisitor;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.mine.data.SuperStructRepre;
import cz.muni.fi.sscc.util.Util;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Collection;

public abstract class ExpressionVisitor extends SSCBaseVisitor<String> {
    protected final CommonTokenStream tokens;
    protected final Collection<SuperStructRepre> superstructs;

    protected ExpressionVisitor(CommonTokenStream tokens, Collection<SuperStructRepre> sss) {
        this.tokens = tokens;
        this.superstructs = sss;
    }
}
