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
package de.monticore.lang.math._symboltable;

import de.monticore.assignmentexpressions._ast.ASTMinusPrefixExpression;
import de.monticore.expressionsbasis._ast.ASTExpression;
import de.monticore.lang.math._ast.ASTAssignmentType;
import de.monticore.lang.math._ast.ASTMathAssignmentDeclarationExpression;
import de.monticore.lang.math._ast.ASTMathCompilationUnit;
import de.monticore.lang.math._ast.ASTMathDeclarationExpression;
import de.monticore.numberunit._ast.ASTNumberWithUnit;
import de.monticore.symboltable.ArtifactScope;
import de.monticore.symboltable.ImportStatement;
import de.monticore.symboltable.MutableScope;
import de.monticore.symboltable.ResolvingConfiguration;
import de.monticore.types.types._ast.ASTImportStatement;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.SourcePosition;
import de.se_rwth.commons.logging.Log;

import javax.measure.unit.Unit;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static de.monticore.lang.math._symboltable.ExpressionEvaluator.evaluate;

public class MathSymbolTableCreator extends MathSymbolTableCreatorTOP {
    public MathSymbolTableCreator(ResolvingConfiguration resolvingConfig, MutableScope enclosingScope) {
        super(resolvingConfig, enclosingScope);
    }

    public MathSymbolTableCreator(ResolvingConfiguration resolvingConfig, Deque<MutableScope> scopeStack) {
        super(resolvingConfig, scopeStack);
    }

    @Override
    public void visit(final ASTMathCompilationUnit compilationUnit) {
        Log.debug("Building Symboltable for Script: " + compilationUnit.getMathScript().getName(),
                MathSymbolTableCreator.class.getSimpleName());

        // imports
        List<ImportStatement> imports = new ArrayList<>();
        for (ASTImportStatement astImportStatement : compilationUnit.getImportStatementList()) {
            String qualifiedImport = Names.getQualifiedName(astImportStatement.getImportList());
            ImportStatement importStatement = new ImportStatement(qualifiedImport,
                    astImportStatement.isStar());
            imports.add(importStatement);
        }
        String package2 = "";
        if (compilationUnit.isR__packagePresent()) {
            package2 = Names.getQualifiedName(compilationUnit.getPackage().getParts());
        }
        ArtifactScope artifactScope = new ArtifactScope(
                Optional.empty(),
                package2,
                imports);
        //this.currentImports = imports;
        putOnStack(artifactScope);
    }

    public void visit(ASTMathAssignmentDeclarationExpression node) {
        createMatrixSymbol(node.getName(), node.getType(), node.get_SourcePositionStart());
    }

    @Override
    public void visit(ASTMathDeclarationExpression node) {
        createMatrixSymbol(node.getName(), node.getType(), node.get_SourcePositionStart());

    }

    protected void createMatrixSymbol(String name, ASTAssignmentType type, SourcePosition pos) {
        MatrixSymbol sym = new MatrixSymbol(name);

        //Creating the Dimensions
        if (type.isDimPresent()) {
            if (type.getDim().isVecDimPresent()) {
                Optional<ASTNumberWithUnit> number = evaluate(type.getDim().getVecDim()); // evaluate(type.getDim().getVecDim());
                // calculate dim
                sym.setCol(testDimension(number, pos));
                sym.setRow(1); // have a row vector
            } else {
                if (type.getDim().getMatrixDimList().size() > 2) {
                    Log.error("0xMATH15: Dimension can just contains maximal two numbers", pos);
                }
                if (type.getDim().getMatrixDimList().isEmpty()) {
                    Log.error("0xMATH16: Dimension cannot be empty", pos);
                }
                Optional<ASTNumberWithUnit> numberCol = evaluate(type.getDim().getMatrixDim(0)); // evaluate(type.getDim().getMatrixDim(0));
                // calculate dim
                sym.setCol(testDimension(numberCol, pos));

                Optional<ASTNumberWithUnit> numberRow = evaluate(type.getDim().getMatrixDim(1)); // evaluate(type.getDim().getMatrixDim(1));
                // calculate dim
                sym.setCol(testDimension(numberRow, pos));

            }
        }else {
            // default like `Z` which is the same as `Z^{1,1}`
            sym.setRow(1);
            sym.setCol(1);
        }

        //Creating the Min Max and Steps
        //ElementType with ranges
        if (type.getElementType().isRangesPresent()) {

            Optional<ASTNumberWithUnit> numberMin = evaluate(type.getElementType().getRanges().getMin());
            sym.setMin(testElementType(numberMin, pos));
            Optional<ASTNumberWithUnit> numberMax = evaluate(type.getElementType().getRanges().getMax());
            sym.setMin(testElementType(numberMax, pos));
            if (testElementType(numberMin, pos).get() > testElementType(numberMax, pos).get()) {
                Log.error("0xMATH17: Min cannot be bigger than Max", pos);
            }

            if (type.getElementType().getRanges().isStepPresent()) {
                Optional<ASTNumberWithUnit> numberStep = evaluate(type.getElementType().getRanges().getStep());
                sym.setMin(testElementType(numberStep, pos));
                if (testElementType(numberMax, pos).get() - testElementType(numberMin, pos).get() < testElementType(numberStep, pos).get()) {
                    Log.error("0xMATH19: The Steps cannot be bigger than the difference between Min and Max", pos);
                }
            }
        }
    }


    public static Optional<Double> testElementType(Optional<ASTNumberWithUnit> number, SourcePosition pos) {
        if (!number.isPresent()) {
            Log.error("0xMATH20: Could not evaluate Ranges for Math Variable Decleration", pos);
        }
        else if (!number.get().getNumber().isPresent()) {
            Log.error("0xMATH18: Min, Max and Steps are not allowed to be plus or minus infinity or to be a complex number", pos);
        } else {
            Optional<Double> d = number.get().getNumber();
            return d;
        }
        return null;
    }


    public static int testDimension(Optional<ASTNumberWithUnit> number, SourcePosition pos) {
        if (!number.isPresent()) {
            Log.error("0xMATH10: Could not evaluate dimension for Math Variable Decleration", pos);
        } else if (!number.get().getUnit().isCompatible(Unit.ONE)) { // dimensionless
            Log.error("0xMATH11: A dimension is a natural number therefore has no unit", pos);
        } else if (!number.get().getNumber().isPresent()) {
            Log.error("0xMATH12: dimension is not allowed to be plus or minus infinity or to be a complex number", pos);
        } else {
            double n = number.get().getNumber().get();
            double n2 = Math.round(n);
            if (Math.abs(n2 - n) > 0.000001) { // computer does not work correctly on double
                Log.error("0xMATH13: dimension is not a integer number", pos);
            }
            int i = (int) n2;
            if (i < 1) {
                Log.error("0xMATH14: dimension must be greater or equals to one", pos);
            }
            return i;
        }
        return -1;
    }
}