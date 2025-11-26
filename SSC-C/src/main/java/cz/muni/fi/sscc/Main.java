package cz.muni.fi.sscc;

import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.mine.ExpressionVisitor;
import cz.muni.fi.sscc.mine.PostfixExpressionVisitor;
import cz.muni.fi.sscc.mine.SSVisitor;
import cz.muni.fi.sscc.mine.data.SSMember;
import cz.muni.fi.sscc.mine.data.SuperStructRepre;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import cz.muni.fi.sscc.antlr.SSCLexer;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collection;

public class Main {
    private static Collection<SuperStructRepre> sss;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: Main <file> [<file>...]");
            System.exit(1);
        }

        for (String fileArg : args) {
            System.out.println("Parsing file: `" + fileArg + "`");
            Path fileAbsolutePath = Path.of(fileArg);

            Path dir = fileAbsolutePath.getParent();
            Path file = fileAbsolutePath.getFileName();

            String input = null;
            try {
                // Fixme
                input = Files.readString(fileAbsolutePath);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }

            SSCLexer lexer = new SSCLexer(CharStreams.fromString(input));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SSCParser parser = new SSCParser(tokens);

            ParseTree tree = parser.compilationUnit();

            try {
                Path tempFile = Files.createTempFile(dir, file.toString(), ".tmp");
                tempFile.toFile().deleteOnExit();
                System.out.println(tempFile);

                /* TODO:
                 *  in `setup.sh`:
                 *      prepend package to .java files
                 *      move files to correct place
                 *  -> Recompile SSC.g4 ('::' operator)
                 *  here:
                 *      extract Superstructs
                 *      print SSs to output file (all at once)
                 *      second pass -- replace `superstruct ‹Name›` with `struct ‹Name›`
                 *          && method calls
                 */
                extractSuperstructMembers(tokens, tree);
                replaceSuperstructCalls(tokens, tree);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            } catch (SSCSyntaxException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static void replaceSuperstructCalls(CommonTokenStream tokens, ParseTree tree) {
        ExpressionVisitor visitor = new PostfixExpressionVisitor(tokens, sss);
        visitor.visit(tree);
    }

    private static void extractSuperstructMembers(CommonTokenStream tokens, ParseTree tree) {
        SSVisitor visitor = new SSVisitor(tokens);
        visitor.visit(tree);

        sss = visitor.getSuperStructs();
        for (SuperStructRepre ss : sss) {
            convertSuperstructToStruct(ss);
        }
    }

    private static void convertSuperstructToStruct(SuperStructRepre ss) {
        System.out.printf("struct %s {\n", ss.name());
        for (SSMember s : ss.member()) {
            if (s.isDeclaration()) {
                assert s.getData().getLeft().isPresent();
                System.out.println("\t" + s.getData().getLeft().get().data());
            }
        }
        System.out.println("}\n");

        for (SSMember s : ss.member()) {
            if (s.isFunctionDefinition()) {
                assert s.getData().getRight().isPresent();
                System.out.println(s.getData().getRight().get().getText().stripLeading());
            }
        }
    }
}
