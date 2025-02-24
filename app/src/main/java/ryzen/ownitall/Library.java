package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;

import com.fasterxml.jackson.databind.JsonNode;

public class Library {
    private static final Logger logger = LogManager.getLogger(Library.class);
    private static final Sync sync = Sync.load();
    private static Library instance;
    private final String baseUrl = "https://musicbrainz.org/ws/2/";
    private static long lastQueryTime = 0;
    private ObjectMapper objectMapper;

    /**
     * arrays to save api queries if they already exist
     */
    private LinkedHashMap<String, Artist> artists;
    private LinkedHashMap<String, Album> albums;
    private LinkedHashMap<String, Song> songs;
    private LinkedHashMap<String, String> mbids;

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
        this.artists = sync.cacheArtists(new LinkedHashMap<>());
        this.albums = sync.cacheAlbums(new LinkedHashMap<>());
        this.songs = sync.cacheSongs(new LinkedHashMap<>());
        this.mbids = sync.cacheMbids(new LinkedHashMap<>());
    }

    /**
     * dump all data into cache
     */
    public void save() {
        sync.cacheArtists(this.artists);
        sync.cacheAlbums(this.albums);
        sync.cacheSongs(this.songs);
        sync.cacheMbids(this.mbids);
    }

    /**
     * clear in memory cache
     */
    public void clear() {
        this.artists.clear();
        this.albums.clear();
        this.songs.clear();
        this.mbids.clear();
    }

    public Album getAlbum(String albumName, String artistName) {
        String mbid = this.searchAlbum(albumName, artistName);
        if (mbid == null) {
            return null;
        }
        return this.getAlbum(mbid);
    }

    private String searchAlbum(String albumName, String artistName) {
        if (albumName == null) {
            logger.debug("Empty albumName parsed in searchAlbum");
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append('"').append(albumName).append('"');
        if (artistName != null) {
            builder.append("artistname:").append('"').append(artistName).append('"');
        }
        builder.append("primarytype:").append('"').append("Album").append('"');
        String foundMbid = this.mbids.get(builder.toString());
        if (foundMbid != null) {
            return foundMbid;
        }
        JsonNode response = this.searchQuery("release", builder.toString());
        if (response != null) {
            JsonNode albumNode = response.path("releases").get(0);
            if (albumNode != null && !albumNode.isMissingNode()) {
                String mbid = albumNode.path("id").asText();
                this.mbids.put(builder.toString(), mbid);
                return mbid;
            } else {
                logger.debug("missing data in album search result " + response.toString());
            }
        }
        logger.debug("Could not find Album " + albumName + " in Library");
        return null;
    }

    public Album getAlbum(String mbid) {
        if (mbid == null || mbid.isEmpty()) {
            logger.debug("null or empty mbid provided in getAlbum");
            return null;
        }
        Album foundAlbum = this.albums.get(mbid);
        if (foundAlbum != null) {
            return foundAlbum;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(mbid);
        // get all songs, artists and external urls
        builder.append("?inc=").append("recordings").append('+').append("artists");
        // check cache
        JsonNode response = this.directQuery("release", builder.toString());
        if (response != null) {
            Album album = new Album(response.path("title").asText());
            album.addId("mbid", response.path("id").asText());
            JsonNode coverArtNode = response.path("cover-art-archive");
            if (!coverArtNode.isMissingNode()) {
                if (coverArtNode.path("artwork").asBoolean()) {
                    // TODO: get album cover art
                    // https://musicbrainz.org/doc/Cover_Art_Archive/API
                }
            }
            JsonNode artistNodes = response.path("artist-credit");
            if (!artistNodes.isMissingNode()) {
                if (artistNodes.isArray()) {
                    for (JsonNode rootArtistNode : artistNodes) {
                        JsonNode artistNode = rootArtistNode.path("artist");
                        Artist artist = new Artist(artistNode.path("name").asText());
                        artist.addId("mbid", artistNode.path("id").asText());
                    }
                }
            } else {
                logger.debug("album missing artists: " + response.toString());
            }
            JsonNode discNodes = response.path("media");
            if (!discNodes.isMissingNode()) {
                if (discNodes.isArray()) {
                    for (JsonNode discNode : discNodes) {
                        JsonNode songNodes = discNode.path("tracks");
                        if (songNodes.isArray()) {
                            for (JsonNode songNode : songNodes) {
                                Song song = this.getSong(songNode.path("recording").path("id").asText());
                                if (song != null) {
                                    album.addSong(song);
                                }
                            }
                        }
                    }
                }
            } else {
                logger.debug("Album missing songs: " + response.toString());
            }
            this.albums.put(mbid, album);
            return album;
        }
        logger.debug("Unable to find album with mbid: " + mbid + " in library");
        return null;
    }

    public Song getSong(String songName, String artistName) {
        String mbid = this.searchSong(songName, artistName);
        if (mbid == null) {
            return null;
        }
        return this.getSong(mbid);
    }

    private String searchSong(String songName, String artistName) {
        if (songName == null) {
            logger.debug("Empty songName passed in getSong");
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append('"').append(songName).append('"');
        if (artistName != null) {
            builder.append("artistname:").append('"').append(artistName).append('"');
        }
        String foundMbid = this.mbids.get(builder.toString());
        if (foundMbid != null) {
            return foundMbid;
        }
        JsonNode response = this.searchQuery("recording", builder.toString());
        if (response != null) {
            JsonNode trackNode = response.path("recordings").get(0);
            if (trackNode != null && !trackNode.isMissingNode()) {
                String mbid = trackNode.path("id").asText();
                this.mbids.put(builder.toString(), mbid);
                return mbid;
            } else {
                logger.error("Missing data while getting Song: " + response.toString());
            }
        }
        logger.debug("Could not find song '" + songName + "' in library");
        return null;
    }

    public Song getSong(String mbid) {
        if (mbid == null || mbid.isEmpty()) {
            logger.debug("null or empty mbid provided in getSong");
            return null;
        }
        Song foundSong = this.songs.get(mbid);
        if (foundSong != null) {
            return foundSong;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(mbid);
        // get all songs, artists and external urls
        builder.append("?inc=").append("artists").append('+').append("url-rels");
        JsonNode response = this.directQuery("recording", builder.toString());
        if (response != null) {
            Song song = new Song(response.path("title").asText());
            song.addId("mbid", response.path("id").asText());
            JsonNode artistNode = response.path("artist-credit").get(0).path("artist");
            if (artistNode != null && !artistNode.isMissingNode()) {
                Artist artist = new Artist(artistNode.path("name").asText());
                artist.addId("mbid", artistNode.path("id").asText());
                song.setArtist(artist);
            } else {
                logger.debug("Song missing artists: " + response.toString());
            }
            // TODO: song cover art
            this.songs.put(mbid, song);
            return song;
            // TODO: get external links (spotify & youtube)
        }
        logger.debug("Could not find song with mbid " + mbid + " in library");
        return null;
    }

    public Artist getArtist(String artistName) {
        if (artistName == null) {
            logger.debug("Empty artistName passed in getArtist");
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("artist:").append('"').append(artistName).append('"');
        JsonNode response = this.searchQuery("artist", builder.toString());
        if (response != null) {
            JsonNode artistNode = response.path("artists").get(0);
            if (artistNode != null && !artistNode.isMissingNode()) {
                Artist artist = new Artist(artistNode.path("name").asText());
                artist.addId("mbid", artistNode.path("id").asText());
                // TODO: artist cover image
                return artist;
            }
        }
        logger.debug("could not find '" + artistName + "' in library");
        return null;
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
    private JsonNode searchQuery(String type, String query) {
        if (type == null || type.isEmpty()) {
            logger.error("null or empty type provided in searchquery");
            return null;
        }
        if (query == null || query.isEmpty()) {
            logger.error("null or empty query provided in searchquery");
            return null;
        }
        try {
            StringBuilder urlBuilder = new StringBuilder(this.baseUrl);
            urlBuilder.append(type);
            // check docs for all possible parameters:
            // https://musicbrainz.org/doc/MusicBrainz_API/Search
            urlBuilder.append("?query=").append(URLEncoder.encode(query, "UTF-8"));
            urlBuilder.append("&fmt=").append("json");
            urlBuilder.append("&limit").append("1");
            URI url = new URI(urlBuilder.toString());
            return this.query(url);
        } catch (Exception e) {
            logger.error("Error constructing API searchQuery: " + e);
            return null;
        }
    }

    private JsonNode directQuery(String type, String query) {
        if (type == null || type.isEmpty()) {
            logger.error("null or empty type provided in directquery");
            return null;
        }
        if (query == null || query.isEmpty()) {
            logger.error("null or empty query provided in directquery");
            return null;
        }
        try {
            StringBuilder urlBuilder = new StringBuilder(this.baseUrl);
            urlBuilder.append(type);
            // check docs for all possible parameters:
            // https://musicbrainz.org/doc/MusicBrainz_API
            urlBuilder.append("/").append(query);
            urlBuilder.append("&fmt=").append("json");
            URI url = new URI(urlBuilder.toString());
            return this.query(url);
        } catch (Exception e) {
            logger.error("Error constructing API directQuery: " + e);
            return null;
        }
    }

    private JsonNode query(URI url) {
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