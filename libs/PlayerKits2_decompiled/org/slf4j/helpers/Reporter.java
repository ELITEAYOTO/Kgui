/*
 * Decompiled with CFR 0.152.
 */
package org.slf4j.helpers;

import java.io.PrintStream;

public class Reporter {
    static final String SLF4J_DEBUG_PREFIX = "SLF4J(D): ";
    static final String SLF4J_INFO_PREFIX = "SLF4J(I): ";
    static final String SLF4J_WARN_PREFIX = "SLF4J(W): ";
    static final String SLF4J_ERROR_PREFIX = "SLF4J(E): ";
    public static final String SLF4J_INTERNAL_REPORT_STREAM_KEY = "slf4j.internal.report.stream";
    private static final String[] SYSOUT_KEYS = new String[]{"System.out", "stdout", "sysout"};
    public static final String SLF4J_INTERNAL_VERBOSITY_KEY = "slf4j.internal.verbosity";
    private static final TargetChoice TARGET_CHOICE = Reporter.getTargetChoice();
    private static final Level INTERNAL_VERBOSITY = Reporter.initVerbosity();

    private static TargetChoice getTargetChoice() {
        String reportStreamStr = System.getProperty(SLF4J_INTERNAL_REPORT_STREAM_KEY);
        if (reportStreamStr == null || reportStreamStr.isEmpty()) {
            return TargetChoice.Stderr;
        }
        for (String s : SYSOUT_KEYS) {
            if (!s.equalsIgnoreCase(reportStreamStr)) continue;
            return TargetChoice.Stdout;
        }
        return TargetChoice.Stderr;
    }

    private static Level initVerbosity() {
        String verbosityStr = System.getProperty(SLF4J_INTERNAL_VERBOSITY_KEY);
        if (verbosityStr == null || verbosityStr.isEmpty()) {
            return Level.INFO;
        }
        if (verbosityStr.equalsIgnoreCase("DEBUG")) {
            return Level.DEBUG;
        }
        if (verbosityStr.equalsIgnoreCase("ERROR")) {
            return Level.ERROR;
        }
        if (verbosityStr.equalsIgnoreCase("WARN")) {
            return Level.WARN;
        }
        return Level.INFO;
    }

    static boolean isEnabledFor(Level level) {
        return level.levelInt >= Reporter.INTERNAL_VERBOSITY.levelInt;
    }

    private static PrintStream getTarget() {
        switch (TARGET_CHOICE.ordinal()) {
            case 1: {
                return System.out;
            }
        }
        return System.err;
    }

    public static void debug(String msg) {
        if (Reporter.isEnabledFor(Level.DEBUG)) {
            Reporter.getTarget().println(SLF4J_DEBUG_PREFIX + msg);
        }
    }

    public static void info(String msg) {
        if (Reporter.isEnabledFor(Level.INFO)) {
            Reporter.getTarget().println(SLF4J_INFO_PREFIX + msg);
        }
    }

    public static final void warn(String msg) {
        if (Reporter.isEnabledFor(Level.WARN)) {
            Reporter.getTarget().println(SLF4J_WARN_PREFIX + msg);
        }
    }

    public static final void error(String msg, Throwable t) {
        Reporter.getTarget().println(SLF4J_ERROR_PREFIX + msg);
        Reporter.getTarget().println("SLF4J(E): Reported exception:");
        t.printStackTrace(Reporter.getTarget());
    }

    public static final void error(String msg) {
        Reporter.getTarget().println(SLF4J_ERROR_PREFIX + msg);
    }

    private static enum TargetChoice {
        Stderr,
        Stdout;

    }

    private static enum Level {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3);

        int levelInt;

        private Level(int levelInt) {
            this.levelInt = levelInt;
        }

        private int getLevelInt() {
            return this.levelInt;
        }
    }
}

