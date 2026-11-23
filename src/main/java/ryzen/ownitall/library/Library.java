package ryzen.ownitall.library;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import jakarta.ws.rs.client.WebTarget;

import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.Storage;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.ClassLoader;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.WebTools.AuthenticationException;
import ryzen.ownitall.util.WebTools.QueryException;

/**
 * <p>
 * Library class.
 * </p>
 *
 * @author ryzen
 */
abstract public class Library implements Interface {
    private static final Logger logger = new Logger(Library.class);
    private static final Map<String, String> headers = Map.of("User-Agent",
            "OwnItAll/1.0 (https://github.com/ryzenpay/ownitall)");

    /**
     * arrays to cache api queries
     */
    protected static LinkedHashMap<String, Artist> artists;
    protected static LinkedHashMap<String, Album> albums;
    protected static LinkedHashMap<String, Song> songs;
    protected static LinkedHashMap<String, String> ids;

    /**
     * default Library constructor
     * initializes all values and loads from cache
     */
    protected Library() {
        if (artists == null) {
            artists = new LinkedHashMap<>();
        }
        if (albums == null) {
            albums = new LinkedHashMap<>();
        }
        if (songs == null) {
            songs = new LinkedHashMap<>();
        }
        if (ids == null) {
            ids = new LinkedHashMap<>();
        }
        cache();
    }

    /**
     * <p>
     * Getter for the field <code>libraries</code>.
     * </p>
     *
     * @return a {@link java.util.LinkedHashSet} object
     */
    public static LinkedHashSet<Class<? extends Library>> getLibraries() {
        return ClassLoader.load().getSubClasses(Library.class);
    }

    public static Class<? extends Library> getLibrary(String name) {
        return ClassLoader.load().getSubClass(Library.class, name);
    }

    /**
     * <p>
     * initLibrary.
     * </p>
     *
     * @param libraryClass a {@link java.lang.Class} object
     * @return a {@link ryzen.ownitall.library.Library} object
     * @throws ryzen.ownitall.Settings.MissingSettingException        if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException   if any.
     * @throws ryzen.ownitall.library.Library.InvalidLibraryException if any.
     */
    public static Library initLibrary(Class<? extends Library> libraryClass)
            throws MissingSettingException, AuthenticationException,
            InvalidLibraryException {
        if (libraryClass == null) {
            logger.debug("null library class provided in initLibrary");
            throw new InvalidLibraryException();
        }
        try {
            logger.debug("Initializing '" + libraryClass + "' library");
            return libraryClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("Exception while setting up library '" + libraryClass.getSimpleName() + "'", e);
            Throwable cause = e.getCause();
            if (cause instanceof MissingSettingException) {
                throw new MissingSettingException(e);
            }
            if (cause instanceof AuthenticationException) {
                throw new AuthenticationException(e);
            }
            throw new InvalidLibraryException(libraryClass.getName());
        }
    }

    /**
     * instance call method
     * sets library type with the integer set in settings
     * automates caching
     *
     * @return - new or existing Library
     */
    public static Library load() {
        if (Settings.libraryType.isEmpty()) {
            return null;
        }
        try {
            return initLibrary(ClassLoader.load().getSubClass(Library.class, Settings.libraryType));
        } catch (MissingSettingException e) {
            logger.warn("Library '" + Settings.libraryType + "' is missing credentials: " + e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn("Library '" + Settings.libraryType + "' had an exception authenticating: "
                    + e.getMessage());
        } catch (InvalidLibraryException e) {
            logger.error("Library '" + Settings.libraryType + "' does not exist", e);
        }
        return null;
    }

    /**
     * <p>
     * clearCredentials.
     * </p>
     *
     * @param type a {@link java.lang.Class} object
     */
    public static void clearCredentials(Class<? extends Library> type) {
        if (type == null) {
            logger.debug("null type provided in clearCredentials");
            return;
        }
        Settings settings = Settings.load();
        LinkedHashSet<String> credentials = settings.getGroup(type);
        if (credentials == null) {
            logger.debug("Unable to find credentials for '" + type.getSimpleName() + "'");
            return;
        }
        for (String credential : credentials) {
            try {
                settings.set(credential, "");
            } catch (NoSuchFieldException e) {
                logger.warn("Unable to find library setting '" + credential + "'");
            }
        }
        logger.debug("Cleared credentials for '" + type.getSimpleName() + "'");
    }

    /**
     * dump all data into cache
     */
    public static void cache() {
        artists = Storage.cacheArtists(artists);
        albums = Storage.cacheAlbums(albums);
        songs = Storage.cacheSongs(songs);
        ids = Storage.cacheIds(ids);
    }

    /**
     * clear in memory cache
     */
    public static void clear() {
        artists.clear();
        albums.clear();
        songs.clear();
        ids.clear();
        Storage.clearCacheFiles();
    }

    /**
     * <p>
     * getArtistAlbums.
     * </p>
     *
     * @param artist a {@link ryzen.ownitall.classes.Artist} object
     * @return a {@link java.util.ArrayList} object
     * @throws java.lang.InterruptedException if any.
     */
    public ArrayList<Album> getArtistAlbums(Artist artist) throws InterruptedException {
        logger.warn("get artist albums unsupported for library type: " + Settings.libraryType);
        return null;
    }

    /**
     * query a target a library built out of path segments and parameters rather
     * than a url string: nothing here joins a '/' or a '?' by hand, so an id with
     * a reserved character in it lands encoded the way the server reads it
     *
     * @param <T>          the bean the response binds to
     * @param target       - the built request, no headers or body of its own
     * @param responseType - the bean class to bind the response to
     * @return - bound response or null if error
     * @throws java.lang.InterruptedException - when the user interrupts a
     *                                        pacing or retry wait, rethrown so
     *                                        callers abort instead of mistaking
     *                                        the interrupt for a missing entry
     */
    protected <T> T query(WebTarget target, Class<T> responseType) throws InterruptedException {
        if (target == null) {
            logger.debug("null target provided to query");
            return null;
        }
        try {
            return WebTools.get(target, Library.headers, responseType);
        } catch (QueryException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw (InterruptedException) e.getCause();
            }
            logger.warn("Received error code while querying: " + e.getMessage());
            return null;
        }
    }

    /**
     * <p>
     * queryErrorHandle.
     * </p>
     *
     * @param code    a int
     * @param message a {@link java.lang.String} object
     */
    protected void queryErrorHandle(int code, String message) {
        logger.warn("Received error code (" + code + ") while querying: " + message);
    }

    /**
     * <p>
     * getArtistCacheSize.
     * </p>
     *
     * @return a int
     */
    public int getArtistCacheSize() {
        return artists.size();
    }

    /**
     * <p>
     * getAlbumCacheSize.
     * </p>
     *
     * @return a int
     */
    public int getAlbumCacheSize() {
        return albums.size();
    }

    /**
     * <p>
     * getSongCacheSize.
     * </p>
     *
     * @return a int
     */
    public int getSongCacheSize() {
        return songs.size();
    }

    /**
     * <p>
     * getIdCacheSize.
     * </p>
     *
     * @return a int
     */
    public int getIdCacheSize() {
        return ids.size();
    }

    public static class InvalidLibraryException extends Exception {

        public InvalidLibraryException() {
            super();
        }

        public InvalidLibraryException(String message) {
            super(message);
        }
    }
}

interface Interface {
    /**
     * <p>
     * getAlbum.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @return a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    public Album getAlbum(Album album) throws InterruptedException;

    /**
     * <p>
     * getSong.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     * @throws java.lang.InterruptedException if any.
     */
    public Song getSong(Song song) throws InterruptedException;

    /**
     * <p>
     * getArtist.
     * </p>
     *
     * @param artist a {@link ryzen.ownitall.classes.Artist} object
     * @return a {@link ryzen.ownitall.classes.Artist} object
     * @throws java.lang.InterruptedException if any.
     */
    public Artist getArtist(Artist artist) throws InterruptedException;
}
