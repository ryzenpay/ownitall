package ryzen.ownitall.library;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Library;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;

import com.fasterxml.jackson.databind.JsonNode;

public class MusicBrainz extends Library {
    private static final Logger logger = LogManager.getLogger(MusicBrainz.class);
    private final String musicBeeUrl = "https://musicbrainz.org/ws/2/";
    private final String coverArtUrl = "https://coverartarchive.org/";

    /**
     * default MusicBrainz constructor
     * initializes all values and loads from cache
     */
    public MusicBrainz() {
        super();
        this.queryDiff = 1000;
    }

    @Override
    public Album getAlbum(Album album) throws InterruptedException {
        String id = this.searchAlbum(album);
        if (id == null) {
            return null;
        }
        return this.getAlbum(id);
    }

    private String searchAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("Empty album passed in searchAlbum");
            return null;
        }
        if (album.getId("id") != null) {
            return album.getId("id");
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("release", album.getName());
        if (album.getMainArtist() != null) {
            String artistId = this.searchArtist(album.getMainArtist());
            if (artistId != null) {
                params.put("arid", artistId);
            } else {
                params.put("artist", album.getMainArtist().getName());
            }
        }
        params.put("primarytype", "Album");
        String foundId = this.ids.get(params.toString());
        if (foundId != null) {
            return foundId;
        }
        JsonNode response = this.musicBeeQuery("release", this.searchQueryBuilder(params));
        if (response != null) {
            JsonNode albumNode = response.path("releases").get(0);
            if (albumNode != null) {
                String id = albumNode.path("id").asText();
                this.ids.put(params.toString(), id);
                return id;
            } else {
                logger.debug("missing data in album search result " + response.toString());
            }
        }
        logger.info("Could not find Album '" + album.getName() + "' in Library");
        return null;
    }

    public Album getAlbum(String id) throws InterruptedException {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getAlbum");
            return null;
        }
        Album foundAlbum = this.albums.get(id);
        if (foundAlbum != null) {
            return foundAlbum;
        }
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("recordings");
        inclusions.add("artists");
        // check cache
        JsonNode response = this.musicBeeQuery("release", this.directQueryBuilder(id, inclusions));
        if (response != null) {
            Album album = new Album(response.path("title").asText());
            album.addId("id", response.path("id").asText());
            JsonNode coverArt = response.path("cover-art-archive");
            if (!coverArt.isMissingNode()) {
                if (coverArt.path("count").asInt() > 1) {
                    URI albumCover = this.getCoverArt(response.path("id").asText());
                    if (albumCover != null) {
                        album.setCoverImage(albumCover);
                    }
                }
            } else {
                logger.debug("Album missing coverart: '" + response.toString());
            }
            JsonNode artistNodes = response.path("artist-credit");
            if (artistNodes.isArray()) {
                for (JsonNode rootArtistNode : artistNodes) {
                    JsonNode artistNode = rootArtistNode.path("artist");
                    Artist artist = this.getArtist(artistNode.path("id").asText());
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
            this.albums.put(id, album);
            return album;
        }
        logger.info("Unable to find Album: '" + id + "' in library");
        return null;
    }

    @Override
    public Song getSong(Song song) throws InterruptedException {
        String id = this.searchSong(song);
        if (id == null) {
            return null;
        }
        return this.getSong(id);
    }

    private String searchSong(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("Empty song passed in searchRecordingSong");
            return null;
        }
        if (song.getId("id") != null) {
            return song.getId("id");
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("recording", song.getName());
        params.put("release", song.getName());
        if (song.getArtist().getName() != null) {
            String artistId = this.searchArtist(song.getArtist());
            if (artistId != null) {
                params.put("arid", artistId);
            } else {
                params.put("artist", song.getArtist().getName());
            }
        }
        if (song.getDuration() != null) {
            params.put("dur", String.valueOf(song.getDuration().toMillis()));
        }
        params.put("type", "Single");
        params.put("video", "false");
        String foundId = this.ids.get(params.toString());
        if (foundId != null) {
            return foundId;
        }
        JsonNode response = this.musicBeeQuery("recording", this.searchQueryBuilder(params));
        if (response != null) {
            JsonNode trackNode = response.path("recordings").get(0);
            if (trackNode != null) {
                String id = trackNode.path("id").asText();
                this.ids.put(params.toString(), id);
                return id;
            } else {
                logger.error("Missing data while getting Song: " + response.toString());
            }
        }
        logger.info("Could not find song '" + song.getName() + "' in library");
        return null;
    }

    public Song getSong(String id) throws InterruptedException {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getSong");
            return null;
        }
        Song foundSong = this.songs.get(id);
        if (foundSong != null) {
            return foundSong;
        }
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("artists");
        inclusions.add("url-rels");
        inclusions.add("releases");
        JsonNode response = this.musicBeeQuery("recording", this.directQueryBuilder(id, inclusions));
        if (response != null) {
            Song song = new Song(response.path("title").asText());
            song.setDuration(response.path("length").asLong(), ChronoUnit.MILLIS);
            song.addId("id", response.path("id").asText());
            JsonNode artistNode = response.path("artist-credit").get(0).path("artist");
            if (artistNode != null && !artistNode.isMissingNode()) {
                Artist artist = this.getArtist(artistNode.path("id").asText());
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
            this.songs.put(id, song);
            return song;
        }
        logger.info("Could not find Song '" + id + "' in library");
        return null;
    }

    @Override
    public Artist getArtist(Artist artist) throws InterruptedException {
        String id = this.searchArtist(artist);
        if (id == null) {
            return null;
        }
        return this.getArtist(id);
    }

    public String searchArtist(Artist artist) throws InterruptedException {
        if (artist == null) {
            logger.debug("Empty artist passed in searchArtist");
            return null;
        }
        if (artist.getId("id") != null) {
            return artist.getId("id");
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("artist", artist.getName());
        String foundId = this.ids.get(params.toString());
        if (foundId != null) {
            return foundId;
        }
        JsonNode response = this.musicBeeQuery("artist", this.searchQueryBuilder(params));
        if (response != null) {
            JsonNode artistNode = response.path("artists").get(0);
            if (artistNode != null) {
                String id = artistNode.path("id").asText();
                this.ids.put(params.toString(), id);
                return id;
            }
        }
        logger.debug("could not find artist '" + artist.getName() + "' in library");
        return null;
    }

    public Artist getArtist(String id) throws InterruptedException {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getArtist");
            return null;
        }
        Artist foundArtist = this.artists.get(id);
        if (foundArtist != null) {
            return foundArtist;
        }
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("releases");
        JsonNode response = this.musicBeeQuery("artist", this.directQueryBuilder(id, inclusions));
        if (response != null) {
            Artist artist = new Artist(response.path("name").asText());
            artist.addId("id", response.path("id").asText());
            this.artists.put(id, artist);
            return artist;
        }
        logger.info("Could not find Artist '" + id + "' in library");
        return null;
    }

    private URI getCoverArt(String id) {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getCoverArt");
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder(this.coverArtUrl);
        urlBuilder.append("release").append('/');
        urlBuilder.append(id).append('/');
        urlBuilder.append("front");
        try {
            URI url = new URI(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.connect();
            String redirectUrl = connection.getHeaderField("Location");
            connection.disconnect();
            if (redirectUrl != null) {
                return new URI(redirectUrl);
            }
        } catch (IOException | URISyntaxException e) {
            logger.debug("Exception while getting coverArt: " + e);
        }
        logger.debug("No coverart found for: '" + id + "'");
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

    private JsonNode musicBeeQuery(String type, String query) throws InterruptedException {
        if (type == null || type.isEmpty()) {
            logger.debug("null or empty type provided in musicBeeQuery");
            return null;
        }
        if (query == null || query.isEmpty()) {
            logger.debug("null or empty query provided in musicBeeQuery");
            return null;
        }
        try {
            StringBuilder urlBuilder = new StringBuilder(this.musicBeeUrl);
            urlBuilder.append(type);
            urlBuilder.append(query);
            URI url = new URI(urlBuilder.toString());
            JsonNode rootNode = this.query(url);
            if (rootNode != null) {
                return rootNode;
            }
        } catch (URISyntaxException e) {
            logger.error("Error querying MusicBee: " + e);
        }
        return null;
    }
}