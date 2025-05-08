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

    public static Entry<Level, String> getLatestLog() {
        for (Entry<Level, String> entry : globalHistory) {
            if (entry.getKey().intLevel() == globalLevel.intLevel()) {
                return entry;
            }
        }
        return null;
    }

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

    public static void printLogs() {
        for (Entry<Level, String> entry : globalHistory) {
            // log level or higher
            if (entry.getKey().intLevel() <= globalLevel.intLevel()) {
                System.out.println("[" + entry.getKey().toString().toUpperCase() + "] " + entry.getValue());
            }
        }
    }

    public static void addLog(Entry<Level, String> entry) {
        if (globalHistory.size() >= historySize) {
            globalHistory.remove(0);
        }
        globalHistory.add(entry);
    }

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
    public static boolean isDebug() {
        if (globalLevel == null) {
            System.err.println("Log level not set");
            return false;
        }
        if (globalLevel.intLevel() == Level.DEBUG.intLevel()) {
            return true;
        }
        return false;
    }
}
