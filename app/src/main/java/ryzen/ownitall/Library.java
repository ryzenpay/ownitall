package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.LinkedHashSet;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
    private static Sync sync = Sync.load();
    private static Library instance;
    private final String baseUrl = "https://musicbrainz.org/ws/2/";
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
        Artist foundArtist = Collection.getArtist(artists, tmpArtist);
        if (foundArtist != null) {
            return foundArtist;
        }
        JsonNode response = query("artist", tmpArtist.getName());
        if (response != null) {
            JsonNode artistNode = response.path("artists").get(0);
            if (artistNode != null) {
                Artist artist = new Artist(artistNode.path("name").asText());
                artist.setId(artistNode.path("id").asText());
                this.artists.add(artist);
                return artist;
            }
        }
        logger.debug("Could not find artist '" + artistName + "' in Library");
        return tmpArtist;
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
        Album foundAlbum = Collection.getAlbum(albums, tmpAlbum);
        if (foundAlbum != null) {
            return foundAlbum;
        }
        JsonNode response = query("release", tmpAlbum.toString());
        if (response != null) {
            JsonNode albumNode = response.path("releases").get(0);
            if (albumNode != null) {
                Album album = new Album(albumNode.path("title").asText());
                album.setId(albumNode.path("id").asText());
                JsonNode artistsNode = albumNode.path("artist-credit");
                for (JsonNode artistNode : artistsNode) {
                    Artist artist = new Artist(artistNode.path("artist").path("name").asText());
                    artist.setId(artistNode.path("artist").path("id").asText());
                    album.addArtist(artist);
                }
                this.albums.add(album);
                return album;
            }
        }
        logger.debug("Could not find Album '" + albumName + "' in Library");
        return tmpAlbum;
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
        Song foundSong = Collection.getSong(songs, tmpSong);
        if (foundSong != null) {
            return foundSong;
        }
        JsonNode response = query("recording", tmpSong.toString());
        if (response != null) {
            JsonNode trackNode = response.path("recordings").get(0);
            if (trackNode != null) {
                Song song = new Song(trackNode.path("title").asText());
                song.setId(trackNode.path("id").asText());
                JsonNode artistNode = trackNode.path("artist-credit").get(0);
                Artist artist = new Artist(artistNode.path("artist").path("name").asText());
                artist.setId(artistNode.path("artist").path("id").asText());
                song.setArtist(artist);
                JsonNode relations = response.path("relations");
                if (relations != null && relations.isArray()) {
                    for (JsonNode relation : relations) {
                        if (relation.path("type").asText().equals("url")) {
                            String urlType = relation.path("type-id").asText();
                            String url = relation.path("url").path("resource").asText();
                            song.addLink(urlType, url);
                        }
                    }
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
    private JsonNode query(String type, String query) {
        try {
            StringBuilder urlBuilder = new StringBuilder(this.baseUrl);
            // what type of entity to return (artist, recording, album)
            urlBuilder.append(type);
            // what to search for
            urlBuilder.append("?query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
            // result limit & offset
            urlBuilder.append("&limit=").append(1);
            urlBuilder.append("&offset=").append(0);
            // response format
            // urlBuilder.append("&fmt=").append("json");
            // get external links
            urlBuilder.append("inc=").append("url-rels");

            URL url = new URL(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // required by their documentation
            connection.setRequestProperty("User-Agent", "OwnitAll/1.0 (https://github.com/ryzenpay/ownitall)");
            connection.setRequestProperty("Accept", "application/json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            logger.debug(response.toString());
            String jsonResponse = response.toString();

            // error handling
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            if (rootNode.path("status").asText().equals("error")) {
                int errorCode = rootNode.path("code").asInt();
                String errorMessage = rootNode.path("message").asText();
                handleApiError(errorCode, errorMessage);
                return null;
            }

            return rootNode;
        } catch (Exception e) {
            logger.error("Error querying Library API: " + e);
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
        switch (code) { // TODO: error handling
            default:
                logger.error("Unknown error (" + code + "): " + message);
                break;
        }
    }
}
