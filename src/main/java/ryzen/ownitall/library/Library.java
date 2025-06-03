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
abstract public class Library implements LibraryInterface {
    private static final Logger logger = new Logger(Library.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    /** Constant <code>libraries</code> */
    private static final LinkedHashSet<Class<? extends Library>> libraries;
    private static long lastQueryTime = 0;
    protected long queryDiff;
    /**
     * arrays to cache api queries
     */
    protected LinkedHashMap<String, Artist> artists;
    protected LinkedHashMap<String, Album> albums;
    protected LinkedHashMap<String, Song> songs;
    protected LinkedHashMap<String, String> ids;

    // TODO: make it detect these
    static {
        libraries = new LinkedHashSet<>();
        libraries.add(LastFM.class);
        libraries.add(MusicBrainz.class);
    }

    /**
     * default Library constructor
     * initializes all values and loads from cache
     */
    protected Library() {
        this.artists = new LinkedHashMap<>();
        this.albums = new LinkedHashMap<>();
        this.songs = new LinkedHashMap<>();
        this.ids = new LinkedHashMap<>();
        this.cache();
    }

    public static LinkedHashSet<Class<? extends Library>> getLibraries() {
        return libraries;
    }

    public static Class<? extends Library> getLibrary(String name) {
        if (name == null) {
            logger.debug("null name provided in getLibrary");
            return null;
        }
        for (Class<? extends Library> library : libraries) {
            if (library.getSimpleName().equals(name)) {
                return library;
            }
        }
        return null;
    }

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
            Class<? extends Library> libraryClass = getLibrary(Settings.libraryType);
            return initLibrary(libraryClass);
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
     * @return a boolean
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
    public void cache() {
        Storage storage = new Storage();
        this.artists = storage.cacheArtists(this.artists);
        this.albums = storage.cacheAlbums(this.albums);
        this.songs = storage.cacheSongs(this.songs);
        this.ids = storage.cacheIds(this.ids);
    }

    /**
     * clear in memory cache
     */
    public void clear() {
        this.artists.clear();
        this.albums.clear();
        this.songs.clear();
        this.ids.clear();
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
     * getCacheSize.
     * </p>
     *
     * @return a int
     */
    public int getCacheSize() {
        int size = 0;
        size += this.getArtistCacheSize();
        size += this.getAlbumCacheSize();
        size += this.getSongCacheSize();
        size += this.getIdCacheSize();
        return size;
    }

    /**
     * <p>
     * getArtistCacheSize.
     * </p>
     *
     * @return a int
     */
    public int getArtistCacheSize() {
        return this.artists.size();
    }

    /**
     * <p>
     * getAlbumCacheSize.
     * </p>
     *
     * @return a int
     */
    public int getAlbumCacheSize() {
        return this.albums.size();
    }

    /**
     * <p>
     * getSongCacheSize.
     * </p>
     *
     * @return a int
     */
    public int getSongCacheSize() {
        return this.songs.size();
    }

    /**
     * <p>
     * getIdCacheSize.
     * </p>
     *
     * @return a int
     */
    public int getIdCacheSize() {
        return this.ids.size();
    }

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
