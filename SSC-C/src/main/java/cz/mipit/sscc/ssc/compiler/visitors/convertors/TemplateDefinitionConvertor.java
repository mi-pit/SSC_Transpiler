package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.RuleContext;

import java.util.stream.Collectors;

public class TemplateDefinitionConvertor extends AbstractConvertor<SSCParser.TemplateDefinitionContext> {
    public TemplateDefinitionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.TemplateDefinitionContext.class);
    }

    @Override
    public String convert(SSCParser.TemplateDefinitionContext ctx) {
        collect(ctx);
        return ""; // templates only exist when called
    }

    private void collect(SSCParser.TemplateDefinitionContext ctx) {
        Main.logger.printDebug("Found template definition");

        if (ctx.declarationSpecifiers() != null) {
            processTypedef(ctx);
            return;
        }

        final String name = parseRawName(ctx);
        Main.logger.printDebug("\tName: " + name);

        if (!dispatcher.state.templates().containsKey(name)) {
            Main.logger.printDebug("\tTemplate '" + name + "' does not exist yet; creating new template.");
            final Template thisTmpl = new Template(name, ctx);

            Main.logger.printDebug(() -> "\t" + thisTmpl);
            final String tmplName = thisTmpl.name();

            dispatcher.state.templates().put(tmplName, thisTmpl);
            return;
        }

        final Template definedBefore = dispatcher.state.templates().get(name);
        if (definedBefore.contexts().isEmpty()) {
            throw new IllegalStateException("Template '" + name + "' has no contexts, yet it exists");
        }

        Main.logger.printDebug("Template with name \"" + name + "\" has been seen before. Trying to see if it's valid...");
        final boolean justFinishingSSDefinition;
        if (definedBefore.contexts().size() > 1) {
            Main.logger.printDebug(() ->
                    "\tPreviously defined in too many contexts: \""
                    + definedBefore.contexts().stream().map(RuleContext::getText).collect(Collectors.joining(", "))
                    + "\""
            );
            justFinishingSSDefinition = false;
        } else if (definedBefore.contexts().getFirst().superStructInterface() == null) {
            Main.logger.printDebug("\tPrevious context was not a superstruct interface");
            justFinishingSSDefinition = false;
        } else if (ctx.superStructSpecifier() == null) {
            Main.logger.printDebug("\tCurrent context is not a superstruct specifier");
            justFinishingSSDefinition = false;
        } else {
            Main.logger.printDebug("\tTemplate definition is valid in this context, continuing...");
            justFinishingSSDefinition = true;
        }

        if (!justFinishingSSDefinition) {
            throw dispatcher.getSSCLanguageException(
                    "Template '" + name + "' already exists", ctx
            );
        }

        definedBefore.addContext(ctx);
    }

    private void processTypedef(SSCParser.TemplateDefinitionContext tmplDef) {
        final SSCParser.DeclarationSpecifiersContext declSpecsCtx = tmplDef.declarationSpecifiers();
        if (declSpecsCtx
                .declarationSpecifier()
                .stream()
                .noneMatch(ds ->
                        ds.storageClassSpecifier() != null
                        && ds.storageClassSpecifier().Typedef() != null
                )
        ) {
            throw dispatcher.getSSCLanguageException(
                    "Declaration in a template definition must be a typedef", tmplDef
            );
        }

        throw dispatcher.getSSCLanguageException(
                "template typedefs not supported yet", tmplDef.declarationSpecifiers()
        );
    }

    private String parseRawName(SSCParser.TemplateDefinitionContext templateDefinitionContext) {
        if (templateDefinitionContext.functionDefinition() != null) {
            return dispatcher.visit(
                    templateDefinitionContext
                            .functionDefinition()
                            .functionHeader()
                            .declarator()
                            .directDeclarator()
                            .Identifier()
            );
        } else if (templateDefinitionContext.superStructInterface() != null) {
            // superStructInterface : Superstruct Identifier Interface '{' (functionHeader ';')+ '}'
            return dispatcher.visit(
                    templateDefinitionContext
                            .superStructInterface()
                            .Identifier()
            );
        } else if (templateDefinitionContext.superStructSpecifier() != null) {
            // TODO: parse directDeclarator: (declarator)
            //  (not just here)

            return dispatcher.visit(
                    templateDefinitionContext
                            .superStructSpecifier()
                            .Identifier()
            );
        }

        throw new IllegalStateException("Template definition context must be a function, ss-interface or declaration");
    }
}
