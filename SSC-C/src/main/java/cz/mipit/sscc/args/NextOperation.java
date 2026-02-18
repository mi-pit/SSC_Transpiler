package cz.mipit.sscc.args;

enum NextOperation {
    None(false),
    FilesOnly(false),
    CompileTarget(true),
    LibPath(true),
    //FileTypeSSC, FileTypeC
    ;
    private final boolean r;

    NextOperation(boolean r) {
        this.r = r;
    }

    public boolean requiresArgument() {
        return r;
    }
}
