package ryzen.ownitall.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * <p>Logs class.</p>
 *
 * @author ryzen
 */
public class Logs {
    /** Constant <code>globalLevel</code> */
    public static Level globalLevel;

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
     * <p>isDebug.</p>
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

    /**
     * <p>logTest.</p>
     */
    public static void logTest() {
        Logger logger = LogManager.getLogger(Logs.class);
        logger.debug("Debug test");
        logger.warn("Warn test");
        logger.info("Info test");
        logger.error("Error test");
        logger.debug("Debug Exception test", new InterruptedException("test"));
    }
}
