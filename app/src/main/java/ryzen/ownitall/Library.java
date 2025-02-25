package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final String musicBeeUrl = "https://musicbrainz.org/ws/2/";
    private final String coverArtUrl = "https://coverartarchive.org/";
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
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("release", albumName);
        if (artistName != null) {
            params.put("artistname", artistName);
        }
        params.put("primarytype", "Album");
        String foundMbid = this.mbids.get(params.toString());
        if (foundMbid != null) {
            return foundMbid;
        }
        JsonNode response = this.musicBeeQuery("release", this.searchQueryBuilder(params));
        if (response != null) {
            JsonNode albumNode = response.path("releases").get(0);
            if (albumNode != null) {
                String mbid = albumNode.path("id").asText();
                this.mbids.put(params.toString(), mbid);
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
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("recordings");
        inclusions.add("artists");
        // check cache
        JsonNode response = this.musicBeeQuery("release", this.directQueryBuilder(mbid, inclusions));
        if (response != null) {
            Album album = new Album(response.path("title").asText());
            album.addId("mbid", response.path("id").asText());
            URI albumCover = this.getCoverArt(response.path("id").asText());
            if (albumCover != null) {
                album.setCoverImage(albumCover);
            }
            JsonNode artistNodes = response.path("artist-credit");
            if (artistNodes.isArray()) {
                for (JsonNode rootArtistNode : artistNodes) {
                    JsonNode artistNode = rootArtistNode.path("artist");
                    Artist artist = this.getArtistDirect(artistNode.path("id").asText());
                    if (artist != null) {
                        album.addArtist(artist);
                    }
                }
            } else {
                logger.debug("album missing artists: " + response.toString());
            }
            JsonNode discNodes = response.path("media");
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
                    } else {
                        logger.debug("Album disc missing songs: " + discNode.toString());
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
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("recording", songName);
        if (artistName != null) {
            params.put("artistname", artistName);
        }
        String foundMbid = this.mbids.get(params.toString());
        if (foundMbid != null) {
            return foundMbid;
        }
        JsonNode response = this.musicBeeQuery("recording", this.searchQueryBuilder(params));
        if (response != null) {
            JsonNode trackNode = response.path("recordings").get(0);
            if (trackNode != null) {
                String mbid = trackNode.path("id").asText();
                this.mbids.put(params.toString(), mbid);
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
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("artists");
        inclusions.add("url-rels");
        inclusions.add("releases");
        JsonNode response = this.musicBeeQuery("recording", this.directQueryBuilder(mbid, inclusions));
        if (response != null) {
            Song song = new Song(response.path("title").asText());
            song.setDuration(response.path("length").asLong(), ChronoUnit.MILLIS);
            song.addId("mbid", response.path("id").asText());
            JsonNode artistNode = response.path("artist-credit").get(0).path("artist");
            if (artistNode != null && !artistNode.isMissingNode()) {
                Artist artist = this.getArtistDirect(artistNode.path("id").asText());
                if (artist != null) {
                    song.setArtist(artist);
                }
            } else {
                logger.debug("Song missing artists: " + response.toString());
            }
            JsonNode releaseNode = response.path("releases").get(0);
            if (releaseNode != null) {
                URI songCover = this.getCoverArt(releaseNode.path("id").asText());
                if (songCover != null) {
                    song.setCoverImage(songCover);
                }
            }
            this.songs.put(mbid, song);
            return song;
            // TODO: get external links (spotify & youtube)
        }
        logger.debug("Could not find song with mbid " + mbid + " in library");
        return null;
    }

    public Artist getArtist(String artistName) {
        String mbid = this.searchArtist(artistName);
        if (mbid == null) {
            return null;
        }
        return this.getArtistDirect(mbid);
    }

    public String searchArtist(String artistName) {
        if (artistName == null) {
            logger.debug("Empty artistName passed in getArtist");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("artist", artistName);
        String foundMbid = this.mbids.get(params.toString());
        if (foundMbid != null) {
            return foundMbid;
        }
        JsonNode response = this.musicBeeQuery("artist", this.searchQueryBuilder(params));
        if (response != null) {
            JsonNode artistNode = response.path("artists").get(0);
            if (artistNode != null) {
                String mbid = artistNode.path("id").asText();
                this.mbids.put(params.toString(), mbid);
                return mbid;
            }
        }
        logger.debug("could not find '" + artistName + "' in library");
        return null;
    }

    public Artist getArtistDirect(String mbid) {
        if (mbid == null || mbid.isEmpty()) {
            logger.debug("null or empty mbid provided in getArtist");
            return null;
        }
        Artist foundArtist = this.artists.get(mbid);
        if (foundArtist != null) {
            return foundArtist;
        }
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("releases");
        JsonNode response = this.musicBeeQuery("artist", this.directQueryBuilder(mbid, inclusions));
        if (response != null) {
            Artist artist = new Artist(response.path("name").asText());
            artist.addId("mbid", response.path("id").asText());
            this.artists.put(mbid, artist);
            return artist;
        }
        logger.debug("Could not find song with mbid " + mbid + " in library");
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

    private URI getCoverArt(String mbid) {
        if (mbid == null || mbid.isEmpty()) {
            logger.debug("null or empty mbid provided in getCoverArt");
            return null;
        }
        JsonNode response = this.coverArtQuery(mbid);
        if (response != null) {
            JsonNode imageNodes = response.path("images");
            if (imageNodes.isArray()) {
                for (JsonNode imageNode : imageNodes) {
                    if (imageNode.path("front").asBoolean()) {
                        String imagePath = imageNode.path("image").asText();
                        try {
                            return new URI(imagePath);
                        } catch (URISyntaxException e) {
                            logger.debug("Problem parsing image: " + imagePath);
                        }
                    }
                }
            } else {
                logger.debug("Cover Art missing images: " + response.toString());
            }
        }
        logger.debug("No Coverart found for mbid: " + mbid);
        return null;
    }

    private String searchQueryBuilder(LinkedHashMap<String, String> params) {
        StringBuilder builder = new StringBuilder();
        builder.append("?query=");
        StringBuilder paramBuilder = new StringBuilder();
        for (String param : params.keySet()) {
            paramBuilder.append(param).append(':').append('"').append(params.get(param)).append('"');
        }
        try {
            builder.append(URLEncoder.encode(paramBuilder.toString(), StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            logger.error("Exception while encoding query: " + e);
            return null;
        }
        builder.append("&fmt=json");
        builder.append("&limit=1");
        return builder.toString();
    }

    private String directQueryBuilder(String mbid, LinkedHashSet<String> inclusions) {
        StringBuilder builder = new StringBuilder();
        builder.append("/").append(mbid);
        StringBuilder incBuilder = new StringBuilder("?inc=");
        for (String inc : inclusions) {
            if (!incBuilder.toString().isEmpty()) {
                incBuilder.append("+");
            }
            incBuilder.append(inc);
        }
        builder.append(incBuilder.toString());
        builder.append("&fmt=json");
        return builder.toString();
    }

    private JsonNode coverArtQuery(String mbid) {
        if (mbid == null || mbid.isEmpty()) {
            logger.debug("null or empty mbid provided in coverArtQuery");
            return null;
        }
        try {
            StringBuilder urlBuilder = new StringBuilder(this.coverArtUrl);
            urlBuilder.append("release").append("/");
            urlBuilder.append(mbid).append('/');
            URI url = new URI(urlBuilder.toString());
            JsonNode rootNode = this.query(url);
            if (rootNode != null) {
                return rootNode;
            }
        } catch (Exception e) {
            logger.error("Error querying CoverArtArchive: " + e);
        }
        return null;
    }

    private JsonNode musicBeeQuery(String type, String query) {
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
            StringBuilder urlBuilder = new StringBuilder(this.musicBeeUrl);
            urlBuilder.append(type);
            urlBuilder.append(query);
            URI url = new URI(urlBuilder.toString());
            JsonNode rootNode = this.query(url);
            if (rootNode != null) {
                return rootNode;
            }
        } catch (Exception e) {
            logger.error("Error querying MusicBee: " + e);
        }
        return null;
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