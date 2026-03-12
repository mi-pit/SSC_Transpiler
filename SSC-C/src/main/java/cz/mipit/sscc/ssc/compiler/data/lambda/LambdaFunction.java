package cz.mipit.sscc.ssc.compiler.data.lambda;

import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.util.annotations.NotNull;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class LambdaFunction {
    private final String returnType;
    private final String params;
    private final String body;

    private final String prettifier;

    private final long id;
    private static final Random RANDOM = new Random();
    private static final Set<@NotNull Long> USED_IDS = new HashSet<>();

    public LambdaFunction(
            InputFile inFile,
            String functionName,
            String returnType,
            String params,
            String ctx
    ) {
        this.returnType = returnType;
        this.params = params;
        this.body = ctx;

        final String fileNamePrettifier =
                (inFile.name() + "_" + inFile.suffix())
                        .chars()
                        .mapToObj(i -> {
                            if ((i > 'A' && i < 'Z') || (i > 'a' && i < 'z'))
                                return (char) i;
                            else
                                return '_';
                        })
                        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                        .toString();

        this.prettifier = fileNamePrettifier + "_" + functionName;

        long id_candidate = Math.abs(RANDOM.nextLong());
        // is this even necessary?
        while (USED_IDS.contains(id_candidate)) {
            id_candidate = Math.abs(RANDOM.nextLong());
        }
        USED_IDS.add(id_candidate);

        id = id_candidate;
    }

    public String getName() {
        return "SSC_LAMBDA_FUNCTION__" + prettifier + "__" + id;
    }

    public String getDefinition() {
        return returnType + " " + getName() + "(" + params + ")" + body;
    }
}
