package ryzen.ownitall.integrations;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.client.WebTarget;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.integrations.Integration.Discover;
import ryzen.ownitall.integrations.Integration.Import;
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.WebTools.QueryException;
import ryzen.superbeans.lastfm.Albums;
import ryzen.superbeans.lastfm.Artists;
import ryzen.superbeans.lastfm.Commons;
import ryzen.superbeans.lastfm.Tracks;
import ryzen.superbeans.lastfm.Users;

/**
 * <p>
 * LastFM integration, unlike the library of the same name this reads the user's
 * own last.fm profile: discoveries, loved tracks and top albums. last.fm is
 * read only here, its scrobble history (fed from your platforms through
 * something like multi-scrobbler) is the taste data this integration works from
 * and the collection is where keeping discoveries lands, syncing that to your
 * platforms is the other integrations' job
 * </p>
 *
 * @author ryzen
 */
public class LastFM implements Import, Discover {
    private static final Logger logger = new Logger(LastFM.class);
    private static final String baseUrl = "http://ws.audioscrobbler.com/2.0";
    private static final Map<String, String> headers = Map.of("User-Agent",
            "OwnItAll/1.0 (https://github.com/ryzenpay/ownitall)");
    private static final int pageSize = 50;
    private static final int seedArtists = 5;
    private static final int similarLimit = 10;
    private static final int discoveryArtists = 10;
    private static final int tracksPerArtist = 5;

    /**
     * Default LastFM integration constructor
     *
     * @throws ryzen.ownitall.Settings.MissingSettingException if any.
     */
    public LastFM() throws MissingSettingException {
        if (Settings.load().isGroupEmpty(LastFM.class)) {
            logger.debug("Empty LastFM integration credentials");
            throw new MissingSettingException(LastFM.class);
        }
    }

    /**
     * {@inheritDoc}
     *
     * last.fm has no liked songs, the loved tracks list is the closest thing a
     * profile carries
     */
    @Override
    public LikedSongs getLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        int page = 1;
        try (IPIterator<?> pagination = IPIterator.manual("Loved Tracks", -1)) {
            while (true) {
                Users.UserGetlovedtracks response = this.query("user.getLovedTracks",
                        Map.of("limit", String.valueOf(pageSize), "page", String.valueOf(page)),
                        Users.UserGetlovedtracks.class);
                if (response == null || response.lovedtracks == null || response.lovedtracks.track == null
                        || response.lovedtracks.track.isEmpty()) {
                    break;
                }
                for (Tracks.Track track : response.lovedtracks.track) {
                    likedSongs.addSong(this.getSong(track));
                    pagination.step(track.name);
                }
                if (response.lovedtracks.track.size() < pageSize) {
                    break;
                }
                page++;
            }
        }
        return likedSongs;
    }

    /**
     * {@inheritDoc}
     *
     * last.fm saves no albums, the top albums of the profile are taken as the
     * closest thing, their full track lists come from album.getInfo
     */
    @Override
    public ArrayList<Album> getAlbums() throws InterruptedException {
        ArrayList<Album> albums = new ArrayList<>();
        int page = 1;
        try (IPIterator<?> pagination = IPIterator.manual("Top Albums", -1)) {
            while (true) {
                Users.UserGettopalbums response = this.query("user.getTopAlbums",
                        Map.of("limit", String.valueOf(pageSize), "page", String.valueOf(page)),
                        Users.UserGettopalbums.class);
                if (response == null || response.topalbums == null || response.topalbums.album == null
                        || response.topalbums.album.isEmpty()) {
                    break;
                }
                for (Artists.TopalbumsAlbum topAlbum : response.topalbums.album) {
                    Album album = this.getAlbum(topAlbum.mbid, topAlbum.name,
                            topAlbum.artist == null ? null : topAlbum.artist.name);
                    if (album != null) {
                        albums.add(album);
                    }
                    pagination.step(topAlbum.name);
                }
                if (response.topalbums.album.size() < pageSize) {
                    break;
                }
                page++;
            }
        }
        return albums;
    }

    /** {@inheritDoc} */
    @Override
    public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        if (albumName == null || albumArtistName == null) {
            logger.debug("null album name or artist provided in getAlbum");
            return null;
        }
        Album album = new Album(albumName);
        album.addArtist(new Artist(albumArtistName));
        if (albumId != null && !albumId.isEmpty()) {
            album.addId("mbid", albumId);
        }
        Albums.AlbumGetinfo response = this.query("album.getInfo",
                Map.of("artist", albumArtistName, "album", albumName, "autocorrect", "1"),
                Albums.AlbumGetinfo.class);
        Albums.Album foundAlbum = response == null ? null : response.album;
        if (foundAlbum == null) {
            logger.debug("album.getInfo missing album '" + albumName + "'");
            return album;
        }
        String coverImage = this.coverImage(foundAlbum.image);
        if (coverImage != null) {
            album.setCoverImage(coverImage);
        }
        if (foundAlbum.tracks != null && foundAlbum.tracks.track != null) {
            for (Tracks.Track track : foundAlbum.tracks.track) {
                Song song = new Song(track.name);
                if (track.artist != null && track.artist.name != null) {
                    song.addArtist(new Artist(track.artist.name));
                }
                if (track.duration != null) {
                    try {
                        song.setDuration(Long.parseLong(track.duration), ChronoUnit.MILLIS);
                    } catch (NumberFormatException e) {
                        logger.debug(song + ": unparseable track duration '" + track.duration + "'");
                    }
                }
                album.addSong(song);
            }
        } else {
            logger.debug("album '" + albumName + "' has no track list");
        }
        return album;
    }

    /**
     * {@inheritDoc}
     *
     * last.fm has no playlists
     */
    @Override
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        logger.warn("LastFM has no playlists to import");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException {
        logger.warn("LastFM has no playlists to import");
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * last.fm removed its personal recommendations endpoint, so this rebuilds
     * the old radio model from live endpoints: the profile's most scrobbled
     * artists fan out through artist.getSimilar and the artists the collection
     * does not know yet contribute their artist.getTopTracks
     */
    @Override
    public Playlist getDiscoveries(int limit) throws InterruptedException {
        Playlist playlist = new Playlist("lastfm discoveries");
        if (limit < 1) {
            limit = 1;
        }
        ArrayList<String> seeds = this.getDiscoverySeeds();
        if (seeds.isEmpty()) {
            return playlist;
        }
        LikedSongs likedSongs = Collection.getLikedSongs();
        try (IPIterator<?> pb = IPIterator.manual("Discoveries", seeds.size())) {
            for (String artist : seeds) {
                Artists.Toptracks response = this.query("artist.getTopTracks",
                        Map.of("artist", artist, "limit", String.valueOf(tracksPerArtist)),
                        Artists.Toptracks.class);
                if (response != null && response.track != null) {
                    for (Artists.ToptracksTrack track : response.track) {
                        Song song = this.getSong(track.name, track.artist, track.image, null);
                        if (likedSongs != null && likedSongs.contains(song)) {
                            continue;
                        }
                        playlist.addSong(song);
                        if (playlist.getCoverImage() == null) {
                            String coverImage = this.coverImage(track.image);
                            if (coverImage != null) {
                                playlist.setCoverImage(coverImage);
                            }
                        }
                        if (playlist.size() >= limit) {
                            break;
                        }
                    }
                }
                pb.step(artist);
                if (playlist.size() >= limit) {
                    break;
                }
            }
        }
        return playlist;
    }

    /**
     * the artists to pull discovery tracks from: artists similar to the
     * profile's most scrobbled artists that the collection has no songs from
     * yet, the same fans also like signal last.fm's removed recommendations
     * endpoint used to serve directly
     *
     * @return - artist names to pull top tracks from, empty if the profile has
     *         no scrobble history to fan out from
     * @throws java.lang.InterruptedException if any.
     */
    private ArrayList<String> getDiscoverySeeds() throws InterruptedException {
        ArrayList<String> seeds = new ArrayList<>();
        TopArtistsResponse response = this.query("user.getTopArtists",
                Map.of("limit", String.valueOf(seedArtists)), TopArtistsResponse.class);
        if (response == null || response.topartists == null || response.topartists.artist == null) {
            logger.warn("LastFM returned no top artists, does the profile have enough scrobbles?");
            return seeds;
        }
        LinkedHashSet<String> known = this.collectionArtistNames();
        ArrayList<String> fanArtists = new ArrayList<>();
        for (Artists.Artist topArtist : response.topartists.artist) {
            if (topArtist.name == null) {
                continue;
            }
            known.add(topArtist.name.toLowerCase(Locale.ROOT));
            fanArtists.add(topArtist.name);
        }
        for (String fanArtist : fanArtists) {
            Artists.ArtistGetsimilar similar = this.query("artist.getSimilar",
                    Map.of("artist", fanArtist, "limit", String.valueOf(similarLimit)),
                    Artists.ArtistGetsimilar.class);
            if (similar == null || similar.similarartists == null || similar.similarartists.artist == null) {
                continue;
            }
            for (Artists.Artist similarArtist : similar.similarartists.artist) {
                if (similarArtist.name == null || !known.add(similarArtist.name.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                seeds.add(similarArtist.name);
                if (seeds.size() >= discoveryArtists) {
                    return seeds;
                }
            }
        }
        if (seeds.isEmpty()) {
            logger.warn("LastFM found no unknown similar artists, the collection knows them all");
        }
        return seeds;
    }

    /**
     * lower case artist names the collection already owns songs from, used to
     * keep discoveries away from artists the user has already heard of
     *
     * @return - a set of lower case artist names
     */
    private LinkedHashSet<String> collectionArtistNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        LikedSongs likedSongs = Collection.getLikedSongs();
        if (likedSongs == null) {
            return names;
        }
        for (Song song : likedSongs.getSongs()) {
            Artist artist = song.getMainArtist();
            if (artist != null && artist.getName() != null) {
                names.add(artist.getName().toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private Song getSong(Tracks.Track track) {
        return this.getSong(track.name, track.artist, track.image, null);
    }

    private Song getSong(String name, Artists.Artist artist, List<Commons.Image> images, String duration) {
        Song song = new Song(name);
        if (artist != null && artist.name != null) {
            song.addArtist(new Artist(artist.name));
        }
        if (duration != null) {
            try {
                song.setDuration(Long.parseLong(duration), ChronoUnit.MILLIS);
            } catch (NumberFormatException e) {
                logger.debug("song '" + name + "': unparseable track duration '" + duration + "'");
            }
        }
        String coverImage = this.coverImage(images);
        if (coverImage != null) {
            song.setCoverImage(coverImage);
        }
        return song;
    }

    /**
     * lastfm orders an image list small to large, so the last entry is the biggest
     *
     * @param images - list of sized image urls
     * @return - the cover url or null if there is none
     */
    private String coverImage(List<Commons.Image> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        String coverImage = images.get(images.size() - 1)._text;
        if (coverImage == null || coverImage.isEmpty()) {
            return null;
        }
        return coverImage;
    }

    /**
     * state a lastfm request: the method it asks for, the credentials and the
     * profile every call needs, and the params of the call itself
     *
     * @param <T>          the response bean type
     * @param type         - ex: user.getLovedTracks
     * @param params       - the extra params of that method, may be empty
     * @param responseType - the response bean the whole response binds to
     * @return - bound response or null if error
     * @throws java.lang.InterruptedException - when the user interrupts
     */
    private <T> T query(String type, Map<String, String> params, Class<T> responseType) throws InterruptedException {
        WebTarget target = WebTools.target(baseUrl)
                .queryParam("method", type)
                .queryParam("api_key", Settings.lastFMApiKey)
                .queryParam("username", Settings.lastFMUsername)
                .queryParam("format", "json");
        if (params != null) {
            for (String param : params.keySet()) {
                target = target.queryParam(param, params.get(param));
            }
        }
        try {
            return WebTools.get(target, headers, responseType);
        } catch (QueryException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw (InterruptedException) e.getCause();
            }
            logger.warn("Received error code while querying '" + type + "': " + e.getMessage());
            return null;
        }
    }

    // the lastfm root envelope superbeans 0.5.7 has no bean for yet:
    // user.getTopArtists, shaped like the Users.Lovedtracks and
    // UserGettopalbums envelopes it does have
    public static class TopArtistsResponse {
        public Topartists topartists;
    }

    public static class Topartists {
        public List<Artists.Artist> artist;
        @JsonProperty("@attr")
        public Commons.Attr _attr;
    }
}
