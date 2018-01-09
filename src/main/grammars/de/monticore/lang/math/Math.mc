/*
 * ******************************************************************************
 * MontiCore Language Workbench, www.monticore.de
 * Copyright (c) 2017, MontiCore, All rights reserved.
 *
 * This project is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this project. If not, see <http://www.gnu.org/licenses/>.
 * ******************************************************************************
 */

package de.monticore.lang.math;

grammar Math extends de.monticore.lang.monticar.Common2, siunit.monticoresiunit.SI, de.monticore.lang.monticar.MCExpressions {
    MathCompilationUnit =
      ("package" package:(Name& || ".")+ ";")?
      (ImportStatement)* // to import later on other scripts/functions
      MathScript;

    // here the script name is concretely given (in contrast to Matlab), b/c otherwise we would get
    // trouble when embedding this language into another one (due to the name-based nature of the MontiCore 4 symboltable)
    symbol scope MathScript = 
    	"script" Name& MathStatements "end";

    MathStatements = 
    	MathExpression+;

    interface MathExpression extends Expression;
    interface MathArithmeticExpression extends MathExpression;
    interface MathArithmeticMatrixExpression extends MathExpression;
    interface MathValueExpression extends MathExpression;
    interface MathValueMatrixExpression extends MathExpression;
    interface MathBooleanExpression extends MathExpression;

    //general MathExpressions
    MathParenthesisExpression implements MathArithmeticMatrixExpression, MathArithmeticExpression, MathExpression, Expression = 
    	"(" MathExpression ")";

    MathPreMinusExpression implements MathArithmeticMatrixExpression, MathArithmeticExpression, MathExpression, Expression = 
    	"-" MathExpression;

    MathForLoopExpression implements MathExpression = 
    	"for" head:MathForLoopHead body:MathExpression* "end";

    MathForLoopHead = 
    	Name& "=" MathExpression;

    MathDeclarationExpression implements MathExpression = 
    	type:AssignmentType Name& ";";

    MathAssignmentDeclarationExpression implements MathExpression = 
    	type:AssignmentType Name& MathAssignmentOperator MathExpression ";";

    MathAssignmentExpression implements MathExpression = 
    	(Name&|MathMatrixNameExpression|DottedName) MathAssignmentOperator MathExpression ";";

    MathAssignmentOperator = 
    	operator:"=" | operator:"+=" | operator:"-=" | operator:"*=" | operator:"/=";

    MathConditionalExpression implements MathExpression = 
    	MathIfExpression MathElseIfExpression* MathElseExpression? "end";

    MathConditionalExpressionShort implements MathExpression = 
    	MathExpression /*MathBooleanExpression*/ "?" trueCase:MathExpression+ ":" falseCase:MathExpression+;

    MathIfExpression  = 
    	"if" /*condition:MathBooleanExpression*/ condition:MathExpression "{" body:MathExpression* "}" ;

    MathElseIfExpression = 
    	"elseif" /*condition:MathBooleanExpression*/ condition:MathExpression "{" body:MathExpression* "}" ;

    MathElseExpression = 
    	"else" "{" body:MathExpression* "}";

    Dimension = 
    	"^" "{" (MathArithmeticExpression || ",")+ "}";

    //Expression for number/variable name
    
    MathNumberExpression implements MathValueExpression, MathExpression, Expression  = 
    	Number;

    MathNameExpression implements MathArithmeticMatrixExpression, MathValueExpression, MathExpression, Expression  = 
    	Name&;

    MathDottedNameExpression implements MathArithmeticMatrixExpression, MathValueExpression, MathExpression, Expression  = 
    	Name "." Name;

    MathMatrixNameExpression implements MathArithmeticMatrixExpression, MathValueMatrixExpression, MathExpression, Expression  = 
    	Name& "(" MathMatrixAccessExpression ")" | Name& "(" EndOperator ")";

    MathMatrixElementAccessExpression implements MathValueExpression, MathArithmeticExpression, Expression = 
    	MathMatrixNameExpression;

    MathMatrixValueExplicitExpression implements MathValueMatrixExpression, Expression = 
    	"[" (MathMatrixAccessExpression || ";")* "]" | MathVectorExpression;

    MathMatrixAccessExpression = 
    	("," | MathMatrixAccess)+;
    	
    MathMatrixAccess = 
    	doubleDot:":" | MathArithmeticExpression;
    	
    //Matrix = "[" (MathMatrixAccessExpression || ";")* "]" | MathVectorExpression;
    //MatrixName = Name& "(" MathMatrixAccessExpression ")" | Name& "(" EndOperator ")";

    MathVectorExpression = 
    	MathArithmeticExpression ":" MathArithmeticExpression (":" MathArithmeticExpression)?;

    //TODO convert to new stuff
    //supports only 2D matrices
    EndOperator = 
    	  (":" "," endVecRight:MathVectorExpression)
    	| (endVecLeft:MathVectorExpression ","  ":")
    	| (endVecLeft:MathVectorExpression "," endVecRight:MathVectorExpression)
    	| endVec:MathVectorExpression;

    //Arithmetic expressions for numbers

    MathArithmeticValueExpression implements MathArithmeticExpression, MathExpression, Expression  = 
    	MathValueExpression;

    MathArithmeticPowerOfExpression implements MathArithmeticExpression<1>, MathExpression<1>, Expression<1>  = 
    	MathArithmeticExpression "^" MathArithmeticExpression;

    MathArithmeticMultiplicationExpression implements MathArithmeticExpression<10>, MathExpression<10>, Expression<10>  = 
    	MathArithmeticExpression "*" MathArithmeticExpression;

    MathArithmeticDivisionExpression implements MathArithmeticExpression<10>, MathExpression<10>, Expression<10>  = 
    	MathArithmeticExpression "/" MathArithmeticExpression;

    MathArithmeticAdditionExpression implements MathArithmeticExpression<900>, MathExpression<900>, Expression<900>  = 
    	MathArithmeticExpression "+" MathArithmeticExpression;

    MathArithmeticSubtractionExpression implements MathArithmeticExpression<900>, MathExpression<900>, Expression<900>  = 
    	MathArithmeticExpression "-" MathArithmeticExpression;

    MathArithmeticModuloExpression implements MathArithmeticExpression<10>, MathExpression<10>, Expression<10>  = 
    	MathArithmeticExpression "%" MathArithmeticExpression;

    MathArithmeticIncreaseByOneExpression implements MathArithmeticExpression<900>, MathExpression<900>, Expression<900>  = 
    	MathArithmeticExpression "+" "+";

    MathArithmeticDecreaseByOneExpression implements MathArithmeticExpression<900>, MathExpression<900>, Expression<900>  = 
    	MathArithmeticExpression "-" "-";

    //Arithmetic expressions for matrices

    MathMatrixArithmeticMatrixValueExpression implements MathArithmeticMatrixExpression, MathExpression, Expression  = 
    	MathValueMatrixExpression;

    MathArithmeticMatrixPowerOfExpression implements MathArithmeticMatrixExpression<1>, MathExpression<1>, Expression<1>  = 
    	MathArithmeticMatrixExpression "^" MathValueExpression;

    MathArithmeticMatrixMultiplicationExpression implements MathArithmeticMatrixExpression<10>, MathExpression<10>, Expression<10>  = 
    	MathArithmeticMatrixExpression "*" MathArithmeticMatrixExpression ;

    MathArithmeticMatrixDivisionExpression implements MathArithmeticMatrixExpression<10>, MathExpression<10>, Expression<10>  = 
    	MathArithmeticMatrixExpression "/" MathArithmeticMatrixExpression;

    MathArithmeticMatrixAdditionExpression implements MathArithmeticMatrixExpression<900>, MathExpression<900>, Expression<900>  = 
    	MathArithmeticMatrixExpression "+" MathArithmeticMatrixExpression ;

    MathArithmeticMatrixSubtractionExpression implements MathArithmeticMatrixExpression<900>, MathExpression<900>, Expression<900>  = 
    	MathArithmeticMatrixExpression "-" MathArithmeticMatrixExpression ;

    MathArithmeticMatrixTransposeExpression implements MathArithmeticMatrixExpression, MathExpression, Expression  = 
    	MathArithmeticMatrixExpression "\'";

    MathArithmeticMatrixSolutionExpression implements MathArithmeticMatrixExpression, MathExpression, Expression = 
    	MathArithmeticMatrixExpression "\\\\" MathArithmeticMatrixExpression;

    MathArithmeticMatrixEEPowerOfExpression implements MathArithmeticMatrixExpression<1>, MathExpression<1>, Expression<1>  = 
    	MathArithmeticMatrixExpression ".^" MathArithmeticExpression;

    MathArithmeticMatrixEEMultiplicationExpression implements MathArithmeticMatrixExpression<10>, MathExpression<10>, Expression<10>  = 
    	MathArithmeticMatrixExpression "." "*" MathArithmeticMatrixExpression;

    MathArithmeticMatrixEEDivisionExpression implements MathArithmeticMatrixExpression<10>, MathExpression<10>, Expression<10>  = 
    	MathArithmeticMatrixExpression "./" MathArithmeticMatrixExpression;

    //Boolean expressions
    MathTrueExpression implements MathBooleanExpression, MathExpression, Expression  = 
    	"true";
    MathFalseExpression implements MathBooleanExpression, MathExpression, Expression  = 
    	"false";

    MathBooleanOrExpression implements MathBooleanExpression, MathExpression, Expression  = 
    	MathExpression "||" MathExpression;

    MathBooleanAndExpression implements MathBooleanExpression, MathExpression, Expression  = 
    	MathExpression "&&" MathExpression;

    //Compare expressions
    MathCompareEqualExpression implements MathExpression, Expression  = 
    	MathExpression "==" MathExpression;

    MathCompareNotEqualExpression implements MathExpression, Expression  = 
    	MathExpression "!=" MathExpression;

    MathCompareSmallerThanExpression implements MathExpression, Expression = 
    	MathExpression "<" MathExpression;

    MathCompareSmallerEqualThanExpression implements MathExpression, Expression = 
    	MathExpression "<=" MathExpression;

    MathCompareGreaterThanExpression implements MathExpression, Expression = 
    	MathExpression ">" MathExpression;

    MathCompareGreaterEqualThanExpression implements MathExpression, Expression = 
    	MathExpression ">=" MathExpression;


    //new types and general stuff
     /* Like this in grammar Common2
     ast CommonMatrixType = method public String printType(){return "CommonMatrixType";};
     
     CommonMatrixType implements PrintType, Type = 
     	ElementType CommonDimension;

     CommonDimension = 
     	"^" "{" (CommonDimensionElement || ",")+ "}";
     	
     CommonDimensionElement = 
     	Name& | UnitNumber;
     */
    ast AssignmentType = method public String printType(){return "AssignmentType";};
    AssignmentType implements PrintType = 
    	matrixProperty:Name* (ElementType dim:Dimension? | CommonMatrixType );

    MatrixType = 
    	ElementType Dimension;

    DottedName = 
    	Name "." Name;


/* old grammar
    MathStatements = MathStatement+;
    
    MathStatement = ConditionalStatement
                  | Assignment
                  | forStatement:ForStatement2
                  | MathExpression;
                  
    DottedName = Name "." Name;
    
    symbol Assignment = type:AssignmentType name:Name& (assignmentOp:"=" MathExpression)? ";"
                 | (Name& | MatrixName | DottedName)
                   (assignmentOp:"=" | assignmentOp:"+=" | assignmentOp:"-=" | assignmentOp:"*=" | assignmentOp:"/=")
                    MathExpression ";"
                 ;

    ConditionalStatement = 
    	IfStatement2 (ElseIfStatement2)* (ElseStatement2)? "end" | BooleanExpression "?" (m1:MathStatement)+ ":" (m2:MathStatement)+;

    IfStatement2 = 
    	"if"  BooleanExpression "{" (MathStatement)+ "}";
    	
    ElseIfStatement2 = 
    	"elseif"  BooleanExpression "{" (MathStatement)+ "}";
    	
    ElseStatement2 = 
    	"else" "{"(MathStatement)+ "}";

    ForStatement2 = 
    	"for" forAssignment:ForAssignment2 (MathStatement)+ "end";

    ForAssignment2 = 
    	name:Name& "=" MathExpression;

    ast AssignmentType= method public String printType(){return "AssignmentType";};
    AssignmentType implements PrintType = 
    	matrixProperty:Name* ElementType dim:Dimension?;

    //ast MatrixType= method public String printType(){return "MatrixType";};
    MatrixType = 
    	ElementType Dimension;

    Dimension = 
    	"^" "{" (ArithmeticExpression || ",")+ "}";

	//TODO Refactor grammar to make ast look better and profit from left recursion possible in the new monticore versions
    MathExpression implements Expression = ArithmeticExpression
                   | ArithmeticMatrixExpression
                   | BooleanExpression;

    MathPrimaryExpression = Numberf
                      | Name&
                      | MatrixName;

    ArithmeticMatrixExpression = PlusMinusMatrixExpression;

    PlusMinusMatrixExpression = MultSolMatrixExpression  PlusMinusMatrixHelpExpression;
    PlusMinusMatrixHelpExpression = (additiveOp:"+" | additiveOp:"-") MultSolMatrixExpression PlusMinusMatrixHelpExpression
                                  |;

    MultSolMatrixExpression = UnaryAritMatrixExpression MultSolMatrixHelpExpression;
    MultSolMatrixHelpExpression = (muldivOp:"*" | muldivOp:"." star:"*" | muldivOp:"./" | muldivOp:"\\\\") UnaryAritMatrixExpression MultSolMatrixHelpExpression
                                |;

    UnaryAritMatrixExpression = (unaryaritOp:"-" | unaryaritOp:"+") TransposeMatrixExpression
                              | TransposeMatrixExpression;

    TransposeMatrixExpression = ParenthesisMatrixExpression (transposeOp:"\'")
                              | ParenthesisMatrixExpression (powOp:"^" | powOp:".^") ArithmeticExpression
                              | ParenthesisMatrixExpression;

    ParenthesisMatrixExpression = "(" ArithmeticMatrixExpression ")"
                                | Matrix | Name& | MatrixName;

    ArithmeticExpression = PlusMinusExpression;

    PlusMinusExpression = MultDivModExpression PlusMinusHelpExpression;
    PlusMinusHelpExpression = (additiveOp:"+" | additiveOp:"-") MultDivModExpression PlusMinusHelpExpression
                            |;

    MultDivModExpression = UnaryOpExpression MultDivModHelpExpression;
    MultDivModHelpExpression = (muldivOp:"*" | muldivOp:"/" | muldivOp:"%") UnaryOpExpression MultDivModHelpExpression
                             |;

    UnaryOpExpression = ParenthesisAritExpression (unaryOp:"++" | unaryOp:"--" | transposeOp:"\'")
                      | (unaryOp:"++" | unaryOp:"--") ParenthesisAritExpression
                      | "(" Type ")" ParenthesisAritExpression
                      | ParenthesisAritExpression powOp:"^" ArithmeticExpression
                      | ParenthesisAritExpression;

    ParenthesisAritExpression = "(" ArithmeticExpression ")"
                              | MathPrimaryExpression;

    BooleanExpression =  LogicalOrExpression | DottedName;

    LogicalOrExpression = LogicalAndExpression LogicalOrHelpExpression;
    LogicalOrHelpExpression = or:"||" LogicalAndExpression LogicalOrHelpExpression
                                |;

    LogicalAndExpression = LogicalEqualsExpression LogicalAndHelpExpression;
    LogicalAndHelpExpression = and:"&&" LogicalEqualsExpression LogicalAndHelpExpression
                                 |;

    LogicalEqualsExpression = LogicalCompareExpression LogicalEqualsHelpExpression;
    LogicalEqualsHelpExpression = equalsOp:"==" LogicalCompareExpression LogicalEqualsHelpExpression
                                    | equalsOp:"!=" LogicalCompareExpression LogicalEqualsHelpExpression
                                    |;

    LogicalCompareExpression = LogicalParenthesisExpression LogicalCompareHelpExpression;
    LogicalCompareHelpExpression = compareOp:">" LogicalParenthesisExpression LogicalCompareHelpExpression
                                     | compareOp:">=" LogicalParenthesisExpression LogicalCompareHelpExpression
                                     | compareOp:"<" LogicalParenthesisExpression LogicalCompareHelpExpression
                                     | compareOp:"<=" LogicalParenthesisExpression LogicalCompareHelpExpression
                                     |;

    LogicalParenthesisExpression = "(" BooleanExpression ")"
                                     | ArithmeticExpression;

    Matrix = "[" (VectorInner || ";")* "]" | Vector;
    MatrixName = Name& "(" VectorInner ")" | Name& "(" Endoperator ")";

    //supports only 2D matrices
    Endoperator = ":" "," EndVecRight | EndVecLeft ","  ":" | EndVecLeft "," EndVecRight | EndVec ;
    EndVecRight = EndVec;
    EndVecLeft = EndVec;
    EndVec =  ArithmeticExpression (":" ArithmeticExpression (":" ArithmeticExpression)?)?;

    Vector = ArithmeticExpression ":" ArithmeticExpression (":" ArithmeticExpression)?;
    VectorInner = ArithmeticExpression ((",")? ArithmeticExpression)*;

*/

}
