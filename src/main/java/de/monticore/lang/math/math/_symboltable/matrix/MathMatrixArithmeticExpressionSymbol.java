package de.monticore.lang.math.math._symboltable.matrix;

import de.monticore.lang.math.math._symboltable.expression.MathExpressionSymbol;

/**
 * @author Sascha Schneiders
 */
public class MathMatrixArithmeticExpressionSymbol extends MathMatrixExpressionSymbol {

    protected MathExpressionSymbol rightExpression;
    protected MathExpressionSymbol leftExpression;

    protected String mathOperator;

    public MathMatrixArithmeticExpressionSymbol() {
        super();
    }

    public MathExpressionSymbol getRightExpression() {
        return rightExpression;
    }

    public void setRightExpression(MathExpressionSymbol rightExpression) {
        this.rightExpression = rightExpression;
    }

    public MathExpressionSymbol getLeftExpression() {
        return leftExpression;
    }

    public void setLeftExpression(MathExpressionSymbol leftExpression) {
        this.leftExpression = leftExpression;
    }

    public String getMathOperator() {
        return mathOperator;
    }

    public void setMathOperator(String mathOperator) {
        this.mathOperator = mathOperator;
    }

    @Override
    public String getTextualRepresentation() {
        return leftExpression.getTextualRepresentation() + mathOperator + rightExpression.getTextualRepresentation();
    }

    @Override
    public boolean isMatrixArithmeticExpression(){
        return true;
    }
    public boolean isAdditionOperator() {
        return mathOperator.equals("+");
    }


    public boolean isSubtractionOperator() {
        return mathOperator.equals("-");
    }


    public boolean isMultiplicationOperator() {
        return mathOperator.equals("*");
    }

    public boolean isDivisionOperator() {
        return mathOperator.equals("/");
    }


    public boolean isModuloOperator() {
        return mathOperator.equals("%");
    }


    public boolean isPowerOfOperator() {
        return mathOperator.equals("^");
    }

}
