package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import ryzen.ownitall.tools.Input;

import java.net.URL;

public class Library {
    private static final Logger logger = LogManager.getLogger(Library.class);
    private static Credentials credentials = Credentials.load();
    private static Library instance;
    private final String baseUrl = "http://ws.audioscrobbler.com/2.0/";
    private ObjectMapper objectMapper;

    /**
     * arrays to save api queries if they already exist
     * TODO: dump them and reload to save API queries
     */
    private LinkedHashMap<Integer, Artist> artists;
    private LinkedHashMap<Integer, Song> songs;
    private LinkedHashMap<Integer, Album> albums;

    public static Library load() {
        if (instance == null) {
            instance = new Library();
        }
        return instance;
    }

    public Library() {
        if (credentials.lastFMIsEmpty()) {
            setCredentials();
        }
        this.objectMapper = new ObjectMapper();
        this.artists = new LinkedHashMap<>();
        this.songs = new LinkedHashMap<>();
        this.albums = new LinkedHashMap<>();
    }

    public static void setCredentials() {
        logger.info("A guide to obtaining the following variables is in the readme");
        System.out.print("Please enter LastFM API key: ");
        credentials.setLastFMApiKey(Input.request().getString());
    }

    public Artist getArtist(String artistName) {
        Artist tmpArtist = new Artist(artistName);
        if (this.artists.containsKey(tmpArtist.hashCode())) {
            return this.artists.get(tmpArtist.hashCode());
        }
        Map<String, String> params = Map.of("artist", artistName, "limit", "1");
        String response = query("artist.search", params);
        if (response != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode artistNode = rootNode.path("results").path("artistmatches").path("artist").get(0);

                if (artistNode != null) {
                    Artist artist = new Artist(artistNode.path("name").asText());
                    JsonNode imageNode = artistNode.path("image");
                    if (imageNode != null && imageNode.size() > 0) {
                        artist.setProfilePicture(imageNode.get(0).path("#text").asText());
                    }
                    this.artists.put(artist.hashCode(), artist);
                    return artist;
                }
            } catch (JsonProcessingException e) {
                logger.error("Error parsing json while getting artist " + artistName + ": " + e);
            }
        }
        return new Artist(artistName);
    }

    public Album getAlbum(String albumName, String artistName) {
        Album tmpAlbum = new Album(albumName);
        tmpAlbum.addArtist(new Artist(artistName));
        if (this.albums.containsKey(tmpAlbum.hashCode())) { // TODO: needs to be optimized (maybe use
                                                            // tmpAlbum.hashcode() as key)
            return this.albums.get(tmpAlbum.hashCode());
        }
        Map<String, String> params = Map.of("album", albumName, "artist", artistName, "limit", "1");
        String response = query("album.search", params);
        if (response != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode albumNode = rootNode.path("results").path("albummatches").path("album").get(0);

                if (albumNode != null) {
                    Album album = new Album(albumNode.path("name").asText());
                    String artist = albumNode.path("artist").asText();
                    if (this.artists.containsKey(artist.hashCode())) {
                        album.addArtist(this.artists.get(artist.hashCode()));
                    } else {
                        album.addArtist(this.getArtist(artist));
                        // album.addArtist(new Artist(artist));
                    }
                    JsonNode imageNode = albumNode.path("image");
                    if (imageNode != null && imageNode.size() > 0) {
                        album.setCoverImage(imageNode.get(0).path("#text").asText());
                    }
                    this.albums.put(album.hashCode(), album);
                    return album;
                }
            } catch (JsonProcessingException e) {
                logger.error("Error parsing json while getting album " + albumName + ": " + e);
            }
        }
        return new Album(albumName);
    }

    public Song getSong(String songName, String artistName) {
        Song tmpSong = new Song(songName);
        if (this.songs.containsKey(tmpSong.hashCode())) {
            return this.songs.get(tmpSong.hashCode());
        }
        Map<String, String> params;
        if (artistName == null) {
            params = Map.of("track", songName, "limit", "1");
        } else {
            params = Map.of("track", songName, "artist", artistName, "limit", "1");
        }
        String response = query("track.search", params);
        if (response != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode trackNode = rootNode.path("results").path("trackmatches").path("track").get(0);

                if (trackNode != null) {
                    if (trackNode.has("error")) {
                        this.handleApiError(rootNode.path("error").asInt(), rootNode.path("message").asText());
                        return new Song(songName); // TODO: error handling
                    }
                    Song song = new Song(trackNode.path("name").asText());
                    String artist = trackNode.path("artist").asText();
                    if (this.artists.containsKey(artist.hashCode())) {
                        song.addArtist(this.artists.get(artist.hashCode()));
                    } else {
                        if (artistName != null) {
                            song.addArtist(this.getArtist(artistName));
                        }
                    }
                    JsonNode imageNode = trackNode.path("image");
                    if (imageNode != null && imageNode.size() > 0) {
                        song.setCoverImage(imageNode.get(0).path("#text").asText());
                    }
                    this.songs.put(song.hashCode(), song);
                    return song;
                }
            } catch (JsonProcessingException e) {
                logger.error("Error parshing json while getting song " + songName + ": " + e);
            }
        }
        return new Song(songName);
    }

    private String query(String method, Map<String, String> params) {
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

            return response.toString();
        } catch (Exception e) {
            logger.error("Error querying API: " + e);
            return null;
        }
    }

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
