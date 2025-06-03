package ryzen.ownitall.util.exceptions;

/**
 * <p>
 * MissingSettingException class.
 * </p>
 *
 * @author ryzen
 */
public class MissingSettingException extends Exception {
    /**
     * <p>
     * Constructor for MissingSettingException.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     */
    public MissingSettingException(String message) {
        super(message);
    }

    /**
     * <p>
     * Constructor for MissingSettingException.
     * </p>
     *
     * @param group a {@link java.lang.Class} object
     */
    public MissingSettingException(Class<?> group) {
        super("Missing Setting from group '" + group.getSimpleName() + "'");
    }

    /**
     * <p>Constructor for MissingSettingException.</p>
     *
     * @param e a {@link java.lang.Throwable} object
     */
    public MissingSettingException(Throwable e) {
        super(e);
    }

    /**
     * <p>Constructor for MissingSettingException.</p>
     *
     * @param message a {@link java.lang.String} object
     * @param e a {@link java.lang.Throwable} object
     */
    public MissingSettingException(String message, Throwable e) {
        super(message, e);
    }
}
