package ryzen.ownitall.library;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;

import com.fasterxml.jackson.databind.JsonNode;

public class LastFM extends Library {
    private static final Logger logger = LogManager.getLogger(LastFM.class);
    private static final Credentials credentials = Credentials.load();
    private final String baseUrl = "http://ws.audioscrobbler.com/2.0/";

    /**
     * default LastFM constructor
     * initializes all values and loads from cache
     */
    public LastFM() throws InterruptedException {
        super();
        if (credentials.lastFMIsEmpty()) {
            credentials.setLastFMCredentials();
        }
        this.queryDiff = 10;
    }

    // https://www.last.fm/api/show/album.getInfo
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
                album = new Album(albumNode.path("name").asText());
                Artist artist = this.getArtist(new Artist(albumNode.path("artist").asText()));
                if (artist != null) {
                    album.addArtist(artist);
                }
                JsonNode idNode = albumNode.path("id");
                if (!idNode.isMissingNode()) {
                    album.addId("lastfm", String.valueOf(idNode.asInt()));
                }
                JsonNode mbidNode = albumNode.path("mbid");
                if (!mbidNode.isMissingNode()) {
                    album.addId("mbid", mbidNode.asText());
                }
                JsonNode imageNode = albumNode.path("image");
                if (imageNode.isArray() && !imageNode.isEmpty()) {
                    String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                    if (coverImage != null && !coverImage.isEmpty()) {
                        album.setCoverImage(coverImage);
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
                            song.setArtist(artist);
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

    @Override
    public Song getSong(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("null song passed in getSong");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("track", song.getName());
        if (song.getArtist() != null) {
            params.put("artist", song.getArtist().getName());
        }
        params.put("autocorrect", "1");
        if (this.songs.containsKey(params.toString())) {
            return this.songs.get(params.toString());
        }
        JsonNode response = this.lastFMQuery("track.getInfo", this.queryBuilder(params));
        if (response != null) {
            JsonNode trackNode = response.path("track");
            if (!trackNode.isMissingNode()) {
                song = new Song(trackNode.path("name").asText());
                JsonNode idNode = trackNode.path("id");
                if (!idNode.isMissingNode()) {
                    song.addId("lastfm", String.valueOf(idNode.asInt()));
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
                        song.setArtist(artist);
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
                            song.setCoverImage(coverImage);
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
                artist = new Artist(artistNode.path("name").asText());
                JsonNode idNode = artistNode.path("id");
                if (!idNode.isMissingNode()) {
                    artist.addId("lastfm", String.valueOf(idNode.asInt()));
                }
                JsonNode mbidNode = artistNode.path("mbid");
                if (!mbidNode.isMissingNode()) {
                    artist.addId("mbid", mbidNode.asText());
                }
                JsonNode imageNode = artistNode.path("image");
                if (imageNode.isArray() && !imageNode.isEmpty()) {
                    String coverImage = imageNode.get(imageNode.size() - 1).path("#text").asText();
                    if (coverImage != null && !coverImage.isEmpty()) {
                        artist.setCoverImage(coverImage);
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

    private String queryBuilder(LinkedHashMap<String, String> params) {
        try {
            StringBuilder builder = new StringBuilder();
            for (String param : params.keySet()) {
                builder.append('&').append(param).append('=')
                        .append(URLEncoder.encode(params.get(param), StandardCharsets.UTF_8.toString()));
            }
            return builder.toString();
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to build a query: " + e);
            return null;
        }
    }

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
            urlBuilder.append("&api_key=").append(credentials.getLastFMApiKey());
            urlBuilder.append("&format=json");
            urlBuilder.append(query);
            URI url = new URI(urlBuilder.toString());
            JsonNode rootNode = this.query(url);
            if (rootNode != null) {
                return rootNode;
            }
        } catch (URISyntaxException e) {
            logger.error("Exception querying LastFM: " + e);
        }
        return null;
    }

    /**
     * barebones method of handling library query response errors
     * 
     * @param code    - error code thrown
     * @param message - additional message
     */
    @Override
    protected void queryErrorHandle(int code, String message) {
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