package com.github.checkstyle.regression.customcheck.unittestprocessorcheck;

import java.io.File;

import org.junit.Test;

import com.github.sevntu.checkstyle.checks.design.InputCheckstyleTestMakeupCheck;

public class InputUnitTestProcessor {
    private String s;

    public void method1() {
    }

    public void method2() {
        String s = "";
        method1();
    }

    @Test
    public void method3() {
    }

    @org.junit.Test
    public void method4() {
    }

    @Test
    public void method5() {
        new Thread(new Runnable() {
            private String s;

            @Override
            public void run() {
            }
        });
    }

    @Test
    public void method6() {
        java.util.List<String> t;
    }

    private static void test() {
    }
}