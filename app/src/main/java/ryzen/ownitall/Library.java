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
    private static Library instance;
    private final String baseUrl = "http://ws.audioscrobbler.com/2.0/";
    private String apiKey; // TODO: save credentials
    private ObjectMapper objectMapper;

    /**
     * arrays to save api queries if they already exist
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
        System.out.print("Please enter lastfm api key: ");
        this.apiKey = Input.request().getString();
        this.objectMapper = new ObjectMapper();
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
                    artist.setProfilePicture(artistNode.path("image").get(2).path("#text").asText());
                    this.artists.put(artist.hashCode(), artist);
                    return artist;
                }
            } catch (JsonProcessingException e) {
                logger.error("Error parsing json while getting artist " + artistName + ": " + e);
            }
        }
        return null;
    }

    public Album getAlbum(String albumName, String artistName) {
        Album tmpAlbum = new Album(albumName);
        tmpAlbum.addArtist(new Artist(artistName));
        if (this.albums.containsKey(tmpAlbum.hashCode())) {
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
                    } else { // TODO: here again, should it query again?
                        album.addArtist(new Artist(artist));
                    }
                    album.setCoverImage(albumNode.path("image").get(2).path("#text").asText());
                    this.albums.put(album.hashCode(), album);
                    return album;
                }
            } catch (JsonProcessingException e) {
                logger.error("Error parshing json while getting album " + albumName + ": " + e);
            }
        }
        return null;
    }

    public Song getSong(String trackName, String artistName) {
        Song tmpSong = new Song(trackName);
        if (this.songs.containsKey(tmpSong.hashCode())) {
            return this.songs.get(tmpSong.hashCode());
        }
        Map<String, String> params = Map.of("track", trackName, "artist", artistName, "limit", "1");
        String response = query("track.search", params);
        if (response != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode trackNode = rootNode.path("results").path("trackmatches").path("track").get(0);

                if (trackNode != null) {
                    Song song = new Song(trackNode.path("name").asText());
                    String artist = trackNode.path("artist").asText();
                    if (this.artists.containsKey(artist.hashCode())) {
                        song.addArtist(this.artists.get(artist.hashCode()));
                    } else { // TODO: should it query to get all artist details?
                        song.addArtist(new Artist(artistName));
                    }
                    song.setCoverImage(trackNode.path("image").get(2).path("#text").asText());
                    this.songs.put(song.hashCode(), song);
                    return song;
                }
            } catch (JsonProcessingException e) {
                logger.error("Error parshing json while getting song " + trackName + ": " + e);
            }
        }
        return null;
    }

    private String query(String method, Map<String, String> params) {
        try {
            StringBuilder urlBuilder = new StringBuilder(this.baseUrl);
            urlBuilder.append("?method=").append(method);
            urlBuilder.append("&api_key=").append(apiKey);
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
}
