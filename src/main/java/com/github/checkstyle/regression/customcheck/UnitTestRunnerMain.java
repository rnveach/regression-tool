
package com.github.checkstyle.regression.customcheck;

import java.io.File;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.github.checkstyle.regression.data.ModuleInfo;
import com.github.checkstyle.regression.data.ModuleInfo.Property;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

public class UnitTestRunnerMain {
    private static Comparator<Property> propertyComparable = new Comparator<Property>() {
        @Override
        public int compare(Property left, Property right) {
            int diff = left.name().compareTo(right.name());

            if (diff == 0) {
                diff = left.value().compareTo(right.value());
            }

            return diff;
        }
    };

    public static void main(String[] arguments) throws CheckstyleException {
        process(new File(
                "C:\\Rickys\\Java\\checkstyleWorkspaceEclipse\\checkstyle\\src\\test\\java"));

        final Map<String, Set<ModuleInfo.Property>> results = UnitTestProcessorCheck.getResults();

        for (Entry<String, Set<Property>> entry : results.entrySet()) {
            System.out.println(entry.getKey() + ":");

            final Set<Property> properties = new TreeSet<Property>(propertyComparable);
            properties.addAll(entry.getValue());

            for (Property property : properties) {
                System.out.println("\t" + property.name() + " = " + property.value());
            }
        }
    }

    private static void process(File file) throws CheckstyleException {
        if (file.isDirectory()) {
            final File[] files = file.listFiles();

            if (files != null) {
                for (File item : files) {
                    process(item);
                }
            }
        }
        else {
            CustomCheckProcessor.process(file.getAbsolutePath(), UnitTestProcessorCheck.class);
        }
    }
}
