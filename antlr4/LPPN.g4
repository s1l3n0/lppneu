grammar LPPN;

@header {}

/*----------------
* PARSER RULES
*----------------*/

/** The overall program consists of directives, facts and rules. */
program : (situationfact | eventfact | logicrule | causalrule)* EOF ;

/** A fact is the head followed by a dot. */
situationfact : head DOT ;

/** An event is a DO prefix with a head followed by a dot. */
eventfact : CAUSES operation DOT ;

/** A logic rule can be a normal rule or a constraint. */
logicrule : normrule | constraint ;

/** A normal rule has a head and body. */
normrule : head IS_IMPLIED_BY body DOT ;

/** A constraint has NO head, only a body. */
constraint : IS_IMPLIED_BY body DOT ;

/** A causal rule can be a Condition-Action, an Event-Condition-Action rule or a dependency. */
causalrule : eventcondition_expression CAUSES operation DOT ;

eventcondition_expression : body_situation |
| event WHEN body_expression
| LPAR eventcondition_expression RPAR
| eventcondition_expression SEQ eventcondition_expression
| eventcondition_expression (PAR | ALT) eventcondition_expression
| body_expression AND body_expression
| body_expression (OR | XOR) body_expression
;

/** The head consists of a literal. */
head : head_expression ;

/** The body consists of a list of literals or expressions separated. */
body : body_expression ;

/** A list of literals is separated by comma */
list_literals : literal ( COMMA list_literals )? ;

/** A literal expression is a literal or compositions of it */

head_situation : literal ;

head_expression : head_situation
| LPAR head_expression RPAR
| head_expression SEQ head_expression
| head_expression (PAR | ALT) head_expression
| head_expression AND head_expression
| head_expression (OR | XOR) head_expression
;

event : literal ;

operation : event
| LPAR operation RPAR
| operation SEQ operation
| operation (PAR | ALT) operation
;

/** An extended literal expression is an extended literal situation or compositions of it */
body_situation : ext_literal ;

body_expression : body_situation | body_constraint
| LPAR body_expression RPAR
| body_expression SEQ body_expression
| body_expression (PAR | ALT) body_expression
| body_expression AND body_expression
| body_expression (OR | XOR) body_expression
;

/** A constraint is a boolean function operating variables and constants */
body_constraint : ( identifier | variable | INTEGER | num_expression ) ( EQ | NEQ | LT | LE | GT | GE ) ( IDENTIFIER | VARIABLE | INTEGER | num_expression) ;

/** A numeric expression is an integer expression made by numeric variables and constants */
num_expression : (variable | INTEGER) (PLUS | MINUS) (variable | INTEGER) ;

/** An extended literal adds the default negation to the classic literal */
ext_literal : ( NOT | TILDE )? literal ;

/** A literal can be positive or negative. */
literal : ( NEG | MINUS )? pos_literal ;

/** A positive literal consists of symbols (no predicates) or symbols and terms (predicate literal). */
pos_literal : predicate ( LPAR list_parameters RPAR )? ;

/** Parameters are separated by comma. */
list_parameters :  parameter (COMMA list_parameters)? ;

/** Parameters are variables, constants (e.g. numbers), numeric expressions of those or positive literals  */
parameter : variable | constant | pos_literal | num_expression ;

predicate : IDENTIFIER ;
identifier : IDENTIFIER ;
constant : INTEGER ;
variable : VARIABLE ;

/*----------------
* LEXER RULES
*----------------*/

WS : (' ' | '\t' | '\n' | '\r' | '\f')+ -> skip ;

// boolean operators
AND : '&' | 'AND' | 'and' ;
OR : '|' | 'OR' | 'or' ;
XOR : 'XOR' | 'xor' ;

// event operators
SEQ : 'SEQ' | 'seq' ;
PAR : 'PAR' | 'par' ;
ALT : 'ALT' | 'alt' ;
NOT : 'not' | 'NOT' ; // default negation
NEG : 'neg' | 'NEG' ; // strong negation

CAUSES : '->' |  '=>';
IS_IMPLIED_BY : ':-' ;


WHEN : ':' | 'in' ;
PLUS : '+' ;
MINUS : '-' ; // both for strong negation and arithmetic deletion

TILDE : '~' ; // used for the union between default and strong

DOT : '.' ;
COMMA : ',' ;

LPAR : '(' ;
RPAR : ')' ;
LACC : '{' ;
RACC : '}' ;

EQ : '=' ;
NEQ : '!=' ;
GT : '>' ;
LT : '<' ;
GE : '>=' ;
LE : '<=' ;

DOMAIN : '#domain' ;

RANGE : '..' ;
INTEGER : '0' | [1-9] ([0-9])* ;
IDENTIFIER: [a-z] ([0-9a-zA-Z_])* ;
VARIABLE: [_A-Z] ([0-9a-zA-Z_])* ;

// in addition to ASP comments (%) I added the standard C, Java comment style.
SINGLE_LINE_COMMENT : ('%' | '//') ~('\n'|'\r')* -> channel(HIDDEN) ;
MULTILINE_COMMENT : '/*' .*? ( '*/' | EOF ) -> channel(HIDDEN) ;