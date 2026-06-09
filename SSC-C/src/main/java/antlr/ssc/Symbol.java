package antlr.ssc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringJoiner;

public class Symbol {
    private String name;
    private Symbol parent;
    private boolean predefined = false;
    private HashSet<TypeClassification> classification;

    // For blocks (scopes)
    private final Map<String, Symbol> members = new HashMap<>();

    // Very optional
    private String definedFile = "";
    private int definedLine = 0;
    private int definedColumn = 0;

    public String getDefinedFile() {
        return definedFile;
    }

    public void setDefinedFile(String definedFile) {
        this.definedFile = definedFile;
    }

    public int getDefinedLine() {
        return definedLine;
    }

    public void setDefinedLine(int definedLine) {
        this.definedLine = definedLine;
    }

    public int getDefinedColumn() {
        return definedColumn;
    }

    public void setDefinedColumn(int definedColumn) {
        this.definedColumn = definedColumn;
    }

    public boolean isPredefined() {
        return predefined;
    }

    public void setPredefined(boolean predefined) {
        this.predefined = predefined;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashSet<TypeClassification> getClassification() {
        return classification;
    }

    public void setClassification(HashSet<TypeClassification> classification) {
        this.classification = classification;
    }

    public Map<String, Symbol> getMembers() {
        return members;
    }

    public Symbol getParent() {
        return parent;
    }

    public void setParent(Symbol parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        final StringJoiner classificationString = new StringJoiner(", ");
        for (final TypeClassification typeClassification : this.classification) {
            classificationString.add(typeClassification.name());
        }

        final StringBuilder result = new StringBuilder(name);
        result.append(" (with classification ")
                .append(classificationString)
                .append(")");

        if (definedFile != null && !definedFile.isEmpty()) {
            result
                    .append(" defined at ")
                    .append(definedFile)
                    .append(":")
                    .append(definedLine)
                    .append(":")
                    .append(definedColumn);
        }

        return result.toString();
    }
}
