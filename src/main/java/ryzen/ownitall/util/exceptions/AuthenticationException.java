package ryzen.ownitall.util.exceptions;

/**
 * <p>
 * AuthenticationException class.
 * </p>
 *
 * @author ryzen
 */
public class AuthenticationException extends Exception {
    /**
     * <p>
     * Constructor for AuthenticationException.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * <p>Constructor for AuthenticationException.</p>
     *
     * @param e a {@link java.lang.Throwable} object
     */
    public AuthenticationException(Throwable e) {
        super(e);
    }

    /**
     * <p>Constructor for AuthenticationException.</p>
     *
     * @param message a {@link java.lang.String} object
     * @param e a {@link java.lang.Throwable} object
     */
    public AuthenticationException(String message, Throwable e) {
        super(message, e);
    }
}
