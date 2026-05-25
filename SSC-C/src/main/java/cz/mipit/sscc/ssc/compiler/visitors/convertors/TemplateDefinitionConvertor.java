package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.RuleContext;

import java.util.stream.Collectors;

public class TemplateDefinitionConvertor extends AbstractConvertor<SSCParser.TemplateDefinitionContext> {
    public TemplateDefinitionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public String convert(SSCParser.TemplateDefinitionContext ctx) {
        collect(ctx);
        return ""; // templates only exist when called
    }

    private void collect(SSCParser.TemplateDefinitionContext ctx) {
        final String name = parseRawName(ctx);

        if (!dispatcher.data.templates().containsKey(name)) {
            final Template thisTmpl = new Template(name, ctx);
            final String tmplName = thisTmpl.name();

            dispatcher.data.templates().put(tmplName, thisTmpl);
            return;
        }

        final Template definedBefore = dispatcher.data.templates().get(name);
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

    private String parseRawName(SSCParser.TemplateDefinitionContext templateDefinitionContext) {
        if (templateDefinitionContext.functionDefinition() != null) {
            return dispatcher.visitTerminal(
                    templateDefinitionContext
                            .functionDefinition()
                            .functionHeader()
                            .declarator()
                            .directDeclarator()
                            .Identifier()
            );
        } else if (templateDefinitionContext.superStructInterface() != null) {
            // superStructInterface : Superstruct Identifier Interface '{' (functionHeader ';')+ '}'
            return dispatcher.visitTerminal(
                    templateDefinitionContext
                            .superStructInterface()
                            .Identifier()
            );
        } else if (templateDefinitionContext.superStructSpecifier() != null) {
            // TODO: parse directDeclarator: (declarator)
            //  (not just here)

            return dispatcher.visitTerminal(
                    templateDefinitionContext
                            .superStructSpecifier()
                            .Identifier()
            );
        }

        throw new IllegalStateException("Template definition context must be one function, ss-interface or declaration");
    }
}
