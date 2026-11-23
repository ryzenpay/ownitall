package ryzen.ownitall.library;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import jakarta.ws.rs.client.WebTarget;

import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.WebTools;
import ryzen.superbeans.musicbrainz.Artists;
import ryzen.superbeans.musicbrainz.Commons;
import ryzen.superbeans.musicbrainz.Recordings;
import ryzen.superbeans.musicbrainz.Releases;
import ryzen.superbeans.musicbrainz.Search;

/**
 * <p>
 * MusicBrainz class.
 * </p>
 *
 * @author ryzen
 */
public class MusicBrainz extends Library {
    private static final Logger logger = new Logger(MusicBrainz.class);
    private static final String baseUrl = "https://musicbrainz.org/ws/2";
    private static final String coverArtUrl = "https://coverartarchive.org";
    /**
     * default MusicBrainz constructor
     * initializes all values and loads from cache
     */
    public MusicBrainz() {
        super();
    }

    /** {@inheritDoc} */
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
            logger.debug("null album passed in searchAlbum");
            return null;
        }
        if (album.getId("mbid") != null) {
            return album.getId("mbid");
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
        String foundId = ids.get(params.toString());
        if (foundId != null) {
            return foundId;
        }
        Search.ReleaseSearch response = this.query(this.searchTarget("release", params),
                Search.ReleaseSearch.class);
        if (response != null) {
            if (response.releases != null && !response.releases.isEmpty()) {
                String id = response.releases.get(0).id;
                ids.put(params.toString(), id);
                return id;
            } else {
                logger.debug("missing data in album '" + album.toString() + "' search result");
            }
        }
        if (!album.getName().equals(MusicTools.removeBrackets(album.getName()))) {
            logger.debug("Trying album '" + album.getName() + "' again with trimmed brackets");
            album.setName(MusicTools.removeBrackets(album.getName()));
            return searchAlbum(album);
        } else {
            logger.info("Unable to find album '" + album.toString() + "' in library");
            return null;
        }
    }

    /**
     * <p>
     * getAlbum.
     * </p>
     *
     * @param id a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Album} object
     * @throws java.lang.InterruptedException if any.
     */
    public Album getAlbum(String id) throws InterruptedException {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getAlbum");
            return null;
        }
        Album foundAlbum = albums.get(id);
        if (foundAlbum != null) {
            return foundAlbum;
        }
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("recordings");
        inclusions.add("artists");
        // check cache
        Releases.ReleaseLookup response = this.query(this.lookupTarget("release", id, inclusions),
                Releases.ReleaseLookup.class);
        if (response != null) {
            Album album = new Album(response.title);
            album.addId("mbid", response.id);
            if (response.cover_art_archive != null) {
                if (response.cover_art_archive.count != null && response.cover_art_archive.count > 1) {
                    URI albumCover = this.getCoverArt(response.id);
                    if (albumCover != null) {
                        album.setCoverImage(albumCover);
                    }
                }
            } else {
                logger.debug("Album '" + album.getName() + "' missing coverart");
            }
            if (response.artist_credit != null) {
                for (Commons.ArtistCredit credit : response.artist_credit) {
                    if (credit.artist == null) {
                        continue;
                    }
                    Artist artist = this.getArtist(credit.artist.id);
                    if (artist != null) {
                        album.addArtist(artist);
                    }
                }
            } else {
                logger.debug("album '" + album.getName() + "' missing artists");
            }
            if (response.media != null) {
                for (Releases.Media medium : response.media) {
                    if (medium.tracks == null) {
                        logger.debug("Album '" + album.getName() + "' disc missing songs");
                        continue;
                    }
                    for (Releases.Tracks track : medium.tracks) {
                        if (track.recording == null) {
                            continue;
                        }
                        Song song = this.getSong(track.recording.id);
                        if (song != null) {
                            album.addSong(song);
                        }
                    }
                }
            } else {
                logger.debug("Album '" + album.getName() + "' missing songs");
            }
            albums.put(id, album);
            return album;
        }
        logger.info("Unable to find Album: '" + id + "' in library");
        return null;
    }

    /** {@inheritDoc} */
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
            logger.debug("null song passed in searchRecordingSong");
            return null;
        }
        if (song.getId("mbid") != null) {
            return song.getId("mbid");
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("recording", song.getName());
        params.put("release", song.getName());
        if (song.getMainArtist().getName() != null) {
            String artistId = this.searchArtist(song.getMainArtist());
            if (artistId != null) {
                params.put("arid", artistId);
            } else {
                params.put("artist", song.getMainArtist().getName());
            }
        }
        if (song.getDuration() != null) {
            params.put("dur", String.valueOf(song.getDuration().toMillis()));
        }
        params.put("type", "Single");
        params.put("video", "false");
        String foundId = ids.get(params.toString());
        if (foundId != null) {
            return foundId;
        }
        Search.RecordingSearch response = this.query(this.searchTarget("recording", params),
                Search.RecordingSearch.class);
        if (response != null) {
            if (response.recordings != null && !response.recordings.isEmpty()) {
                String id = response.recordings.get(0).id;
                ids.put(params.toString(), id);
                return id;
            } else {
                logger.warn("Missing data while getting Song '" + song.getName() + "'");
            }
        }
        if (!song.getName().equals(MusicTools.removeBrackets(song.getName()))) {
            logger.debug("Trying song '" + song.getName() + "' again with trimmed brackets");
            song.setName(MusicTools.removeBrackets(song.getName()));
            return searchSong(song);
        } else {
            logger.info("Unable to find song '" + song.toString() + "' in library");
            return null;
        }
    }

    /**
     * <p>
     * getSong.
     * </p>
     *
     * @param id a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     * @throws java.lang.InterruptedException if any.
     */
    public Song getSong(String id) throws InterruptedException {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getSong");
            return null;
        }
        Song foundSong = songs.get(id);
        if (foundSong != null) {
            return foundSong;
        }
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("artists");
        inclusions.add("url-rels");
        inclusions.add("releases");
        Recordings.RecordingLookup response = this.query(this.lookupTarget("recording", id, inclusions),
                Recordings.RecordingLookup.class);
        if (response != null) {
            Song song = new Song(response.title);
            if (response.length != null) {
                song.setDuration(response.length.longValue(), ChronoUnit.MILLIS);
            }
            song.addId("mbid", response.id);
            if (response.artist_credit != null && !response.artist_credit.isEmpty()
                    && response.artist_credit.get(0).artist != null) {
                Artist artist = this.getArtist(response.artist_credit.get(0).artist.id);
                if (artist != null) {
                    song.addArtist(artist);
                }
            } else {
                logger.debug("Song '" + song.getName() + "' missing artists");
            }
            if (response.releases != null && !response.releases.isEmpty()) {
                URI songCover = this.getCoverArt(response.releases.get(0).id);
                if (songCover != null) {
                    song.setCoverImage(songCover);
                }
            }
            this.addStreamingIds(song, response.relations);
            songs.put(id, song);
            return song;
        }
        logger.info("Could not find Song '" + id + "' in library");
        return null;
    }

    /**
     * pull the platform ids out of a recording's url relations: the spotify
     * track or youtube video the musicbrainz editors name for this exact
     * recording beats the top hit of any platform search, and the spotify and
     * youtube integrations read both ids straight off the song
     *
     * @param song          - the song to attach the found ids to
     * @param relations     - the recording's url relations from the lookup
     */
    private void addStreamingIds(Song song, List<Recordings.Relations> relations) {
        if (relations == null) {
            return;
        }
        String youtube = null;
        for (Recordings.Relations relation : relations) {
            if (relation == null || relation.url == null || relation.url.resource == null) {
                continue;
            }
            String resource = relation.url.resource;
            if (song.getId("spotify") == null) {
                String spotifyId = this.matchingId(resource, "open.spotify.com", "/track/");
                if (spotifyId != null) {
                    song.addId("spotify", spotifyId);
                    logger.debug("MusicBrainz relations give spotify id '" + spotifyId + "' for '"
                            + song.getName() + "'");
                }
            }
            String videoId = this.youtubeVideoId(resource);
            if (videoId != null) {
                if (resource.contains("music.youtube.com")) {
                    youtube = videoId;
                } else if (youtube == null) {
                    youtube = videoId;
                }
            }
        }
        if (youtube != null) {
            song.addId("youtube", youtube);
            logger.debug("MusicBrainz relations give youtube id '" + youtube + "' for '" + song.getName() + "'");
        }
    }

    private String matchingId(String resource, String host, String marker) {
        if (!resource.contains(host) || !resource.contains(marker)) {
            return null;
        }
        String tail = resource.substring(resource.indexOf(marker) + marker.length());
        String id = tail.split("[?#]")[0];
        return id.isEmpty() ? null : id;
    }

    private String youtubeVideoId(String resource) {
        if (resource.contains("youtu.be/")) {
            String tail = resource.substring(resource.indexOf("youtu.be/") + "youtu.be/".length());
            String id = tail.split("[?#&]")[0];
            return id.isEmpty() ? null : id;
        }
        if (!resource.contains("youtube.com")) {
            return null;
        }
        String query = resource.contains("?") ? resource.substring(resource.indexOf('?') + 1) : "";
        for (String param : query.split("&")) {
            if (param.startsWith("v=")) {
                String id = param.substring(2).split("#")[0];
                return id.isEmpty() ? null : id;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Artist getArtist(Artist artist) throws InterruptedException {
        String id = this.searchArtist(artist);
        if (id == null) {
            return null;
        }
        return this.getArtist(id);
    }

    /**
     * <p>
     * searchArtist.
     * </p>
     *
     * @param artist a {@link ryzen.ownitall.classes.Artist} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException if any.
     */
    public String searchArtist(Artist artist) throws InterruptedException {
        if (artist == null) {
            logger.debug("null artist passed in searchArtist");
            return null;
        }
        if (artist.getId("mbid") != null) {
            return artist.getId("mbid");
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("artist", artist.getName());
        String foundId = ids.get(params.toString());
        if (foundId != null) {
            return foundId;
        }
        Search.ArtistSearch response = this.query(this.searchTarget("artist", params),
                Search.ArtistSearch.class);
        if (response != null) {
            if (response.artists != null && !response.artists.isEmpty()) {
                String id = response.artists.get(0).id;
                ids.put(params.toString(), id);
                return id;
            }
        }
        logger.debug("could not find artist '" + artist.getName() + "' in library");
        return null;
    }

    /**
     * <p>
     * getArtist.
     * </p>
     *
     * @param id a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Artist} object
     * @throws java.lang.InterruptedException if any.
     */
    public Artist getArtist(String id) throws InterruptedException {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getArtist");
            return null;
        }
        Artist foundArtist = artists.get(id);
        if (foundArtist != null) {
            return foundArtist;
        }
        LinkedHashSet<String> inclusions = new LinkedHashSet<>();
        inclusions.add("aliases");
        Artists.ArtistLookup response = this.query(this.lookupTarget("artist", id, inclusions),
                Artists.ArtistLookup.class);
        if (response != null) {
            Artist artist = new Artist(response.name);
            artist.addId("mbid", response.id);
            this.cacheArtistNames(id, response.name, response.aliases);
            artists.put(id, artist);
            return artist;
        }
        logger.info("Could not find Artist '" + id + "' in library");
        return null;
    }

    /**
     * seed the search cache with the artist's own name and every alias the
     * lookup carries, keyed exactly like searchArtist keys its params
     *
     * @param id      - the artist mbid
     * @param name    - the artist's primary name
     * @param aliases - the other spellings the lookup found, may be null
     */
    private void cacheArtistNames(String id, String name, List<Commons.Aliases> aliases) {
        if (name != null && !name.isEmpty()) {
            ids.putIfAbsent("{artist=" + name + "}", id);
        }
        if (aliases != null) {
            for (Commons.Aliases alias : aliases) {
                if (alias.name != null && !alias.name.isEmpty()) {
                    ids.putIfAbsent("{artist=" + alias.name + "}", id);
                }
            }
        }
    }

    private URI getCoverArt(String id) {
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in getCoverArt");
            return null;
        }
        WebTarget target = WebTools.target(coverArtUrl).path("release").path(id).path("front");
        URI redirect = WebTools.redirect(target);
        if (redirect != null) {
            return redirect;
        }
        logger.debug("No coverart found for: '" + id + "'");
        return null;
    }

    /**
     * a search of one type: every param becomes a field:"value" pair of one
     * lucene filter, in the order they are cached under
     *
     * @param type   - ex: recording
     * @param params - field names to filter on
     * @return a {@link jakarta.ws.rs.client.WebTarget} object
     */
    private WebTarget searchTarget(String type, LinkedHashMap<String, String> params) {
        if (type == null || type.isEmpty()) {
            logger.debug("null or empty type provided in searchTarget");
            return null;
        }
        if (params == null || params.isEmpty()) {
            logger.debug("no params provided in searchTarget");
            return null;
        }
        StringBuilder filter = new StringBuilder();
        for (String param : params.keySet()) {
            if (params.get(param) == null) {
                continue;
            }
            filter.append(param).append(':').append('"').append(params.get(param)).append('"');
        }
        WebTarget target = WebTools.target(baseUrl).path(type)
                .queryParam("query", filter.toString())
                .queryParam("fmt", "json")
                .queryParam("limit", 1);
        return target;
    }

    /**
     * a lookup of one id: the relations to ride along come as one '+' separated
     * list, which webtools encodes as %2B, the only form musicbrainz takes
     * (a comma is read as one long relation name and answered with a 400)
     *
     * @param type       - ex: release
     * @param id         - the musicbrainz id
     * @param inclusions - relations to ask for, may be empty
     * @return a {@link jakarta.ws.rs.client.WebTarget} object
     */
    private WebTarget lookupTarget(String type, String id, LinkedHashSet<String> inclusions) {
        if (type == null || type.isEmpty()) {
            logger.debug("null or empty type provided in lookupTarget");
            return null;
        }
        if (id == null || id.isEmpty()) {
            logger.debug("null or empty id provided in lookupTarget");
            return null;
        }
        WebTarget target = WebTools.target(baseUrl).path(type).path(id);
        if (inclusions != null && !inclusions.isEmpty()) {
            target = target.queryParam("inc", String.join("+", inclusions));
        }
        return target.queryParam("fmt", "json");
    }

}
