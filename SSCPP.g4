
grammar SSCPP;

topLevelDeclaration
    : directive
    | NonDirective
    ;

directive
    : '#' (Include | Define | If)
    ;

NonDirective
    : ~'#' ~[\r?\n]*
    ;

Include
    : 'include'
    ;

Define
    : 'define'
    ;

If
    : 'el'? 'if' 'n'? 'def'?
    ;


possibleMacroInvocation
    : Identifier
    | Identifier '(' macroArgumentList ')'
    ;

macroArgumentList
    : macroArgument (',' macroArgument)+
    ;

macroArgument
    : MacroArgument
    ;

MacroArgument
    : ~[)]+?
    ;

LeftParen
    : '('
    ;

RightParen
    : ')'
    ;


/* off */
Identifier
    : IdentifierNondigit (IdentifierNondigit | Digit)*
    ;

fragment IdentifierNondigit
    : Nondigit
    | UniversalCharacterName
    //|   // other implementation-defined characters...
    ;

fragment Nondigit
    : [a-zA-Z_]
    ;

fragment Digit
    : [0-9]
    ;

fragment UniversalCharacterName
    : '\\u' HexQuad
    | '\\U' HexQuad HexQuad
    ;

fragment HexQuad
    : HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit
    ;

fragment HexadecimalDigit
    : [0-9a-fA-F]
    ;


Whitespace
    : [ \t]+ -> channel(HIDDEN)
    ;
