package cz.mipit.sscc.args;

enum NextOperation {
    None(false),
    FilesOnly(false),
    CompileTarget(true),
    LibPath(true),

    FileType(true),
    File(true),
    ;

    private final boolean r;

    NextOperation(boolean r) {
        this.r = r;
    }

    public boolean requiresArgument() {
        return r;
    }
}
