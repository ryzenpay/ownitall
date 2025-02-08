package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;

import com.fasterxml.jackson.databind.JsonNode;

public class Library {
    private static final Logger logger = LogManager.getLogger(Library.class);
    private static final Settings settings = Settings.load();
    private static final Credentials credentials = Credentials.load();
    private static final Sync sync = Sync.load();
    private static Library instance;
    private final String baseUrl = "http://ws.audioscrobbler.com/2.0/";
    private ObjectMapper objectMapper;

    /**
     * arrays to save api queries if they already exist
     */
    private LinkedHashSet<Song> songs;
    private LinkedHashSet<Album> albums;

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
        if (settings.useLibrary && credentials.lastFMIsEmpty()) {
            credentials.setLastFMCredentials();
        }
        this.objectMapper = new ObjectMapper();
        this.songs = sync.cacheSongs(new LinkedHashSet<>());
        this.albums = sync.cacheAlbums(new LinkedHashSet<>());
    }

    /**
     * dump all data into cache
     */
    public void save() {
        sync.cacheAlbums(this.albums);
        sync.cacheSongs(this.songs);
    }

    /**
     * clear in memory cache
     */
    public void clear() {
        this.albums.clear();
        this.songs.clear();
    }

    /**
     * get album from cached albums
     * 
     * @param album - constructed album to find
     * @return - constructed album or null
     */
    private Album getAlbum(Album album) {
        for (Album thisAlbum : this.albums) {
            if (thisAlbum.equals(album)) {
                return thisAlbum;
            }
        }
        return null;
    }

    /**
     * do a lastfm search for the album
     * 
     * @param albumName  - string album name
     * @param artistName - string album artist name
     * @return - constructed album with confirmed album name and album artist name
     */
    public Album searchAlbum(String albumName, String artistName) {
        if (albumName == null) {
            logger.debug("Empty albumName parsed in searchAlbum");
            return null;
        }
        Map<String, String> params;
        if (artistName != null) {
            params = Map.of("album", albumName, "artist", artistName, "limit", "1");
        } else {
            params = Map.of("album", albumName, "limit", "1");
        }
        JsonNode response = query("album.search", params);
        if (response != null) {
            JsonNode albumNode = response.path("results").path("albummatches").path("album").get(0);
            if (albumNode != null) {
                Album album = new Album(albumNode.path("name").asText());
                String artist = albumNode.path("artist").asText();
                if (artist != null && !artist.isEmpty()) {
                    album.addArtist(new Artist(artist));
                }
                return album;
            }
        }
        logger.debug("Could not find Album " + albumName + " in Library");
        return null;
    }

    /**
     * get constructed album backed up with music library
     * 
     * @param albumName  - name of album
     * @param artistName - optional to aid search
     * @return - constructed album backed by library or with provided values
     */
    public Album getAlbum(String albumName, String artistName) {
        if (albumName == null) {
            return null;
        }
        Album foundAlbum = this.searchAlbum(albumName, artistName);
        if (foundAlbum == null) {
            return null;
        }
        if (this.albums.contains(foundAlbum)) {
            return this.getAlbum(foundAlbum);
        }

        Map<String, String> params = Map.of("album", foundAlbum.getName(), "artist",
                foundAlbum.getMainArtist().getName());
        JsonNode response = query("album.getInfo", params);
        if (response != null) {
            JsonNode albumNode = response.path("album");
            if (albumNode != null && !albumNode.isMissingNode()) {
                Album album = new Album(albumNode.path("name").asText());
                album.addArtist(albumNode.path("artist").asText());

                JsonNode imageNode = albumNode.path("image");
                if (imageNode.isArray() && imageNode.size() > 0) {
                    album.setCoverImage(imageNode.get(imageNode.size() - 1).path("#text").asText());
                }
                album.addId("lastfm", albumNode.path("id").asText());
                this.albums.add(album);
                return album;
            }
        }
        logger.debug(
                "Could not find detailed information for album '" + foundAlbum.getName() + "' by '"
                        + foundAlbum.getMainArtist().getName() + "'");
        return null;
    }

    /**
     * get song from cache
     * 
     * @param song - constructed song to find
     * @return - constructed song from cache or null
     */
    private Song getSong(Song song) {
        for (Song thisSong : this.songs) {
            if (thisSong.equals(song)) {
                return thisSong;
            }
        }
        return null;
    }

    /**
     * search lastFM for song
     * 
     * @param songName   - song name
     * @param artistName - song main artist name
     * @return - constructed song with confirfmed song name and song main artist
     *         name
     */
    public Song searchSong(String songName, String artistName) {
        if (songName == null) {
            logger.debug("Empty songName parsed in searchSong");
            return null;
        }
        Map<String, String> params;
        if (artistName == null) {
            params = Map.of("track", songName, "limit", "1");
        } else {
            params = Map.of("track", songName, "artist", artistName, "limit", "1");
        }
        JsonNode response = query("track.search", params);
        if (response != null) {
            JsonNode trackNode = response.path("results").path("trackmatches").path("track").get(0);
            if (trackNode != null) {
                Song song = new Song(trackNode.path("name").asText());
                String artist = trackNode.path("artist").asText();
                if (artist != null && !artist.isEmpty()) {
                    song.setArtist(new Artist(artist));
                }
                return song;
            }
        }
        logger.debug("Could not find song '" + songName + "' in Library");
        return null;
    }

    /**
     * construct a song using the LastFM api and their search.
     * constructs everything except the: duration
     * 
     * @param songName   - name of song to search
     * @param artistName - optional artist to match with the song
     * @return - constructed song
     */
    public Song getSong(String songName, String artistName) {
        if (songName == null) {
            return null;
        }
        Song foundSong = this.searchSong(songName, artistName);
        if (foundSong == null) {
            return null;
        }
        if (this.songs.contains(foundSong)) {
            return this.getSong(foundSong);
        }

        Map<String, String> params = Map.of("track", foundSong.getName(), "artist", foundSong.getArtist().getName());
        JsonNode response = query("track.getInfo", params);

        if (response != null) {
            JsonNode trackNode = response.path("track");
            if (trackNode != null && !trackNode.isMissingNode()) {
                Song song = new Song(trackNode.path("name").asText());
                song.setArtist(trackNode.path("artist").path("name").asText());

                JsonNode albumNode = trackNode.path("album");
                if (!albumNode.isMissingNode()) {
                    JsonNode imageNode = albumNode.path("image");
                    if (imageNode.isArray() && imageNode.size() > 0) {
                        song.setCoverImage(imageNode.get(imageNode.size() - 1).path("#text").asText());
                    }
                }
                song.setDuration(trackNode.path("duration").asLong(), ChronoUnit.MILLIS);
                song.addId("lastfm", trackNode.path("id").asText());
                this.songs.add(song);
                return song;
            }
        }

        logger.debug(
                "Could not find detailed information for song '" + foundSong.getName() + "' by '"
                        + foundSong.getArtist().getName() + "'");
        return null;
    }

    /**
     * make a query to the music library
     * 
     * @param method - method (POST,...)
     * @param params - search parameters
     * @return - JsonNode response
     */
    private JsonNode query(String method, Map<String, String> params) {
        try {
            StringBuilder urlBuilder = new StringBuilder(this.baseUrl);
            urlBuilder.append("?method=").append(method);
            urlBuilder.append("&api_key=").append(credentials.lastFMApiKey);
            urlBuilder.append("&format=json");

            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.append("&").append(entry.getKey()).append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            URI url = new URI(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            // logger.debug(response.toString());

            // error handling
            JsonNode rootNode = objectMapper.readTree(response.toString());

            if (rootNode.has("error")) {
                int errorCode = rootNode.path("error").asInt();
                String errorMessage = rootNode.path("message").asText();
                handleApiError(errorCode, errorMessage);
                return null;
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