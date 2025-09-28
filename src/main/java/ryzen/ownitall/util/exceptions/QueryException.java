package ryzen.ownitall.util.exceptions;

public class QueryException extends Exception {

    public QueryException(String message) {
        super(message);
    }

    public QueryException(Throwable e) {
        super(e);
    }

    public QueryException(String message, Throwable e) {
        super(message, e);
    }

    public QueryException(String code, String message) {
        super("(" + code + ") " + message);
    }
}
