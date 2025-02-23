package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;

import com.fasterxml.jackson.databind.JsonNode;

public class Library {
    // TODO: rewrite to use musicbrainz + coverart archive
    private static final Logger logger = LogManager.getLogger(Library.class);
    private static final Sync sync = Sync.load();
    private static Library instance;
    private final String baseUrl = "https://musicbrainz.org/ws/2/";
    private static long lastQueryTime = 0;
    private ObjectMapper objectMapper;

    /**
     * arrays to save api queries if they already exist
     */
    private LinkedHashSet<Song> songs;
    private LinkedHashSet<Album> albums;
    private LinkedHashSet<Artist> artists;

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
        this.objectMapper = new ObjectMapper();
        this.songs = sync.cacheSongs(new LinkedHashSet<>());
        this.albums = sync.cacheAlbums(new LinkedHashSet<>());
        this.artists = sync.cacheArtists(new LinkedHashSet<>());
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

    public Album getAlbum(String albumName, String artistName) {
        if (albumName == null) {
            logger.debug("Empty albumName parsed in searchAlbum");
            return null;
        }
        Album tmpAlbum = new Album(albumName);
        if (artistName != null) {
            tmpAlbum.addArtist(new Artist(artistName));
        }
        if (this.albums.contains(tmpAlbum)) {
            return this.getAlbum(tmpAlbum);
        }
        Map<String, String> params = Map.of("release", albumName);
        if (artistName != null) {
            params.put("artistname", artistName);
        }
        params.put("limit", "1");
        JsonNode response = query("release", params);
        if (response != null) {
            JsonNode albumNode = response.path("releases").get(0);
            if (albumNode != null && !albumNode.isMissingNode()) {
                Album album = new Album(albumNode.path("title").asText());
                album.addId("mbid", albumNode.path("id").asText());
                JsonNode artistNodes = albumNode.path("artist-credit");
                if (!artistNodes.isMissingNode()) {
                    if (artistNodes.isArray() && !artistNodes.isEmpty()) {
                        for (JsonNode rootArtistNode : artistNodes) {
                            JsonNode artistNode = rootArtistNode.path("artist");
                            if (!artistNode.isMissingNode()) {
                                Artist artist = new Artist(artistNode.path("name").asText());
                                artist.addId("mbid", artistNode.path("id").asText());
                                album.addArtist(artist);
                            } else {
                                logger.debug("artist missing info: " + artistNode.toString());
                            }
                        }
                    }
                } else {
                    logger.debug("album missing artists: " + albumNode.toString());
                }
                // TODO: get album cover image
                // TODO: get album tracks
                this.albums.add(album);
                return album;
            } else {
                logger.debug("missing data in album search result " + response.toString());
            }
        }
        logger.debug("Could not find Album " + albumName + " in Library");
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
     * construct a song using the LastFM api and their search.
     * constructs everything except the: duration
     * 
     * @param songName   - name of song to search
     * @param artistName - optional artist to match with the song
     * @return - constructed song
     */
    public Song getSong(String songName, String artistName) {
        if (songName == null) {
            logger.debug("Empty songName passed in getSong");
            return null;
        }
        Song tmpSong = new Song(songName);
        if (artistName != null) {
            tmpSong.setArtist(new Artist(artistName));
        }
        if (this.songs.contains(tmpSong)) {
            return this.getSong(tmpSong);
        }
        Map<String, String> params = Map.of("recording", songName);
        if (artistName != null) {
            params.put("artistname", artistName);
        }
        params.put("limit", "1");
        JsonNode response = query("recording", params);
        if (response != null) {
            JsonNode trackNode = response.path("recordings").get(0);
            if (trackNode != null && !trackNode.isMissingNode()) {
                Song song = new Song(trackNode.path("title").asText());
                JsonNode artistNode = trackNode.path("artist-credit").get(0).path("artist");
                if (artistNode != null && !artistNode.isMissingNode()) {
                    Artist artist = new Artist(artistNode.path("name").asText());
                    artist.addId("mbid", artistNode.path("id").asText());
                    song.setArtist(artist);
                } else {
                    logger.debug("song missing artist: " + trackNode.toString());
                }
                song.setDuration((long) trackNode.path("length").asInt(), ChronoUnit.MILLIS);
                song.addId("mbid", trackNode.path("id").asText());
                // TODO: song cover image
                this.songs.add(song);
                return song;
            } else {
                logger.error("Missing data while getting Song: " + response.toString());
            }
        }
        logger.debug("Could not find song '" + songName + "' in library");
        return null;
    }

    private Artist getArtist(Artist artist) {
        for (Artist thisArtist : this.artists) {
            if (thisArtist.equals(artist)) {
                return thisArtist;
            }
        }
        return null;
    }

    public Artist getArtist(String artistName) {
        if (artistName == null) {
            logger.debug("Empty artistName passed in getArtist");
            return null;
        }
        Artist tmpArtist = new Artist(artistName);
        if (this.artists.contains(tmpArtist)) {
            return this.getArtist(tmpArtist);
        }
        Map<String, String> params = Map.of("artist", artistName);
        params.put("limit", "1");
        JsonNode response = query("artist", params);
        if (response != null) {
            JsonNode artistNode = response.path("artists").get(0);
            if (artistNode != null && !artistNode.isMissingNode()) {
                Artist artist = new Artist(artistNode.path("name").asText());
                artist.addId("mbid", artistNode.path("id").asText());
                // TODO: artist cover image
                this.artists.add(artist);
                return artist;
            }
        }
        logger.debug("could not find '" + artistName + "' in library");
        return null;
    }

    public LinkedHashSet<Album> getArtistAlbums(Artist artist) {
        if (artist == null) {
            logger.debug("Empty artist passed to getArtistAlbums");
            return null;
        }
        LinkedHashSet<Album> albums = new LinkedHashSet<>();
        logger.info("CURRENTLY NOT SUPPORTED");
        return albums;
    }

    private void timeoutManager() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastQueryTime;

        if (elapsedTime < 1000) {
            try {
                TimeUnit.MILLISECONDS.sleep(1000 - elapsedTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting second: " + e);
            }
        }
        lastQueryTime = System.currentTimeMillis();
    }

    /**
     * make a query to the music library
     * 
     * @param type  - search, browse, ...
     * @param query - search, browse parameters
     * @return - JsonNode response
     */
    private JsonNode query(String type, Map<String, String> params) {
        timeoutManager();
        try {
            StringBuilder urlBuilder = new StringBuilder(this.baseUrl);
            urlBuilder.append(type);
            urlBuilder.append("?fmt=").append("json");
            // check docs for all possible parameters:
            // https://musicbrainz.org/doc/MusicBrainz_API
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.append("&").append(entry.getKey()).append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            URI url = new URI(urlBuilder.toString());
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

            if (rootNode.path("status").asText().equals("error")) {
                logger.error("unexpected query response (" + rootNode.path("code").asInt() + "): " + rootNode
                        .path("message").asText());
                return null;
            }
            return rootNode;
        } catch (Exception e) {
            logger.error("Error querying API: " + e);
            return null;
        }
    }
}