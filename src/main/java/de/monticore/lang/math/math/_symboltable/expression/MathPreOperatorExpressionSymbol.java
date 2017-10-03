package de.monticore.lang.math.math._symboltable.expression;

/**
 * @author Sascha Schneiders
 */
public class MathPreOperatorExpressionSymbol extends MathExpressionSymbol {
    protected MathExpressionSymbol mathExpressionSymbol;
    protected String operator;

    public MathExpressionSymbol getMathExpressionSymbol() {
        return mathExpressionSymbol;
    }

    public void setMathExpressionSymbol(MathExpressionSymbol mathExpressionSymbol) {
        this.mathExpressionSymbol = mathExpressionSymbol;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public boolean isPreOperatorExpression() {
        return true;
    }

    @Override
    public String getTextualRepresentation() {
        return operator + mathExpressionSymbol.getTextualRepresentation();
    }

    /* TODO return negated expression
    @Override
    public MathExpressionSymbol getRealMathExpressionSymbol(){
        return mathExpressionSymbol;
    }*/
}
