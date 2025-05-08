package ryzen.ownitall.library;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>
 * LastFM class.
 * </p>
 *
 * @author ryzen
 */
public class LastFM extends Library {
    private static final Logger logger = new Logger(LastFM.class);
    private final String baseUrl = "http://ws.audioscrobbler.com/2.0/";
    private final String defaultImg = "https://lastfm.freetls.fastly.net/i/u/300x300/2a96cbd8b46e442fc41c2b86b821562f.png";

    /**
     * default LastFM constructor
     * initializes all values and loads from cache
     *
     * @throws java.lang.InterruptedException - thrown when user interrupts
     */
    public LastFM() throws InterruptedException {
        super();
        if (Library.isCredentialsEmpty(LastFM.class)) {
            logger.debug("Empty LastFM credentials");
            throw new InterruptedException("Empty LastFM credentials");
        }
        this.queryDiff = 10;
    }

    /**
     * {@inheritDoc}
     *
     * get album from lastfm
     * https://www.last.fm/api/show/album.getInfo
     */
    @Override
    public Album getAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album passed in getAlbum");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("album", album.getName());
        if (album.getMainArtist() != null) {
            params.put("artist", album.getMainArtist().getName());
        } else {
            logger.debug(album.toString() + ": Album requires atleast one artist");
            return null;
        }
        params.put("autocorrect", "1");
        if (this.albums.containsKey(params.toString())) {
            return this.albums.get(params.toString());
        }
        JsonNode response = this.lastFMQuery("album.getInfo", this.queryBuilder(params));
        if (response != null) {
            JsonNode albumNode = response.path("album");
            if (!albumNode.isMissingNode()) {
                // new album here so the songs are the correction version + complete
                album = new Album(albumNode.path("name").asText());
                Artist artist = this.getArtist(new Artist(albumNode.path("artist").asText()));
                if (artist != null) {
                    album.addArtist(artist);
                }
                JsonNode idNode = albumNode.path("id");
                if (!idNode.isMissingNode()) {
                    album.addId("lastfm", idNode.asText());
                }
                JsonNode mbidNode = albumNode.path("mbid");
                if (!mbidNode.isMissingNode()) {
                    album.addId("mbid", mbidNode.asText());
                }
                JsonNode imageNode = albumNode.path("image");
                if (imageNode.isArray() && !imageNode.isEmpty()) {
                    String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                    if (coverImage != null && !coverImage.isEmpty()) {
                        if (!defaultImg.equals(coverImage)) {
                            album.setCoverImage(coverImage);
                        }
                    }
                } else {
                    logger.debug(album.toString() + ": album missing images: " + albumNode.toString());
                }
                JsonNode trackNodes = albumNode.path("tracks").path("track");
                if (trackNodes != null && trackNodes.isArray() && !trackNodes.isEmpty()) {
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
                            song.addArtist(artist);
                        }
                        song = this.getSong(song);
                        if (song != null) {
                            album.addSong(song);
                        }
                    }
                } else {
                    logger.debug(album.toString() + ": album missing tracks: " + albumNode.toString());
                }
                if (album.size() != 0) {
                    this.albums.put(params.toString(), album);
                    return album;
                }
            } else {
                logger.debug(album.toString() + ": album.getInfo missing album: " + response.toString());
            }
        }
        logger.info("Unable to find Album '" + album.toString() + "' in library ");
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * get song from lastfm
     */
    @Override
    public Song getSong(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("null song passed in getSong");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("track", song.getName());
        if (song.getMainArtist() != null) {
            params.put("artist", song.getMainArtist().getName());
        }
        params.put("autocorrect", "1");
        if (this.songs.containsKey(params.toString())) {
            return this.songs.get(params.toString());
        }
        JsonNode response = this.lastFMQuery("track.getInfo", this.queryBuilder(params));
        if (response != null) {
            JsonNode trackNode = response.path("track");
            if (!trackNode.isMissingNode()) {
                song.setName(trackNode.path("name").asText());
                JsonNode idNode = trackNode.path("id");
                if (!idNode.isMissingNode()) {
                    song.addId("lastfm", idNode.asText());
                }
                JsonNode mbidNode = trackNode.path("mbid");
                if (!mbidNode.isMissingNode()) {
                    song.addId("mbid", mbidNode.asText());
                }
                song.setDuration(trackNode.path("duration").asLong(), ChronoUnit.MILLIS);
                JsonNode artistNode = trackNode.path("artist");
                if (!artistNode.isMissingNode()) {
                    Artist artist = this.getArtist(new Artist(artistNode.path("name").asText()));
                    if (artist != null) {
                        song.addArtist(artist);
                    }
                } else {
                    logger.debug(song.toString() + ": track missing artist: " + trackNode.toString());
                }
                JsonNode albumNode = trackNode.path("album");
                if (!albumNode.isMissingNode()) {
                    song.setAlbumName(albumNode.path("title").asText());
                    JsonNode imageNode = albumNode.path("image");
                    if (imageNode.isArray() && !imageNode.isEmpty()) {
                        String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                        if (coverImage != null && !coverImage.isEmpty()) {
                            if (!defaultImg.equals(coverImage)) {
                                song.setCoverImage(coverImage);
                            }
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
        logger.info("Unable to find song '" + song.toString() + "' in library");
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * get artist from LastFM
     */
    @Override
    public Artist getArtist(Artist artist) throws InterruptedException {
        if (artist == null) {
            logger.debug("null artist passed in getArtist");
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
                artist.setName(artistNode.path("name").asText());
                JsonNode idNode = artistNode.path("id");
                if (!idNode.isMissingNode()) {
                    artist.addId("lastfm", idNode.asText());
                }
                JsonNode mbidNode = artistNode.path("mbid");
                if (!mbidNode.isMissingNode()) {
                    artist.addId("mbid", mbidNode.asText());
                }
                JsonNode imageNode = artistNode.path("image");
                if (imageNode.isArray() && !imageNode.isEmpty()) {
                    String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                    if (coverImage != null && !coverImage.isEmpty()) {
                        if (!defaultImg.equals(coverImage)) {
                            artist.setCoverImage(coverImage);
                        }
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
        logger.info("Unable to find artist " + artist.toString() + " in Library");
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * get all albums from artist
     */
    @Override
    public ArrayList<Album> getArtistAlbums(Artist artist) throws InterruptedException {
        if (artist == null) {
            logger.debug("null artist passed to getArtistAlbums");
            return null;
        }
        ArrayList<Album> albums = new ArrayList<>();
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
                        album.addArtist(artist);
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
            logger.info("Unable to find albums for '" + artist.getName() + "' in Library");
            return null;
        }
        return albums;
    }

    /**
     * build lastfm query in their format
     * 
     * @param params - linkedhashmap with param:value
     * @return - string of url to query
     */
    private String queryBuilder(LinkedHashMap<String, String> params) {
        try {
            StringBuilder builder = new StringBuilder();
            for (String param : params.keySet()) {
                builder.append('&').append(param).append('=')
                        .append(URLEncoder.encode(params.get(param), StandardCharsets.UTF_8.toString()));
            }
            return builder.toString();
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to build a query", e);
            return null;
        }
    }

    /**
     * query lastfm with the build query and the request type
     * 
     * @param type  - ex: getTrack
     * @param query - built string query using queryBuilder
     * @return - JsonNode of lastfm response
     * @throws InterruptedException - thrown when user interrupts
     */
    private JsonNode lastFMQuery(String type, String query) throws InterruptedException {
        if (type == null || type.isEmpty()) {
            logger.debug("null or empty type provided in lastFMQuery");
            return null;
        }
        if (query == null || query.isEmpty()) {
            logger.debug("null or empty query provided in lastFMQuery");
            return null;
        }
        try {
            StringBuilder urlBuilder = new StringBuilder(this.baseUrl);
            urlBuilder.append("?method=").append(type);
            urlBuilder.append("&api_key=").append(Credentials.lastFMApiKey);
            urlBuilder.append("&format=json");
            urlBuilder.append(query);
            URI url = new URI(urlBuilder.toString());
            JsonNode rootNode = this.query(url);
            if (rootNode != null) {
                return rootNode;
            }
        } catch (URISyntaxException e) {
            logger.error("Exception querying LastFM", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * barebones method of handling library query response errors
     */
    @Override
    protected void queryErrorHandle(int code, String message) {
        switch (code) {
            case 2:
                logger.error("Invalid service - This service does not exist", new Exception());
                break;
            case 3:
                logger.error("Invalid Method - No method with that name in this package", new Exception());
                break;
            case 4:
                logger.error("Authentication Failed - You do not have permissions to access the service",
                        new Exception());
                break;
            case 5:
                logger.error("Invalid format - This service doesn't exist in that format", new Exception());
                break;
            case 6:
                logger.error("Invalid parameters - Your request is missing a required parameter", new Exception());
                break;
            case 7:
                logger.error("Invalid resource specified", new Exception());
                break;
            case 8:
                logger.error("Operation failed - Something else went wrong", new Exception());
                break;
            case 9:
                logger.error("Invalid session key - Please re-authenticate", new Exception());
                break;
            case 10:
                logger.error("Invalid API key - You must be granted a valid key by last.fm", new Exception());
                break;
            case 11:
                logger.error("Service Offline - This service is temporarily offline. Try again later", new Exception());
                break;
            case 13:
                logger.error("Invalid method signature supplied", new Exception());
                break;
            case 16:
                logger.error("There was a temporary error processing your request. Please try again", new Exception());
                break;
            case 26:
                logger.error("Suspended API key - Access for your account has been suspended, please contact Last.fm",
                        new Exception());
                break;
            case 29:
                logger.error("Rate limit exceeded - Your IP has made too many requests in a short period",
                        new Exception());
                break;
            default:
                logger.error("Unknown error: " + message, new Exception());
                break;
        }
    }
}
