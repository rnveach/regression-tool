package com.github.checkstyle.regression.customcheck.unittestprocessorcheck;

import java.io.File;

import org.junit.Test;

public class InputUnitTestProcessorValid {
    @Test
    public void method1() {
        final Configuration config = null;
        verify();
    }

    @Test
    public void method2() {
        final Configuration config = createModuleConfig("method2");
        verify();
    }

    @Test
    public void method3() {
        final DefaultConfiguration config = createRootConfig();
        final DefaultConfiguration config2 = createTreeWalkerConfig();
        verifyWarns();
    }

    @Test
    public void method4() {
        final Configuration config = createModuleConfig(InputUnitTestProcessor.class);
        config.addAttribute(null, null);
        config.addAttribute("null", null);
        config.addAttribute("string", "value");
        config.addAttribute("string+", "string" + "plus");
        final File file = new File("");
        config.addAttribute("", file.getPath());
        config.addAttribute("enum1", ENUM.TEST.toString());
        config.addAttribute("enum2", ENUM.TEST.getName());
        config.addAttribute("enum3", ENUM.TEST.name());
        config.addAttribute("path1", getPath("getPath"));
        config.addAttribute("path2", getNonCompilablePath("getNonCompilablePath"));
        config.addAttribute("path3", getUriString("getUriString"));
        config.addAttribute("path4", getResourcePath("getResourcePath"));
        verify();
    }

    @Test
    public void method5() {
        final Configuration config = createModuleConfig(InputUnitTestProcessor.class);
        config.addAttribute("string", "value2", "ignore this");
        verify();
    }

    private static void verify() {
    }

    private static void verifyWarns() {
    }

    private static void verifySuppressed() {
    }

    private static DefaultConfiguration createModuleConfig(String className) {
        return new DefaultConfiguration();
    }

    private static DefaultConfiguration createModuleConfig(Class<?> clss) {
        return new DefaultConfiguration();
    }

    private static DefaultConfiguration createRootConfig() {
        return new DefaultConfiguration();
    }

    private static DefaultConfiguration createTreeWalkerConfig() {
        return new DefaultConfiguration();
    }

    private static class Configuration {
        public void addAttribute(String s, String t) {
        }

        public void addAttribute(String s, String t, String q) {
        }
    }

    private static class DefaultConfiguration extends Configuration {
    }

    private enum ENUM {
        TEST;

        public String getName() {
            return name();
        }
    }
}
