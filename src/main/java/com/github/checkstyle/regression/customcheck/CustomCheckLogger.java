
package com.github.checkstyle.regression.customcheck;

import com.puppycrawl.tools.checkstyle.AuditEventDefaultFormatter;
import com.puppycrawl.tools.checkstyle.AuditEventFormatter;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean;

public class CustomCheckLogger extends AutomaticBean implements AuditListener {
    /** Formatter for the log message. */
    private final AuditEventFormatter formatter = new AuditEventDefaultFormatter();

    @Override
    public void auditStarted(AuditEvent event) {
        // no code needed
    }

    @Override
    public void auditFinished(AuditEvent event) {
        // no code needed
    }

    @Override
    public void fileStarted(AuditEvent event) {
        System.out.println("Scanning File: " + event.getFileName());
    }

    @Override
    public void fileFinished(AuditEvent event) {
        // no code needed
    }

    @Override
    public void addError(AuditEvent event) {
        final String errorMessage = formatter.format(event);
        System.err.println(errorMessage);
    }

    @Override
    public void addException(AuditEvent event, Throwable throwable) {
        throwable.printStackTrace(System.err);
    }
}
