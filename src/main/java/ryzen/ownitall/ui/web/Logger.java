package ryzen.ownitall.ui.web;

import org.apache.logging.log4j.Level;
import org.springframework.ui.Model;

import ryzen.ownitall.util.LogConfig;

public class Logger extends ryzen.ownitall.util.Logger {
    public Logger(Class<?> clazz) {
        super(clazz);
    }

    // TODO: redesign as a FlashAttribute
    // TODO: logs site?
    public void debug(Model model, String message) {
        if (LogConfig.is(Level.DEBUG)) {
            model.addAttribute("debug", message);
        }
        super.log(Level.DEBUG, message, null);
    }

    public void info(Model model, String message) {
        if (LogConfig.is(Level.INFO)) {
            model.addAttribute("info", message);
        }
        super.log(Level.INFO, message, null);
    }

    public void warn(Model model, String message) {
        if (LogConfig.is(Level.WARN)) {
            model.addAttribute("warn", message);
        }
        super.log(Level.WARN, message, null);
    }

    public void error(Model model, String message, Throwable error) {
        if (LogConfig.is(Level.ERROR)) {
            model.addAttribute("error", message);
        }
        super.log(Level.ERROR, message, error);
    }
}
