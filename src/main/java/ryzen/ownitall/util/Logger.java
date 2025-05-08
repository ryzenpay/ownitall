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

    public Logger(Class<?> clazz) {
        if (clazz == null) {
            logger = LogManager.getLogger();
        } else {
            logger = LogManager.getLogger(clazz);
        }
    }

    public void log(Level level, String message, Throwable error) {
        if (error == null) {
            this.logger.log(level, message);
        } else {
            this.logger.log(level, message, error);
        }
        this.addLog(Map.entry(level, message));
    }

    public void debug(String message) {
        this.log(Level.DEBUG, message, null);
    }

    public void info(String message) {
        this.log(Level.INFO, message, null);
    }

    public void warn(String message) {
        this.log(Level.WARN, message, null);
    }

    public void error(String message, Throwable error) {
        this.log(Level.ERROR, message, error);
    }

    public Entry<Level, String> getLatestLog() {
        for (Entry<Level, String> entry : history) {
            if (entry.getKey().intLevel() == LogConfig.globalLevel.intLevel()) {
                return entry;
            }
        }
        return null;
    }

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
