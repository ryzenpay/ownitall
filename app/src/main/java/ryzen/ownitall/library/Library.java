package ryzen.ownitall.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.Settings;
import ryzen.ownitall.Storage;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;

public class Library {
    private static final Logger logger = LogManager.getLogger(Library.class);
    private static final Settings settings = Settings.load();
    private static final Storage sync = Storage.load();
    public static final LinkedHashMap<String, Class<? extends Library>> libraries;
    private static Library instance;
    private ObjectMapper objectMapper;
    private long lastQueryTime = 0;
    protected long queryDiff;
    /**
     * arrays to save api queries if they already exist
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

    /**
     * instance call method
     * sets library type with the integer set in settings
     * automates caching
     * 
     * @return - new or existing Library
     */
    public static Library load() {
        if (instance == null) {
            if (!settings.getLibraryType().isEmpty()) {
                Class<? extends Library> libraryClass = libraries.get(settings.getLibraryType());
                if (libraryClass != null) {
                    try {
                        instance = libraryClass.getDeclaredConstructor().newInstance();
                    } catch (InstantiationException e) {
                        logger.debug("Interrupted while setting up library type '" + settings.getLibraryType() + "'");
                    } catch (IllegalAccessException | NoSuchMethodException
                            | InvocationTargetException e) {
                        logger.error("Exception instantiating library '" + settings.getLibraryType() + "': " + e);
                    }
                } else {
                    logger.warn("Unsupported library method '" + settings.getLibraryType() + "' provided");
                }
            }
        }
        if (instance != null) {
            try {
                if (instance.credentialsIsEmpty()) {
                    instance.setCredentials();
                }
                instance.cache();
            } catch (InterruptedException e) {
                logger.debug("Interrupted while setting '" + settings.getLibraryType() + "' credentials");
                instance = null;
            }
        }
        return instance;
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
        this.objectMapper = new ObjectMapper();
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
        this.artists = sync.cacheArtists(this.artists);
        this.albums = sync.cacheAlbums(this.albums);
        this.songs = sync.cacheSongs(this.songs);
        this.ids = sync.cacheIds(this.ids);
    }

    /**
     * clear in memory cache
     */
    public void clear() {
        this.artists.clear();
        this.albums.clear();
        this.songs.clear();
        this.ids.clear();
        Storage.load().clearCache();
        instance = null;
    }

    public void setCredentials() throws InterruptedException {
        logger.warn("Setting credentials supported for library type: " + settings.getLibraryType());
    }

    public boolean credentialsIsEmpty() {
        logger.warn("Checking credentials supported for library type: " + settings.getLibraryType());
        return false;
    }

    public Album getAlbum(Album album) throws InterruptedException {
        logger.warn("get album supported for library type: " + settings.getLibraryType());
        return null;
    }

    public Song getSong(Song song) throws InterruptedException {
        logger.warn("get song supported for library type: " + settings.getLibraryType());
        return null;
    }

    public Artist getArtist(Artist artist) throws InterruptedException {
        logger.warn("get artist for library type: " + settings.getLibraryType());
        return null;
    }

    public ArrayList<Album> getArtistAlbums(Artist artist) throws InterruptedException {
        logger.warn("get artist albums for library type: " + settings.getLibraryType());
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
     * @throws InterruptedException - if user interrupts timeout
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
            logger.error("Exception querying API: " + e);
            return null;
        }
    }

    protected void queryErrorHandle(int code, String message) {
        logger.error("Received error code (" + code + ") while querying: " + message);
    }
}
