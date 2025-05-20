package ryzen.ownitall.util.exceptions;

/**
 * <p>MissingSettingException class.</p>
 *
 * @author ryzen
 */
public class MissingSettingException extends Exception {
    /**
     * <p>Constructor for MissingSettingException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public MissingSettingException(String message) {
        super(message);
    }

    // TODO: more debugging like what setting?
    /**
     * <p>Constructor for MissingSettingException.</p>
     *
     * @param group a {@link java.lang.Class} object
     */
    public MissingSettingException(Class<?> group) {
        super("Missing Setting from group '" + group.getSimpleName() + "'");
    }
}
