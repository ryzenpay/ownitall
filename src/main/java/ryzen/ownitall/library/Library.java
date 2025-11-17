package ryzen.ownitall.library;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.databind.JsonNode;

import ryzen.ownitall.Settings;
import ryzen.ownitall.Storage;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.ClassLoader;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;
import ryzen.ownitall.util.exceptions.QueryException;

/**
 * <p>
 * Library class.
 * </p>
 *
 * @author ryzen
 */
abstract public class Library implements LibraryInterface {
    private static final Logger logger = new Logger(Library.class);

    protected long queryDiff;
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
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     * @throws java.lang.NoSuchMethodException                        if any.
     */
    public static Library initLibrary(Class<? extends Library> libraryClass)
            throws MissingSettingException, AuthenticationException,
            NoSuchMethodException {
        if (libraryClass == null) {
            logger.debug("null library class provided in initLibrary");
            throw new NoSuchMethodException();
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
            throw new NoSuchMethodException(libraryClass.getName());
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
        } catch (NoSuchMethodException e) {
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
                logger.warn("Unable to find method setting '" + credential + "'");
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
     * query the specified url, catch errors or return response
     *
     * @param url - url to query
     * @return - JsonNode response or null if error
     * @throws java.lang.InterruptedException - if user interrupts timeout
     */
    protected JsonNode query(URI url) throws InterruptedException {
        if (url == null) {
            logger.debug("null url provided to query");
            return null;
        }
        WebTools.queryPacer(queryDiff);
        try {
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "OwnItAll/1.0 (https://github.com/ryzenpay/ownitall)");
            return WebTools.query(connection);
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException | QueryException e) {
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
}
