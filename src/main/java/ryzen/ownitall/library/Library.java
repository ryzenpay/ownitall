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
public class Library {
    private static final Logger logger = new Logger(Library.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    /** Constant <code>libraries</code> */
    public static final LinkedHashMap<String, Class<? extends Library>> libraries;
    private static Library instance;
    private long lastQueryTime = 0;
    protected long queryDiff;
    /**
     * arrays to cache api queries
     */
    protected LinkedHashMap<String, Artist> artists;
    protected LinkedHashMap<String, Album> albums;
    protected LinkedHashMap<String, Song> songs;
    protected LinkedHashMap<String, String> ids;

    static {
        libraries = new LinkedHashMap<>();
        libraries.put("LastFM", LastFM.class);
        libraries.put("MusicBrainz", MusicBrainz.class);
    }

    public static Library initLibrary(Class<? extends Library> libraryClass)
            throws MissingSettingException, AuthenticationException,
            NoSuchMethodException {
        if (libraryClass == null) {
            logger.debug("null library class provided in initLibrary");
            return null;
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
        Class<? extends Library> libraryClass = Library.libraries.get(Settings.libraryType);
        if (libraryClass == null) {
            logger.warn("Invalid library type set in settings");
            return null;
        }
        if (instance == null || !instance.getClass().isInstance(libraryClass)) {
            try {
                instance = initLibrary(libraryClass);
            } catch (MissingSettingException e) {
                logger.warn("Library '" + libraryClass.getSimpleName() + "' is missing credentials");
            } catch (AuthenticationException e) {
                logger.warn("Library '" + libraryClass.getSimpleName() + "' had an exception authenticating");
            } catch (NoSuchMethodException e) {
                logger.error("Library '" + libraryClass.getSimpleName() + "' does not exist", e);
            }
        }
        if (instance != null) {
            instance.cache();
        }
        return instance;
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
     * check if library has an instance
     * to prevent setting it up and logging in when clearing
     *
     * @return - true if instance set
     */
    public static boolean checkInstance() {
        if (instance != null) {
            return true;
        }
        return false;
    }

    /**
     * default Library constructor
     * initializes all values and loads from cache
     */
    public Library() {
        this.artists = new LinkedHashMap<>();
        this.albums = new LinkedHashMap<>();
        this.songs = new LinkedHashMap<>();
        this.ids = new LinkedHashMap<>();
        this.cache();
    }

    /**
     * dump all data into cache
     */
    public void cache() {
        this.artists = Storage.cacheArtists(this.artists);
        this.albums = Storage.cacheAlbums(this.albums);
        this.songs = Storage.cacheSongs(this.songs);
        this.ids = Storage.cacheIds(this.ids);
    }

    /**
     * clear in memory cache
     */
    public static void clear() {
        if (instance != null) {
            instance.artists.clear();
            instance.albums.clear();
            instance.songs.clear();
            instance.ids.clear();
            instance = null;
        }
        Storage.clearCacheFiles();
    }

    /**
     * <p>
     * getAlbum.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @return a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    public Album getAlbum(Album album) throws InterruptedException {
        logger.warn("get album unsupported for library type: " + Settings.libraryType);
        return null;
    }

    /**
     * <p>
     * getSong.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     * @throws java.lang.InterruptedException if any.
     */
    public Song getSong(Song song) throws InterruptedException {
        logger.warn("get song unsupported for library type: " + Settings.libraryType);
        return null;
    }

    /**
     * <p>
     * getArtist.
     * </p>
     *
     * @param artist a {@link ryzen.ownitall.classes.Artist} object
     * @return a {@link ryzen.ownitall.classes.Artist} object
     * @throws java.lang.InterruptedException if any.
     */
    public Artist getArtist(Artist artist) throws InterruptedException {
        logger.warn("get artist unsupported for library type: " + Settings.libraryType);
        return null;
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
        logger.error("Received error code (" + code + ") while querying: " + message, new Exception());
    }

    /**
     * <p>
     * getCacheSize.
     * </p>
     *
     * @return a int
     */
    public static int getCacheSize() {
        int size = 0;
        if (instance != null) {
            size += instance.getArtistCacheSize();
            size += instance.getAlbumCacheSize();
            size += instance.getSongCacheSize();
            size += instance.getIdCacheSize();
        }
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
}
