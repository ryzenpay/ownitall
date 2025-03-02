package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.LastFM;
import ryzen.ownitall.library.MusicBrainz;

public class Library {
    private static final Logger logger = LogManager.getLogger(Library.class);
    private static final Settings settings = Settings.load();
    private static final Sync sync = Sync.load();
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

    /**
     * instance call method
     * 
     * @return - new or existing Library
     */
    public static Library load() {
        if (instance == null) {
            switch (settings.getLibrayType()) {
                case 0:
                    instance = null;
                    break;
                case 1:
                    instance = new LastFM();
                    break;
                case 2:
                    instance = new MusicBrainz();
                    break;
                default:
                    logger.error("Library type set in settings '" + settings.getLibrayType() + "' does not exist");
                    break;
            }
        }
        if (instance != null) {
            instance.save();
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
        this.artists = sync.cacheArtists(new LinkedHashMap<>());
        this.albums = sync.cacheAlbums(new LinkedHashMap<>());
        this.songs = sync.cacheSongs(new LinkedHashMap<>());
        this.ids = sync.cacheIds(new LinkedHashMap<>());
    }

    /**
     * dump all data into cache
     */
    public void save() {
        sync.cacheArtists(this.artists);
        sync.cacheAlbums(this.albums);
        sync.cacheSongs(this.songs);
    }

    /**
     * clear in memory cache
     */
    public void clear() {
        this.artists.clear();
        this.albums.clear();
        this.songs.clear();
    }

    public Album getAlbum(Album album) throws InterruptedException {
        return null;
    }

    public Song getSong(Song song) throws InterruptedException {
        return null;
    }

    public Artist getArtist(Artist artist) throws InterruptedException {
        return null;
    }

    private void timeoutManager() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastQueryTime;
        if (elapsedTime < queryDiff) {
            TimeUnit.MILLISECONDS.sleep(queryDiff - elapsedTime);
        }
        lastQueryTime = System.currentTimeMillis();
    }

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
                int errorCode = rootNode.path("error").asInt();
                // to prevent it triggering on songs named "error" or "failed"
                if (errorCode != 0) {
                    String errorMessage = rootNode.path("message").asText();
                    logger.error("Received error code (" + errorCode + ") while querying: " + errorMessage);
                    return null;
                }
            }
            return rootNode;
        } catch (Exception e) {
            logger.error("Error querying API: " + e);
            return null;
        }
    }
}
