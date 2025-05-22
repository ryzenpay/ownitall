package ryzen.ownitall.util;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * <p>
 * Logs class.
 * </p>
 *
 * @author ryzen
 */
public class LogConfig {
    /** Constant <code>globalLevel</code> */
    public static Level globalLevel;
    private static Stack<Map.Entry<Level, String>> globalHistory = new Stack<>();
    private static final int historySize = 10;

    /**
     * <p>
     * getLatestLog.
     * </p>
     *
     * @return a {@link java.util.Map.Entry} object
     */
    public static Entry<Level, String> getLatestLog() {
        for (Entry<Level, String> entry : globalHistory) {
            if (entry.getKey().intLevel() == globalLevel.intLevel()) {
                return entry;
            }
        }
        return null;
    }

    /**
     * <p>
     * getLogs.
     * </p>
     *
     * @return a {@link java.util.LinkedHashSet} object
     */
    public static LinkedHashSet<Entry<Level, String>> getLogs() {
        LinkedHashSet<Entry<Level, String>> logs = new LinkedHashSet<>();
        for (Entry<Level, String> entry : globalHistory) {
            // log level or higher
            if (entry.getKey().intLevel() <= globalLevel.intLevel()) {
                logs.add(entry);
            }
        }
        return logs;
    }

    /**
     * <p>
     * printLogs.
     * </p>
     */
    public static void printLogs() {
        printLogs(globalLevel);
    }

    /**
     * <p>
     * printLogs.
     * </p>
     *
     * @param level a {@link org.apache.logging.log4j.Level} object
     */
    public static void printLogs(Level level) {
        for (Entry<Level, String> entry : globalHistory) {
            // log level or higher
            if (entry.getKey().intLevel() <= level.intLevel()) {
                System.out.println("[" + entry.getKey().toString().toUpperCase() + "] " + entry.getValue());
            }
        }
    }

    /**
     * <p>
     * addLog.
     * </p>
     *
     * @param entry a {@link java.util.Map.Entry} object
     */
    public static void addLog(Entry<Level, String> entry) {
        if (globalHistory.size() >= historySize) {
            globalHistory.remove(0);
        }
        globalHistory.add(entry);
    }

    /**
     * <p>
     * clearLogs.
     * </p>
     */
    public static void clearLogs() {
        globalHistory.clear();
    }

    /**
     * <p>
     * setLogLevel.
     * </p>
     *
     * @param level a {@link java.lang.String} object
     */
    public static void setLogLevel(String level) {
        if (level == null) {
            System.err.println("null level provided in setLogLevel");
            return;
        }
        switch (level) {
            case "debug":
                Configurator.setRootLevel(Level.DEBUG);
                globalLevel = Level.DEBUG;
                break;
            case "info":
                Configurator.setRootLevel(Level.INFO);
                globalLevel = Level.INFO;
                break;
            case "off":
                Configurator.setRootLevel(Level.OFF);
                globalLevel = Level.OFF;
                break;
            default:
                System.err.println("Unsuported log level parameter provided: " + level);
        }
    }

    /**
     * <p>
     * isDebug.
     * </p>
     *
     * @return a boolean
     */
    public static boolean is(Level level) {
        if (globalLevel == null) {
            System.err.println("Log level not set");
            return false;
        }
        if (globalLevel.isLessSpecificThan(level)) {
            return true;
        }
        return false;
    }
}
