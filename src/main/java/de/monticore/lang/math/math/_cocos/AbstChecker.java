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
package de.monticore.lang.math.math._cocos;


import de.monticore.lang.math.math._symboltable.*;
import de.monticore.lang.monticar.ranges._ast.ASTRange;
import de.se_rwth.commons.logging.Log;
import javax.measure.unit.Unit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jscience.mathematics.number.Rational;

import static java.lang.Math.*;
import static org.jscience.mathematics.number.Rational.valueOf;
import siunit.monticoresiunit.si._ast.*;
/**
 * @author math-group
 *          abstract checker class
 */
public abstract class AbstChecker {

    /**
     * get the Dimension of the given symbol
     *
     * @param symbol MathVariableDeclarationSymbol we want to get the Dimensions from
     * @return Dimension as a Integer List
     */
    public List<Integer> checkDimension(MathVariableDeclarationSymbol symbol) {
        if (symbol.getMatrixProperties() != null && symbol.getMatrixProperties().contains("forVariable")) {
            return Arrays.asList(1, 1);
        }
        //MathExpression has no Dimension so we have to calculate it
        if (symbol.getValue() instanceof MathExpression) {
            MathExpression exp = (MathExpression) symbol.getValue();
            Operator op = exp.getOp();
            List<MathVariableDeclarationSymbol> operands = exp.getOperands();
            if (op.equals(Operator.Trans)) {
                return testTranspose(operands.get(0));
            } else {
                List<Integer> dim1 = checkDimension(operands.get(0));
                List<Integer> dim2 = checkDimension(operands.get(1));
                return test(op, dim1, dim2);
            }
        }
        //same for References
        else if (symbol.getValue() instanceof MathValueReference) {
            //if we have no Matrix index or Endoperator we have to calculate the dimension otherwise we have dimension 1x1
            if (!((MathValueReference) symbol.getValue()).getMatrixIndex().isPresent()) {
                MathVariableDeclarationSymbol sym = ((MathValueReference) symbol.getValue()).getReferencedSymbol();
                return checkDimension(sym);
            } else if (((MathValueReference) symbol.getValue()).isEndoperator()) {
                JSMatrix m = (JSMatrix) (((MathValueReference) symbol.getValue()).getMatrixIndex().get());
                if (m.getRowDimension() == 1) {
                    return Arrays.asList(1, m.getColDimension());
                } else {
                    return Arrays.asList(m.getRow(0).size(), m.getRow(1).size());
                }
            } else {
                return Arrays.asList(1, 1);
            }
        } else {
            return symbol.getDimensions();
        }
    }

    /**
     * calculate Dimension for Transpose Operator
     *
     * @param sym MathVariableDeclarationSymbol we want to get the Dimensions from
     * @return Dimension as a Integer List after transpose operation
     */
    public List<Integer> testTranspose(MathVariableDeclarationSymbol sym) {
        //first calculate dimension of the symbol
        List<Integer> dim = checkDimension(sym);
        //then switch row and coloumn dimension
        if (dim.size() == 2) {
            return Arrays.asList(dim.get(1), dim.get(0));
        } else {
            return Arrays.asList(dim.get(0), 1);
        }

    }

    /**
     * check the dimensions of the matrices for this operation
     *
     * @param op   operator
     * @param dim1 dimension of fisrt Value
     * @param dim2 dimension of second Value
     * @return Dimension as an Integer List after Operation op
     */
    public List<Integer> test(Operator op, List<Integer> dim1, List<Integer> dim2) {
        //get the right row and col dimensions for dims >2 the operators are not defind
        int row1 = 1, row2 = 1, col1 = 1, col2 = 1;
        if (dim1.size() == 2) {
            row1 = dim1.get(0);
            col1 = dim1.get(1);
        } else {
            col1 = dim1.get(0);
        }
        if (dim2.size() == 2) {
            row2 = dim2.get(0);
            col2 = dim2.get(1);
        } else {
            col2 = dim2.get(0);
        }
        switch (op) {
            /** "+" the rows and cols need to be of the same size */
            case Plus:
                if (row1 != row2 || col1 != col2) {
                    Log.error("0xMATH04 Matrix Addition with different Dimensions\n" +
                            "Matrix1: " + row1 + " x " + col1 + "\n" + "Matrix2: " + row2 + " x " + col2);
                }
                return dim1;
            /** "-" the rows and cols need to be of the same size */
            case Minus:
                if (row1 != row2 || col1 != col2) {
                    Log.error("0xMATH05 Matrix Subtraction with different Dimensions\n" +
                            "Matrix1: " + row1 + " x " + col1 + "\n" + "Matrix2: " + row2 + " x " + col2);
                }
                return dim1;
            case Times:
                /** "+" the amount of cols of the first matrix must fit the number of rows of the second matrix */
                if (col1 != row2) {
                    Log.error("0xMATH06 Matrix Multiplication with wrong Dimensions\n" +
                            "Matrix1: " + row1 + " x " + col1 + "\n" + "Matrix2: " + row2 + " x " + col2);
                }
                return Arrays.asList(row1, col2);
            /** "\" the matrices need to have the same amount of rows */
            case Mod:
                if (row1 != 1 || col1 != 1 || col2 != 1 || row2 != 1) {
                    Log.error("0xMATH19 Modulo Operator not defined in this Dimension");
                }
                return dim1;
            case SolEqu:
                if (row1 != row2 && col2 != 1) {
                    Log.error("0xMATH07 Matrix SolEqu with wrong Dimensions\n" +
                            "Matrix1: " + row1 + " x " + col1 + "\n" + "Matrix2: " + row2 + " x " + col2);
                }
                return Arrays.asList(col1, 1);
            case Power:
                if (row1 != col1 || col2 != row2) {
                    Log.error("0xMATH11 Matrix Power with different Column and Row Dimensions");
                }
                return dim1;
            /** the exponent has to be an Integer ".^" */
            case PowerWise:
                if (col2 != 1 || row2 != 1) {
                    Log.error("0xMATH08 Matrix PowerWise with wrong Dimensions\n" +
                            "Matrix1: " + row1 + " x " + col1 + "\n" + "Exponent: " + row2 + " x " + col2);
                }
                return dim1;
            /** the matrices should have the same amount of rows and cols ".*" */
            case TimesWise:
                if (row1 != row2 || col1 != col2) {
                    Log.error("0xMATH09 Matrix TimeWise with different Dimensions\n" +
                            "Matrix1: " + row1 + " x " + col1 + "\n" + "Matrix2: " + row2 + " x " + col2);
                }
                return dim1;
            /** the matrices should have the same amount of rows and cols */
            case Div:
                if (row1 != row2 || col1 != col2) {
                    Log.error("0xMATH10 Matrix DivWise with different Dimensions\n" +
                            "Matrix1: " + row1 + " x " + col1 + "\n" + "Matrix2: " + row2 + " x " + col2);
                }
                return dim1;
            /** the matrices should have the same amount of rows and cols */
            case Slash:
                if (row1 != row2 || col1 != col2) {
                    Log.error("0xMATH10 Matrix DivWise with different Dimensions\n" +
                            "Matrix1: " + row1 + " x " + col1 + "\n" + "Matrix2: " + row2 + " x " + col2);
                }
                return dim1;
            /** the matrices should have the same amount of rows and cols */
            case Assign:
                if (row1 != row2 || col1 != col2) {
                    Log.error("0xMATH17 Assignment with different Dimensions\n" +
                            "Matrix1: " + row1 + " x " + col1 + "\n" + "Matrix2: " + row2 + " x " + col2);
                }
                return dim1;
            default:
                Log.error("Unexpected case");
                return Arrays.asList(1, 1);
        }
    }

    /**
     * check the Units of the MathExpression
     *
     * @param mathExpression MathExpression we want to check the Units
     */
    public void checkUnits(MathExpression mathExpression) {
        getExpUnit(mathExpression);
    }

    /**
     * check the Units of the MathValue
     *
     * @param value MathValue we want to check the Units
     * @return Unit we calculated for the Mathvalue
     */
    public static Unit checkUnits(MathValue value) {
        if (value instanceof MathExpression) {
            return getExpUnit((MathExpression) value);
        } else if (value instanceof MathValueReference) {
            return getRefUnit((MathValueReference) value);
        } else {
            return value.getUnit();
        }
    }

    /**
     * check the Units of the MathValue
     *
     * @param ref MathValueReference we want to check the Units
     * @return Unit we calculated for the MathvalueReference
     */
    public static Unit getRefUnit(MathValueReference ref) {
        MathVariableDeclarationSymbol tmp = ref.getReferencedSymbol();
        return checkUnits(tmp.getValue());
    }

    /**
     * calculates the Unit for the Power Operation
     *
     * @param unit unit from the base
     * @param exp  exponent
     * @return Unit we calculate
     */
    public static Unit getUnitPower(Unit unit, int exp) {
        return unit.pow(exp);
    }

    /**
     * calculates the Exponent for the Power Operation
     *
     * @param value Mathvalue for the Exponent
     * @return int Value for the exponent
     */
    public static int calculateExponent(MathValue value) {
        if (value instanceof MathExpression) {
            return calculateExponent((MathExpression) value);
        } else if (value instanceof MathValueReference) {
            return calculateExponent((MathValueReference) value);
        } else if (value instanceof JSValue) {
            return ((JSValue) value).getRealNumber().intValue();
        } else {
            //exponent with dim > 1 is not defined
            Log.error("Wrong Expression in Exponent");
            return -1;
        }

    }

    /**
     * calculates the Exponent for the Power Operation
     *
     * @param expression MathExpression for the Exponent
     * @return int Value for the exponent
     */
    public static int calculateExponent(MathExpression expression) {
        List<MathVariableDeclarationSymbol> ops = expression.getOperands();
        Operator op = expression.getOp();
        return calculateExponent(op, ops.get(0).getValue(), ops.get(1).getValue());
    }

    /**
     * calculates the Exponent for the Power Operation
     *
     * @param ref MathValue Reference for the Exponent
     * @return int Value for the exponent
     */
    public static int calculateExponent(MathValueReference ref) {
        MathVariableDeclarationSymbol sym = ref.getReferencedSymbol();
        return calculateExponent(sym.getValue());
    }

    /**
     * calculates the Exponent for the Power Operation
     *
     * @param op  Operator
     * @param op1 Mathvalue on the left side of the operator
     * @param op2 Mathvalue on the right side of the operator
     * @return int value after doing op on op1 and op2
     */
    public static int calculateExponent(Operator op, MathValue op1, MathValue op2) {
        switch (op) {
            case Plus:
                return calculateExponent(op1) + calculateExponent(op2);
            case Minus:
                return calculateExponent(op1) - calculateExponent(op2);
            case Times:
                return calculateExponent(op1) * calculateExponent(op2);
            case Div:
                return calculateExponent(op1) / calculateExponent(op2);
            case Slash:
                return calculateExponent(op1) / calculateExponent(op2);
            case Power:
                return (int) pow(calculateExponent(op1), calculateExponent(op2));
            case Mod:
                return calculateExponent(op1) % calculateExponent(op2);
            /** otherwise */
            default:
                return 0;
        }
    }

    /**
     * checks if two units are compatible
     *
     * @param u1 Unit no1
     * @param u2 Unit no2
     */
    public static void checkUnits(Unit u1, Unit u2) {
        if (!u1.isCompatible(u2)) {
            /** units of the matrices are incompatible for + & -*/
            Log.error("0xMATH14 Arithmetic Matrix Expression with incompatible Units");
        }
    }

    /**
     * check and get the unit of a math expression
     *
     * @param exp math expression
     * @return the calculated Unit
     */
    public static Unit getExpUnit(MathExpression exp) {
        ArrayList<MathVariableDeclarationSymbol> ops = exp.getOperands();
        Operator op = exp.getOp();
        //if op is Transpose we only have a unary function (only one operator)
        if (op.equals(Operator.Trans)) {
            return checkUnits(ops.get(0).getValue());
        }
        //if op is a Power operation we have to calculate the exponent
        else if (op.equals(Operator.Power) || op.equals(Operator.PowerWise)) {
            Unit u1 = checkUnits(ops.get(0).getValue());
            return getUnitPower(u1, calculateExponent(ops.get(1).getValue()));
        }
        //calculate the Units of the operands of the Mathexpression
        Unit u1, u2;
        if (ops.get(0).getValue() instanceof MathExpression) {
            u1 = getExpUnit((MathExpression) ops.get(0).getValue());
            u2 = checkUnits(ops.get(1).getValue());
        } else if (ops.get(0).getValue() instanceof MathValueReference) {
            u1 = getRefUnit((MathValueReference) ops.get(0).getValue());
            u2 = checkUnits(ops.get(1).getValue());
        } else {
            u1 = ops.get(0).getValue().getUnit();
            u2 = checkUnits(ops.get(1).getValue());
        }
        //if we have minus or plus operation we have to check if the Units are compatible
        if (op.equals(Operator.Minus) || op.equals(Operator.Plus)) {
            checkUnits(u1, u2);
        }
        return getUnit(op, u1, u2);
    }

    /**
     * get the unit of a mathexpression with the help of jscience
     *
     * @param op operator
     * @param u1 first unit
     * @param u2 second unit
     * @return resulting unit
     */
    private static Unit getUnit(Operator op, Unit u1, Unit u2) {
        switch (op) {
            /** "+" the unit stays the same */
            case Plus:
                return u1;
            /** "-" the unit stays the same */
            case Minus:
                return u1;
            /** "*" the units are multiplied */
            case Times:
                return u1.times(u2);
            /** "/" the units are divided */
            case Div:
                return u1.divide(u2);
            case Slash:
                return u1.divide(u2);
            /** ".*" units are multiplied */
            case TimesWise:
                return u1.times(u2);
            /** "%" unit stays the same */
            case Mod:
                return u1;
            case SolEqu:
                return u2.divide(u1);
            /** otherwise */
            default:
                return u1;
        }
    }

    // return the range as ASTRange
    public ASTRange rangeCheck(MathVariableDeclarationSymbol symbol, boolean isMatrix) {
        ArrayList<Optional<Rational>> rangeList = checkRange(symbol, isMatrix);
        ASTRange range = new ASTRange();
        if(rangeList.get(0).isPresent())
            range.setStart(ASTUnitNumber.valueOf(rangeList.get(0).get(), Unit.ONE));
        if(rangeList.get(1).isPresent())
            range.setEnd((ASTUnitNumber.valueOf(rangeList.get(1).get(), Unit.ONE)));
        return range;
    }

    // determine range of a given Symbol with the information,
    // whether Symbol is a Matrix or not, b/c Operations work different
    // e.g. Matrix^Matrix not allowed
    public ArrayList<Optional<Rational>> checkRange(MathVariableDeclarationSymbol symbol, boolean isMatrix) {

        // Mathexpression has no Range so we have to calculate it
        if (symbol.getValue() instanceof MathExpression) {

            MathExpression mathExpression = (MathExpression) symbol.getValue();
            ArrayList<MathVariableDeclarationSymbol> ops = mathExpression.getOperands();
            Operator op = mathExpression.getOp();
            // Factor to determine range in respect to dimension of multiplied matrices
            int factor = 1;
            if(op.equals(Operator.Power) || op.equals(Operator.PowerWise))
                if(isMatrix)
                    factor = ((MathValueReference) ops.get(0).getValue()).getReferencedSymbol().getDimensions().get(1);

            // Transpose doesn't change the range
            if (op.equals(Operator.Trans)) {
                return checkRange(ops.get(0), isMatrix);
            } else {
                ArrayList<Optional<Rational>> range1 = checkRange(ops.get(0), isMatrix);
                ArrayList<Optional<Rational>> range2 = checkRange(ops.get(1), isMatrix);
                return range(op, range1, range2, factor, isMatrix);
            }
        }

        //if we don't have Range given we have to calculate it
        else if (symbol.getValue() instanceof MathValueReference) {
            MathValueReference mvr = (MathValueReference)symbol.getValue();
            if(mvr.getRange() == null) {
                MathVariableDeclarationSymbol sym = ((MathValueReference) symbol.getValue()).getReferencedSymbol();
                if (!sym.getRange().startIsPresent() && !sym.getRange().endIsPresent())
                    return checkRange(sym, isMatrix);
                ArrayList<Optional<Rational>> range = new ArrayList<Optional<Rational>>();

                if (sym.getRange().startIsPresent() && sym.getRange().endIsPresent()) {
                    range.add(0, Optional.of(sym.getRange().getStartValue()));
                    range.add(1, Optional.of(sym.getRange().getEndValue()));
                    return range;
                } else if (!sym.getRange().startIsPresent() && sym.getRange().endIsPresent()) {
                    range.add(0, Optional.empty());
                    range.add(1, Optional.of(sym.getRange().getEndValue()));
                    return range;
                } else if (sym.getRange().startIsPresent() && !sym.getRange().endIsPresent()) {
                    range.add(0, Optional.of(sym.getRange().getStartValue()));
                    range.add(1, Optional.empty());
                    return range;
                }
            }
            // if we have given Range
            if(mvr.getRange() != null) {

                if (((MathValueReference) symbol.getValue()).getRange().hasNoLowerLimit() &&
                        ((MathValueReference) symbol.getValue()).getRange().hasNoUpperLimit()) {
                    MathVariableDeclarationSymbol sym = ((MathValueReference) symbol.getValue()).getReferencedSymbol();
                    return checkRange(sym, isMatrix);
                }
            }
            else {

                // Check whether MathValueReference partly has infinite Range
                ArrayList<Optional<Rational>> range = new ArrayList<Optional<Rational>>();
                if(symbol.getRange().startIsPresent() && symbol.getRange().endIsPresent()) {
                    range.add(0, Optional.of(symbol.getRange().getStartValue()));
                    range.add(1, Optional.of(symbol.getRange().getEndValue()));
                }
                else if (!symbol.getRange().startIsPresent() && symbol.getRange().endIsPresent()) {
                    range.add(0, Optional.empty());
                    range.add(1, Optional.of(symbol.getRange().getEndValue()));
                    }
                else if (symbol.getRange().startIsPresent() && !symbol.getRange().endIsPresent()) {
                    range.add(0,Optional.of(symbol.getRange().getStartValue()));
                    range.add(1, Optional.empty());
                }
                return range;
            }
        } else if (symbol.getValue() instanceof JSMatrix) {
            // determine and return the range of a given Matrix
            ASTRange range = rangeOfMatrix(symbol);
            ArrayList<Optional<Rational>> rangeList = new ArrayList<Optional<Rational>>();
            if(range.startIsPresent())
                rangeList.add(0, Optional.of(range.getStartValue()));
            if(range.endIsPresent())
                rangeList.add(1, Optional.of(range.getEndValue()));

            return rangeList;
        }
        else { // symbol is a JSValue
            ArrayList<Optional<Rational>> range = new ArrayList<Optional<Rational>>();

            range.add(0, Optional.of((((JSValue) symbol.getValue()).getRealNumber())));
            range.add(1, Optional.of((((JSValue) symbol.getValue()).getRealNumber())));
            return range;
        }
        ArrayList<Optional<Rational>> range = new ArrayList<Optional<Rational>>();

        return range;
    }

    // returns the Range of a MathExpression with given Operator and Ranges of operands
    public ArrayList<Optional<Rational>> range(Operator op, ArrayList<Optional<Rational>> pRange1, ArrayList<Optional<Rational>> pRange2, int factor, boolean isMatrix) {
        ArrayList<Optional<Rational>> range = new ArrayList<Optional<Rational>>();
        range.add(0, Optional.empty());
        range.add(1, Optional.empty());

        switch(op) {
            // w/ and w/o matrices the same procedure
            case Plus:
                if (!pRange1.get(0).isPresent() || !pRange2.get(0).isPresent()) range.add(0, Optional.empty());
                else range.set(0, Optional.of(pRange1.get(0).get().plus(pRange2.get(0).get())));
                if (!pRange1.get(1).isPresent() || !pRange2.get(0).isPresent()) range.add(1, Optional.empty());
                else range.set(1, Optional.of(pRange1.get(1).get().plus(pRange2.get(1).get())));
                return range;
            // w/ and w/o matrices the same procedure
            case Minus:
                if (pRange1.get(0).isPresent() || pRange2.get(1).isPresent()) range.add(0, Optional.empty());
                else range.set(0, Optional.of(pRange1.get(0).get().minus(pRange2.get(1).get())));
                if (pRange1.get(1).isPresent() || pRange2.get(0).isPresent()) range.add(1, Optional.empty());
                else range.set(1, Optional.of(pRange1.get(1).get().minus(pRange2.get(0).get())));
                return range;
            // w/ and w/o matrices the same procedure
            case Times:
                range = rangeForTimes(pRange1, pRange2, factor);
                return range;
            // different calculations depending on parameter isMatrix
            case Power:
                if (isMatrix) {

                    // Second operand can only be an int value
                    int x = pRange2.get(0).get().intValue();
                    range = pRange1;
                    while (x > 1) {
                        range.set(0, (rangeForTimes(range, pRange1, factor)).get(0));
                        range.set(1, (rangeForTimes(range, pRange1, factor)).get(1));
                        x--;
                    }
                    return range;
                }
                else {
                    // x^2, x^4, x^6 etc. may change range positions.
                    // e.g. (-4:-3) with operand 2 would be (16:9),
                    // so we have to change to (9:16)
                    double valOfsecondOp = pRange2.get(0).get().doubleValue();
                    range = pRange1;

                    Optional<Rational> r1 = Optional.empty();
                    Optional<Rational> r2 = Optional.empty();

                    if(range.get(0).isPresent())
                         r1 = Optional.of(Rational.valueOf((long)Math.pow(range.get(0).get().doubleValue(), valOfsecondOp), (long)1.0));
                    if(range.get(1).isPresent())
                         r2 = Optional.of(Rational.valueOf((long)Math.pow(range.get(1).get().doubleValue(), valOfsecondOp), (long)1.0));

                    // checkorder
                    range.set(0, r1);
                    range.set(1, r2);
                    range = checkOrder(range);

                    return range;
                }
            // only for matrices
            case TimesWise:
                if (isMatrix) {

                    Optional<Rational> r1 = Optional.empty();
                    Optional<Rational> r2 = Optional.empty();

                    if(pRange1.get(1).isPresent() && pRange2.get(0).isPresent())
                        r1 = Optional.of(Rational.valueOf((long)(pRange1.get(0).get().doubleValue() * pRange2.get(0).get().doubleValue()), (long)1.0));
                    if(pRange2.get(1).isPresent() && pRange2.get(0).isPresent())
                        r2 = Optional.of(Rational.valueOf((long)(pRange1.get(1).get().doubleValue() * pRange2.get(1).get().doubleValue()), (long)1.0));

                    // if startrange > endrange change them
                    range.set(0, r1);
                    range.set(1, r2);
                    range = checkOrder(range);

                    return range;
                }
                return range;
            // only for matrices
            case PowerWise:

                if (isMatrix) {
                    // if second Operator is Optional.empty the range will be Optional.empty
                    if(!pRange2.get(0).isPresent()) {
                        return range; }


                    Optional<Rational> r1 = Optional.empty();
                    Optional<Rational> r2 = Optional.empty();

                    if(pRange1.get(0).isPresent())
                        r1 = Optional.of(Rational.valueOf((long) Math.pow(pRange1.get(0).get().doubleValue(), pRange2.get(0).get().doubleValue()), (long)1.0));
                    else
                        r1 = Optional.empty();
                    if(pRange1.get(1).isPresent())
                        r2 = Optional.of(Rational.valueOf((long) Math.pow(pRange1.get(1).get().doubleValue(), pRange2.get(0).get().doubleValue()), (long)1.0));
                    else
                        r2 = Optional.empty();

                    // check whether 2nd operand is odd or not
                    if(pRange1.get(0).isPresent()) {
                        if ((pRange2.get(0).get().doubleValue() % 2 == 0) && pRange1.get(0).get().doubleValue() < 0) {
                            if(pRange1.get(1).isPresent()) {
                                if (pRange1.get(1).get().doubleValue() < 0) {
                                    r1 = Optional.of(Rational.valueOf((long) Math.pow(pRange1.get(1).get().doubleValue(), pRange2.get(0).get().doubleValue()), (long) 1.0));
                                    r2 = Optional.of(Rational.valueOf((long) Math.pow(pRange1.get(0).get().doubleValue(), pRange2.get(0).get().doubleValue()), (long) 1.0));
                                } else {
                                    r1 = Optional.of(Rational.ZERO);
                                    r2 = Optional.of(Rational.valueOf((long) Math.pow(pRange1.get(1).get().doubleValue(), pRange2.get(0).get().doubleValue()), (long) 1.0));
                                }
                            }

                        }
                    }

                    // if startrange > endrange change them
                    range.set(0, r1);
                    range.set(1, r2);
                    range = checkOrder(range);
                }

                return range;

            case Div:
                // not for matrices
                if (!isMatrix)
                    range = rangeForDiv(pRange1, pRange2);
                return range;
                // not for matrices
            case Slash:
                if (!isMatrix)
                    range = rangeForDiv(pRange1, pRange2);
                return range;
                // not for matrices
            case Mod:
                Optional<Rational> r1 = Optional.empty();
                Optional<Rational> r2 = Optional.empty();

                if(pRange1.get(0).isPresent())
                    r1 = Optional.of(Rational.valueOf((long) (pRange1.get(0).get().doubleValue() % pRange2.get(0).get().doubleValue()), (long) 1.0));
                if(pRange1.get(1).isPresent())
                    r2 = Optional.of(Rational.valueOf((long) (pRange1.get(1).get().doubleValue() % pRange2.get(0).get().doubleValue()), (long) 1.0));

                // if startrange > endrange change them
                range.set(0, r1);
                range.set(1, r2);

                // if startrange > endrange change them
                checkOrder(range);

                return range;
            case SolEqu:
                // you cannot decide any range, so it will be -infinity to infinity
                return range;
            default:
                Log.error("unexpected case");
        }
        return range;
    }

    // if range is wrong ordered e.g. due to Power Operation, we have to change the order
    public ArrayList<Optional<Rational>> checkOrder(ArrayList<Optional<Rational>> range) {
        if(range.get(0).isPresent() && range.get(1).isPresent()) {
        Optional<Rational> r1 = range.get(0);
        Optional<Rational> r2 = range.get(1);

            if (r1.get().doubleValue() > r2.get().doubleValue()) {
                Optional<Rational> tmp = Optional.of(r1.get());
                range.set(0, Optional.of(r2.get()));
                range.set(1, tmp);
            }
        }
        return range;
    }

    // Determines the Range of given Matrix depending on highest and lowest value
    public ASTRange rangeOfMatrix(MathVariableDeclarationSymbol symbol) {
        JSMatrix matrix = (JSMatrix)symbol.getValue();
        Rational min = ((JSValue)matrix.getElement(0,0)).getRealNumber();
        Rational max = ((JSValue)matrix.getElement(0,0)).getRealNumber();

        // check every value of the matrix
        for(int i=0; i<(matrix.getRowDimension()); i++) {
            for(int j=0; j<(matrix.getColDimension());j++) {
                if(((JSValue)matrix.getElement(i,j)).getRealNumber().doubleValue()  > (max.doubleValue())) max = ((JSValue)matrix.getElement(i,j)).getRealNumber();
                if(((JSValue)matrix.getElement(i,j)).getRealNumber().doubleValue()  < (min.doubleValue())) min = ((JSValue)matrix.getElement(i,j)).getRealNumber();
            }
        }
        ArrayList<Optional<Rational>> range = new ArrayList<Optional<Rational>>();

        range.add(0, Optional.of(min));
        range.add(1, Optional.of(max));

        // return as ASTRange
        ASTRange astRange = new ASTRange();
        if(range.get(0).isPresent())
            astRange.setStart(ASTUnitNumber.valueOf(range.get(0).get(), Unit.ONE));
        if(range.get(1).isPresent())
            astRange.setEnd(ASTUnitNumber.valueOf(range.get(1).get(), Unit.ONE));

        return astRange;
    }

    // computes the Range for multiplication
    public ArrayList<Optional<Rational>> rangeForTimes(ArrayList<Optional<Rational>> pRange1, ArrayList<Optional<Rational>> pRange2, int factor) {
        ArrayList<Optional<Rational>> range = new ArrayList<Optional<Rational>>();

        // if (-infinity:infinity) for one operator, then the range is always (-infinity:infinity)
        if((!pRange1.get(0).isPresent() && !pRange1.get(1).isPresent()) || (!pRange2.get(0).isPresent() && !pRange2.get(1).isPresent())) {
            range.add(0, Optional.empty());
            range.add(1, Optional.empty());
            return range;
        }

        Optional<Rational> min = Optional.empty();
        Optional<Rational> max = Optional.empty();

        // set initial min and max
        if(pRange1.get(0).isPresent()) {
            if (pRange2.get(0).isPresent()) {
                min = Optional.of(pRange1.get(0).get().times(pRange2.get(0).get()));
                max = Optional.of(pRange1.get(0).get().times(pRange2.get(0).get()));
            } else {
                min = Optional.of(pRange1.get(0).get().times(pRange2.get(1).get()));
                max = Optional.of(pRange1.get(0).get().times(pRange2.get(1).get()));
            }
        }
        else {
            if (pRange2.get(0).isPresent()) {
                min = Optional.of(pRange1.get(1).get().times(pRange2.get(0).get()));
                max = Optional.of(pRange1.get(1).get().times(pRange2.get(0).get()));
            }
            else {
                min = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));
                max = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));
            }
        }

        // 9 cases for min,max; for example
        // (x:infinity) * (x:infinity) or
        // (-infinity:x) * (x:x)

        // case (x:x) (x:x)
        if(pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            if ((pRange1.get(0).get().times(pRange2.get(0).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(0).get().times(pRange2.get(0).get()));
            if ((pRange1.get(0).get().times(pRange2.get(1).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(0).get().times(pRange2.get(1).get()));
            if ((pRange1.get(1).get().times(pRange2.get(0).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(1).get().times(pRange2.get(0).get()));
            if ((pRange1.get(1).get().times(pRange2.get(1).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));

            if ((pRange1.get(0).get().times(pRange2.get(0).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(0).get().times(pRange2.get(0).get()));
            if ((pRange1.get(0).get().times(pRange2.get(1).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(0).get().times(pRange2.get(1).get()));
            if ((pRange1.get(1).get().times(pRange2.get(0).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(1).get().times(pRange2.get(0).get()));
            if ((pRange1.get(1).get().times(pRange2.get(1).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));
        }
        // case (x:x) (x:infinity)
        if(pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && pRange2.get(0).isPresent() && !pRange2.get(1).isPresent()) {
            if(pRange1.get(0).get().doubleValue() < 0 || pRange1.get(1).get().doubleValue() < 0) min = Optional.empty();
            if(pRange1.get(0).get().doubleValue() > 0 || pRange1.get(1).get().doubleValue() > 0) max = Optional.empty();

            if(max.isPresent()) {
                if ((pRange1.get(0).get().times(pRange2.get(0).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(0).get().times(pRange2.get(0).get()));
                if ((pRange1.get(1).get().times(pRange2.get(0).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(1).get().times(pRange2.get(0).get()));
            }
            if(min.isPresent()) {
                if ((pRange1.get(0).get().times(pRange2.get(0).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(0).get().times(pRange2.get(0).get()));
                if ((pRange1.get(1).get().times(pRange2.get(0).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(1).get().times(pRange2.get(0).get()));
            }
        }
        // case (x:x) (infinity:x)
        if(pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && !pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            if(pRange1.get(0).get().doubleValue() < 0 || pRange1.get(1).get().doubleValue() < 0) max = Optional.empty();
            if(pRange1.get(0).get().doubleValue() > 0 || pRange1.get(1).get().doubleValue() > 0) min = Optional.empty();

            if(max.isPresent())
            if ((pRange1.get(0).get().times(pRange2.get(1).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(0).get().times(pRange2.get(1).get()));
            if ((pRange1.get(1).get().times(pRange2.get(1).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));

            if(min.isPresent())
            if ((pRange1.get(0).get().times(pRange2.get(1).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(0).get().times(pRange2.get(1).get()));
            if ((pRange1.get(1).get().times(pRange2.get(1).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));
        }
        // case (x:infinity) (x:x)
        if(pRange1.get(0).isPresent() && !pRange1.get(1).isPresent() && pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            if(pRange2.get(0).get().doubleValue() < 0 || pRange2.get(1).get().doubleValue() < 0) min = Optional.empty();
            if(pRange2.get(0).get().doubleValue() > 0 || pRange2.get(1).get().doubleValue() > 0) max = Optional.empty();

            if(max.isPresent()) {
                if ((pRange1.get(0).get().times(pRange2.get(0).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(0).get().times(pRange2.get(0).get()));
                if ((pRange1.get(0).get().times(pRange2.get(1).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(0).get().times(pRange2.get(1).get()));
            }
            if(min.isPresent()) {
                if ((pRange1.get(0).get().times(pRange2.get(0).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(0).get().times(pRange2.get(0).get()));
                if ((pRange1.get(0).get().times(pRange2.get(1).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(0).get().times(pRange2.get(1).get()));
            }
        }
        // case (x:infinity) (x:infinity)
        if(pRange1.get(0).isPresent() && !pRange1.get(1).isPresent()  && pRange2.get(0).isPresent() && !pRange2.get(1).isPresent()) {
            max = Optional.empty();
            if(pRange1.get(0).get().doubleValue() < 0 || pRange2.get(0).get().doubleValue() < 0)
                min = Optional.empty();
            else min = Optional.of(pRange1.get(0).get().times(pRange2.get(0).get()));
        }
        // case (x:infinity) (-infinity:x)
        if(pRange1.get(0).isPresent() && !pRange1.get(1).isPresent()  && pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            min = Optional.empty();
            if(pRange1.get(0).get().doubleValue() < 0 || pRange2.get(1).get().doubleValue() > 0 ) max = Optional.empty();

            if(max.isPresent()) {
                if ((pRange1.get(0).get().times(pRange2.get(1).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(0).get().times(pRange2.get(1).get()));
                if ((pRange1.get(1).get().times(pRange2.get(1).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));
            }
        }
        // case (infinity:x) (x:x)
        if(!pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            if(pRange2.get(0).get().doubleValue() > 0 || pRange2.get(1).get().doubleValue() > 0) min = Optional.empty();
            if(pRange2.get(0).get().doubleValue() < 0 || pRange2.get(1).get().doubleValue() < 0) max = Optional.empty();

            if(max.isPresent()) {
                if ((pRange1.get(1).get().times(pRange2.get(0).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(1).get().times(pRange2.get(0).get()));
                if ((pRange1.get(1).get().times(pRange2.get(1).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));
            }
            if(min.isPresent()) {
                if ((pRange1.get(1).get().times(pRange2.get(0).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(1).get().times(pRange2.get(0).get()));
                if ((pRange1.get(1).get().times(pRange2.get(1).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));
            }
        }
        // case (infinity:x) (x:infinity)
        if(pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && pRange2.get(0).isPresent() && !pRange2.get(1).isPresent()) {
            min = Optional.empty();
            if(pRange1.get(1).get().doubleValue() > 0 || pRange2.get(0).get().doubleValue() < 0) max = Optional.empty();

            if(max.isPresent())
                if ((pRange1.get(1).get().times(pRange2.get(0).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(1).get().times(pRange2.get(0).get()));

            if(min.isPresent())
                if ((pRange1.get(1).get().times(pRange2.get(0).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(1).get().times(pRange2.get(0).get()));
        }
        // case (infinity:x) (infinity:x)
        if(!pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && !pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            max = Optional.empty();
            if(pRange1.get(1).get().doubleValue() > 0 || pRange2.get(1).get().doubleValue() > 0) min = Optional.empty();

            if(max.isPresent()) {
                if ((pRange1.get(1).get().times(pRange2.get(1).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));
            }
            if(min.isPresent()) {
                if ((pRange1.get(1).get().times(pRange2.get(1).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(1).get().times(pRange2.get(1).get()));
            }
        }

        Rational x = valueOf(factor, 1);
        // for 3x4 matrix for example factor will be 4, so we have to multiply 4 times.
        // for no Matrix given, factor will be 1
        if(min.isPresent())
            min = Optional.of(min.get().times(x));
        if(max.isPresent())
            max = Optional.of(max.get().times(x));

        range.add(0, min);
        range.add(1, max);
        return range;
    }

    // computes the range for division, nearly the same as multiplication
    public ArrayList<Optional<Rational>> rangeForDiv(ArrayList<Optional<Rational>> pRange1, ArrayList<Optional<Rational>> pRange2) {
        ArrayList<Optional<Rational>> range = new ArrayList<Optional<Rational>>();

        // if (-infinity:infinity) for one operator, then the range is always (-infinity:infinity)
        if((!pRange1.get(0).isPresent() && !pRange1.get(1).isPresent()) || (!pRange2.get(0).isPresent() && !pRange2.get(1).isPresent())) {
            range.add(0, Optional.empty());
            range.add(1, Optional.empty());
            return range;
        }

        Optional<Rational> min = Optional.empty();
        Optional<Rational> max = Optional.empty();

        // set initial min and max
        if(pRange1.get(0).isPresent()) {
            if (pRange2.get(0).isPresent()) {
                min = Optional.of(pRange1.get(0).get().divide(pRange2.get(0).get()));
                max = Optional.of(pRange1.get(0).get().divide(pRange2.get(0).get()));
            } else {
                min = Optional.of(pRange1.get(0).get().divide(pRange2.get(1).get()));
                max = Optional.of(pRange1.get(0).get().divide(pRange2.get(1).get()));
            }
        }
        else {
            if (pRange2.get(0).isPresent()) {
                min = Optional.of(pRange1.get(1).get().divide(pRange2.get(0).get()));
                max = Optional.of(pRange1.get(1).get().divide(pRange2.get(0).get()));
            }
            else {
                min = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));
                max = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));
            }


        }

        // 9 cases for min,max; for example
        // (x:infinity) / (x:infinity) or
        // (-infinity:x) / (x:x)

        // case (x:x) (x:x)
        if(pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            if ((pRange1.get(0).get().divide(pRange2.get(0).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(0).get().divide(pRange2.get(0).get()));
            if ((pRange1.get(0).get().divide(pRange2.get(1).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(0).get().divide(pRange2.get(1).get()));
            if ((pRange1.get(1).get().divide(pRange2.get(0).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(1).get().divide(pRange2.get(0).get()));
            if ((pRange1.get(1).get().divide(pRange2.get(1).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));

            if ((pRange1.get(0).get().divide(pRange2.get(0).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(0).get().divide(pRange2.get(0).get()));
            if ((pRange1.get(0).get().divide(pRange2.get(1).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(0).get().divide(pRange2.get(1).get()));
            if ((pRange1.get(1).get().divide(pRange2.get(0).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(1).get().divide(pRange2.get(0).get()));
            if ((pRange1.get(1).get().divide(pRange2.get(1).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));
        }
        // case (x:x) (x:infinity)
        if(pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && pRange2.get(0).isPresent() && !pRange2.get(1).isPresent()) {
            if(pRange1.get(0).get().doubleValue() < 0 || pRange1.get(1).get().doubleValue() < 0) min = Optional.empty();
            if(pRange1.get(0).get().doubleValue() > 0 || pRange1.get(1).get().doubleValue() > 0) max = Optional.empty();

            if(max.isPresent()) {
                if ((pRange1.get(0).get().divide(pRange2.get(0).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(0).get().divide(pRange2.get(0).get()));
                if ((pRange1.get(1).get().divide(pRange2.get(0).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(1).get().divide(pRange2.get(0).get()));
            }
            if(min.isPresent()) {
                if ((pRange1.get(0).get().divide(pRange2.get(0).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(0).get().divide(pRange2.get(0).get()));
                if ((pRange1.get(1).get().divide(pRange2.get(0).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(1).get().divide(pRange2.get(0).get()));
            }
        }
        // case (x:x) (infinity:x)
        if(pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && !pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            if(pRange1.get(0).get().doubleValue() < 0 || pRange1.get(1).get().doubleValue() < 0) max = Optional.empty();
            if(pRange1.get(0).get().doubleValue() > 0 || pRange1.get(1).get().doubleValue() > 0) min = Optional.empty();

            if(max.isPresent())
                if ((pRange1.get(0).get().divide(pRange2.get(1).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(0).get().divide(pRange2.get(1).get()));
            if ((pRange1.get(1).get().divide(pRange2.get(1).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));

            if(min.isPresent())
                if ((pRange1.get(0).get().divide(pRange2.get(1).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(0).get().divide(pRange2.get(1).get()));
            if ((pRange1.get(1).get().divide(pRange2.get(1).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));
        }
        // case (x:infinity) (x:x)
        if(pRange1.get(0).isPresent() && !pRange1.get(1).isPresent() && pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            if(pRange2.get(0).get().doubleValue() < 0 || pRange2.get(1).get().doubleValue() < 0) min = Optional.empty();
            if(pRange2.get(0).get().doubleValue() > 0 || pRange2.get(1).get().doubleValue() > 0) max = Optional.empty();

            if(max.isPresent()) {
                if ((pRange1.get(0).get().divide(pRange2.get(0).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(0).get().divide(pRange2.get(0).get()));
                if ((pRange1.get(0).get().divide(pRange2.get(1).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(0).get().divide(pRange2.get(1).get()));
            }
            if(min.isPresent()) {
                if ((pRange1.get(0).get().divide(pRange2.get(0).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(0).get().divide(pRange2.get(0).get()));
                if ((pRange1.get(0).get().divide(pRange2.get(1).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(0).get().divide(pRange2.get(1).get()));
            }
        }
        // case (x:infinity) (x:infinity)
        if(pRange1.get(0).isPresent() && !pRange1.get(1).isPresent()  && pRange2.get(0).isPresent() && !pRange2.get(1).isPresent()) {
            max = Optional.empty();
            if(pRange1.get(0).get().doubleValue() < 0 || pRange2.get(0).get().doubleValue() < 0)
                min = Optional.empty();
            else min = Optional.of(pRange1.get(0).get().divide(pRange2.get(0).get()));
        }
        // case (x:infinity) (-infinity:x)
        if(pRange1.get(0).isPresent() && !pRange1.get(1).isPresent()  && pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            min = Optional.empty();
            if(pRange1.get(0).get().doubleValue() < 0 || pRange2.get(1).get().doubleValue() > 0 ) max = Optional.empty();

            if(max.isPresent()) {
                if ((pRange1.get(0).get().divide(pRange2.get(1).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(0).get().divide(pRange2.get(1).get()));
                if ((pRange1.get(1).get().divide(pRange2.get(1).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));
            }
        }
        // case (infinity:x) (x:x)
        if(!pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            if(pRange2.get(0).get().doubleValue() > 0 || pRange2.get(1).get().doubleValue() > 0) min = Optional.empty();
            if(pRange2.get(0).get().doubleValue() < 0 || pRange2.get(1).get().doubleValue() < 0) max = Optional.empty();

            if(max.isPresent()) {
                if ((pRange1.get(1).get().divide(pRange2.get(0).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(1).get().divide(pRange2.get(0).get()));
                if ((pRange1.get(1).get().divide(pRange2.get(1).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));
            }
            if(min.isPresent()) {
                if ((pRange1.get(1).get().divide(pRange2.get(0).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(1).get().divide(pRange2.get(0).get()));
                if ((pRange1.get(1).get().divide(pRange2.get(1).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));
            }
        }
        // case (infinity:x) (x:infinity)
        if(pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && pRange2.get(0).isPresent() && !pRange2.get(1).isPresent()) {
            min = Optional.empty();
            if(pRange1.get(1).get().doubleValue() > 0 || pRange2.get(0).get().doubleValue() < 0) max = Optional.empty();

            if(max.isPresent())
                if ((pRange1.get(1).get().divide(pRange2.get(0).get())).doubleValue()  > (max.get().doubleValue())) max = Optional.of(pRange1.get(1).get().divide(pRange2.get(0).get()));

            if(min.isPresent())
                if ((pRange1.get(1).get().divide(pRange2.get(0).get())).doubleValue()  < (min.get().doubleValue())) min = Optional.of(pRange1.get(1).get().divide(pRange2.get(0).get()));
        }
        // case (infinity:x) (infinity:x)
        if(!pRange1.get(0).isPresent() && pRange1.get(1).isPresent() && !pRange2.get(0).isPresent() && pRange2.get(1).isPresent()) {
            max = Optional.empty();
            if(pRange1.get(1).get().doubleValue() > 0 || pRange2.get(1).get().doubleValue() > 0) min = Optional.empty();

            if(max.isPresent()) {
                if ((pRange1.get(1).get().divide(pRange2.get(1).get())).doubleValue() > (max.get().doubleValue()))
                    max = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));
            }
            if(min.isPresent()) {
                if ((pRange1.get(1).get().divide(pRange2.get(1).get())).doubleValue() < (min.get().doubleValue()))
                    min = Optional.of(pRange1.get(1).get().divide(pRange2.get(1).get()));
            }
        }

        range.add(0, Optional.of(min.get()));
        range.add(1, Optional.of(max.get()));
        return range;
    }



}