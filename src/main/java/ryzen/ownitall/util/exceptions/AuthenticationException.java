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

    public AuthenticationException(Throwable e) {
        super(e);
    }

    public AuthenticationException(String message, Throwable e) {
        super(message, e);
    }
}
