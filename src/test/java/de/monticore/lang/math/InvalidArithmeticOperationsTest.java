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
package de.monticore.lang.math;

import de.monticore.lang.math._cocos.MathCocos;
import de.monticore.lang.math._cocos.MathCoCoChecker;
import de.se_rwth.commons.logging.Finding;
import de.se_rwth.commons.logging.Log;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by Tobias PC on 30.12.2016.
 */
public class InvalidArithmeticOperationsTest extends AbstractMathChecker {
    @Override
    protected MathCoCoChecker getChecker() {
        return MathCocos.createChecker();
    }
    @BeforeClass
    public static void init() {
        Log.enableFailQuick(false);
    }

    @Before
    public void setUp() {
        Log.getFindings().clear();
    }

    private static String MODEL_PATH_INVALID = "src/test/resources/symtab/";
    @Ignore
    @Test
    public void testInvalidArithmeticOperations() {
        String modelName = "InvalidArithmeticOperations.m";
        String errorCode1 = "0xMATH04";
        String errorCode2 = "0xMATH06";
        String errorCode3 = "0xMATH11";

        Collection<Finding> expectedErrors = Arrays
                .asList(
                        Finding.error(errorCode1 + " Matrix Addition with different Dimensions\nMatrix1: "+1+" x "+ 2+ "\nMatrix2: "+1+" x "+ 3),
                        Finding.error(errorCode2 + " Matrix Multiplication with wrong Dimensions\nMatrix1: "+3+" x "+ 2+ "\nMatrix2: "+3+" x "+ 2),
                        Finding.error(errorCode3 + " Matrix Power with different Column and Row Dimensions")
                );
        testModelForErrors(MODEL_PATH_INVALID + modelName, expectedErrors);
    }
}
