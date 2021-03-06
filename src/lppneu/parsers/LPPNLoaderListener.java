package lppneu.parsers;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.apache.log4j.Logger;
import lppneu.components.language.*;
import lppneu.parser.LPPNBaseListener;
import lppneu.parser.LPPNParser;

import java.util.ArrayList;
import java.util.List;


public class LPPNLoaderListener extends LPPNBaseListener {

    private LPPNProgram program;

    public LPPNProgram getProgram() {
        return program;
    }

    private final static Logger log = Logger.getLogger("LPPNLoaderListener");

    // to enrich the parsing tree with ids to already computed values (atoms, formulas, ...)

    // Mapping of nodes
    private ParseTreeProperty<Atom> atomNodes = new ParseTreeProperty<Atom>();
    private ParseTreeProperty<Variable> variableNodes = new ParseTreeProperty<Variable>();
    private ParseTreeProperty<Parameter> parameterNodes = new ParseTreeProperty<Parameter>();
    private ParseTreeProperty<PosLiteral> posLiteralNodes = new ParseTreeProperty<PosLiteral>();
    private ParseTreeProperty<Literal> literalNodes = new ParseTreeProperty<Literal>();
    private ParseTreeProperty<Situation> situationNodes = new ParseTreeProperty<Situation>();
    private ParseTreeProperty<Expression> expressionNodes = new ParseTreeProperty<Expression>();
    private ParseTreeProperty<Event> eventNodes = new ParseTreeProperty<Event>();
    private ParseTreeProperty<Operation> operationNodes = new ParseTreeProperty<Operation>();
    private ParseTreeProperty<LogicRule> logicRuleNodes = new ParseTreeProperty<LogicRule>();
    private ParseTreeProperty<CausalRule> causalRuleNodes = new ParseTreeProperty<CausalRule>();

    // Mapping of (list-type) nodes
    private ParseTreeProperty<List<Object>> listNodes = new ParseTreeProperty<List<Object>>();

    public void addToDecorationList(ParseTree node, Object decoration) {
        List<Object> list = listNodes.get(node);
        if (list == null) {
            list = new ArrayList<Object>();
            listNodes.put(node, list);
        }
        list.add(decoration);
    }

    public List<Object> getDecorationList(ParseTree node) {
        return listNodes.get(node);
    }

    ///////////////// LISTENERS

    public void exitPredicate(LPPNParser.PredicateContext ctx) {
        Atom predicate = Atom.build(ctx.IDENTIFIER().getText());
        atomNodes.put(ctx, predicate);
        log.trace("attaching predicate " + predicate + " to node.");
    }

    public void exitIdentifier(LPPNParser.IdentifierContext ctx) {
        Atom predicate = Atom.build(ctx.IDENTIFIER().getText());
        atomNodes.put(ctx, predicate);
        log.trace("attaching predicate " + predicate + " to node.");
    }

    public void exitVariable(LPPNParser.VariableContext ctx) {
        Variable variable = Variable.build(ctx.VARIABLE().getText());
        variableNodes.put(ctx, variable);
        log.trace("attaching variable " + variable + " to node.");
    }

    public void exitVariable_structure(LPPNParser.Variable_structureContext ctx) {

        // example: A
        Variable variable = variableNodes.get(ctx.variable(0));

        if (ctx.COLON() != null) {

            // example: A: a
            if (ctx.constant() != null) {
                variable.setIdentifier(ctx.constant().getText());
            } else if (ctx.variable(1) != null) {
                variable.setVariable(variableNodes.get(ctx.variable(1)));
            } else {
                throw new RuntimeException("Not valid structure.");
            }
        }

        variableNodes.put(ctx, variable);
        log.trace("attaching variable " + variable + " to node.");
    }

    public void exitParameter(LPPNParser.ParameterContext ctx) {
        Parameter parameter;
        if (ctx.pos_literal() != null) {
            parameter = Parameter.build(posLiteralNodes.get(ctx.pos_literal()));
        } else if (ctx.variable_structure() != null) {
            parameter = Parameter.build(variableNodes.get(ctx.variable_structure()));
        } else if (ctx.constant() != null) {
            throw new RuntimeException("to be implemented");
        } else if (ctx.num_expression() != null) {
            throw new RuntimeException("to be implemented");
        } else {
            throw new RuntimeException("Unknown type of parameter.");
        }
        parameterNodes.put(ctx, parameter);
    }

    // note: the list is constructed in inverse order
    public void exitList_parameters(LPPNParser.List_parametersContext ctx) {

        Parameter parameter = parameterNodes.get(ctx.parameter());
        addToDecorationList(ctx, parameter);

        if (ctx.COMMA() != null) {
            log.trace("attaching parameter " + parameter + " to parameter_list node.");

            List<Object> list = getDecorationList(ctx);
            List<Object> childList = getDecorationList(ctx.list_parameters());
            list.addAll(childList);
            log.trace("merging list with child list node.");

        } else {
            log.trace("attaching single parameter " + parameter + " to new parameter_list node.");
        }
    }

    public void exitPos_literal(LPPNParser.Pos_literalContext ctx) {

        PosLiteral posLiteral = new PosLiteral();
        Atom predicate = atomNodes.get(ctx.predicate());
        posLiteral.setFunctor(predicate);

        if (ctx.list_parameters() != null) {
            List<Object> parameter_list = getDecorationList(ctx.list_parameters());
            List<Parameter> parameters = new ArrayList<Parameter>();
            for (Object parameter : parameter_list) {
                parameters.add((Parameter) parameter);
            }
            posLiteral.setParameters(parameters);
        }

        posLiteralNodes.put(ctx, posLiteral);
    }

    public void exitLiteral(LPPNParser.LiteralContext ctx) {
        Literal literal = Literal.build(posLiteralNodes.get(ctx.pos_literal()));

        if (ctx.NEG() != null)
            literal.negate();
        else if (ctx.NULL() != null)
            literal.nullify();

        literalNodes.put(ctx, literal);
    }

    public void exitExt_literal(LPPNParser.Ext_literalContext ctx) {
        Expression expression;

        Literal literal = literalNodes.get(ctx.literal());

        if (ctx.NOT() != null)
            expression = Expression.build(literal, Operator.DUAL);
        else {
            expression = Expression.build(literal);
        }

        expressionNodes.put(ctx, expression);
    }


    public void exitSituation(LPPNParser.SituationContext ctx) {
        Situation situation = Situation.build(literalNodes.get(ctx.literal()));
        situationNodes.put(ctx, situation);
    }

    public void exitBody_expression(LPPNParser.Body_expressionContext ctx) {
        Expression expression;

        if (ctx.situation() != null) {
            expression = Expression.build(situationNodes.get(ctx.situation()));
        } else if (ctx.body_constraint() != null) {
            throw new RuntimeException("to be implemented");
        } else if (ctx.WHEN() != null) {
            log.trace("operation: " + operationNodes.get(ctx.operation()));
            log.trace("expression: " + operationNodes.get(ctx.operation()).toExpression());
            expression = Expression.build(operationNodes.get(ctx.operation()).toExpression(), expressionNodes.get(ctx.body_expression(0)), Operator.OCCURS_IN);
        } else if (ctx.LPAR() != null) {
            expression = expressionNodes.get(ctx.body_expression(0));
        } else if (ctx.NEG() != null) {
            expression = Expression.build(
                    expressionNodes.get(ctx.body_expression(0)), Operator.NEG
            );
        } else if (ctx.NOT() != null) {
            expression = Expression.build(
                    expressionNodes.get(ctx.body_expression(0)), Operator.DUAL
            );
        } else {
            Operator op;
            if (ctx.AND() != null) op = Operator.AND;
            else if (ctx.OR() != null) op = Operator.OR;
            else if (ctx.XOR() != null) op = Operator.XOR;
            else if (ctx.SEQ() != null) op = Operator.SEQ;
            else if (ctx.PAR() != null) op = Operator.PAR;
            else if (ctx.ALT() != null) op = Operator.ALT;
            else {
                throw new RuntimeException("Unknown operator in expression.");
            }

            expression = Expression.build(
                    expressionNodes.get(ctx.body_expression(0)),
                    expressionNodes.get(ctx.body_expression(1)),
                    op
            );
        }
        expressionNodes.put(ctx, expression);
    }

    public void exitHead_expression(LPPNParser.Head_expressionContext ctx) {
        Expression expression;

        if (ctx.situation() != null) {
            expression = Expression.build(situationNodes.get(ctx.situation()));
        } else if (ctx.WHEN() != null) {
            expression = Expression.build(operationNodes.get(ctx.operation()).toExpression(), expressionNodes.get(ctx.head_expression(0)), Operator.OCCURS_IN);
        } else if (ctx.LPAR() != null) {
            expression = expressionNodes.get(ctx.head_expression(0));
        } else if (ctx.NEG() != null) {
            expression = Expression.build(
                    expressionNodes.get(ctx.head_expression(0)), Operator.NEG
            );
        } else {
            Operator op;
            if (ctx.AND() != null) op = Operator.AND;
            else if (ctx.OR() != null) op = Operator.OR;
            else if (ctx.XOR() != null) op = Operator.XOR;
            else if (ctx.SEQ() != null) op = Operator.SEQ;
            else if (ctx.PAR() != null) op = Operator.PAR;
            else if (ctx.ALT() != null) op = Operator.ALT;
            else {
                throw new RuntimeException("Unknown operator in expression.");
            }

            expression = Expression.build(
                    expressionNodes.get(ctx.head_expression(0)),
                    expressionNodes.get(ctx.head_expression(1)),
                    op
            );
        }
        expressionNodes.put(ctx, expression);
    }

    public void exitHead(LPPNParser.HeadContext ctx) {
        expressionNodes.put(ctx, expressionNodes.get(ctx.head_expression()));
    }

    public void exitBody(LPPNParser.BodyContext ctx) {
        expressionNodes.put(ctx, expressionNodes.get(ctx.body_expression()));
    }

    public void exitEvent(LPPNParser.EventContext ctx) {
        Event event = Event.build(literalNodes.get(ctx.literal()));
        eventNodes.put(ctx, event);
    }

    public void exitCausalrule(LPPNParser.CausalruleContext ctx) {
        CausalRule rule = new CausalRule();
        rule.setCondition(expressionNodes.get(ctx.body_expression()));
        rule.setAction(operationNodes.get(ctx.operation()));
        causalRuleNodes.put(ctx, rule);
    }

    public void exitOperation(LPPNParser.OperationContext ctx) {

        Operation operation;

        if (ctx.event() != null) {
            operation = Operation.build(eventNodes.get(ctx.event()));
        } else if (ctx.LPAR() != null) {
            operation = operationNodes.get(ctx.operation(0));
        } else {
            Operator op;
            if (ctx.SEQ() != null) op = Operator.SEQ;
            else if (ctx.PAR() != null) op = Operator.PAR;
            else if (ctx.ALT() != null) op = Operator.ALT;
            else {
                throw new RuntimeException("Unknown operator in operation.");
            }

            operation = Operation.build(
                    operationNodes.get(ctx.operation(0)),
                    operationNodes.get(ctx.operation(1)),
                    op
            );
        }

        operationNodes.put(ctx, operation);
    }

    public void exitSituationfact(LPPNParser.SituationfactContext ctx) {
        LogicRule rule = new LogicRule();
        rule.setHead(expressionNodes.get(ctx.head()));
        logicRuleNodes.put(ctx, rule);
    }

    public void exitConstraint(LPPNParser.ConstraintContext ctx) {
        LogicRule rule = new LogicRule();
        rule.setBody(expressionNodes.get(ctx.body()));
        logicRuleNodes.put(ctx, rule);
    }

    public void exitNormrule(LPPNParser.NormruleContext ctx) {
        LogicRule rule = new LogicRule();

        if (ctx.IS_EQUIVALENT_TO() != null) { // double implication
            rule.setBiconditional(true);
        } else { // normal implication
            rule.setBiconditional(false);
        }

        rule.setHead(expressionNodes.get(ctx.head()));
        rule.setBody(expressionNodes.get(ctx.body()));
        logicRuleNodes.put(ctx, rule);
    }

    public void exitEventfact(LPPNParser.EventfactContext ctx) {
        CausalRule rule = new CausalRule();
        rule.setAction(operationNodes.get(ctx.operation()));
        causalRuleNodes.put(ctx, rule);
    }

    public void exitLogicrule(LPPNParser.LogicruleContext ctx) {

        if (ctx.constraint() != null) {
            logicRuleNodes.put(ctx, logicRuleNodes.get(ctx.constraint()));
        } else if (ctx.normrule() != null) {
            logicRuleNodes.put(ctx, logicRuleNodes.get(ctx.normrule()));
        }

    }

    public void exitProgram(LPPNParser.ProgramContext ctx) {
        program = new LPPNProgram();

        for (LPPNParser.CausalruleContext childCtx : ctx.causalrule()) {
            program.getCausalRules().add(causalRuleNodes.get(childCtx));
        }

        for (LPPNParser.LogicruleContext childCtx : ctx.logicrule()) {
            program.getLogicRules().add(logicRuleNodes.get(childCtx));
        }

        for (LPPNParser.SituationfactContext childCtx : ctx.situationfact()) {
            program.getLogicRules().add(logicRuleNodes.get(childCtx));
        }

        for (LPPNParser.EventfactContext childCtx : ctx.eventfact()) {
            program.getCausalRules().add(causalRuleNodes.get(childCtx));
        }
    }

}

