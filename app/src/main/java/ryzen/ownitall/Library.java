package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URL;

public class Library {
    private static final Logger logger = LogManager.getLogger(Library.class);
    private static Settings settings = Settings.load();
    private static Credentials credentials = Credentials.load();
    private static Sync sync = Sync.load();
    private static Library instance;
    private final String baseUrl = "http://ws.audioscrobbler.com/2.0/";
    private ObjectMapper objectMapper;

    /**
     * arrays to save api queries if they already exist
     */
    private LinkedHashSet<Artist> artists;
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
        }
        return instance;
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
        this.artists = sync.cacheArtists(new LinkedHashSet<>());
        this.songs = sync.cacheSongs(new LinkedHashSet<>());
        this.albums = sync.cacheAlbums(new LinkedHashSet<>());
    }

    /**
     * dump all data into cache
     */
    public void save() {
        sync.cacheAlbums(this.albums);
        sync.cacheSongs(this.songs);
        sync.cacheArtists(this.artists);
    }

    /**
     * clear in memory cache
     */
    public void clear() {
        this.albums.clear();
        this.songs.clear();
        this.artists.clear();
    }

    /**
     * get artist from cached artists
     * 
     * @param artist - constructed artist to find
     * @return - constructed artist in array or null
     */
    private Artist getArtist(Artist artist) {
        for (Artist thisArtist : this.artists) {
            if (thisArtist.equals(artist)) {
                return thisArtist;
            }
        }
        return null;
    }

    /**
     * get artist using music library
     * 
     * @param artistName - name of artist to find
     * @return - constructed artist backed with music library data or with the
     *         provided name
     */
    public Artist getArtist(String artistName) {
        if (artistName == null) {
            return null;
        }
        Artist tmpArtist = new Artist(artistName);
        if (!settings.useLibrary) {
            return tmpArtist;
        }
        if (this.artists.contains(tmpArtist)) {
            return this.getArtist(tmpArtist);
        }
        Map<String, String> params = Map.of("artist", artistName, "limit", "1");
        JsonNode response = query("artist.search", params);
        if (response != null) {
            JsonNode artistNode = response.path("results").path("artistmatches").path("artist").get(0);
            if (artistNode != null) {
                Artist artist = new Artist(artistNode.path("name").asText());
                this.artists.add(artist);
                return artist;
            }
        }
        logger.debug("Could not find artist '" + artistName + "' in Library");
        return tmpArtist;
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
        Album tmpAlbum = new Album(albumName);
        if (artistName != null) {
            tmpAlbum.addArtist(new Artist(artistName));
        }
        if (!settings.useLibrary) {
            return tmpAlbum;
        }
        if (this.albums.contains(tmpAlbum)) {
            return this.getAlbum(tmpAlbum);
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
                    album.addArtist(this.getArtist(artist));
                }
                String albumCover = albumNode.path("image").get(albumNode.path("image").size() - 1).path("#text")
                        .asText();
                if (albumCover != null && !albumCover.isEmpty()) {
                    album.setCoverImage(albumCover);
                }
                this.albums.add(album);
                return album;
            }
        }
        logger.debug("Could not find Album '" + albumName + "' in Library");
        return tmpAlbum;
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
        Song tmpSong = new Song(songName);
        if (artistName != null) {
            tmpSong.setArtist(new Artist(artistName));
        }
        if (!settings.useLibrary) {
            return tmpSong;
        }
        if (this.songs.contains(tmpSong)) {
            return this.getSong(tmpSong);
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
                    song.setArtist(this.getArtist(artist));
                }
                String songCover = trackNode.path("image").get(trackNode.path("image").size() - 1).path("#text")
                        .asText();
                if (songCover != null && !songCover.isEmpty()) {
                    song.setCoverImage(songCover);
                }
                this.songs.add(song);
                return song;
            }
        }
        logger.debug("Could not find song '" + songName + "' in Library");
        return tmpSong;
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

            URL url = new URL(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            String jsonResponse = response.toString();

            // error handling
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

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