package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.List;

public abstract class LambdaVariable extends Variable {
    protected LambdaVariable(String identifier, List<Pointer> pointers) {
        super(identifier, pointers);
    }

    abstract public String getDeclarationForLambda(boolean removeConst, String newIdentifier);
    abstract public boolean isCompound();
}
