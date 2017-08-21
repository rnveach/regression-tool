////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2017 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.github.checkstyle.regression.customcheck;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.github.checkstyle.regression.data.ImmutableProperty;
import com.github.checkstyle.regression.data.ModuleInfo;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class UnitTestProcessorCheckTest {
    private static String getPath(String name) {
        return "src/test/resources/com/github/checkstyle/regression/customcheck/"
                + "unittestprocessorcheck/" + name;
    }

    @Test
    public void testTokens() {
        final UnitTestProcessorCheck check = new UnitTestProcessorCheck();

        assertArrayEquals("expected tokens", new int[] {
            TokenTypes.PACKAGE_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.VARIABLE_DEF,
            TokenTypes.METHOD_CALL,
        }, check.getAcceptableTokens());
    }

    @Test
    public void testNormal() throws Exception {
        CustomCheckProcessor.process(
                getPath("InputUnitTestProcessor.java"),
                UnitTestProcessorCheck.class);

        final Map<String, Set<ModuleInfo.Property>> map = UnitTestProcessorCheck.getResults();

        assertEquals("The size of UTs is wrong", 0, map.size());
    }

    @Test
    public void testInvalid() throws Exception {
        CustomCheckProcessor.process(
                getPath("InputUnitTestProcessorInvalid.java"),
                UnitTestProcessorCheck.class);

        final Map<String, Set<ModuleInfo.Property>> map = UnitTestProcessorCheck.getResults();

        assertEquals("The size of UTs is wrong", 0, map.size());
    }

    @Test
    public void testValid() throws Exception {
        UnitTestProcessorCheck.setCheckstyleBasePath("csBasePath/");
        CustomCheckProcessor.process(
                getPath("InputUnitTestProcessorValid.java"),
                UnitTestProcessorCheck.class);

        final Map<String, Set<ModuleInfo.Property>> map = UnitTestProcessorCheck.getResults();

        assertEquals("The size of UTs is wrong", 1, map.size());
        assertPropertiesEquals(map, "InputUnitTestProcessor",
                ImmutableProperty.builder().name("string").value("value").build(),
                ImmutableProperty.builder().name("string").value("value2").build(),
                ImmutableProperty.builder().name("string+").value("stringplus").build(),
                ImmutableProperty.builder().name("enum1").value("TEST").build(),
                ImmutableProperty.builder().name("enum2").value("TEST").build(),
                ImmutableProperty.builder().name("enum3").value("TEST").build(),
                ImmutableProperty.builder().name("path1")
                    .value("csBasePath/src/test/resources/getPath").build(),
                ImmutableProperty.builder().name("path2")
                    .value("csBasePath/src/test/resources-noncompilable/getNonCompilablePath")
                    .build(),
                ImmutableProperty.builder().name("path3")
                    .value("csBasePath/src/test/resources/getUriString").build(),
                ImmutableProperty.builder().name("path4")
                    .value("csBasePath/src/test/resources/getResourcePath").build());
    }

    @Test
    public void testUnacceptableToken() {
        final UnitTestProcessorCheck check = new UnitTestProcessorCheck();
        final DetailAST ast = new DetailAST();
        ast.setType(TokenTypes.LITERAL_SYNCHRONIZED);
        ast.setText("Test");
        ast.setLineNo(1);
        ast.setColumnNo(2);

        try {
            check.visitToken(ast);
            fail("Exception expected");
        }
        catch (Exception ex) {
            assertEquals("expected exception", "the processor cannot support this ast: Test[1x2]",
                    ex.getMessage());
        }
    }

    private static void assertPropertiesEquals(Map<String, Set<ModuleInfo.Property>> map,
                                               String key, ModuleInfo.Property... properties) {
        assertEquals("properties is wrong from UT:" + key,
                Arrays.stream(properties).collect(Collectors.toSet()), map.get(key));
    }
}
