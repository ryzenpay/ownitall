package ryzen.ownitall.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class Logs {

    public static Level globalLevel;

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
