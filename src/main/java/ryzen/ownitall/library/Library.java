package ryzen.ownitall.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.Settings;
import ryzen.ownitall.Storage;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.ClassLoader;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>
 * Library class.
 * </p>
 *
 * @author ryzen
 */
// TODO: youtube library?
// then use in yt_dl
abstract public class Library implements LibraryInterface {
    private static final Logger logger = new Logger(Library.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    /** Constant <code>libraries</code> */
    private static long lastQueryTime = 0;
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
        Storage storage = new Storage();
        artists = storage.cacheArtists(artists);
        albums = storage.cacheAlbums(albums);
        songs = storage.cacheSongs(songs);
        ids = storage.cacheIds(ids);
    }

    /**
     * clear in memory cache
     */
    public static void clear() {
        artists.clear();
        albums.clear();
        songs.clear();
        ids.clear();
        new Storage().clearCacheFiles();
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
     * ensure querys are only executed at an interval to prevent api limits
     * 
     * @throws InterruptedException - when the user interrupts
     */
    private void timeoutManager() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastQueryTime;
        if (elapsedTime < queryDiff) {
            TimeUnit.MILLISECONDS.sleep(queryDiff - elapsedTime);
        }
        lastQueryTime = System.currentTimeMillis();
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
        timeoutManager();
        try {
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "OwnItAll/1.0 (https://github.com/ryzenpay/ownitall)");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            JsonNode rootNode = objectMapper.readTree(response.toString());

            if (rootNode.has("error") || rootNode.has("failed")) {
                logger.debug("Query error: " + rootNode.toString());
                int errorCode = rootNode.path("error").asInt();
                // to prevent it triggering on songs named "error" or "failed"
                if (errorCode != 0) {
                    String errorMessage = rootNode.path("message").asText();
                    logger.debug("Received error code on query: " + url.toString());
                    this.queryErrorHandle(errorCode, errorMessage);
                    return null;
                }
            }
            return rootNode;
        } catch (IOException e) {
            logger.error("Exception querying API", e);
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

    /**
     * <p>
     * removeBrackets.
     * </p>
     *
     * @param string a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    protected static String removeBrackets(String string) {
        if (string == null) {
            logger.debug("null string provided in removeBrackets");
            return null;
        }
        if (!string.contains("(")) {
            return string;
        }
        // ()
        string = string.replaceAll("\\(.*?\\)", "");
        // []
        string = string.replaceAll("\\[.*?\\]", "");
        // remove spaces in middle of string
        return string.trim().replaceAll("  ", " ");
    }
}
