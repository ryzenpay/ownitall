package ryzen.ownitall.ui.web;

import org.apache.logging.log4j.Level;
import org.springframework.ui.Model;

public class Logger extends ryzen.ownitall.util.Logger {
    public Logger(Class<?> clazz) {
        super(clazz);
    }

    public void debug(Model model, String message) {
        if (is(Level.DEBUG)) {
            model.addAttribute("debug", message);
        }
        super.log(Level.DEBUG, message, null);
    }

    public void info(Model model, String message) {
        if (is(Level.INFO)) {
            model.addAttribute("info", message);
        }
        super.log(Level.INFO, message, null);
    }

    public void warn(Model model, String message) {
        if (is(Level.WARN)) {
            model.addAttribute("warn", message);
        }
        super.log(Level.WARN, message, null);
    }

    public void error(Model model, String message, Throwable error) {
        if (is(Level.ERROR)) {
            model.addAttribute("error", message);
        }
        super.log(Level.ERROR, message, error);
    }
}
