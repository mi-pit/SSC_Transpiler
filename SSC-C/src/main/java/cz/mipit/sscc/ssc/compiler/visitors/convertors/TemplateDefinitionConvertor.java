package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

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

        final boolean justFinishingSSDefinition = definedBefore.contexts().size() == 1
                && definedBefore.contexts().getFirst().superStructInterface() != null
                && ctx.superStructSpecifier() != null;

        if (!justFinishingSSDefinition) {
            throw dispatcher.getSSCSyntaxException("Template '" + name + "' already exists", ctx);
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
