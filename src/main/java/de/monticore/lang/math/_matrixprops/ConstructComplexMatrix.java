/**
 *
 *  ******************************************************************************
 *  MontiCAR Modeling Family, www.se-rwth.de
 *  Copyright (c) 2017, Software Engineering Group at RWTH Aachen,
 *  All rights reserved.
 *
 *  This project is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3.0 of the License, or (at your option) any later version.
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this project. If not, see <http://www.gnu.org/licenses/>.
 * *******************************************************************************
 */
package de.monticore.lang.math._matrixprops;

import de.monticore.lang.math._symboltable.JSValue;
import de.monticore.lang.math._symboltable.expression.*;
import de.monticore.lang.math._symboltable.matrix.MathMatrixAccessOperatorSymbol;
import de.monticore.lang.math._symboltable.matrix.MathMatrixArithmeticValueSymbol;
import de.monticore.lang.math._symboltable.expression.*;
import de.monticore.symboltable.Symbol;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexField;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;

public class ConstructComplexMatrix {
    public static Array2DRowFieldMatrix<Complex> constructComplexMatrix(MathMatrixArithmeticValueSymbol symbol){
        Complex[][] c = new Complex[symbol.getVectors().size()][symbol.getVectors().get(0).getMathMatrixAccessSymbols().size()];
        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[0].length; j++) {
                getValue(symbol, c, i, j);
            }
        }
        return new Array2DRowFieldMatrix<>(ComplexField.getInstance(), c);
    }

    private static void getValue(MathMatrixArithmeticValueSymbol symbol, Complex[][] c, int i, int j) {
        MathMatrixAccessOperatorSymbol innerVector = symbol.getVectors().get(i);
        if(innerVector.getMathMatrixAccessSymbol(j).isPresent()) {
            MathExpressionSymbol expression = innerVector.getMathMatrixAccessSymbol(j).get();
            c[i][j] = dissolveMathExpression(expression);
        }
        else c[i][j] = new Complex(0);
    }

    private static Complex dissolveMathExpression(MathExpressionSymbol exp){
        if (exp.isParenthesisExpression())
            exp = ((MathParenthesisExpressionSymbol)exp).getMathExpressionSymbol();

        if (exp.isValueExpression()) {
            if (((MathValueExpressionSymbol) exp).isNameExpression())
                exp = resolveName((MathNameExpressionSymbol) exp);
            if (exp.isValueExpression())
                return castToComplex((MathNumberExpressionSymbol) exp);
            else return dissolveMathExpression(exp);
        }

        Complex value1 = dissolveChildExpression(((MathArithmeticExpressionSymbol)exp).getLeftExpression());
        Complex value2 = dissolveChildExpression(((MathArithmeticExpressionSymbol)exp).getRightExpression());

        return getResult((MathArithmeticExpressionSymbol) exp, value1, value2);
    }

    private static Complex getResult(MathArithmeticExpressionSymbol exp, Complex value1, Complex value2) {
        Complex res;
        switch (exp.getMathOperator()) {
            case "+": {
                res = value1.add(value2);
                break; }
            case "-": {
                res = value1.subtract(value2);
                break; }
            case "*": {
                res = value1.multiply(value2);
                break; }
            case "/": {
                res = value1.divide(value2);
                break; }
            case "^": {
                res = value1.pow(value2);
                break; }
            default: {
                res = value1;
            } }
        return res;
    }

    private static Complex castToComplex(MathNumberExpressionSymbol sym){
        JSValue Value = sym.getValue();
        if (Value.getImagNumber().isPresent()) {
            return new Complex(Value.getRealNumber().doubleValue(), Value.getImagNumber().get().doubleValue());
        } else {
            return new Complex(Value.getRealNumber().doubleValue());
        }
    }

    private static Complex dissolveChildExpression(MathExpressionSymbol expressionSymbol){
        if (expressionSymbol.isParenthesisExpression())
            expressionSymbol = ((MathParenthesisExpressionSymbol)expressionSymbol).getMathExpressionSymbol();

        if (expressionSymbol.isValueExpression()) {
            if (((MathValueExpressionSymbol) expressionSymbol).isNameExpression())
                expressionSymbol = resolveName((MathNameExpressionSymbol) expressionSymbol);
            Complex childExpressionSymbol = getComplex(expressionSymbol);
            if (childExpressionSymbol != null) return childExpressionSymbol;

        }else if (expressionSymbol.isArithmeticExpression())
            return dissolveMathExpression(expressionSymbol);

        return new Complex(0);
    }

    private static Complex getComplex(MathExpressionSymbol expressionSymbol) {
        if (expressionSymbol.isParenthesisExpression())
            expressionSymbol = ((MathParenthesisExpressionSymbol)expressionSymbol).getMathExpressionSymbol();

        if (((MathValueExpressionSymbol)expressionSymbol).isNumberExpression())
            return castToComplex((MathNumberExpressionSymbol)expressionSymbol);
        else if (expressionSymbol.isArithmeticExpression())
            return dissolveMathExpression(expressionSymbol);
        return null;
    }

    private static MathExpressionSymbol resolveName(MathNameExpressionSymbol expressionSymbol){
        Symbol symbol = expressionSymbol.getEnclosingScope()
                .resolve( expressionSymbol.getNameToResolveValue(), expressionSymbol.getKind()).get();
        return ((MathValueSymbol)symbol).getValue();
    }
}