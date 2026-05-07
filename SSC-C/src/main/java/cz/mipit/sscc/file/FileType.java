package cz.mipit.sscc.file;

public enum FileType {
    C, SSC, OTHER,
    ;

    public static FileType fromString(String string) {
        for (FileType fileType : FileType.values()) {
            if (fileType.toString().equalsIgnoreCase(string)) {
                return fileType;
            }
        }
        return OTHER;
    }
}
