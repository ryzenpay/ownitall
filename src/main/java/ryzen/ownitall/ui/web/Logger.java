package ryzen.ownitall.ui.web;

import org.apache.logging.log4j.Level;
import org.springframework.ui.Model;

/**
 * <p>
 * Logger class.
 * </p>
 *
 * @author ryzen
 */
public class Logger extends ryzen.ownitall.util.Logger {
    /**
     * <p>
     * Constructor for Logger.
     * </p>
     *
     * @param clazz a {@link java.lang.Class} object
     */
    public Logger(Class<?> clazz) {
        super(clazz);
    }

    /**
     * <p>
     * debug.
     * </p>
     *
     * @param model   a {@link org.springframework.ui.Model} object
     * @param message a {@link java.lang.String} object
     */
    public void debug(Model model, String message) {
        if (is(Level.DEBUG)) {
            model.addAttribute("debug", message);
        }
        super.log(Level.DEBUG, message, null);
    }

    /**
     * <p>
     * info.
     * </p>
     *
     * @param model   a {@link org.springframework.ui.Model} object
     * @param message a {@link java.lang.String} object
     */
    public void info(Model model, String message) {
        if (is(Level.INFO)) {
            model.addAttribute("info", message);
        }
        super.log(Level.INFO, message, null);
    }

    /**
     * <p>
     * warn.
     * </p>
     *
     * @param model   a {@link org.springframework.ui.Model} object
     * @param message a {@link java.lang.String} object
     */
    public void warn(Model model, String message) {
        if (is(Level.WARN)) {
            model.addAttribute("warn", message);
        }
        super.log(Level.WARN, message, null);
    }

    /**
     * <p>
     * error.
     * </p>
     *
     * @param model   a {@link org.springframework.ui.Model} object
     * @param message a {@link java.lang.String} object
     * @param error   a {@link java.lang.Throwable} object
     */
    public void error(Model model, String message, Throwable error) {
        if (is(Level.ERROR)) {
            model.addAttribute("error", message);
        }
        super.log(Level.ERROR, message, error);
    }
}
