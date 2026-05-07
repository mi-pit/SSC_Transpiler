package antlr.ssc;

import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.util.color.ConsoleColorFactory;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SSCParserBase extends Parser {
    private final SymbolTable symbolTable = new SymbolTable();

    private final boolean debug;
    private final boolean outputSymbolTable;
    private final boolean outputAppliedOccurrences;
    private final Set<String> noSemantics;

    // List of all semantic function names
    private static final String[] ALL_SEMANTIC_FUNCTIONS = {
            "IsAlignmentSpecifier", "IsAtomicTypeSpecifier", "IsAttributeDeclaration",
            "IsAttributeSpecifier", "IsAttributeSpecifierSequence", "IsDeclaration",
            "IsDeclarationSpecifier", "IsTypeSpecifierQualifier", "IsEnumSpecifier",
            "IsFunctionSpecifier", "IsStatement", "IsStaticAssertDeclaration",
            "IsStorageClassSpecifier", "IsStructOrUnionSpecifier", "IsTypedefName",
            "IsTypeofSpecifier", "IsTypeQualifier", "IsTypeSpecifier", "IsCast",
            "IsNullStructDeclarationListExtension"
    };

    protected SSCParserBase(TokenStream input) {
        super(input);
        // Get options from system property
        String cmdLine = System.getProperty("sun.java.command");
        String[] args = cmdLine != null ? cmdLine.split("\\s+") : new String[0];
        noSemantics = parseNoSemantics(args);
        debug = hasArg(args, SSCCOptions.OPTSTR_ANTLR_DEBUG);
        outputSymbolTable = hasArg(args, "--output-symbol-table");
        outputAppliedOccurrences = hasArg(args, "--output-applied-occurrences");
    }

    private static boolean hasArg(String[] args, String arg) {
        for (final String a : args) {
            if (a.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> parseNoSemantics(String[] args) {
        final Set<String> result = new HashSet<>();
        for (String a : args) {
            if (!a.toLowerCase().startsWith("--no-semantics")) {
                continue;
            }

            final int eqIndex = a.indexOf('=');
            if (eqIndex == -1) {
                // --no-semantics without value: disable all semantic functions
                result.addAll(Arrays.asList(ALL_SEMANTIC_FUNCTIONS));
                continue;
            }

            // --no-semantics=Func1,Func2,...
            final String value = a.substring(eqIndex + 1);
            final String[] funcs = value.split(",");
            Arrays.stream(funcs)
                    .map(String::trim)
                    .forEach(result::add);
        }
        return result;
    }

    public boolean IsAlignmentSpecifier() {
        if (noSemantics.contains("IsAlignmentSpecifier"))
            return true;
        Token lt1 = this.getInputStream().LT(1);
        debugPrint("IsAlignmentSpecifier " + lt1);

        final Symbol resolved = resolveWithOutput(lt1);
        final boolean result;
        if (resolved == null) {
            result = false;
        } else {
            result = resolved.getClassification().contains(TypeClassification.AlignmentSpecifier_);
        }
        debugPrintln(" " + result);
        return result;
    }

    public boolean IsAtomicTypeSpecifier() {
        if (noSemantics.contains("IsAtomicTypeSpecifier"))
            return true;
        Token lt1 = this.getInputStream().LT(1);
        debugPrint("IsAtomicTypeSpecifier " + lt1);

        final Symbol resolved = resolveWithOutput(lt1);
        final boolean result = resolved != null
                && resolved.getClassification().contains(TypeClassification.AtomicTypeSpecifier_);

        debugPrintln(" " + result);
        return result;
    }

    public boolean IsAttributeDeclaration() {
        if (noSemantics.contains("IsAttributeDeclaration"))
            return true;
        return IsAttributeSpecifierSequence();
    }

    public boolean IsAttributeSpecifier() {
        if (noSemantics.contains("IsAttributeSpecifier"))
            return true;
        final Token lt1 = this.getInputStream().LT(1);
        debugPrint("IsAttributeSpecifier " + lt1);

        final boolean result = lt1.getType() == SSCLexer.LeftBracket;
        debugPrintln(" " + result);
        return result;
    }

    public boolean IsAttributeSpecifierSequence() {
        if (noSemantics.contains("IsAttributeSpecifierSequence"))
            return true;
        return IsAttributeSpecifier();
    }

    public boolean IsDeclaration() {
        if (noSemantics.contains("IsDeclaration"))
            return true;
        debugPrintln("IsDeclaration");

        final boolean result = IsDeclarationSpecifiers()
                || IsAttributeSpecifierSequence()
                || IsStaticAssertDeclaration()
                || IsAttributeDeclaration();
        debugPrintln("IsDeclaration " + result);
        return result;
    }

    public boolean IsDeclarationSpecifier() {
        if (noSemantics.contains("IsDeclarationSpecifier"))
            return true;
        Token lt1 = this.getInputStream().LT(1);
        debugPrintln("IsDeclarationSpecifier " + lt1);

        final boolean result = IsStorageClassSpecifier()
                || IsTypeSpecifierQualifier()
                || IsFunctionSpecifier()
                || IsAlignmentSpecifier();
        debugPrintln("IsDeclarationSpecifier " + result + " for " + lt1);
        return result;
    }

    public boolean IsTypeSpecifierQualifier() {
        if (noSemantics.contains("IsTypeSpecifierQualifier"))
            return true;
        debugPrintln("IsDeclarationSpecifier");

        final boolean result = IsTypeSpecifier()
                || IsTypeQualifier()
                || IsAlignmentSpecifier()
                || IsTemplateTypeSpecifier() // SSC
                ;

        debugPrintln("IsDeclarationSpecifier " + result);
        return result;
    }

    public boolean IsDeclarationSpecifiers() {
        return IsDeclarationSpecifier();
    }

    public boolean IsEnumSpecifier() {
        if (noSemantics.contains("IsEnumSpecifier"))
            return true;
        Token lt1 = this.getInputStream().LT(1);
        debugPrint("IsEnumSpecifier " + lt1);
        boolean result = lt1.getType() == SSCLexer.Enum;
        debugPrintln(" " + result);
        return result;
    }

    public boolean IsFunctionSpecifier() {
        if (noSemantics.contains("IsFunctionSpecifier"))
            return true;
        Token lt1 = this.getInputStream().LT(1);
        debugPrint("IsFunctionSpecifier " + lt1);
        Symbol resolved = resolveWithOutput(lt1);

        final boolean result;
        if (resolved == null) {
            result = false;
        } else {
            result = resolved.getClassification().contains(TypeClassification.FunctionSpecifier_);
        }
        debugPrintln("IsFunctionSpecifier " + result);
        return result;
    }

    public boolean IsStatement() {
        if (noSemantics.contains("IsStatement"))
            return true;
        Token t1 = this.getInputStream().LT(1);
        Token t2 = this.getInputStream().LT(2);
        debugPrintln("IsStatement1 " + t1);
        debugPrintln("IsStatement2 " + t2);
        if (t1.getType() == SSCLexer.Identifier && t2.getType() == SSCLexer.Colon) {
            debugPrint("IsStatement3 true");
            return true;
        }

        final boolean result = !IsDeclaration();
        debugPrintln("IsStatement " + result);
        return result;
    }

    public boolean IsStaticAssertDeclaration() {
        if (noSemantics.contains("IsStaticAssertDeclaration"))
            return true;
        Token token = this.getInputStream().LT(1);
        debugPrint("IsStaticAssertDeclaration " + token);
        final boolean result = token.getType() == SSCLexer.Static_assert;
        debugPrintln(" " + result);
        return result;
    }

    public boolean IsStorageClassSpecifier() {
        if (noSemantics.contains("IsStorageClassSpecifier"))
            return true;
        Token lt1 = this.getInputStream().LT(1);
        debugPrint("IsStorageClassSpecifier " + lt1);
        Symbol resolved = resolveWithOutput(lt1);

        final boolean result;
        if (resolved == null) {
            result = false;
        } else {
            result = resolved.getClassification().contains(TypeClassification.StorageClassSpecifier_);
        }

        debugPrintln(" " + result);
        return result;
    }

    public boolean IsStructOrUnionSpecifier() {
        if (noSemantics.contains("IsStructOrUnionSpecifier"))
            return true;
        Token token = this.getInputStream().LT(1);
        debugPrint("IsStructOrUnionSpecifier " + token);
        boolean result = token.getType() == SSCLexer.Struct ||
                token.getType() == SSCLexer.Union;
        debugPrintln(" " + result);
        return result;
    }

    public boolean IsTypedefName() {
        if (noSemantics.contains("IsTypedefName"))
            return true;
        Token lt1 = this.getInputStream().LT(1);
        debugPrint("IsTypedefName " + lt1);
        Symbol resolved = resolveWithOutput(lt1);

        // Assume typedef if symbol is not classified as these
        boolean result = resolved != null
                && !resolved.getClassification().contains(TypeClassification.Variable_)
                && !resolved.getClassification().contains(TypeClassification.Function_)
                && !resolved.getClassification().contains(TypeClassification.TemplateTypeSpecifier_) // treat these differently
                ;

        debugPrintln(" " + result);
        return result;
    }

    public boolean IsTypeofSpecifier() {
        if (noSemantics.contains("IsTypeofSpecifier"))
            return true;
        Token token = this.getInputStream().LT(1);
        debugPrint("IsTypeofSpecifier " + token);
        final boolean result = token.getType() == SSCLexer.Typeof ||
                token.getType() == SSCLexer.Typeof_unqual;
        debugPrintln(" " + result);
        return result;
    }

    public boolean IsTypeQualifier() {
        if (noSemantics.contains("IsTypeQualifier"))
            return true;
        Token lt1 = this.getInputStream().LT(1);
        debugPrint("IsTypeQualifier " + lt1);
        Symbol resolved = resolveWithOutput(lt1);
        final boolean result =
                resolved != null && resolved.getClassification().contains(TypeClassification.TypeQualifier_);
        debugPrintln(" " + result);
        return result;
    }

    public boolean IsTypeSpecifier() {
        if (noSemantics.contains("IsTypeSpecifier"))
            return true;
        Token lt1 = this.getInputStream().LT(1);
        debugPrint("IsTypeSpecifier " + lt1);
        Symbol resolved = resolveWithOutput(lt1);
        final boolean isExplicitlyTypeSpecifier =
                resolved != null && resolved.getClassification().contains(TypeClassification.TypeSpecifier_);

        if (isExplicitlyTypeSpecifier) {
            debugPrintln(" true");
            return true;
        }

        debugPrintln(" not explicitly... maybe one of these:");
        final boolean result = IsAtomicTypeSpecifier()
                || IsStructOrUnionSpecifier()
                || IsEnumSpecifier()
                || IsTypedefName()
                || IsTypeofSpecifier()
                || IsTemplateTypeSpecifier();

        debugPrintln("> IsTypeSpecifier = " + result);

        return result;
    }

    // SSC
    public boolean IsTemplateTypeSpecifier() {
        final Token lt1 = this.getInputStream().LT(1);

        debugPrint("IsTemplateTypeSpecifier " + lt1);

        final Symbol resolved = resolveWithOutput(lt1);
        if (resolved == null) {
            debugPrintln(" false (did not find any matching symbol)");
            return false;
        }

        final boolean result = resolved.getClassification().contains(TypeClassification.TemplateTypeSpecifier_);
        debugPrintln(" " + result);
        return result;
    }

    // SSC
    public void EnterTemplate() {
        if (!(this.getContext() instanceof SSCParser.FunctionTemplateDefinitionContext tmplDefCtx)) {
            return;
        }
        debugPrintln("Entering template function definition");

        debugPrintln("Template types: " + tmplDefCtx.Identifier().stream().map(TerminalNode::getText).toList());

        symbolTable.pushBlockScope();

        for (final var identifier : tmplDefCtx.Identifier()) {
            final String identifierText = identifier.getText();

            final HashSet<TypeClassification> classSet = new HashSet<>(Arrays.asList(
                    TypeClassification.TemplateTypeSpecifier_,
                    TypeClassification.TypeSpecifier_
            ));

            final Symbol symbol = new Symbol();
            symbol.setName(identifierText);
            symbol.setClassification(classSet);

            symbolTable.define(symbol);
            debugPrintln("New template type specifier defined: '" + identifierText + "'\t// " + symbol);
        }
    }

    public void EnterDeclaration() {
        debugPrintln("EnterDeclaration");
        ParserRuleContext context = this.getContext();
        while (context != null) {
            if (context instanceof SSCParser.DeclarationContext declaration_context) {
                SSCParser.DeclarationSpecifiersContext declaration_specifiers = declaration_context.declarationSpecifiers();
                SSCParser.DeclarationSpecifierContext[] declaration_specifier = declaration_specifiers != null ?
                        declaration_specifiers.declarationSpecifier().toArray(new SSCParser.DeclarationSpecifierContext[0]) : null;

                // Declare any typeSpecifiers that declare something.
                if (declaration_specifier != null) {
                    for (SSCParser.DeclarationSpecifierContext ds : declaration_specifier) {
                        if (ds.storageClassSpecifier() != null && ds.storageClassSpecifier().Typedef() != null) {
                            break;
                        }
                    }
                    for (SSCParser.DeclarationSpecifierContext ds : declaration_specifier) {
                        if (ds != null && ds.typeSpecifier() != null) {
                            var sous = ds.typeSpecifier().structOrUnionSpecifier();
                            if (sous != null && sous.Identifier() != null) {
                                var idToken = sous.Identifier().getSymbol();
                                var id = idToken.getText();
                                if (id != null) {
                                    debugPrintln("New symbol Declaration1 Declarator " + id);
                                    Symbol symbol = new Symbol();
                                    symbol.setName(id);
                                    HashSet<TypeClassification> classSet = new HashSet<>();
                                    classSet.add(TypeClassification.TypeSpecifier_);
                                    symbol.setClassification(classSet);
                                    SourceLocation loc = getSourceLocation(idToken);
                                    symbol.setDefinedFile(loc.file);
                                    symbol.setDefinedLine(loc.line);
                                    symbol.setDefinedColumn(loc.column);
                                    symbolTable.define(symbol);
                                }
                            }
                        }
                    }
                }

                SSCParser.InitDeclaratorListContext init_declarator_list = declaration_context.initDeclaratorList();
                List<SSCParser.InitDeclaratorContext> init_declarators = init_declarator_list != null ?
                        init_declarator_list.initDeclarator() : null;

                if (init_declarators != null) {
                    boolean isTypedef = false;
                    if (declaration_specifier != null) {
                        for (SSCParser.DeclarationSpecifierContext ds : declaration_specifier) {
                            if (ds.storageClassSpecifier() != null && ds.storageClassSpecifier().Typedef() != null) {
                                isTypedef = true;
                                break;
                            }
                        }
                    }
                    for (SSCParser.InitDeclaratorContext id : init_declarators) {
                        SSCParser.DeclaratorContext y = id != null ? id.declarator() : null;
                        Token idToken = getDeclarationToken(y);
                        if (idToken != null) {
                            String text = idToken.getText();
                            SourceLocation loc = getSourceLocation(idToken);
                            Symbol symbol = new Symbol();
                            symbol.setName(text);
                            HashSet<TypeClassification> classSet = new HashSet<>();
                            if (isTypedef) {
                                classSet.add(TypeClassification.TypeSpecifier_);
                                symbol.setClassification(classSet);
                                symbol.setDefinedFile(loc.file);
                                symbol.setDefinedLine(loc.line);
                                symbol.setDefinedColumn(loc.column);
                                symbolTable.define(symbol);
                                debugPrintln("New symbol Declaration2 Declarator " + symbol);
                            } else {
                                classSet.add(TypeClassification.Variable_);
                                symbol.setClassification(classSet);
                                symbol.setDefinedFile(loc.file);
                                symbol.setDefinedLine(loc.line);
                                symbol.setDefinedColumn(loc.column);
                                symbolTable.define(symbol);
                                debugPrintln("New symbol Declaration3 Declarator " + symbol);
                            }
                        }
                    }
                }
            }
            if (context instanceof SSCParser.FunctionDefinitionContext funcdefCtx) {
                SSCParser.DeclaratorContext de = funcdefCtx.declarator();
                SSCParser.DirectDeclaratorContext dd = de != null ? de.directDeclarator() : null;
                if (dd != null && dd.Identifier() != null) {
                    Token idToken = dd.Identifier().getSymbol();
                    String text = idToken.getText();
                    SourceLocation loc = getSourceLocation(idToken);
                    Symbol symbol = new Symbol();
                    symbol.setName(text);
                    HashSet<TypeClassification> classSet = new HashSet<>();
                    classSet.add(TypeClassification.Function_);
                    symbol.setClassification(classSet);
                    symbol.setDefinedFile(loc.file);
                    symbol.setDefinedLine(loc.line);
                    symbol.setDefinedColumn(loc.column);
                    symbolTable.define(symbol);
                    debugPrintln("New symbol Declarationf Declarator " + symbol);
                    return;
                }
            }
            context = context.getParent();
        }
    }

    private String getDeclarationId(SSCParser.DeclaratorContext y) {
        Token token = getDeclarationToken(y);
        return token != null ? token.getText() : null;
    }

    private Token getDeclarationToken(SSCParser.DeclaratorContext y) {
        // Go down the tree and find a declarator with Identifier.
        if (y == null) {
            return null;
        }

        // Check if this declarator has a direct declarator with an identifier
        SSCParser.DirectDeclaratorContext directDeclarator = y.directDeclarator();
        if (directDeclarator == null) {
            return null;
        }

        SSCParser.DeclaratorContext more = directDeclarator.declarator();
        Token token = getDeclarationToken(more);
        if (token != null) {
            return token;
        }

        if (directDeclarator.Identifier() != null) {
            return directDeclarator.Identifier().getSymbol();
        }

        return null;
    }

    // Define to return "true" because "gcc -c -std=c2x" accepts an empty
    // struct-declaration-list.
    public boolean IsNullStructDeclarationListExtension() {
        //TODO? figure out what they meant by this
        if (noSemantics.contains("IsNullStructDeclarationListExtension"))
            return true;
        return true;
    }

    public void EnterScope() {
        debugPrintln("EnterScope");
        symbolTable.pushBlockScope();
    }

    public void ExitScope() {
        debugPrintln("ExitScope");
        symbolTable.popBlockScope();
    }

    public void LookupSymbol() {
        // Get the token that was just parsed (the Identifier)
        Token token = this.getInputStream().LT(-1);
        if (token == null)
            return;
        String text = token.getText();
        Symbol resolved = symbolTable.resolve(text);
        if (outputAppliedOccurrences && resolved != null) {
            SourceLocation appliedLoc = getSourceLocation(token);
            System.err.println("Applied occurrence: " + text + " at " + appliedLoc.file + ":" + appliedLoc.line + ":" + appliedLoc.column +
                    " -> Defined at " + resolved.getDefinedFile() + ":" + resolved.getDefinedLine() + ":" + resolved.getDefinedColumn());
        }
    }

    public void OutputSymbolTable() {
        if (outputSymbolTable) {
            System.err.println(symbolTable);
        }
    }

    private Symbol resolveWithOutput(Token token) {
        if (token == null)
            return null;
        String text = token.getText();
        Symbol resolved = symbolTable.resolve(text);
        if (outputAppliedOccurrences && resolved != null) {
            SourceLocation appliedLoc = getSourceLocation(token);
            System.err.println("Applied occurrence: " + text + " at " + appliedLoc.file + ":" + appliedLoc.line + ":" + appliedLoc.column +
                    " -> Defined at " + resolved.getDefinedFile() + ":" + resolved.getDefinedLine() + ":" + resolved.getDefinedColumn());
        }
        return resolved;
    }

    // Helper class to hold source location information
    private static class SourceLocation {
        String file;
        int line;
        int column;

        SourceLocation(String file, int line, int column) {
            this.file = file;
            this.line = line;
            this.column = column;
        }
    }

    // Compute source file, line, and column from a token, accounting for #line directives
    private SourceLocation getSourceLocation(Token token) {
        if (token == null) {
            return new SourceLocation("", 0, 0);
        }

        String fileName = "<unknown>";
        int line = token.getLine();
        int column = token.getCharPositionInLine();
        int lineAdjusted = line;

        CommonTokenStream ts = (CommonTokenStream) this.getInputStream();
        int ind = token.getTokenIndex();

        // Search back from token index to find last LineDirective
        for (int j = ind; j >= 0; j--) {
            Token t = ts.get(j);
            if (t == null)
                break;
            if (t.getType() != SSCLexer.LineDirective) {
                continue;
            }

            // Found it
            String txt = t.getText();
            String[] parts = txt.split("\\s+");
            if (parts.length < 3) {
                break;
            }

            int dirLine;
            try {
                dirLine = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                break;
            }

            int lineDirective = t.getLine();
            int lineDiff = line - lineDirective;
            lineAdjusted = lineDiff + dirLine - 1;
            fileName = parts[2].trim();
            // Remove quotes if present
            if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                fileName = fileName.substring(1, fileName.length() - 1);
            }
            break;
        }

        return new SourceLocation(fileName, lineAdjusted, column);
    }

    public boolean IsCast() {
        // Look for a cast.
        if (noSemantics.contains("IsCast"))
            return true;

        Token t1 = this.getInputStream().LT(1);
        Token t2 = this.getInputStream().LT(2);
        debugPrintln("IsCast1 " + t1);
        debugPrintln("IsCast2 " + t2);

        final boolean result;
        if (t1.getType() != SSCLexer.LeftParen) {
            result = false;
            debugPrint("No paren");
        } else if (t2.getType() != SSCLexer.Identifier) {
            // Assume typecast until otherwise.
            result = true;
        } else {
            // Check id.
            final Symbol resolved = resolveWithOutput(t2);
            if (resolved == null) {
                result = false;
                debugPrint("Couldn't resolve " + t2);
            } else {
                result = resolved.getClassification().contains(TypeClassification.TypeSpecifier_);
                debugPrint("Resolved classification: " + resolved.getClassification());
            }
        }

        debugPrintln("IsCast " + result);
        return result;
    }


    private void debugPrint(String msg) {
        if (this.debug) {
            System.out.print(msg);
        }
    }

    private void debugPrintln(String msg) {
        if (this.debug) {
            System.out.println(msg);
        }
    }


    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
}
