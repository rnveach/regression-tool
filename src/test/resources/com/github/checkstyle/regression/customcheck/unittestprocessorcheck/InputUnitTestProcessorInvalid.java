package com.github.checkstyle.regression.customcheck.unittestprocessorcheck;

import java.io.File;

import org.junit.Test;

import com.github.sevntu.checkstyle.checks.design.InputCheckstyleTestMakeupCheckInvalid.DefaultConfiguration;
import com.github.sevntu.checkstyle.checks.design.InputCheckstyleTestMakeupCheckInvalid.Configuration;
import com.github.sevntu.checkstyle.checks.design.InputCheckstyleTestMakeupCheckInvalid.ENUM;

public class InputUnitTestProcessorInvalid {
    private String field;

    @Test
    public void method1() {
        verify();
    }

    @Test
    public void method2() {
        DefaultConfiguration config;
        verify();
    }

    @Test
    public void method3() {
        DefaultConfiguration config = customCreateConfig();
        verify();
    }

    @Test
    public void method4() {
        DefaultConfiguration config = new DefaultConfiguration();
        verify();
    }

    @Test
    public void method5() {
        final Configuration config = createModuleConfig("test");
        File file = new File("");
        file = new File("");
        config.addAttribute("", file.getPath());
        config.addAttribute("", file.getAbsolutePath());
        config.addAttribute("", customValue());
        config.addAttribute("", ENUM.TEST.getName(0));
        config.addAttribute("", ENUM.TEST.other());
        config.addAttribute("", ENUM.TEST.same.getName(0));
        config.addAttribute("", 0);
        config.addAttribute("", "" + 0);
        config.addAttribute("", 0 + "");
        final String s = "";
        config.addAttribute("", s);
        verify();
    }

    @Test
    public void method6() {
        final Configuration config = createModuleConfig();
        config.addAttribute("property", "ignore");
        verify();
    }

    @Test
    public void method7() {
        final Configuration config = createModuleConfig(new String("test"));
        config.addAttribute("property", "ignore");
        final Configuration config2 = createModuleConfig(InputUnitTestProcessorInvalid.field);
        verify();
    }

    @Test
    public void method8() {
        final Configuration config = createModuleConfig("test");
        config.addAttribute("property", "ignore");
        try {
            verify();
        }
        catch (Exception ex) {
        }
    }

    private static void verify() {
    }

    private DefaultConfiguration customCreateConfig() {
        return new DefaultConfiguration();
    }

    private static DefaultConfiguration createModuleConfig() {
        return new DefaultConfiguration();
    }

    private static DefaultConfiguration createModuleConfig(String name) {
        return new DefaultConfiguration();
    }

    private static class Configuration {
        public void addAttribute(String s, String t) {
        }

        public void addAttribute(String s, int i) {
        }
    }

    private static class DefaultConfiguration extends Configuration {
    }

    private enum ENUM {
        TEST;

        private static final ENUM same = TEST;

        public String other() {
            return name();
        }

        public String getName(int i) {
            return name();
        }
    }
}