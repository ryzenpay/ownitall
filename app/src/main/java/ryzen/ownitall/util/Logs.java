package ryzen.ownitall.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class Logs {

    public static void setLogLevel(String level) {
        if (level == null) {
            System.err.println("null level provided in setLogLevel");
            return;
        }
        switch (level) {
            case "debug":
                Configurator.setRootLevel(Level.DEBUG);
                break;
            case "info":
                Configurator.setRootLevel(Level.INFO);
                break;
            case "off":
                Configurator.setRootLevel(Level.OFF);
                break;
            default:
                System.err.println("Unsuported log level parameter provided: " + level);
        }
    }
}
