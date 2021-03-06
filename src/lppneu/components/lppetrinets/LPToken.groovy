package lppneu.components.lppetrinets

import groovy.util.logging.Log4j
import lppneu.components.language.Atom
import lppneu.components.language.Expression
import lppneu.components.language.Operator
import lppneu.components.language.Parameter
import lppneu.components.language.PosLiteral
import pneu.components.petrinet.Token

@Log4j
class LPToken extends Token {
    Expression expression

    String toString() {
        if (expression)
            expression.toString()
        else
            ""
    }

    String label() {
        toString()
    }

    // LP net tokens are equal if they are of the same type and transports the same content
    Boolean compare(Token t) {
        if (t.class != LPToken)
            throw new RuntimeException("You should not be here")

        LPToken lpt = (LPToken) t

        // for propositional content
        if (expression == null && lpt.expression == null) return true

//        log.trace "formula: "+ expression.reify().formula == lpt.expression.reify().formula
//        log.trace "situation: "+ expression.reify().formula.inputPorts[0] == lpt.expression.reify().formula.inputPorts[0]
//        log.trace "parameter: "+ expression.reify().formula.inputPorts[0].factLiteral.parameters[0] == lpt.expression.reify().formula.inputPorts[0].factLiteral.parameters[0]
//        log.trace "literal: "+ expression.reify().formula.inputPorts[0].factLiteral.parameters[0].literal == lpt.expression.reify().formula.inputPorts[0].factLiteral.parameters[0].literal
//        log.trace "expression: "+expression.reify().formula.inputPorts[0].factLiteral.parameters[0].literal.dump()
//        log.trace "token experssion: "+lpt.expression.reify().formula.inputPorts[0].factLiteral.parameters[0].literal.dump()

        expression.reify() == lpt.expression.reify()

    }

    static Boolean compare(Token t1, Token t2) {
        t1.compare(t2)
    }


    Boolean subsumes(Token t, Map<String, Map<String, String>> mapIdentifiers = [:], Boolean negateHead = false) {

        if (t.class != LPToken)
            throw new RuntimeException("Wrong type of token")

        Token lpt = (LPToken) t

        if (!negateHead)
            expression.subsumes(lpt.expression, mapIdentifiers)
        else {
            expression.negate().subsumes(lpt.expression, mapIdentifiers) || expression.nullify().subsumes(lpt.expression, mapIdentifiers)
        }
    }

    Token minimalClone() {
        return new LPToken(
                expression: expression.minimalClone()
        )
    }

    // check if it respects the unification filter
    Boolean checkWithFilter(Map<String, String> varWithValuesMap) {
        log.trace("checking token with filter")
        for (param in expression.parameters) {
            log.trace("parameter: " + param)
            if (param.isVariable()) {
                if (varWithValuesMap.keySet().contains(param.variable.name)) {
                    log.trace("variable included in the filter, with value: " + varWithValuesMap[param.variable.name])
                    if (varWithValuesMap[param.variable.name] != param.variable.identifier)
                        return false
                }
            }
        }
        return true
    }

    // check if it respects the unification filter
    Map<String, String> toVarWithValuesMap() {
        Map<String, String> varWithValuesMap = [:]
        for (param in expression.parameters) {
            if (param.isVariable()) {
                varWithValuesMap[param.variable.name] = param.variable.identifier
                // TODO: numeric values
            }
        }
        varWithValuesMap
    }

    // return the list of variables grounded
    static List<String> fillParametersWithContent(List<Parameter> parameters, Map<String, String> content) {
        List<String> groundedVarStringList = []
        for (param in parameters) {
            if (param.isVariable()) {
                if (content.containsKey(param.variable.name)) {
                    log.trace("filling " + param.variable.name + " with " + content[param.variable.name])
                    param.variable.identifier = PosLiteral.build(Atom.build(content[param.variable.name]))
                    groundedVarStringList = groundedVarStringList - [param.variable.name] + [param.variable.name]
                } else {
                    throw new RuntimeException("I don't know how to ground this variable: " + param.variable.name)
                }
            } else if (param.isLiteral()) {
                if (param.literal.parameters.size() > 0) {
                    List<String> newGroundedVars = []
                    newGroundedVars = fillParametersWithContent(param.literal.parameters, content)
                    groundedVarStringList = (groundedVarStringList - newGroundedVars) + newGroundedVars
                }
            }
        }
        groundedVarStringList
    }

    // creates a token with a map that associates identifiers to variables
    // the missing items are filled with anonymous identifiers
    static Token createToken(Expression expression, Map<String, String> content) {

        log.trace("complete content to be forged: " + content)

        Expression tokenExpression, contextExpression
        if (expression != null) {
            tokenExpression = expression.minimalClone()

            log.trace("token expression used as a forge: " + tokenExpression)

            List<String> groundedVarStringList = fillParametersWithContent(tokenExpression.getParameters(), content)

            for (groundedVarString in groundedVarStringList)
                content.remove(groundedVarString)

            log.trace("forged explicit expression: " + tokenExpression)
        }

        if (content.size() > 0) {
            contextExpression = Expression.buildNoFunctorExpFromVarStringList(content.keySet())
            log.trace("context expression used as a forge: " + contextExpression)

            fillParametersWithContent(contextExpression.getParameters(), content)
            log.trace("forged context expression: " + contextExpression)
        }

        if (tokenExpression && contextExpression) {
            new LPToken(expression: Expression.build(tokenExpression, contextExpression, Operator.AND))
        } else if (tokenExpression) {
            new LPToken(expression: tokenExpression)
        } else if (contextExpression) {
            new LPToken(expression: contextExpression)
        } else {
            new LPToken()
        }

    }

    // creates a token with a map that associates identifiers to variables
    // all the items are put into a context anonymous predicate
    static Token createToken(Map<String, String> varStringMap) {
        createToken(null, varStringMap)
    }
}

