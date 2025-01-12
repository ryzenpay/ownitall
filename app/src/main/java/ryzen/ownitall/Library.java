package ryzen.ownitall;

/**
 * this class is to host a wrapper to searching an API of known music content
 * and constructing objects accordingly
 * current API client: lastFM
 */

// https://github.com/aklevans/lastfm-java
public class Library {
    private static Library instance;

    public static Library getInstance() {
        if (instance == null) {
            instance = new Library();
        }
        return instance;
    }
}
