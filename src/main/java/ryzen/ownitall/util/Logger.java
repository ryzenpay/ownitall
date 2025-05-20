package ryzen.ownitall.util;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

/**
 * <p>
 * Logs class.
 * </p>
 *
 * @author ryzen
 */
public class Logger {
    /** Constant <code>globalLevel</code> */
    private Stack<Map.Entry<Level, String>> history = new Stack<>();
    private static final int historySize = 10;
    private org.apache.logging.log4j.Logger logger;

    /**
     * <p>Constructor for Logger.</p>
     *
     * @param clazz a {@link java.lang.Class} object
     */
    public Logger(Class<?> clazz) {
        if (clazz == null) {
            logger = LogManager.getLogger();
        } else {
            logger = LogManager.getLogger(clazz);
        }
    }

    /**
     * <p>log.</p>
     *
     * @param level a {@link org.apache.logging.log4j.Level} object
     * @param message a {@link java.lang.String} object
     * @param error a {@link java.lang.Throwable} object
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
     * <p>debug.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public void debug(String message) {
        this.log(Level.DEBUG, message, null);
    }

    /**
     * <p>info.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public void info(String message) {
        this.log(Level.INFO, message, null);
    }

    /**
     * <p>warn.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public void warn(String message) {
        this.log(Level.WARN, message, null);
    }

    /**
     * <p>error.</p>
     *
     * @param message a {@link java.lang.String} object
     * @param error a {@link java.lang.Throwable} object
     */
    public void error(String message, Throwable error) {
        this.log(Level.ERROR, message, error);
    }

    /**
     * <p>getLatestLog.</p>
     *
     * @return a {@link java.util.Map.Entry} object
     */
    public Entry<Level, String> getLatestLog() {
        for (Entry<Level, String> entry : history) {
            if (entry.getKey().intLevel() == LogConfig.globalLevel.intLevel()) {
                return entry;
            }
        }
        return null;
    }

    /**
     * <p>getLogs.</p>
     *
     * @return a {@link java.util.LinkedHashSet} object
     */
    public LinkedHashSet<Entry<Level, String>> getLogs() {
        LinkedHashSet<Entry<Level, String>> logs = new LinkedHashSet<>();
        for (Entry<Level, String> entry : history) {
            // log level or higher
            if (entry.getKey().intLevel() <= LogConfig.globalLevel.intLevel()) {
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
        LogConfig.addLog(entry);
    }
}
