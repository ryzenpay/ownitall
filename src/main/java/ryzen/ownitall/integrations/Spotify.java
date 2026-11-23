package ryzen.ownitall.integrations;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.integrations.Integration.Export;
import ryzen.ownitall.integrations.Integration.Import;
import ryzen.ownitall.integrations.Integration.Sync;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.OAuth;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.WebTools.AuthenticationException;
import ryzen.ownitall.util.WebTools.QueryException;
import ryzen.superbeans.spotify.Albums;
import ryzen.superbeans.spotify.Artists;
import ryzen.superbeans.spotify.Commons;
import ryzen.superbeans.spotify.Episodes;
import ryzen.superbeans.spotify.Playlists;
import ryzen.superbeans.spotify.Tracks;
import ryzen.superbeans.spotify.Users;

/**
 * <p>
 * Spotify class.
 * </p>
 *
 * @author ryzen
 */
public class Spotify implements Import, Export, Sync {
    private static final Logger logger = new Logger(Spotify.class);
    private static final Library library = Library.load();
    private static final String baseUrl = "https://api.spotify.com/v1";
    private static final String authUrl = "https://accounts.spotify.com";
    private static final String redirectUri = "http://localhost:8081/oauth";
    // read and write scope
    private final String scope = "playlist-read-private,playlist-read-collaborative,user-library-read,user-library-modify,playlist-modify-private,playlist-modify-public";
    private String accessToken;
    private String refreshToken;
    private Map<String, String> authHeaders;

    /**
     * Default spotify constructor asking for user input
     *
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public Spotify() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Spotify.class)) {
            logger.debug("Empty spotify credentials");
            throw new MissingSettingException(Spotify.class);
        }
        try {
            this.authenticate();
            logger.debug("Succesfully authenticated into spotify");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while authenticating with Spotify");
            throw new AuthenticationException(e);
        }
    }

    /**
     * Start an HTTP server to intercept the Spotify API code
     *
     * @throws java.lang.InterruptedException - if an error occurs
     */
    private String getCode() throws InterruptedException {
        WebTarget target = WebTools.target(authUrl).path("authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", Settings.spotifyClientID)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", this.scope)
                .queryParam("show_dialog", String.valueOf(Settings.interactive));
        return OAuth.getOauthCode(target);
    }

    /**
     * set the access token from the code exchanged through the browser
     */
    private void authenticate() throws AuthenticationException, InterruptedException {
        Form form = new Form()
                .param("grant_type", "authorization_code")
                // the code and secret ride in the form body, not the url, so
                // they never end up in server or proxy access logs
                .param("client_id", Settings.spotifyClientID)
                .param("client_secret", Settings.spotifyClientSecret)
                .param("redirect_uri", redirectUri)
                .param("code", this.getCode());
        WebTarget target = WebTools.target(authUrl).path("api").path("token");
        try {
            OAuth.TokenResponse token = WebTools.post(target, null, Entity.form(form), OAuth.TokenResponse.class);
            if (token == null || token.access_token == null) {
                throw new AuthenticationException("no access token in response from: "
                        + target.getUri());
            }
            this.accessToken = token.access_token;
            this.refreshToken = token.refresh_token;
            this.authHeaders = Map.of("Authorization", "Bearer " + this.accessToken);
        } catch (QueryException e) {
            throw new AuthenticationException("failed to get access token from: "
                    + target.getUri(), e);
        }
    }

    /**
     * refresh the spotify api token incase of expiring
     *
     * @return - true if succesfully refreshed, false if not
     */
    public boolean refreshToken() {
        if (this.refreshToken == null) {
            logger.debug("No refresh token to refresh the spotify access token with");
            return false;
        }
        Form form = new Form()
                .param("grant_type", "refresh_token")
                .param("refresh_token", this.refreshToken)
                .param("client_id", Settings.spotifyClientID)
                .param("client_secret", Settings.spotifyClientSecret);
        WebTarget target = WebTools.target(authUrl).path("api").path("token");
        try {
            OAuth.TokenResponse token = WebTools.post(target, null, Entity.form(form), OAuth.TokenResponse.class);
            if (token == null || token.access_token == null) {
                logger.error("Exception refreshing token");
                return false;
            }
            // spotify hands back a new access token and keeps the refresh token valid
            this.accessToken = token.access_token;
            this.authHeaders = Map.of("Authorization", "Bearer " + this.accessToken);
            return true;
        } catch (QueryException e) {
            logger.error("Exception refreshing token", e);
            return false;
        }
    }

    private ArrayList<Artist> getArtists(List<Artists.SimplifiedArtistObject> artistObjects) {
        ArrayList<Artist> artists = new ArrayList<>();
        if (artistObjects == null) {
            return artists;
        }
        for (Artists.SimplifiedArtistObject artistObject : artistObjects) {
            if (artistObject == null || artistObject.name == null
                    || artistObject.name.equals("Various Artists")) {
                continue;
            }
            artists.add(new Artist(artistObject.name));
        }
        return artists;
    }

    private Song getSong(Tracks.TrackObject track) {
        Song song = new Song(track.name);
        song.addArtists(this.getArtists(track.artists));
        if (track.duration_ms != null) {
            song.setDuration(track.duration_ms, ChronoUnit.MILLIS);
        }
        if (track.id != null) {
            song.addId("spotify", track.id);
        }
        String coverImage = this.lastImage(track.album == null ? null : track.album.images);
        if (coverImage != null) {
            song.setCoverImage(coverImage);
        }
        return song;
    }

    private Song getSong(Tracks.SimplifiedTrackObject track) {
        Song song = new Song(track.name);
        song.addArtists(this.getArtists(track.artists));
        if (track.duration_ms != null) {
            song.setDuration(track.duration_ms, ChronoUnit.MILLIS);
        }
        if (track.id != null) {
            song.addId("spotify", track.id);
        }
        return song;
    }

    private Song getSong(Episodes.EpisodeObject episode) {
        Song song = new Song(episode.name);
        if (episode.duration_ms != null) {
            song.setDuration(episode.duration_ms, ChronoUnit.MILLIS);
        }
        if (episode.id != null) {
            song.addId("spotify", episode.id);
        }
        String coverImage = this.lastImage(episode.images);
        if (coverImage != null) {
            song.setCoverImage(coverImage);
        }
        return song;
    }

    /**
     * spotify orders its images small to large, the old wrapper callers always
     * took the last one
     */
    private String lastImage(List<Commons.ImageObject> images) {
        if (images == null || images.isEmpty() || images.get(images.size() - 1) == null) {
            return null;
        }
        return images.get(images.size() - 1).url;
    }

    private Song getPlaylistSong(Object rawTrack, IPIterator<?> pb, String playlistId) throws InterruptedException {
        Tracks.TrackObject track = WebTools.convert(rawTrack, Tracks.TrackObject.class);
        if (track != null && "track".equals(track.type)) {
            pb.step(track.name);
            return this.getSong(track);
        }
        Episodes.EpisodeObject episode = WebTools.convert(rawTrack, Episodes.EpisodeObject.class);
        if (episode != null && "episode".equals(episode.type)) {
            pb.step(episode.name);
            return this.getSong(episode);
        }
        logger.debug("skipped non track '" + rawTrack.toString() + "' in playlist: " + playlistId);
        logger.info("Skipping non-Track in playlist: " + playlistId);
        return null;
    }

    private Song fromLibrary(Song song) throws InterruptedException {
        if (library != null) {
            Song foundSong = library.getSong(song);
            if (foundSong != null) {
                return foundSong;
            }
        }
        return song;
    }

    /**
     * {@inheritDoc}
     *
     * Get all liked songs from current spotify account and add them to collection
     */
    @Override
    public LikedSongs getLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        int limit = Settings.spotifySongLimit;
        int offset = 0;
        try (IPIterator<?> pb = IPIterator.manual("Liked Songs", -1)) {
            while (true) {
                WebTarget target = WebTools.target(baseUrl).path("me").path("tracks").queryParam("limit", limit)
                        .queryParam("offset", offset);
                try {
                    Tracks.PagingSavedTrackObject response = WebTools.get(target, this.authHeaders,
                            Tracks.PagingSavedTrackObject.class);
                    if (response == null || response.items == null || response.items.isEmpty()) {
                        break;
                    }
                    for (Tracks.SavedTrackObject savedTrack : response.items) {
                        if (savedTrack == null || savedTrack.track == null) {
                            // spotify keeps a null track for removed ones
                            continue;
                        }
                        Song song = this.fromLibrary(this.getSong(savedTrack.track));
                        likedSongs.addSong(song);
                        pb.step(song.getName());
                    }
                    offset += limit;
                    if (response.next == null) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.warn("Exception fetching liked songs: " + e);
                    break;
                }
            }
        }
        return likedSongs;
    }

    /** {@inheritDoc} */
    @Override
    public void syncLikedSongs() throws InterruptedException {
        logger.debug("Getting spotify liked songs to remove mismatches");
        LikedSongs likedSongs = this.getLikedSongs();
        if (likedSongs != null && !likedSongs.isEmpty()) {
            likedSongs.removeSongs(Collection.getLikedSongs().getSongs());
            ArrayList<String> songIds = new ArrayList<>();
            for (Song song : likedSongs.getSongs()) {
                String id = this.getTrackId(song);
                if (id != null) {
                    songIds.add(id);
                }
            }
            if (songIds.isEmpty()) {
                return;
            }
            WebTarget target = WebTools.target(baseUrl).path("me").path("tracks");
            this.sendIds(target, WebTools::delete, "Liked Songs", songIds,
                    Settings.spotifySongLimit, Spotify::removeTracks);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        likedSongs.addSongs(Collection.getLikedSongs().getSongs());
        logger.info("Getting current liked songs to prevent duplicates");
        LikedSongs currLikedSongs = this.getLikedSongs();
        if (currLikedSongs != null) {
            likedSongs.removeSongs(currLikedSongs.getSongs());
        }
        ArrayList<String> songIds = new ArrayList<>();
        for (Song song : likedSongs.getSongs()) {
            String id = this.getTrackId(song);
            if (id != null) {
                songIds.add(id);
            }
        }
        if (songIds.isEmpty()) {
            logger.debug("No liked songs in collection");
            return;
        }
        WebTarget target = WebTools.target(baseUrl).path("me").path("tracks");
        this.sendIds(target, WebTools::put, "Liked Songs", songIds,
                Settings.spotifySongLimit, Spotify::saveTracks);
    }

    /**
     * send a batch of spotify ids against one library endpoint, the saved tracks
     * and saved albums routes both answering with an empty body, and both of
     * them wanting the ids in a body of their own: put to add, delete to remove
     */
    private void sendIds(WebTarget target, IdsVerb verb, String label, ArrayList<String> ids, int limit,
            Function<List<String>, Object> request) throws InterruptedException {
        try (IPIterator<?> pb = IPIterator.manual(label, ids.size())) {
            for (int offset = 0; offset < ids.size(); offset += limit) {
                List<String> currentIds = ids.subList(offset, Math.min(offset + limit, ids.size()));
                try {
                    verb.send(target, this.authHeaders,
                            Entity.entity(WebTools.json(request.apply(currentIds)), MediaType.APPLICATION_JSON),
                            Object.class);
                    logger.debug("sent " + currentIds.size() + " ids to " + target.getUri());
                } catch (QueryException | JsonProcessingException e) {
                    logger.warn("Exception while sending ids to " + target.getUri() + ": " + e);
                }
                pb.step(currentIds.size());
            }
        }
    }

    /**
     * the four superbeans library request beans, one per route and direction,
     * each of them nothing but an ids list
     */
    private static Users.SaveTracksUserRequest saveTracks(List<String> ids) {
        Users.SaveTracksUserRequest request = new Users.SaveTracksUserRequest();
        request.ids = ids;
        return request;
    }

    private static Users.RemoveTracksUserRequest removeTracks(List<String> ids) {
        Users.RemoveTracksUserRequest request = new Users.RemoveTracksUserRequest();
        request.ids = ids;
        return request;
    }

    private static Users.SaveAlbumsUserRequest saveAlbums(List<String> ids) {
        Users.SaveAlbumsUserRequest request = new Users.SaveAlbumsUserRequest();
        request.ids = ids;
        return request;
    }

    private static Users.RemoveAlbumsUserRequest removeAlbums(List<String> ids) {
        Users.RemoveAlbumsUserRequest request = new Users.RemoveAlbumsUserRequest();
        request.ids = ids;
        return request;
    }

    /**
     * <p>
     * IdsVerb, the one webtools verb that carries the batch: {@link WebTools#put}
     * or {@link WebTools#delete}, whichever way the ids are meant to move.
     * </p>
     */
    @FunctionalInterface
    private interface IdsVerb {
        <T> T send(WebTarget target, Map<String, ?> headers, Entity<?> body, Class<T> responseType)
                throws QueryException;
    }

    /**
     * {@inheritDoc}
     *
     * get all current user saved albums and add them to collection
     */
    @Override
    public ArrayList<Album> getAlbums() throws InterruptedException {
        ArrayList<Album> albums = new ArrayList<>();
        int limit = Settings.spotifyAlbumLimit;
        int offset = 0;
        try (IPIterator<?> pb = IPIterator.manual("Albums", -1)) {
            while (true) {
                WebTarget target = WebTools.target(baseUrl).path("me").path("albums").queryParam("limit", limit)
                        .queryParam("offset", offset);
                try {
                    Albums.PagingSavedAlbumObject response = WebTools.get(target, this.authHeaders,
                            Albums.PagingSavedAlbumObject.class);
                    if (response == null || response.items == null || response.items.isEmpty()) {
                        break;
                    }
                    for (Albums.SavedAlbumObject savedAlbum : response.items) {
                        if (savedAlbum == null || savedAlbum.album == null) {
                            continue;
                        }
                        String albumId = savedAlbum.album.id;
                        String albumName = savedAlbum.album.name;
                        String albumArtistName = null;
                        for (Artist artist : this.getArtists(savedAlbum.album.artists)) {
                            albumArtistName = artist.getName();
                            break;
                        }
                        Album album = this.getAlbum(albumId, albumName, albumArtistName);
                        albums.add(album);
                        pb.step(album.getName());
                    }
                    offset += limit;
                    if (response.next == null) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.warn("Exception getting albums: " + e);
                    break;
                }
            }
        }
        return albums;
    }

    /** {@inheritDoc} */
    @Override
    public Album getAlbum(String albumId, String albumName, String artistName) throws InterruptedException {
        if (albumId == null || albumName == null) {
            logger.debug("Null albumID or AlbumName provided in getAlbum");
            return null;
        }
        Album album = new Album(albumName);
        if (artistName != null) {
            album.addArtist(new Artist(artistName));
        }
        album.addId("spotify", albumId);
        if (library != null) {
            Album foundAlbum = library.getAlbum(album);
            if (foundAlbum != null) {
                album = foundAlbum;
            }
        }
        if (album.isEmpty()) {
            ArrayList<Song> songs = this.getAlbumSongs(albumId);
            if (songs != null && !songs.isEmpty()) {
                album.addSongs(songs);
            }
        }
        return album;
    }

    /**
     * get all songs in an album
     *
     * @param albumId - spotify album id
     * @return - linkedhashset of songs
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public ArrayList<Song> getAlbumSongs(String albumId) throws InterruptedException {
        if (albumId == null) {
            logger.debug("null albumID provided in getAlbumSongs");
            return null;
        }
        int offset = 0;
        try (IPIterator<?> pb = IPIterator.manual(albumId, -1)) {
            ArrayList<Song> songs = new ArrayList<>();
            int limit = Settings.spotifySongLimit;
            while (true) {
                WebTarget target = WebTools.target(baseUrl).path("albums").path(albumId).path("tracks")
                        .queryParam("limit", limit).queryParam("offset", offset);
                try {
                    Tracks.PagingSimplifiedTrackObject response = WebTools.get(target, this.authHeaders,
                            Tracks.PagingSimplifiedTrackObject.class);
                    if (response == null || response.items == null || response.items.isEmpty()) {
                        break;
                    }
                    for (Tracks.SimplifiedTrackObject track : response.items) {
                        if (track == null) {
                            continue;
                        }
                        Song song = this.fromLibrary(this.getSong(track));
                        songs.add(song);
                        pb.step(song.getName());
                    }
                    offset += limit;
                    if (response.next == null) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.error("Exception fetching songs for album: " + albumId, e);
                    break;
                }
            }
            return songs;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbums() throws InterruptedException {
        ArrayList<Album> albums = this.getAlbums();
        if (albums != null && !albums.isEmpty()) {
            albums.removeAll(Collection.getAlbums());
            ArrayList<String> albumIds = new ArrayList<>();
            for (Album album : albums) {
                String id = this.getAlbumId(album);
                if (id != null) {
                    albumIds.add(id);
                }
            }
            if (albumIds.isEmpty()) {
                return;
            }
            WebTarget target = WebTools.target(baseUrl).path("me").path("albums");
            this.sendIds(target, WebTools::delete, "Albums", albumIds,
                    Settings.spotifyAlbumLimit, Spotify::removeAlbums);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAlbums() throws InterruptedException {
        ArrayList<Album> albums = Collection.getAlbums();
        ArrayList<String> albumIds = new ArrayList<>();
        for (Album album : albums) {
            String id = this.getAlbumId(album);
            if (id != null) {
                albumIds.add(id);
            }
        }
        if (albumIds.isEmpty()) {
            logger.debug("No Saved albums in collection");
            return;
        }
        WebTarget target = WebTools.target(baseUrl).path("me").path("albums");
        this.sendIds(target, WebTools::put, "Albums", albumIds,
                Settings.spotifyAlbumLimit, Spotify::saveAlbums);
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAlbum(Album album) {
        logger.debug("Unsupported method uploadAlbum called");
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbum(Album album) {
        logger.debug("Unsupported method syncAlbum called");
    }

    /**
     * {@inheritDoc}
     *
     * get all playlists contributed by current spotify user and add them to
     * collection
     */
    @Override
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = new ArrayList<>();
        int limit = Settings.spotifyPlaylistLimit;
        int offset = 0;
        try (IPIterator<?> pb = IPIterator.manual("Playlists", -1)) {
            while (true) {
                WebTarget target = WebTools.target(baseUrl).path("me").path("playlists").queryParam("limit", limit)
                        .queryParam("offset", offset);
                try {
                    Playlists.PagingPlaylistObject response = WebTools.get(target, this.authHeaders,
                            Playlists.PagingPlaylistObject.class);
                    if (response == null || response.items == null || response.items.isEmpty()) {
                        break;
                    }
                    for (Playlists.SimplifiedPlaylistObject spotifyPlaylist : response.items) {
                        if (spotifyPlaylist == null) {
                            continue;
                        }
                        String coverImageUrl = this.lastImage(spotifyPlaylist.images);
                        pb.step(spotifyPlaylist.name);
                        Playlist playlist = this.getPlaylist(spotifyPlaylist.id, spotifyPlaylist.name);
                        if (playlist != null) {
                            if (coverImageUrl != null) {
                                playlist.setCoverImage(coverImageUrl);
                            }
                            playlists.add(playlist);
                        }
                    }
                    offset += limit;
                    if (response.next == null) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.warn("Exception fetching playlists: " + e);
                    break;
                }
            }
        }
        return playlists;
    }

    /** {@inheritDoc} */
    @Override
    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException {
        if (playlistId == null || playlistName == null) {
            logger.debug("null playlistID or playlistName provided in getPlaylist");
            return null;
        }
        Playlist playlist = new Playlist(playlistName);
        ArrayList<Song> songs = this.getPlaylistSongs(playlistId);
        if (songs != null) {
            playlist.addSongs(songs);
        }
        playlist.addId("spotify", playlistId);
        if (playlist.isEmpty()) {
            logger.debug("getPlaylist " + playlistId + "playlist is empty");
            return null;
        }
        return playlist;
    }

    /**
     * get all songs from a playlist
     *
     * @param playlistId - spotify ID for a playlist
     * @return - constructed array of Songs
     * @throws java.lang.InterruptedException - when user interrupts
     */
    public ArrayList<Song> getPlaylistSongs(String playlistId) throws InterruptedException {
        if (playlistId == null) {
            logger.debug("null playlistID provided in getPlaylistSongs");
            return null;
        }
        ArrayList<Song> songs = new ArrayList<>();
        int limit = Settings.spotifySongLimit;
        int offset = 0;
        try (IPIterator<?> pb = IPIterator.manual(playlistId, -1)) {
            while (true) {
                WebTarget target = WebTools.target(baseUrl).path("playlists").path(playlistId).path("tracks")
                        .queryParam("limit", limit).queryParam("offset", offset);
                try {
                    Playlists.PagingPlaylistTrackObject response = WebTools.get(target, this.authHeaders,
                            Playlists.PagingPlaylistTrackObject.class);
                    if (response == null || response.items == null || response.items.isEmpty()) {
                        break;
                    }
                    for (Playlists.PlaylistTrackObject playlistTrack : response.items) {
                        if (playlistTrack == null) {
                            continue;
                        }
                        // the track field is a union of tracks and episodes, the
                        // provider parses it as a map and the type decides the bean
                        Object rawTrack = playlistTrack.track != null ? playlistTrack.track : playlistTrack.item;
                        if (rawTrack == null) {
                            continue;
                        }
                        Song song = this.getPlaylistSong(rawTrack, pb, playlistId);
                        if (song != null) {
                            songs.add(this.fromLibrary(song));
                        }
                    }
                    offset += limit;
                    if (response.next == null) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.error("Exception fetching playlist tracks", e);
                    break;
                }
            }
            return songs;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylists() throws InterruptedException {
        logger.debug("Getting spotify playlists to remove mismatches");
        ArrayList<Playlist> playlists = this.getPlaylists();
        if (playlists != null && !playlists.isEmpty()) {
            playlists.removeAll(Collection.getPlaylists());
            for (Playlist playlist : playlists) {
                // spotify offers no endpoint to delete playlists
                logger.warn("Playlist '" + playlist.getName()
                        + "' was found on spotify but not in collection, delete it to stay up to date");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = Collection.getPlaylists();
        for (Playlist playlist : IPIterator.wrap(playlists, "Uploading Playlists", playlists.size())) {
            this.uploadPlaylist(playlist);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in playlistCleanUp");
            return;
        }
        logger.debug("Getting spotify playlist '" + playlist.getName() + "' to remove mismatches");
        String playlistId = this.getPlaylistId(playlist);
        if (playlistId != null) {
            ArrayList<Song> songs = this.getPlaylistSongs(playlistId);
            if (songs != null && !songs.isEmpty()) {
                // only delete one when there are duplicate songs
                songs.removeIf(playlist::contains);
                ArrayList<Map<String, Object>> trackRefs = new ArrayList<>();
                for (Song song : songs) {
                    String id = this.getTrackId(song);
                    if (id != null) {
                        trackRefs.add(Map.of("uri", "spotify:track:" + id));
                    }
                }
                if (trackRefs.isEmpty()) {
                    return;
                }
                int limit = Settings.spotifySongLimit;
                try (IPIterator<?> pb = IPIterator.manual(playlist.getName(), playlist.size())) {
                    for (int offset = 0; offset < trackRefs.size(); offset += limit) {
                        List<Map<String, Object>> currentRefs = trackRefs.subList(offset,
                                Math.min(offset + limit, trackRefs.size()));
                        Playlists.RemoveTracksPlaylistRequest body = new Playlists.RemoveTracksPlaylistRequest();
                        body.tracks = currentRefs;
                        WebTarget target = WebTools.target(baseUrl).path("playlists").path(playlistId).path("tracks");
                        try {
                            WebTools.delete(target, this.authHeaders,
                                    Entity.entity(WebTools.json(body), MediaType.APPLICATION_JSON), Object.class);
                            logger.debug("removed playlist '" + playlist.getName() + "'' songs ("
                                    + currentRefs.size() + ")");
                        } catch (QueryException | JsonProcessingException e) {
                            logger.warn("Exception removing playlist '" + playlist.getName() + "' songs: " + e);
                        }
                        pb.step(currentRefs.size());
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in uploadPlaylist");
            return;
        }
        String playlistId = this.getPlaylistId(playlist);
        ArrayList<Song> currentSongs = new ArrayList<>();
        if (playlistId != null) {
            currentSongs = this.getPlaylistSongs(playlistId);
        }
        ArrayList<Song> songs = new ArrayList<>(playlist.getSongs());
        if (currentSongs.isEmpty()) {
            Playlists.CreatePlaylistRequest body = new Playlists.CreatePlaylistRequest();
            body.name = playlist.getName();
            body.public_ = false;
            WebTarget target = WebTools.target(baseUrl).path("me");
            WebTarget target2 = WebTools.target(baseUrl).path("me").path("playlists");
            try {
                Users.PrivateUserObject user = WebTools.get(target, this.authHeaders, Users.PrivateUserObject.class);
                if (user != null) {
                    Playlists.PlaylistObject created = WebTools.post(target2, this.authHeaders,
                            Entity.entity(WebTools.json(body), MediaType.APPLICATION_JSON),
                            Playlists.PlaylistObject.class);
                    if (created != null && created.id != null) {
                        playlistId = created.id;
                        logger.debug("Created new playlist: " + playlist.getName());
                        playlist.addId("spotify", playlistId);
                    }
                }
            } catch (QueryException | JsonProcessingException e) {
                logger.debug("Exception creating user playlist");
            }
        } else {
            // filter out the existing playlist songs
            songs.removeAll(currentSongs);
        }
        if (playlistId == null) {
            logger.debug("No playlist to upload '" + playlist.toString() + "' to");
            return;
        }
        ArrayList<String> songUris = new ArrayList<>();
        for (Song song : songs) {
            String id = this.getTrackId(song);
            if (id != null) {
                songUris.add("spotify:track:" + id);
            }
        }
        if (songUris.isEmpty()) {
            logger.debug("Playlist '" + playlist.toString() + "' is empty / no update");
            return;
        }
        int limit = Settings.spotifySongLimit;
        try (IPIterator<?> pb = IPIterator.manual(playlist.getName(), playlist.size())) {
            for (int offset = 0; offset < songUris.size(); offset += limit) {
                List<String> currentSongUris = songUris.subList(offset,
                        Math.min(offset + limit, songUris.size()));
                Playlists.AddItemsToPlaylistRequest body = new Playlists.AddItemsToPlaylistRequest();
                body.uris = currentSongUris;
                WebTarget target3 = WebTools.target(baseUrl).path("playlists").path(playlistId).path("tracks");
                try {
                    WebTools.put(target3, this.authHeaders,
                            Entity.entity(WebTools.json(body), MediaType.APPLICATION_JSON), Object.class);
                } catch (QueryException | JsonProcessingException e) {
                    logger.warn("Exception adding songs to playlist '" + playlist.getName() + "': " + e);
                }
                pb.step(currentSongUris.size());
            }
        }
    }

    /**
     * <p>
     * getTrackId.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException if any.
     */
    public String getTrackId(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("null song provided in getTrackId");
            return null;
        }
        if (song.getId("spotify") != null) {
            return song.getId("spotify");
        }
        WebTarget target = WebTools.target(baseUrl).path("search").queryParam("type", "track")
                .queryParam("q", song.toString()).queryParam("limit", 1);
        try {
            SearchResponse response = WebTools.get(target, this.authHeaders, SearchResponse.class);
            if (response == null || response.tracks == null || response.tracks.items == null
                    || response.tracks.items.isEmpty()) {
                logger.debug("Song '" + song.toString() + "' not found");
                return null;
            }
            String id = response.tracks.items.get(0).id;
            song.addId("spotify", id);
            return id;
        } catch (QueryException e) {
            logger.error("Exception searching for song", e);
            return null;
        }
    }

    /**
     * <p>
     * getPlaylistId.
     * </p>
     *
     * @param playlist a {@link ryzen.ownitall.classes.Playlist} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException if any.
     */
    public String getPlaylistId(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in getPlaylistId");
            return null;
        }
        // do not use cached playlist id because the playlist might have been deleted
        int limit = Settings.spotifyPlaylistLimit;
        int offset = 0;
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (true) {
                interruptionHandler.checkInterruption();
                WebTarget target = WebTools.target(baseUrl).path("me").path("playlists").queryParam("limit", limit)
                        .queryParam("offset", offset);
                try {
                    Playlists.PagingPlaylistObject response = WebTools.get(target, this.authHeaders,
                            Playlists.PagingPlaylistObject.class);
                    if (response == null || response.items == null || response.items.isEmpty()) {
                        break;
                    }
                    for (Playlists.SimplifiedPlaylistObject spotifyPlaylist : response.items) {
                        interruptionHandler.checkInterruption();
                        if (spotifyPlaylist.name != null && spotifyPlaylist.name.equalsIgnoreCase(playlist.getName())) {
                            playlist.addId("spotify", spotifyPlaylist.id);
                            return spotifyPlaylist.id;
                        }
                    }
                    offset += limit;
                    if (response.next == null) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.warn("Exception fetching playlists: " + e);
                    break;
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * getAlbumId.
     * </p>
     *
     * @param album a {@link ryzen.ownitall.classes.Album} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException if any.
     */
    public String getAlbumId(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided in getAlbumId");
            return null;
        }
        if (album.getId("spotify") != null) {
            return album.getId("spotify");
        }
        WebTarget target = WebTools.target(baseUrl).path("search").queryParam("type", "album")
                .queryParam("q", album.toString()).queryParam("limit", 1);
        try {
            SearchResponse response = WebTools.get(target, this.authHeaders, SearchResponse.class);
            if (response == null || response.albums == null || response.albums.items == null
                    || response.albums.items.isEmpty()) {
                logger.debug("Album '" + album.toString() + "' not found");
                return null;
            }
            String id = response.albums.items.get(0).id;
            album.addId("spotify", id);
            return id;
        } catch (QueryException e) {
            logger.error("Exception searching for Album", e);
            return null;
        }
    }

    // the search envelope, the one document superbeans has no bean for yet
    public static class SearchResponse {
        public Tracks.PagingTrackObject tracks;
        public Albums.PagingSimplifiedAlbumObject albums;
    }
}
