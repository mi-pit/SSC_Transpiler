package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.tree.TerminalNode;

public class TemplateDefinitionConvertor extends AbstractConvertor<SSCParser.FunctionTemplateDefinitionContext> {
    public TemplateDefinitionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public String convert(SSCParser.FunctionTemplateDefinitionContext ctx) {
        final String name = parseRawName(ctx);
        final Template tmpl = new Template(name, ctx);

        final String tmplName = tmpl.getName();
        if (dispatcher.data.templates().containsKey(tmplName)) {
            throw dispatcher.getSSCSyntaxException("Template '" + tmplName + "' already exists", ctx);
        }
        dispatcher.data.templates().put(tmplName, tmpl);

        return ""; // templates only exist when called
    }

    private String parseRawName(SSCParser.FunctionTemplateDefinitionContext ctx) {
        final TerminalNode nameIdentifier =
                // TODO: nullptr check
                ctx.functionDefinition().functionHeader().declarator().directDeclarator().Identifier();

        return dispatcher.visitTerminal(nameIdentifier);
    }
}
