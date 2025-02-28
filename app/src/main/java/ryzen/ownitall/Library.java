package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;

import com.fasterxml.jackson.databind.JsonNode;
import sun.misc.Signal;

public class Library {
    // TODO: move to library folder
    // make wrapper class
    // settings option
    private static final Logger logger = LogManager.getLogger(Library.class);
    private static final Sync sync = Sync.load();
    private static final Credentials credentials = Credentials.load();
    private static final Settings settings = Settings.load();
    private static Library instance;
    private final String baseUrl = "http://ws.audioscrobbler.com/2.0/";
    private ObjectMapper objectMapper;
    private static long lastQueryTime = 0;
    private static long queryDiff = 10; // query timeout in ms
    private AtomicBoolean interrupted = new AtomicBoolean(false);
    /**
     * arrays to save api queries if they already exist
     */
    private LinkedHashMap<String, Artist> artists;
    private LinkedHashMap<String, Album> albums;
    private LinkedHashMap<String, Song> songs;

    /**
     * instance call method
     * 
     * @return - new or existing Library
     */
    public static Library load() {
        if (instance == null) {
            instance = new Library();
            logger.debug("New instance created");
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
        if (settings.isUseLibrary() && credentials.lastFMIsEmpty()) {
            credentials.setLastFMCredentials();
        }
        this.objectMapper = new ObjectMapper();
        this.artists = sync.cacheArtists(new LinkedHashMap<>());
        this.albums = sync.cacheAlbums(new LinkedHashMap<>());
        this.songs = sync.cacheSongs(new LinkedHashMap<>());
        // TODO: fix interruption catching
        Signal.handle(new Signal("INT"), signal -> {
            logger.debug("SIGINT in input caught");
            interrupted.set(true);
        });
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

    // https://www.last.fm/api/show/album.getInfo
    public Album getAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("Empty album passed in getAlbum");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("album", album.getName());
        if (album.getMainArtist() != null) {
            params.put("artist", album.getMainArtist().getName());
        }
        params.put("autocorrect", "1");
        if (this.albums.containsKey(params.toString())) {
            return this.albums.get(params.toString());
        }
        JsonNode response = this.lastFMQuery("album.getInfo", this.queryBuilder(params));
        if (response != null) {
            JsonNode albumNode = response.path("album");
            if (!albumNode.isMissingNode()) {
                album = new Album(albumNode.path("name").asText());
                Artist artist = this.getArtist(new Artist(albumNode.path("artist").asText()));
                if (artist != null) {
                    album.addArtist(artist);
                }
                album.addId("lastfm", albumNode.path("url").asText());
                JsonNode imageNode = albumNode.path("image");
                if (imageNode.isArray() && !imageNode.isEmpty()) {
                    String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                    if (coverImage != null && !coverImage.isEmpty()) {
                        album.setCoverImage(coverImage);
                    }
                } else {
                    logger.debug(album.toString() + ": album missing images: " + albumNode.toString());
                }
                JsonNode trackNodes = albumNode.path("tracks");
                if (trackNodes.isArray() && !trackNodes.isEmpty()) {
                    for (JsonNode trackNode : trackNodes) {
                        Song song = new Song(trackNode.path("name").asText());
                        Artist songArtist = null;
                        JsonNode artistNode = trackNode.path("artist");
                        if (!artistNode.isMissingNode()) {
                            songArtist = this.getArtist(new Artist(artistNode.path("name").asText()));
                        } else {
                            logger.debug(album.toString() + " track missing artist: " + trackNode.toString());
                        }
                        if (songArtist != null) {
                            song.setArtist(artist);
                        }
                        song = this.getSong(song);
                        if (song != null) {
                            album.addSong(song);
                        }
                    }
                } else {
                    logger.debug(album.toString() + ": album missing tracks: " + albumNode.toString());
                }
                this.albums.put(params.toString(), album);
                return album;
            } else {
                logger.debug(album.toString() + ": album.getInfo missing album: " + response.toString());
            }
        }
        logger.debug("Unable to find Album '" + album.toString() + "' in library ");
        return null;
    }

    public Song getSong(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("Empty song passed in getSong");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("track", song.getName());
        if (song.getArtist() != null) {
            params.put("artist", song.getArtist().getName());
        }
        params.put("autocorrect", "1");
        if (this.songs.containsKey(params.toString())) {
            return this.songs.get(params.toString());
        }
        JsonNode response = this.lastFMQuery("track.getInfo", this.queryBuilder(params));
        if (response != null) {
            JsonNode trackNode = response.path("track");
            if (!trackNode.isMissingNode()) {
                song = new Song(trackNode.path("name").asText());
                song.addId("lastfm", trackNode.path("url").asText());
                song.setDuration(trackNode.path("duration").asLong(), ChronoUnit.MILLIS);
                JsonNode artistNode = trackNode.path("artist");
                if (!artistNode.isMissingNode()) {
                    Artist artist = this.getArtist(new Artist(artistNode.path("name").asText()));
                    if (artist != null) {
                        song.setArtist(artist);
                    }
                } else {
                    logger.debug(song.toString() + ": track missing artist: " + trackNode.toString());
                }
                JsonNode albumNode = trackNode.path("album");
                if (!albumNode.isMissingNode()) {
                    JsonNode imageNode = albumNode.path("image");
                    if (imageNode.isArray() && !imageNode.isEmpty()) {
                        String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                        if (coverImage != null && !coverImage.isEmpty()) {
                            song.setCoverImage(coverImage);
                        }
                    } else {
                        logger.debug(song.toString() + ": song missing images: " + albumNode.toString());
                    }
                } else {
                    // this is printed a lot and just for cover image
                    // logger.debug(song.toString() + ": track missing album: " +
                    // trackNode.toString());
                }
                this.songs.put(params.toString(), song);
                return song;
            } else {
                logger.debug(song.toString() + ": track.getInfo missing track: " + response.toString());
            }
        }
        logger.debug("Unable to find song '" + song.toString() + "' in library");
        return null;
    }

    public Artist getArtist(Artist artist) throws InterruptedException {
        if (artist == null) {
            logger.debug("Empty artist passed in getArtist");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("artist", artist.getName());
        if (this.artists.containsKey(params.toString())) {
            return this.artists.get(params.toString());
        }
        JsonNode response = this.lastFMQuery("artist.getInfo", this.queryBuilder(params));
        if (response != null) {
            JsonNode artistNode = response.path("artist");
            if (!artistNode.isMissingNode()) {
                artist = new Artist(artistNode.path("name").asText());
                artist.addId("lastfm", artistNode.path("url").asText());
                JsonNode imageNode = artistNode.path("image");
                if (imageNode.isArray() && !imageNode.isEmpty()) {
                    String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                    if (coverImage != null && !coverImage.isEmpty()) {
                        artist.setCoverImage(coverImage);
                    }
                } else {
                    logger.debug(artist.toString() + ": artist missing images: " + artistNode.toString());
                }
                this.artists.put(params.toString(), artist);
                return artist;
            } else {
                logger.debug(artist.toString() + ": artist.getInfo missing artist");
            }
        }
        logger.debug("Unable to find artist " + artist.toString() + " in Library");
        return null;
    }

    public LinkedHashSet<Album> getArtistAlbums(Artist artist) throws InterruptedException {
        if (artist == null) {
            logger.debug("Empty artist passed to getArtistAlbums");
            return null;
        }
        LinkedHashSet<Album> albums = new LinkedHashSet<>();
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("artist", artist.getName());
        JsonNode response = this.lastFMQuery("artist.getTopAlbums", this.queryBuilder(params));
        if (response != null) {
            JsonNode topAlbumsNode = response.path("topalbums");
            if (!topAlbumsNode.isMissingNode()) {
                JsonNode albumNodes = topAlbumsNode.path("album");
                if (albumNodes.isArray() && !albumNodes.isEmpty()) {
                    for (JsonNode albumNode : albumNodes) {
                        Album album = new Album(albumNode.path("name").asText());
                        album = this.getAlbum(album);
                        if (album != null) {
                            // ensure the search worked
                            if (album.getArtists().contains(artist)) {
                                // filter out singles / empty albums
                                if (album.size() > 2) {
                                    albums.add(album);
                                } else {
                                    logger.debug("skipping album '" + album.getName() + "' as it is a single / empty ("
                                            + album.size() + ")");
                                }
                            } else {
                                logger.debug("non corresponding artist '" + album.getMainArtist() + "' found in album '"
                                        + album.getName() + "' while searching '" + artist.getName() + "' albums");
                            }
                        }
                    }
                } else {
                    logger.debug(artist.toString() + ": missing data in artist albums: " + topAlbumsNode.toString());
                }
            } else {
                logger.debug(artist.toString() + ": missing data getting top albums: " + response.toString());
            }
        }
        if (albums.isEmpty()) {
            logger.debug("Unable to find albums for '" + artist.getName() + "' in Library");
            return null;
        }
        return albums;
    }

    private void timeoutManager() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastQueryTime;

        if (elapsedTime < queryDiff) {
            try {
                TimeUnit.MILLISECONDS.sleep(queryDiff - elapsedTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting second: " + e);
            }
        }
        lastQueryTime = System.currentTimeMillis();
    }

    private String queryBuilder(LinkedHashMap<String, String> params) {
        try {
            StringBuilder builder = new StringBuilder();
            for (String param : params.keySet()) {
                builder.append('&').append(param).append('=')
                        .append(URLEncoder.encode(params.get(param), StandardCharsets.UTF_8.toString()));
            }
            return builder.toString();
        } catch (Exception e) {
            logger.error("Unalbe to build a query: " + e);
            return null;
        }
    }

    private JsonNode lastFMQuery(String type, String query) {
        if (type == null || type.isEmpty()) {
            logger.debug("null or empty type provided in musicBeeQuery");
            return null;
        }
        if (query == null || query.isEmpty()) {
            logger.debug("null or empty query provided in musicBeeQuery");
            return null;
        }
        timeoutManager();
        try {
            StringBuilder urlBuilder = new StringBuilder(this.baseUrl);
            urlBuilder.append("?method=").append(type);
            urlBuilder.append("&api_key=").append(credentials.getLastFMApiKey());
            urlBuilder.append("&format=json");
            urlBuilder.append(query);
            URI url = new URI(urlBuilder.toString());
            JsonNode rootNode = this.query(url);
            if (rootNode != null) {
                return rootNode;
            }
        } catch (Exception e) {
            logger.error("Error querying LastFM: " + e);
        }
        return null;
    }

    private JsonNode query(URI url) throws InterruptedException {
        if (interrupted.get()) {
            throw new InterruptedException();
        }
        if (url == null) {
            logger.debug("null url provided to query");
            return null;
        }
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
                    handleApiError(errorCode, errorMessage);
                    return null;
                }
            }
            return rootNode;
        } catch (Exception e) {
            logger.error("Error querying API: " + e);
            return null;
        }
    }

    /**
     * barebones method of handling library query response errors
     * 
     * @param code    - error code thrown
     * @param message - additional message
     */
    private void handleApiError(int code, String message) {
        switch (code) {
            case 2:
                logger.error("Invalid service - This service does not exist");
                break;
            case 3:
                logger.error("Invalid Method - No method with that name in this package");
                break;
            case 4:
                logger.error("Authentication Failed - You do not have permissions to access the service");
                break;
            case 5:
                logger.error("Invalid format - This service doesn't exist in that format");
                break;
            case 6:
                logger.error("Invalid parameters - Your request is missing a required parameter");
                break;
            case 7:
                logger.error("Invalid resource specified");
                break;
            case 8:
                logger.error("Operation failed - Something else went wrong");
                break;
            case 9:
                logger.error("Invalid session key - Please re-authenticate");
                break;
            case 10:
                logger.error("Invalid API key - You must be granted a valid key by last.fm");
                break;
            case 11:
                logger.error("Service Offline - This service is temporarily offline. Try again later");
                break;
            case 13:
                logger.error("Invalid method signature supplied");
                break;
            case 16:
                logger.error("There was a temporary error processing your request. Please try again");
                break;
            case 26:
                logger.error("Suspended API key - Access for your account has been suspended, please contact Last.fm");
                break;
            case 29:
                logger.error("Rate limit exceeded - Your IP has made too many requests in a short period");
                break;
            default:
                logger.error("Unknown error: " + message);
                break;
        }
    }
}