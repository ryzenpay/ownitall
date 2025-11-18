package ryzen.ownitall.library;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Id;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.exceptions.MissingSettingException;

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
    private static final String baseUrl = "http://ws.audioscrobbler.com/2.0/";
    private static final String defaultImg = "https://lastfm.freetls.fastly.net/i/u/300x300/2a96cbd8b46e442fc41c2b86b821562f.png";

    /**
     * default LastFM constructor
     * initializes all values and loads from cache
     *
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     */
    public LastFM() throws MissingSettingException {
        super();
        if (Settings.load().isGroupEmpty(LastFM.class)) {
            logger.debug("Empty LastFM credentials");
            throw new MissingSettingException(LastFM.class);
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
            Artist artist = this.getArtist(album.getMainArtist());
            if (artist != null) {
                params.put("artist", artist.getName());
            }
        } else {
            logger.debug(album.toString() + ": Album requires atleast one artist");
            return null;
        }
        params.put("autocorrect", "1");
        if (albums.containsKey(params.toString())) {
            return albums.get(params.toString());
        }
        JsonNode response = this.query("album.getInfo", this.queryBuilder(params));
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
                    // only needed for albums
                    if (!mbidNode.asText().isEmpty()) {
                        album.addId("mbid", mbidNode.asText());
                    }
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
                    logger.debug(album.toString() + ": album missing images");
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
                            logger.debug(album.toString() + " track missing artist");
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
                    logger.debug(album.toString() + ": album missing tracks");
                }
                if (!album.isEmpty()) {
                    albums.put(params.toString(), album);
                    return album;
                }
            } else {
                logger.debug(album.toString() + ": album.getInfo missing album");
            }
        }
        if (!album.getName().equals(MusicTools.removeBrackets(album.getName()))) {
            logger.debug("Trying album '" + album.getName() + "' again with trimmed brackets");
            album.setName(MusicTools.removeBrackets(album.getName()));
            return getAlbum(album);
        }
        logger.info("Unable to find album '" + album.toString() + "' in library");
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
            Artist artist = this.getArtist(song.getMainArtist());
            if (artist != null) {
                params.put("artist", artist.getName());
            }
        }
        params.put("autocorrect", "1");
        if (songs.containsKey(params.toString())) {
            return songs.get(params.toString());
        }
        JsonNode response = this.query("track.getInfo", this.queryBuilder(params));
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
                    logger.debug(song.toString() + ": track missing artist");
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
                        logger.debug(song.toString() + ": song missing images");
                    }
                } else {
                    logger.debug(song.toString() + ": track missing album");
                }
                songs.put(params.toString(), song);
                return song;
            } else {
                logger.debug(song.toString() + ": track.getInfo missing track");
            }
        }
        if (!song.getName().equals(MusicTools.removeBrackets(song.getName()))) {
            logger.debug("Trying song '" + song.getName() + "' again with trimmed brackets");
            song.setName(MusicTools.removeBrackets(song.getName()));
            return getSong(song);
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
        if (artists.containsKey(params.toString())) {
            return artists.get(params.toString());
        }
        JsonNode response = this.query("artist.getInfo", this.queryBuilder(params));
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
                    logger.debug(artist.toString() + ": artist missing images");
                }
                artists.put(params.toString(), artist);
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
        Artist foundArtist = this.getArtist(artist);
        if (foundArtist != null) {
            artist = foundArtist;
        }
        ArrayList<Album> albums = new ArrayList<>();
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("artist", artist.getName());
        JsonNode response = this.query("artist.getTopAlbums", this.queryBuilder(params));
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
                            albums.add(album);
                        }
                    }
                } else {
                    logger.debug(artist.toString() + ": missing data in artist albums");
                }
            } else {
                logger.debug(artist.toString() + ": missing data getting top albums");
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
    private JsonNode query(String type, String query) throws InterruptedException {
        if (type == null || type.isEmpty()) {
            logger.debug("null or empty type provided in lastFMQuery");
            return null;
        }
        if (query == null || query.isEmpty()) {
            logger.debug("null or empty query provided in lastFMQuery");
            return null;
        }
        try {
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            urlBuilder.append("?method=").append(type);
            urlBuilder.append("&api_key=").append(Settings.lastFMApiKey);
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
}