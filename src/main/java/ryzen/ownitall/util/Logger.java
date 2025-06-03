package ryzen.ownitall.util;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

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
    /** Constant <code>globalLevel</code> */
    public static Level globalLevel;
    private static Stack<Map.Entry<Level, String>> globalHistory = new Stack<>();
    private Stack<Map.Entry<Level, String>> history = new Stack<>();
    private static final int historySize = 5;
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
     * <p>setLogLevel.</p>
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
     * <p>is.</p>
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
     * @param error   a {@link java.lang.Throwable} object
     */
    public void error(String message, Throwable error) {
        this.log(Level.ERROR, message, error);
    }

    /**
     * <p>getGlobalLogs.</p>
     *
     * @return a {@link java.util.LinkedHashSet} object
     */
    public static LinkedHashSet<Entry<Level, String>> getGlobalLogs() {
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
     * <p>printLogs.</p>
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
     * getLogs.
     * </p>
     *
     * @return a {@link java.util.LinkedHashSet} object
     */
    public LinkedHashSet<Entry<Level, String>> getLogs() {
        LinkedHashSet<Entry<Level, String>> logs = new LinkedHashSet<>();
        for (Entry<Level, String> entry : history) {
            // log level or higher
            if (entry.getKey().intLevel() <= globalLevel.intLevel()) {
                logs.add(entry);
            }
        }
        return logs;
    }

    private void addLog(Entry<Level, String> entry) {
        if (history.size() >= historySize) {
            history.remove(0);
        }
        history.add(entry);
        globalHistory.add(entry);
    }

    /**
     * <p>clearLogs.</p>
     */
    public static void clearLogs() {
        globalHistory.clear();
    }
}
