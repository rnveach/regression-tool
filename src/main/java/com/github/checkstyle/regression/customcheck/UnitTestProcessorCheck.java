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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.checkstyle.regression.data.ImmutableProperty;
import com.github.checkstyle.regression.data.ModuleInfo;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.AnnotationUtility;
import com.puppycrawl.tools.checkstyle.utils.ScopeUtils;

/**
 * The custom check which processes the unit test class of a checkstyle module and grab the possible
 * properties that could be used for generating config.
 *
 * <p>
 * The check would walk through the {@code @Test} annotation, find variable definition like
 * {@code final DefaultConfiguration checkConfig = createModuleConfig(FooCheck.class)} and grab the
 * property info from {@link DefaultConfiguration#addAttribute(String, String)} method call.
 * </p>
 *
 * <p>
 * The check also support to detect module config which defined as a class field. This kind of
 * config is detected by the assignment in the unit test method, like {@code checkConfig =
 * createModuleConfig(FooCheck.class)}, where {@code checkConfig} could be a class field.
 * </p>
 *
 * @author LuoLiangchen
 */
public class UnitTestProcessorCheck extends AbstractCheck {
    /** The map of unit test's module name to properties. */
    private static final Map<String, Set<ModuleInfo.Property>> MODULE_TO_PROPERTIES =
        new LinkedHashMap<>();

    private static final String UNSUPPORTED_AST_EXCEPTION =
        "the processor cannot support this ast: ";

    private static String checkstyleBasePath = "";

    private String packagePath;

    /** AST of method that is currently being examined. */
    private DetailAST methodAst;
    /** List of variable names that reference a file. */
    private Set<String> fileVariableNames = new HashSet<>();

    private final Map<String, String> checkConfigNameType = new HashMap<String, String>();
    /** {@code true} if the 'verify' method was found in the method. */
    private boolean foundVerify;

    /** Current method's unit test's module name to properties. */
    private final Map<String, Set<ModuleInfo.Property>> methodModuleToProperties = new LinkedHashMap<>();

    /** Regular expression for matching a create method by name. */
    private Pattern createMethodRegexp = Pattern
            .compile("create(Root|Module|TreeWalker)Config|getModuleConfig");

    /** Regular expression for matching a verify method by name. */
    private Pattern verifyMethodRegexp = Pattern.compile("verify(Warns|Suppressed)?");

    @Override
    public int[] getRequiredTokens() {
        return new int[] {
            TokenTypes.PACKAGE_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.VARIABLE_DEF,
            TokenTypes.METHOD_CALL,
        };
    }

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    public static Map<String, Set<ModuleInfo.Property>> getResults() {
        final Map<String, Set<ModuleInfo.Property>> result = new LinkedHashMap<>(
                MODULE_TO_PROPERTIES);
        MODULE_TO_PROPERTIES.clear();
        return result;
    }

    public static void setCheckstyleBasePath(String checkstyleBasePath) {
        UnitTestProcessorCheck.checkstyleBasePath = checkstyleBasePath;
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        resetInternalFields();
    }

    @Override
    public void visitToken(DetailAST ast) {
        switch (ast.getType()) {
            case TokenTypes.PACKAGE_DEF:
                packagePath = FullIdent.createFullIdent(ast.getLastChild().getPreviousSibling())
                        .getText().replace(".", "/")
                        + "/";
                break;
            case TokenTypes.METHOD_DEF:
                checkMethod(ast);
                break;
            case TokenTypes.VARIABLE_DEF:
                checkVariable(ast);
                break;
            case TokenTypes.METHOD_CALL:
                if (methodAst != null) {
                    checkMethodCall(ast);
                }
                break;
            default:
                throw new IllegalStateException(UNSUPPORTED_AST_EXCEPTION + ast);
        }
    }

    private void checkMethod(DetailAST ast) {
        if (methodAst == null && AnnotationUtility.containsAnnotation(ast, "Test")
                || AnnotationUtility.containsAnnotation(ast, "org.junit.Test")) {
            methodAst = ast;
        }
    }

    private void checkVariable(DetailAST ast) {
        if (methodAst != null && ScopeUtils.isLocalVariableDef(ast)) {
            final DetailAST type = ast.findFirstToken(TokenTypes.TYPE).findFirstToken(
                    TokenTypes.IDENT);

            if (type != null) {
                final String typeText = type.getText();

                if ("DefaultConfiguration".equals(typeText) || "Configuration".equals(typeText)) {
                    checkConfigurationVariable(ast);
                }
                else if ("File".equals(typeText)
                        && ast.findFirstToken(TokenTypes.MODIFIERS)
                                .findFirstToken(TokenTypes.FINAL) != null) {
                    fileVariableNames.add(ast.findFirstToken(TokenTypes.IDENT).getText());
                }
            }
        }
    }

    private void checkConfigurationVariable(DetailAST ast) {
        final DetailAST assignment = ast.findFirstToken(TokenTypes.ASSIGN);

        if (assignment != null
                && assignment.getFirstChild().getFirstChild().getType() == TokenTypes.METHOD_CALL) {
            final DetailAST methodCall = assignment.getFirstChild().getFirstChild();
            final DetailAST assignmentMethod = methodCall.findFirstToken(TokenTypes.IDENT);

            if (assignmentMethod != null
                    && createMethodRegexp.matcher(assignmentMethod.getText()).matches()) {
                try {
                    checkConfigNameType.put(
                            ast.findFirstToken(TokenTypes.IDENT).getText(),
                            getCreateMethodClass(assignmentMethod.getText(),
                                    methodCall.findFirstToken(TokenTypes.ELIST)));
                }
                catch (IllegalStateException ex) {
                    // TODO: print violation
                    ex.printStackTrace();
                }
            }
        }
    }

    private void checkMethodCall(DetailAST ast) {
        final DetailAST firstChild = ast.getFirstChild();
        final String methodCallName = getMethodCallName(firstChild);
        final String methodCallerName = getMethodCallerName(firstChild);

        if ("addAttribute".equals(methodCallName) && checkConfigNameType.containsKey(methodCallerName)) {
            try {
                final DetailAST elist = ast.findFirstToken(TokenTypes.ELIST);
                String propertyName = null;
                String propertyValue = null;
                int position = 0;

                for (DetailAST expression = elist.getFirstChild(); expression != null;
                        expression = expression.getNextSibling()) {
                    if (expression.getType() == TokenTypes.EXPR) {
                        if (position == 0) {
                            propertyName = convertExpressionToText(expression.getFirstChild());
                        }
                        else if (position == 1) {
                            propertyValue = convertExpressionToText(expression.getFirstChild());
                        }

                        position++;
                    }
                }

                if (propertyName != null && propertyValue != null) {
                    final String key = checkConfigNameType.get(methodCallerName);
                    Set<ModuleInfo.Property> list = methodModuleToProperties.get(key);

                    if (list == null) {
                        list = new HashSet<>();
                        methodModuleToProperties.put(key, list);
                    }

                    list.add(ImmutableProperty.builder().name(propertyName).value(propertyValue)
                            .build());
                }
            }
            catch (IllegalStateException | UnsupportedOperationException ex) {
                // TODO: print violation
                ex.printStackTrace();
            }
        }
        else if (methodCallerName.equals(methodCallName)
                && ast.getParent().getParent().getType() != TokenTypes.METHOD_CALL
                && verifyMethodRegexp.matcher(methodCallName).matches() && !hasTryParent(ast)) {
            foundVerify = true;
        }
    }

    /**
     * Retrieves the name of the method being called.
     *
     * @param ast
     *        The method call token to examine.
     * @return The name of the method.
     */
    private String getMethodCallName(DetailAST ast) {
        final String result;
        if (ast.getType() == TokenTypes.DOT) {
            result = getMethodCallName(ast.getFirstChild().getNextSibling());
        }
        else {
            result = ast.getText();
        }
        return result;
    }

    /**
     * Retrieves the name of the variable calling the method.
     *
     * @param ast
     *        The method call token to examine.
     * @return The name of who is calling the method.
     */
    private String getMethodCallerName(DetailAST ast) {
        final String result;
        if (ast.getType() == TokenTypes.DOT) {
            result = getMethodCallName(ast.getFirstChild());
        }
        else {
            result = ast.getText();
        }
        return result;
    }

    private static boolean hasTryParent(DetailAST ast) {
        boolean result = false;
        DetailAST node = ast;

        while (node != null) {
            if (node.getType() == TokenTypes.LITERAL_TRY) {
                result = true;
                break;
            }

            node = node.getParent();
        }

        return result;
    }

    @Override
    public void leaveToken(DetailAST ast) {
        if (ast == methodAst) {
            if (foundVerify) {
                for (Entry<String, Set<ModuleInfo.Property>> entry : methodModuleToProperties
                        .entrySet()) {
                    final Set<ModuleInfo.Property> list = MODULE_TO_PROPERTIES.get(entry.getKey());

                    if (list == null) {
                        MODULE_TO_PROPERTIES.put(entry.getKey(), entry.getValue());
                    }
                    else {
                        list.addAll(entry.getValue());
                    }
                }
            }

            resetInternalFields();
        }
    }

    /** Resets the internal fields when a new file/method is to be processed. */
    private void resetInternalFields() {
        packagePath = "";
        methodAst = null;
        fileVariableNames.clear();
        checkConfigNameType.clear();
        foundVerify = false;
        methodModuleToProperties.clear();
    }

    // ////////////////////////////////
    // ////////////////////////////////
    // ////////////////////////////////

    private String getCreateMethodClass(String methodName, DetailAST ast) {
        String result = null;

        if (methodName.contains("Root")) {
            result = Checker.class.getSimpleName();
        }
        else if (methodName.contains("TreeWalker")) {
            result = TreeWalker.class.getSimpleName();
        }
        else {
            result = convertCreateMethodToText(ast);
        }

        return result;
    }

    private String convertCreateMethodToText(DetailAST ast) {
        String result = null;

        if (ast == null) {
            throw new IllegalStateException("the processor cannot support no parameters");
        }

        switch (ast.getType()) {
            case TokenTypes.ELIST:
            case TokenTypes.EXPR:
                result = convertCreateMethodToText(ast.getFirstChild());
                break;
            case TokenTypes.STRING_LITERAL:
                final String original = ast.getText();
                result = original.substring(1, original.length() - 1);
                break;
            case TokenTypes.DOT:
                final DetailAST firstChild = ast.getFirstChild();
                if (firstChild.getNextSibling().getType() == TokenTypes.LITERAL_CLASS) {
                    result = firstChild.getText();
                    break;
                }
                // falls through
            default:
                throw new IllegalStateException(UNSUPPORTED_AST_EXCEPTION + ast);
        }

        return result;
    }

    /**
     * Converts an expression content to raw text.
     *
     * @param ast
     *        the first child of expression ast to convert
     * @return the converted raw text
     */
    private String convertExpressionToText(DetailAST ast) {
        String result = null;

        switch (ast.getType()) {
            case TokenTypes.STRING_LITERAL:
                final String original = ast.getText();
                result = original.substring(1, original.length() - 1);
                break;
            case TokenTypes.METHOD_CALL:
                result = convertExpressionMethodCallToText(ast);
                break;
            case TokenTypes.PLUS:
                result = convertExpressionToText(ast.getFirstChild())
                        + convertExpressionToText(ast.getLastChild());
                break;
            case TokenTypes.LITERAL_NULL:
                break;
            default:
                throw new IllegalStateException(UNSUPPORTED_AST_EXCEPTION + ast);
        }

        return result;
    }

    private String convertExpressionMethodCallToText(DetailAST ast) {
        final DetailAST firstChild = ast.getFirstChild();
        String result = null;

        if (firstChild.getType() == TokenTypes.DOT) {
            if (firstChild.getFirstChild().getType() == TokenTypes.DOT) {
                if (isEnumerationCall(firstChild)) {
                    result = firstChild.getFirstChild().getFirstChild().getNextSibling().getText();
                }
            }
            else if (isFileVariable(firstChild.getFirstChild())) {
                throw new UnsupportedOperationException(
                        "the processor can't support file variables at this time: " + firstChild);
            }
        }
        else {
            final String methodName = firstChild.getText();

            if (isMethodGetPath(methodName)) {
                result = getPathFromMethodCall(methodName, ast.getFirstChild().getNextSibling());
            }
        }

        if (result == null) {
            throw new IllegalStateException(
                    "the processor cannot support this ast in method call: " + firstChild);
        }

        return result;
    }

    /**
     * Checks if the method call is 'getPath' on a {@link File} variable.
     * @param firstChild The AST to examine.
     * @return {@code true} if the method call is on a file variable.
     */
    private boolean isFileVariable(DetailAST firstChild) {
        return "getPath".equals(firstChild.getNextSibling().getText())
                && fileVariableNames.contains(firstChild.getText());
    }

    private String getPathFromMethodCall(String methodName, DetailAST elistAst) {
        String result = null;

        if (elistAst.getChildCount() == 1
                && elistAst.getFirstChild().getType() == TokenTypes.EXPR) {
            final String path = convertExpressionToText(elistAst.getFirstChild().getFirstChild());

            if (path != null) {
                switch (methodName) {
                    case "getPath":
                    case "getUriString":
                    case "getResourcePath":
                        result = checkstyleBasePath + "src/test/resources/" + packagePath + path;
                        break;
                    case "getNonCompilablePath":
                        result = checkstyleBasePath + "src/test/resources-noncompilable/"
                                + packagePath + path;
                        break;
                    default:
                        throw new IllegalStateException("Unknown method name '" + methodName
                                + "': " + elistAst);
                }
            }
        }

        return result;
    }

    /**
     * Checks if the method call is calling toString, getName, or name on an enumeration.
     *
     * @param ast
     *        The AST to examine.
     * @return {@code true} if the method call is on a enumeration.
     */
    private static boolean isEnumerationCall(DetailAST ast) {
        boolean result = false;
        final DetailAST firstChild = ast.getFirstChild();
        final DetailAST methodCalled = firstChild.getNextSibling();
        final DetailAST parameters = ast.getNextSibling();

        if (firstChild.getFirstChild().getType() == TokenTypes.IDENT
                && ("toString".equals(methodCalled.getText())
                        || "getName".equals(methodCalled.getText()) || "name".equals(methodCalled
                        .getText())) && parameters.getChildCount() == 0) {
            result = true;
        }

        return result;
    }

    /**
     * Checks if the method name is a form of 'getPath'.
     *
     * @param methodName
     *        The name to examine.
     * @return {@code true} if the method is of the form.
     */
    private static boolean isMethodGetPath(String methodName) {
        return "getPath".equals(methodName) || "getNonCompilablePath".equals(methodName)
                || "getUriString".equals(methodName) || "getResourcePath".equals(methodName);
    }
}
