package ryzen.ownitall.util;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * <p>
 * Logs class.
 * </p>
 *
 * @author ryzen
 */
public class Logger {
    /** defaults to the log4j2 Root level so logging works before -l overrides it */
    public static Level globalLevel = Level.INFO;
    private static Stack<Map.Entry<Level, String>> globalHistory = new Stack<>();
    private Stack<Map.Entry<Level, String>> history = new Stack<>();
    private static final int historySize = 5;
    // the web UI is long lived and every log line lands in globalHistory, so it needs
    // a cap of its own or the process retains every message forever
    private static final int globalHistorySize = 250;
    private org.apache.logging.log4j.Logger logger;

    /**
     * <p>
     * Constructor for Logger.
     * </p>
     *
     * @param clazz a {@link java.lang.Class} object
     */
    public Logger(Class<?> clazz) {
        logger = LogManager.getLogger(clazz);
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
     * is.
     * </p>
     *
     * @param level a {@link org.apache.logging.log4j.Level} object
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

    /**
     * <p>
     * log.
     * </p>
     *
     * @param level   a {@link org.apache.logging.log4j.Level} object
     * @param message a {@link java.lang.String} object
     * @param error   a {@link java.lang.Throwable} object
     */
    public void log(Level level, String message, Throwable error) {
        if (error == null) {
            this.logger.log(level, message);
        } else {
            this.logger.log(level, message, error);
        }
        this.addLog(Map.entry(level, message));
    }

    /**
     * <p>
     * debug.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     */
    public void debug(String message) {
        this.log(Level.DEBUG, message, null);
    }

    /**
     * <p>
     * info.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     */
    public void info(String message) {
        this.log(Level.INFO, message, null);
    }

    /**
     * info log in console but no file
     * also excluded from log history
     *
     * @param message a {@link java.lang.String} object
     */
    public void temp(String message) {
        this.logger.log(Level.forName("TEMP", 350), message);
    }

    /**
     * <p>
     * warn.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     */
    public void warn(String message) {
        this.log(Level.WARN, message, null);
    }

    /**
     * <p>
     * error.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     */
    public void error(String message) {
        this.log(Level.ERROR, message, null);
    }

    /**
     * <p>
     * error.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     * @param error   a {@link java.lang.Throwable} object
     */
    public void error(String message, Throwable error) {
        this.log(Level.ERROR, message, error);
    }

    /**
     * <p>
     * getGlobalLogs.
     * </p>
     *
     * @return a {@link java.util.LinkedHashSet} object
     */
    public static LinkedHashSet<Entry<Level, String>> getGlobalLogs() {
        return filterHistory(globalHistory, globalLevel);
    }

    /**
     * <p>
     * printLogs.
     * </p>
     *
     * @param level a {@link org.apache.logging.log4j.Level} object
     */
    public static void printLogs(Level level) {
        for (Entry<Level, String> entry : filterHistory(globalHistory, level)) {
            System.out.println("[" + entry.getKey().toString().toUpperCase() + "] " + entry.getValue());
        }
    }

    /**
     * copy out the entries logged at level or higher
     *
     * @param source a {@link java.util.Stack} object
     * @param level  a {@link org.apache.logging.log4j.Level} object
     * @return a {@link java.util.LinkedHashSet} object
     */
    private static LinkedHashSet<Entry<Level, String>> filterHistory(Stack<Entry<Level, String>> source,
            Level level) {
        LinkedHashSet<Entry<Level, String>> logs = new LinkedHashSet<>();
        if (level == null) {
            System.err.println("Log level not set");
            return logs;
        }
        // the web UI polls the history while worker threads are still logging into it
        synchronized (source) {
            for (Entry<Level, String> entry : source) {
                // log level or higher
                if (entry.getKey().intLevel() <= level.intLevel()) {
                    logs.add(entry);
                }
            }
        }
        return logs;
    }

    /**
     * <p>
     * getLogs.
     * </p>
     *
     * @return a {@link java.util.LinkedHashSet} object
     */
    public LinkedHashSet<Entry<Level, String>> getLogs() {
        return filterHistory(history, globalLevel);
    }

    private void addLog(Entry<Level, String> entry) {
        synchronized (history) {
            if (history.size() >= historySize) {
                history.remove(0);
            }
            history.add(entry);
        }
        synchronized (globalHistory) {
            if (globalHistory.size() >= globalHistorySize) {
                globalHistory.remove(0);
            }
            globalHistory.add(entry);
        }
    }

    /**
     * <p>
     * clearLogs.
     * </p>
     */
    public static void clearLogs() {
        synchronized (globalHistory) {
            globalHistory.clear();
        }
    }
}
