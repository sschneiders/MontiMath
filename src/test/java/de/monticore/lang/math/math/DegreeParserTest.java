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
package de.monticore.lang.math.math;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import de.monticore.antlr4.MCConcreteParser;
import de.monticore.lang.math.math._ast.ASTAssignmentType;
import de.monticore.lang.math.math._ast.ASTMathDeclarationExpression;
import de.monticore.lang.math.math._ast.ASTMathStatements;
import de.monticore.lang.math.math._parser.MathParser;
import de.monticore.lang.monticar.types2._ast.ASTElementType;
import org.junit.Test;

/**
 * Created by MichaelvonWenckstern on 20.12.2017.
 */
public class DegreeParserTest {
  // this test fails right now, but I do not understand why?
  // since the same test directly invoked with Types2 parser works:
  // https://github.com/EmbeddedMontiArc/languagescommon/blob/master/src/test/java/de/monticore/lang/monticar/Types2ParserTest.java#L40
  @Test
  public void testElementType() throws IOException {
    MathParser parser = new MathParser();
    parser.setParserTarget(MCConcreteParser.ParserExecution.EOF);
    ASTElementType ast = parser.parseString_ElementType("Q(-90°:90°)").orElse(null);
    assertNotNull(ast);
  }
  
  @Test
  public void testAssignmentType() throws IOException {
    MathParser parser = new MathParser();
    parser.setParserTarget(MCConcreteParser.ParserExecution.EOF);
    ASTAssignmentType ast = parser.parseString_AssignmentType("Q(-90°:90°)^2 x").orElse(null);
    assertNotNull(ast);
  }

  @Test
  public void testMathStatement() throws IOException {
    MathParser parser = new MathParser();
    ASTMathStatements ast = parser.parseString_MathStatements("Q(-90°:90°)^2 x;").orElse(null);
    assertNotNull(ast);
  }

  @Test
  public void testMathDeclarationExpression() throws IOException {
    MathParser parser = new MathParser();
    ASTMathDeclarationExpression ast = parser.parseString_MathDeclarationExpression("Q(-90°:90°)^2 x;").orElse(null);
    assertNotNull(ast);
  }
}
