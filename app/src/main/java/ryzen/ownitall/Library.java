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
        if (settings.useLibrary && credentials.lastFMIsEmpty()) {
            credentials.setLastFMCredentials();
        }
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

    /**
     * do a lastfm search for the album
     * 
     * @param albumName  - string album name
     * @param artistName - string album artist name
     * @return - constructed album with confirmed album name and album artist name
     */
    private Album searchAlbum(String albumName, String artistName) {
        // TODO: filter out throwing off characters (*,:)
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
            Album album;
            JsonNode albumNode = response.path("results").path("albummatches").path("album").get(0);
            if (albumNode != null && !albumNode.isMissingNode()) {
                JsonNode albumNameNode = albumNode.path("name");
                if (!albumNameNode.isMissingNode()) {
                    album = new Album(albumNameNode.asText());
                } else {
                    logger.error("album missing name: " + albumNode.toString());
                    return null;
                }
                JsonNode artistNode = albumNode.path("artist");
                if (!artistNode.isMissingNode()) {
                    album.addArtist(new Artist(artistNode.asText()));
                } else {
                    logger.debug("album missing artist name: " + albumNode.toString());
                }
                return album;
            } else {
                logger.debug("problem searching for album: " + response.toString());
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
            logger.debug("Empty albumName passed in getAlbum");
            return null;
        }
        Album album = this.searchAlbum(albumName, artistName);
        if (album == null) {
            return null;
        }
        if (this.albums.contains(album)) {
            return this.getAlbum(album);
        }

        Map<String, String> params = Map.of("album", album.getName(), "artist",
                album.getMainArtist().getName());
        JsonNode response = query("album.getInfo", params);
        if (response != null) {
            JsonNode albumNode = response.path("album");
            if (!albumNode.isMissingNode()) {
                JsonNode nameNode = albumNode.path("name");
                if (!nameNode.isMissingNode()) {
                    album.setName(nameNode.asText());
                } else {
                    logger.debug("Album missing name: " + albumNode.toString());
                }
                JsonNode artistNode = albumNode.path("artist");
                if (!artistNode.isMissingNode()) {
                    Artist artist = this.getArtist(artistNode.asText());
                    if (artist != null) {
                        album.addArtist(artist);
                    }
                } else {
                    logger.debug("Album missing artist: " + albumNode.toString());
                }
                JsonNode imageNode = albumNode.path("image");
                if (imageNode.isArray() && imageNode.size() > 0) {
                    String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                    if (coverImage != null && !coverImage.isEmpty()) {
                        album.setCoverImage(coverImage);
                    }
                } else {
                    logger.debug("Album missing cover image: " + albumNode.toString());
                }
                JsonNode trackNodes = albumNode.path("tracks").path("track");
                if (trackNodes.isArray() && trackNodes.size() > 0) {
                    for (JsonNode trackNode : trackNodes) {
                        String songName = trackNode.path("name").asText();
                        // TODO: artist is adding album artist
                        String songArtistName = trackNode.path("artist").path("name").asText();
                        Song song = this.getSong(songName, songArtistName);
                        if (song != null) {
                            album.addSong(song);
                        }
                    }
                } else {
                    logger.debug("Album missing tracks: " + albumNode.toString());
                }
                JsonNode urlNode = albumNode.path("url");
                if (!urlNode.isMissingNode()) {
                    album.addId("lastfm", String.valueOf(urlNode.asText().hashCode()));
                } else {
                    logger.debug("Album missing url: " + albumName.toString());
                }
                this.albums.add(album);
                return album;
            } else {
                logger.debug("Problem in albumNode: " + response.toString());
            }
        }
        logger.debug(
                "Could not find detailed information for album '" + album.getName() + "' by '"
                        + album.getMainArtist().getName() + "'");
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
    private Song searchSong(String songName, String artistName) {
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
            if (trackNode != null && !trackNode.isMissingNode()) {
                Song song;
                JsonNode songNameNode = trackNode.path("name");
                if (!songNameNode.isMissingNode()) {
                    song = new Song(songNameNode.asText());
                } else {
                    logger.error("song missing name: " + trackNode.toString());
                    return null;
                }
                JsonNode artistNameNode = trackNode.path("artist");
                if (!artistNameNode.isMissingNode()) {
                    song.setArtist(new Artist(artistNameNode.asText()));
                } else {
                    logger.debug("song missing artist: " + trackNode.toString());
                }
                return song;
            } else {
                logger.error("Problem searching song: " + response.toString());
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
    // TODO: save album name in song?
    // albumNode.path("title").asText()
    public Song getSong(String songName, String artistName) {
        if (songName == null) {
            logger.debug("Empty songName passed in getSong");
            return null;
        }
        Song song = this.searchSong(songName, artistName);
        if (song == null) {
            return null;
        }
        if (this.songs.contains(song)) {
            return this.getSong(song);
        }
        Map<String, String> params = Map.of("track", song.getName(), "artist", song.getArtist().getName());
        JsonNode response = query("track.getInfo", params);
        if (response != null) {
            JsonNode trackNode = response.path("track");
            if (!trackNode.isMissingNode()) {
                JsonNode songNameNode = trackNode.path("name");
                if (!songNameNode.isMissingNode()) {
                    song.setName(songNameNode.asText());
                } else {
                    logger.debug("song missing name: " + trackNode.toString());
                }
                JsonNode artistNode = trackNode.path("artist");
                if (!artistNode.isMissingNode()) {
                    JsonNode artistNameNode = artistNode.path("name");
                    if (!artistNameNode.isMissingNode()) {
                        Artist artist = this.getArtist(artistNameNode.asText());
                        if (artist != null) {
                            song.setArtist(artist);
                        }
                    } else {
                        logger.debug("song missing artist name: " + artistNode.toString());
                    }
                } else {
                    logger.debug("song missing artist: " + trackNode.toString());
                }
                JsonNode albumNode = trackNode.path("album");
                // TODO: currently unable to get image of any song
                if (!albumNode.isMissingNode()) {
                    JsonNode imageNode = artistNode.path("image");
                    if (imageNode.isArray() && !imageNode.isEmpty()) {
                        String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                        if (!coverImage.isEmpty()) {
                            song.setCoverImage(coverImage);
                        }
                    }
                } else {
                    // such a common message as not all songs are in albums
                    // logger.debug("song album missing cover image: " + trackNode.toString());
                }
                JsonNode durationNode = trackNode.path("duration");
                if (!durationNode.isMissingNode()) {
                    song.setDuration(durationNode.asLong(), ChronoUnit.MILLIS);
                } else {
                    logger.debug("song missing duration: " + trackNode.toString());
                }
                JsonNode urlNode = trackNode.path("url");
                if (!urlNode.isMissingNode()) {
                    // because for some reason they dont give the id nor the mbid
                    song.addId("lastfm", String.valueOf(urlNode.asText().hashCode()));
                } else {
                    logger.debug("song missing url: " + trackNode.toString());
                }
                this.songs.add(song);
                return song;
            } else {
                logger.error("Problem getting song: " + response.toString());
            }
        }
        logger.debug(
                "Could not find detailed information for song '" + song.getName() + "' by '"
                        + song.getArtist().getName() + "'");
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

    private Artist searchArtist(String artistname) {
        if (artistname == null) {
            logger.debug("Empty artistName parsed in searchArtist");
            return null;
        }
        Map<String, String> params = Map.of("artist", artistname, "limit", "1");
        JsonNode response = query("artist.search", params);
        if (response != null) {
            JsonNode artistNode = response.path("results").path("artistmatches").path("artist").get(0);
            if (artistNode != null && !artistNode.isMissingNode()) {
                JsonNode artistNameNode = artistNode.path("name");
                if (!artistNameNode.isMissingNode()) {
                    return new Artist(artistNameNode.asText());
                } else {
                    logger.debug("artist missing name: " + artistNode.toString());
                    return null;
                }
            } else {
                logger.debug("problem searching artist: " + response.toString());
            }
        }
        logger.debug("Could not find artist '" + artistname + "' in Library");
        return null;
    }

    public Artist getArtist(String artistName) {
        if (artistName == null) {
            logger.debug("Empty artistName passed in getArtist");
            return null;
        }
        Artist artist = this.searchArtist(artistName);
        if (artist == null) {
            return null;
        }
        if (this.artists.contains(artist)) {
            return this.getArtist(artist);
        }
        Map<String, String> params = Map.of("artist", artist.getName());
        JsonNode response = query("artist.getInfo", params);
        if (response != null) {
            JsonNode artistNode = response.path("artist");
            if (!artistNode.isMissingNode()) {
                JsonNode nameNode = artistNode.path("name");
                if (!nameNode.isMissingNode()) {
                    artist.setName(nameNode.asText());
                } else {
                    logger.debug("artist missing name:" + artistNode.toString());
                }
                JsonNode imageNode = artistNode.path("image");
                if (imageNode.isArray() && imageNode.size() > 0) {
                    String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                    if (coverImage != null && !coverImage.isEmpty()) {
                        artist.setCoverImage(coverImage);
                    }
                } else {
                    logger.debug("artist missing cover image: " + artistNode.toString());
                }
                JsonNode urlNode = artistNode.path("url");
                if (!urlNode.isMissingNode()) {
                    artist.addId("lastfm", String.valueOf(urlNode.asText().hashCode()));
                } else {
                    logger.debug("artist missing url:" + artistNode.toString());
                }
                this.artists.add(artist);
                return artist;
            }
        }
        logger.debug(
                "Could not find detailed information for artist '" + artist.getName());
        return null;
    }

    public LinkedHashSet<Album> getArtistAlbums(Artist artist) {
        if (artist == null) {
            logger.debug("Empty artist passed to getArtistAlbums");
            return null;
        }
        LinkedHashSet<Album> albums = new LinkedHashSet<>();
        Map<String, String> params = Map.of("artist", artist.getName());
        JsonNode response = query("artist.getTopAlbums", params);
        if (response != null) {
            JsonNode topAlbumsNode = response.path("topalbums");
            if (!topAlbumsNode.isMissingNode()) {
                JsonNode albumNodes = topAlbumsNode.path("album");
                if (albumNodes.isArray() && albumNodes.size() != 0) {
                    for (JsonNode albumNode : albumNodes) {
                        JsonNode albumNameNode = albumNode.path("name");
                        Album album;
                        if (!albumNameNode.isMissingNode()) {
                            album = this.getAlbum(albumNameNode.asText(), artist.getName());
                        } else {
                            logger.debug("album missing name: " + albumNode.toString());
                            continue;
                        }
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
                    logger.debug("problem parsing top albums: " + topAlbumsNode.toString());
                }
            } else {
                logger.debug("problem getting top albums: " + response.toString());
            }
        }
        if (albums.isEmpty()) {
            logger.debug("No albums found for artist '" + artist.getName() + "'");
            return null;
        }
        return albums;
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