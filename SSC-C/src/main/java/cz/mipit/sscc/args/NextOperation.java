package cz.mipit.sscc.args;

enum NextOperation {
    None(false),
    FilesOnly(false),

    CompileTarget(true),
    LibPath(true),

    FileType(true),
    InputFile(true),

    OutputFile(true),
    ;

    private final boolean reqArg;

    NextOperation(boolean r) {
        this.reqArg = r;
    }

    public boolean requiresArgument() {
        return reqArg;
    }
}
