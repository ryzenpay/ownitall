package ryzen.ownitall.integrations;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
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
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.WebTools.AuthenticationException;
import ryzen.ownitall.util.WebTools.QueryException;
import ryzen.superbeans.jellyfin.Items.BaseItemDto;
import ryzen.superbeans.jellyfin.Items.BaseItemDtoQueryResult;
import ryzen.superbeans.jellyfin.Playlists.CreatePlaylistDto;
import ryzen.superbeans.jellyfin.Playlists.PlaylistCreationResult;
import ryzen.superbeans.jellyfin.Playlists.PlaylistDto;
import ryzen.superbeans.jellyfin.Users.AuthenticateUserByName;
import ryzen.superbeans.jellyfin.Users.AuthenticationResult;

/**
 * <p>
 * Jellyfin class.
 * </p>
 *
 * @author ryzen
 */
public class Jellyfin implements Import, Export, Sync {
    private static final Logger logger = new Logger(Jellyfin.class);
    private static final Library library = Library.load();
    // /Items answers a page at a time, playlist item add/remove accepts an id list
    // at a time
    private static final int pageSize = 500;
    private static final int batchSize = 100;
    private String userId;
    private String accessToken;
    // https://gist.github.com/nielsvanvelzen/ea047d9028f676185832e51ffaf12a6f
    private Map<String, String> headers;
    private LinkedHashMap<String, BaseItemDto> itemsById;
    private LinkedHashMap<String, String> idByArtistAndName;
    private LinkedHashMap<String, String> idByName;

    // https://api.jellyfin.org/
    /**
     * <p>
     * Constructor for Jellyfin.
     * </p>
     *
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public Jellyfin() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Jellyfin.class)) {
            logger.debug("Empty jellyfin credentials");
            throw new MissingSettingException(Jellyfin.class);
        }
        this.authenticate();
        logger.debug("Successfully authenticated into jellyfin as " + Settings.jellyfinUsername);
    }

    // https://api.jellyfin.org/#tag/User/operation/AuthenticateUserByName
    private void authenticate() throws AuthenticationException {
        AuthenticateUserByName creds = new AuthenticateUserByName();
        creds.Username = Settings.jellyfinUsername;
        creds.Pw = Settings.jellyfinPassword;
        WebTarget target = WebTools.target(Settings.jellyfinURL).path("Users").path("AuthenticateByName");
        try {
            AuthenticationResult response = WebTools.post(target, null,
                    Entity.entity(WebTools.json(creds), MediaType.APPLICATION_JSON), AuthenticationResult.class);
            if (response != null && response.User != null) {
                logger.debug("Authenticated into jellyfin with user " + response.User.Name);
                this.userId = response.User.Id;
                this.accessToken = response.AccessToken;
                this.headers = Map.of(
                        "Authorization",
                        String.format(
                                "MediaBrowser Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\", Token=\"%s\"",
                                "ownitall",
                                "Java",
                                Settings.jellyfinUsername + "ownitall",
                                "10.10.6",
                                this.accessToken),
                        "Accept", "application/json");
                return;
            }
        } catch (QueryException | IOException e) {
            logger.error("Exception while authenticating into jellyfin", e);
        }
        throw new AuthenticationException("Failed to authenticate with jellyfin");
    }

    /**
     * build the library index: one paged /Items pass instead of one query per
     * song name later on, sized by the TotalRecordCount the first page carries
     *
     * @throws java.lang.InterruptedException if any.
     */
    private void ensureIndex() throws InterruptedException {
        if (this.itemsById != null) {
            return;
        }
        LinkedHashMap<String, BaseItemDto> byId = new LinkedHashMap<>();
        LinkedHashMap<String, String> byArtistAndName = new LinkedHashMap<>();
        LinkedHashMap<String, String> byName = new LinkedHashMap<>();
        int startIndex = 0;
        int total = -1;
        IPIterator<?> pb = null;
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (total < 0 || startIndex < total) {
                interruptionHandler.checkInterruption();
                WebTarget target = WebTools.target(Settings.jellyfinURL).path("Items")
                        .queryParam("userId", this.userId).queryParam("IncludeItemTypes", "Audio")
                        .queryParam("recursive", "true").queryParam("StartIndex", startIndex)
                        .queryParam("Limit", pageSize);
                try {
                    BaseItemDtoQueryResult page = WebTools.get(target, this.headers, BaseItemDtoQueryResult.class);
                    if (page == null || page.Items == null || page.Items.isEmpty()) {
                        break;
                    }
                    if (total < 0) {
                        total = page.TotalRecordCount != null ? page.TotalRecordCount : page.Items.size();
                        pb = IPIterator.manual("Indexing Jellyfin", total);
                    }
                    for (BaseItemDto item : page.Items) {
                        if (item.Id == null || item.Name == null) {
                            continue;
                        }
                        byId.putIfAbsent(item.Id, item);
                        byName.putIfAbsent(this.normalize(item.Name), item.Id);
                        if (item.Artists != null) {
                            for (String artist : item.Artists) {
                                byArtistAndName.putIfAbsent(
                                        this.normalize(artist) + "|" + this.normalize(item.Name),
                                        item.Id);
                            }
                        }
                    }
                    startIndex += page.Items.size();
                    if (pb != null) {
                        pb.step(page.Items.size());
                    }
                    if (page.Items.size() < pageSize) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.error("Exception while indexing jellyfin items", e);
                    break;
                }
            }
        } finally {
            if (pb != null) {
                pb.close();
            }
        }
        logger.debug("Indexed " + startIndex + " jellyfin audio items");
        this.itemsById = byId;
        this.idByArtistAndName = byArtistAndName;
        this.idByName = byName;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    /**
     * the song as the list query already describes it, so a page of items does
     * not refetch itself item by item: artists and album ride along, the length
     * rides in RunTimeTicks (100ns ticks) and the musicbrainz id the server
     * stored rides in ProviderIds
     *
     * @param item a {@link ryzen.superbeans.jellyfin.Items.BaseItemDto} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     */
    private Song songFrom(BaseItemDto item) {
        Song song = new Song(item.Name);
        if (item.Artists != null && !item.Artists.isEmpty()) {
            song.addArtist(new Artist(item.Artists.get(0)));
        }
        if (item.Album != null && !item.Album.isEmpty()) {
            song.setAlbumName(item.Album);
        }
        if (item.RunTimeTicks != null && item.RunTimeTicks > 0) {
            song.setDuration(item.RunTimeTicks / 10000, ChronoUnit.MILLIS);
        }
        String mbid = this.musicBrainzId(item.ProviderIds, "recording", "track");
        if (mbid != null) {
            song.addId("mbid", mbid);
        }
        return song;
    }

    /**
     * pull the musicbrainz id out of the provider ids a server stored on an
     * item, preferring the token that names the entity we are after
     *
     * @param providerIds a {@link java.util.Map} object
     * @param tokens      provider names to prefer, most wanted first
     * @return a {@link java.lang.String} object
     */
    private String musicBrainzId(Map<String, String> providerIds, String... tokens) {
        if (providerIds == null || providerIds.isEmpty()) {
            return null;
        }
        for (String token : tokens) {
            for (Map.Entry<String, String> entry : providerIds.entrySet()) {
                String key = entry.getKey().toLowerCase().replaceAll("[^a-z0-9]", "");
                if (key.startsWith("musicbrainz") && key.substring("musicbrainz".length()).equals(token)
                        && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * hand the song to the library for correction, the way getSong always has
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     * @throws java.lang.InterruptedException if any.
     */
    private Song matchedSong(Song song) throws InterruptedException {
        if (library == null) {
            return song;
        }
        Song foundSong = library.getSong(song);
        if (foundSong != null) {
            return foundSong;
        } else if (Settings.libraryVerified) {
            return null;
        }
        return song;
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    /** {@inheritDoc} */
    @Override
    public LikedSongs getLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        WebTarget target = WebTools.target(Settings.jellyfinURL).path("Items").queryParam("userId", this.userId)
                .queryParam("mediaTypes", "Audio").queryParam("recursive", "true")
                .queryParam("isFavorite", "true");
        try {
            BaseItemDtoQueryResult response = WebTools.get(target, this.headers, BaseItemDtoQueryResult.class);
            if (response != null && response.Items != null) {
                int total = response.TotalRecordCount != null ? response.TotalRecordCount : response.Items.size();
                for (BaseItemDto item : IPIterator.wrap(response.Items.iterator(), "Liked Songs", total)) {
                    String id = item.Id;
                    if (id != null && !id.isEmpty()) {
                        Song song = this.matchedSong(this.songFrom(item));
                        if (song != null) {
                            likedSongs.addSong(song);
                        }
                    }
                }
                if (!likedSongs.isEmpty()) {
                    return likedSongs;
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while getting jellyfin liked songs", e);
        }
        logger.debug("Unable to get ids of favorite items");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void syncLikedSongs() throws InterruptedException {
        logger.debug("Getting jellyfin liked songs to remove mismatches");
        LikedSongs likedSongs = this.getLikedSongs();
        if (likedSongs != null && !likedSongs.isEmpty()) {
            likedSongs.removeSongs(Collection.getLikedSongs().getSongs());
            for (Song song : IPIterator.wrap(likedSongs.getSongs(), "Liked Songs", likedSongs.size())) {
                String songId = this.getSongId(song);
                if (songId != null) {
                    WebTarget target = WebTools.target(Settings.jellyfinURL).path("UserFavoriteItems").path(songId)
                            .queryParam("userId", this.userId);
                    try {
                        WebTools.delete(target, this.headers, Void.class);
                        logger.debug("removed '" + song.toString() + "' favorite from jellyfin");
                    } catch (QueryException e) {
                        logger.error("Exception while removing jellyfin favorite '" + songId + "'", e);
                    }
                }
            }
        }
    }

    // https://api.jellyfin.org/#tag/UserLibrary/operation/MarkFavoriteItem
    /** {@inheritDoc} */
    @Override
    public void uploadLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = Collection.getLikedSongs();
        for (Song song : IPIterator.wrap(likedSongs.getSongs().iterator(), "Liked Songs", likedSongs.size())) {
            String songId = this.getSongId(song);
            if (songId != null) {
                WebTarget target = WebTools.target(Settings.jellyfinURL).path("UserFavoriteItems").path(songId)
                        .queryParam("userId", this.userId);
                try {
                    WebTools.post(target, this.headers, Void.class);
                } catch (QueryException e) {
                    logger.error("Exception while adding jellyfin favorite '" + songId + "'", e);
                }
            }
        }
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    /** {@inheritDoc} */
    @Override
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = new ArrayList<>();
        WebTarget target = WebTools.target(Settings.jellyfinURL).path("Items").queryParam("userId", this.userId)
                .queryParam("IncludeItemTypes", "Playlist").queryParam("recursive", "true");
        try {
            BaseItemDtoQueryResult response = WebTools.get(target, this.headers, BaseItemDtoQueryResult.class);
            if (response != null && response.Items != null) {
                int total = response.TotalRecordCount != null ? response.TotalRecordCount : response.Items.size();
                for (BaseItemDto item : IPIterator.wrap(response.Items.iterator(), "Playlists", total)) {
                    Playlist playlist = this.getPlaylist(item.Id, item.Name);
                    if (playlist != null && !playlist.isEmpty()) {
                        playlists.add(playlist);
                    }
                }
                if (!playlists.isEmpty()) {
                    return playlists;
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while getting jellyfin playlists", e);
        }
        logger.debug("Unable to get ids of playlists");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException {
        if (playlistId == null) {
            logger.debug("Null playlist id provided in getPlaylist");
            return null;
        }
        Playlist playlist = new Playlist(playlistName);
        ArrayList<Song> songs = this.getPlaylistSongs(playlistId);
        if (songs != null) {
            playlist.addSongs(songs);
        }
        if (playlist.isEmpty()) {
            logger.debug("getPlaylist " + playlistId + " playlist is empty");
            return null;
        }
        return playlist;
    }

    // https://api.jellyfin.org/#tag/Playlists/operation/GetPlaylistItems
    /**
     * <p>
     * getPlaylistSongs. the playlist items endpoint answers with whole items a
     * page at a time, so no song is refetched by id and the TotalRecordCount of
     * the first page sizes the progress bar.
     * </p>
     *
     * @param playlistId a {@link java.lang.String} object
     * @return a {@link java.util.ArrayList} object
     * @throws java.lang.InterruptedException if any.
     */
    public ArrayList<Song> getPlaylistSongs(String playlistId) throws InterruptedException {
        if (playlistId == null) {
            logger.debug("Null or empty playlist id provided in getPlaylistSongs");
            return null;
        }
        ArrayList<Song> songs = new ArrayList<>();
        int startIndex = 0;
        int total = -1;
        IPIterator<?> pb = null;
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (total < 0 || startIndex < total) {
                interruptionHandler.checkInterruption();
                WebTarget target = WebTools.target(Settings.jellyfinURL).path("Playlists").path(playlistId)
                        .path("Items").queryParam("userId", this.userId)
                        .queryParam("StartIndex", startIndex).queryParam("Limit", pageSize);
                try {
                    BaseItemDtoQueryResult page = WebTools.get(target, this.headers, BaseItemDtoQueryResult.class);
                    if (page == null || page.Items == null || page.Items.isEmpty()) {
                        break;
                    }
                    if (total < 0) {
                        total = page.TotalRecordCount != null ? page.TotalRecordCount : page.Items.size();
                        pb = IPIterator.manual(playlistId, total);
                    }
                    for (BaseItemDto item : page.Items) {
                        Song song = this.matchedSong(this.songFrom(item));
                        if (song != null) {
                            songs.add(song);
                        }
                    }
                    startIndex += page.Items.size();
                    if (pb != null) {
                        pb.step(page.Items.size());
                    }
                    if (page.Items.size() < pageSize) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.error("Exception while getting jellyfin playlist '" + playlistId + "' items", e);
                    break;
                }
            }
        } finally {
            if (pb != null) {
                pb.close();
            }
        }
        if (!songs.isEmpty()) {
            return songs;
        }
        WebTarget target2 = WebTools.target(Settings.jellyfinURL).path("Playlists").path(playlistId)
                .queryParam("userId", this.userId);
        try {
            PlaylistDto response = WebTools.get(target2, this.headers, PlaylistDto.class);
            if (response != null && response.ItemIds != null) {
                for (String itemId : IPIterator.wrap(response.ItemIds.iterator(), playlistId,
                        response.ItemIds.size())) {
                    Song song = this.getSong(itemId);
                    if (song != null) {
                        songs.add(song);
                    }
                }
                if (!songs.isEmpty()) {
                    return songs;
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while getting jellyfin playlist '" + playlistId + "'", e);
        }
        logger.debug("Unable to find playlist songs '" + playlistId + "' in jellyfin");
        return null;
    }

    /**
     * <p>
     * getPlaylistEntries. the playlist as (entry id, item id) pairs: the entry
     * is what deletion asks for, and both ids are already in the item the list
     * query returns.
     * </p>
     *
     * @param playlistId a {@link java.lang.String} object
     * @return a {@link java.util.ArrayList} object
     * @throws java.lang.InterruptedException if any.
     */
    public ArrayList<BaseItemDto> getPlaylistEntries(String playlistId) throws InterruptedException {
        if (playlistId == null) {
            logger.debug("null playlist id provided in getPlaylistEntries");
            return null;
        }
        ArrayList<BaseItemDto> entries = new ArrayList<>();
        int startIndex = 0;
        int total = -1;
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (total < 0 || startIndex < total) {
                interruptionHandler.checkInterruption();
                WebTarget target = WebTools.target(Settings.jellyfinURL).path("Playlists").path(playlistId)
                        .path("Items").queryParam("userId", this.userId)
                        .queryParam("StartIndex", startIndex).queryParam("Limit", pageSize);
                try {
                    BaseItemDtoQueryResult page = WebTools.get(target, this.headers, BaseItemDtoQueryResult.class);
                    if (page == null || page.Items == null || page.Items.isEmpty()) {
                        break;
                    }
                    if (total < 0) {
                        total = page.TotalRecordCount != null ? page.TotalRecordCount : page.Items.size();
                    }
                    entries.addAll(page.Items);
                    startIndex += page.Items.size();
                    if (page.Items.size() < pageSize) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.error("Exception while getting jellyfin playlist '" + playlistId + "' items", e);
                    break;
                }
            }
        }
        return entries;
    }

    // https://api.jellyfin.org/#tag/Playlists/operation/CreatePlaylist
    /** {@inheritDoc} */
    @Override
    public void uploadPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = Collection.getPlaylists();
        for (Playlist playlist : IPIterator.wrap(playlists, "Uploading Playlists", playlists.size())) {
            this.uploadPlaylist(playlist);
        }
    }

    // https://api.jellyfin.org/#tag/Playlists/operation/CreatePlaylist
    /** {@inheritDoc} */
    @Override
    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in uploadPlaylist");
            return;
        }
        ArrayList<String> songIds = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (Song song : playlist.getSongs()) {
            String songId = this.getSongId(song);
            if (songId != null && seen.add(songId)) {
                songIds.add(songId);
            }
        }
        if (songIds.isEmpty()) {
            logger.debug("Playlist '" + playlist.toString() + "' has no songs findable in jellyfin");
            return;
        }
        String playlistId = this.getPlaylistId(playlist);
        if (playlistId == null) {
            CreatePlaylistDto create = new CreatePlaylistDto();
            create.Name = playlist.getName();
            create.Ids = songIds;
            create.UserId = this.userId;
            create.MediaType = "Audio";
            create.IsPublic = false;
            WebTarget target = WebTools.target(Settings.jellyfinURL).path("Playlists")
                    .queryParam("name", playlist.getName())
                    .queryParam("userId", this.userId)
                    .queryParam("mediaType", "Audio");
            if (songIds.size() <= batchSize) {
                target = target.queryParam("ids", String.join(",", songIds));
            }
            try {
                PlaylistCreationResult created = WebTools.post(target, this.headers,
                        Entity.entity(WebTools.json(create), MediaType.APPLICATION_JSON), PlaylistCreationResult.class);
                if (created != null && created.Id != null) {
                    playlist.addId("jellyfin", created.Id);
                    this.setPlaylistDescription(created.Id, playlist.getDescription());
                    logger.debug("Created jellyfin playlist '" + playlist.getName() + "' with " + songIds.size()
                            + " songs");
                } else {
                    logger.debug("Unable to create jellyfin playlist '" + playlist.getName() + "'");
                }
            } catch (QueryException | IOException e) {
                logger.error("Exception while creating jellyfin playlist '" + playlist.getName() + "'", e);
            }
            return;
        }
        this.setPlaylistDescription(playlistId, playlist.getDescription());
        LinkedHashSet<String> remoteIds = new LinkedHashSet<>();
        for (BaseItemDto entry : this.getPlaylistEntries(playlistId)) {
            if (entry.Id != null) {
                remoteIds.add(entry.Id);
            }
        }
        ArrayList<String> missing = new ArrayList<>();
        for (String songId : songIds) {
            if (!remoteIds.contains(songId)) {
                missing.add(songId);
            }
        }
        if (missing.isEmpty()) {
            logger.debug("Playlist '" + playlist.toString() + "' is up to date on jellyfin");
            return;
        }
        this.addToPlaylist(playlistId, missing);
    }

    /**
     * jellyfin stores a playlist description in the item's generic Overview
     * metadata rather than in the playlist endpoints, so it rides a
     * fetch-then-update of the playlist item, the same call the jellyfin web
     * UI's metadata editor makes
     *
     * @param playlistId  - jellyfin id of the playlist item
     * @param description - description to store, skipped when empty
     */
    private void setPlaylistDescription(String playlistId, String description) {
        if (playlistId == null || description == null || description.isEmpty()) {
            return;
        }
        WebTarget target = WebTools.target(Settings.jellyfinURL).path("Users").path(this.userId).path("Items")
                .path(playlistId);
        try {
            BaseItemDto item = WebTools.get(target, this.headers, BaseItemDto.class);
            if (item == null || description.equals(item.Overview)) {
                return;
            }
            item.Overview = description;
            WebTools.post(WebTools.target(Settings.jellyfinURL).path("Items").path(this.userId).path(playlistId),
                    this.headers, Entity.entity(WebTools.json(item), MediaType.APPLICATION_JSON), Object.class);
            logger.debug("Set jellyfin playlist '" + playlistId + "' description");
        } catch (QueryException | IOException e) {
            logger.warn("Exception while setting jellyfin playlist '" + playlistId + "' description: " + e);
        }
    }

    // https://api.jellyfin.org/#tag/Playlists/operation/AddItemToPlaylist
    private void addToPlaylist(String playlistId, ArrayList<String> songIds) throws InterruptedException {
        try (IPIterator<?> pb = IPIterator.manual("Adding to playlist", songIds.size())) {
            for (int offset = 0; offset < songIds.size(); offset += batchSize) {
                ArrayList<String> batch = new ArrayList<>(
                        songIds.subList(offset, Math.min(offset + batchSize, songIds.size())));
                WebTarget target = WebTools.target(Settings.jellyfinURL).path("Playlists").path(playlistId)
                        .path("Items").queryParam("userId", this.userId)
                        .queryParam("ids", String.join(",", batch));
                try {
                    WebTools.post(target, this.headers, Void.class);
                } catch (QueryException e) {
                    logger.error("Exception while adding to jellyfin playlist '" + playlistId + "'", e);
                }
                pb.step(batch.size());
            }
        }
        logger.debug("Added " + songIds.size() + " songs to jellyfin playlist '" + playlistId + "'");
    }

    /**
     * <p>
     * getPlaylistId. finds a playlist by name without building any of them,
     * paging the item list the way getPlaylists pages the songs.
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
        String name = this.normalize(playlist.getName());
        int startIndex = 0;
        int total = -1;
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (total < 0 || startIndex < total) {
                interruptionHandler.checkInterruption();
                WebTarget target = WebTools.target(Settings.jellyfinURL).path("Items")
                        .queryParam("userId", this.userId).queryParam("IncludeItemTypes", "Playlist")
                        .queryParam("recursive", "true").queryParam("StartIndex", startIndex)
                        .queryParam("Limit", pageSize);
                try {
                    BaseItemDtoQueryResult page = WebTools.get(target, this.headers, BaseItemDtoQueryResult.class);
                    if (page == null || page.Items == null || page.Items.isEmpty()) {
                        break;
                    }
                    if (total < 0) {
                        total = page.TotalRecordCount != null ? page.TotalRecordCount : page.Items.size();
                    }
                    for (BaseItemDto item : page.Items) {
                        if (item.Name != null && this.normalize(item.Name).equals(name) && item.Id != null) {
                            playlist.addId("jellyfin", item.Id);
                            return item.Id;
                        }
                    }
                    startIndex += page.Items.size();
                    if (page.Items.size() < pageSize) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.error("Exception while getting jellyfin playlists", e);
                    break;
                }
            }
        }
        logger.debug("Could not find playlist '" + playlist.getName() + "' in jellyfin");
        return null;
    }

    // https://api.jellyfin.org/#tag/Playlists/operation/RemoveItemFromPlaylist
    /** {@inheritDoc} */
    @Override
    public void syncPlaylists() throws InterruptedException {
        logger.debug("Getting jellyfin playlists to remove mismatches");
        ArrayList<String> playlistNames = new ArrayList<>();
        int startIndex = 0;
        int total = -1;
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (total < 0 || startIndex < total) {
                interruptionHandler.checkInterruption();
                WebTarget target = WebTools.target(Settings.jellyfinURL).path("Items")
                        .queryParam("userId", this.userId).queryParam("IncludeItemTypes", "Playlist")
                        .queryParam("recursive", "true").queryParam("StartIndex", startIndex)
                        .queryParam("Limit", pageSize);
                try {
                    BaseItemDtoQueryResult page = WebTools.get(target, this.headers, BaseItemDtoQueryResult.class);
                    if (page == null || page.Items == null || page.Items.isEmpty()) {
                        break;
                    }
                    if (total < 0) {
                        total = page.TotalRecordCount != null ? page.TotalRecordCount : page.Items.size();
                    }
                    for (BaseItemDto item : page.Items) {
                        if (item.Id != null) {
                            playlistNames.add(item.Name);
                        }
                    }
                    startIndex += page.Items.size();
                    if (page.Items.size() < pageSize) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.error("Exception while getting jellyfin playlists", e);
                    break;
                }
            }
        }
        ArrayList<Playlist> collection = Collection.getPlaylists();
        for (Playlist local : IPIterator.wrap(collection, "Syncing Playlists", collection.size())) {
            this.syncPlaylist(local);
        }
        for (String remoteName : playlistNames) {
            boolean found = false;
            for (Playlist local : collection) {
                if (this.normalize(local.getName()).equals(this.normalize(remoteName))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                logger.warn("Playlist '" + remoteName
                        + "' was found on jellyfin but not in collection, delete it to stay up to date");
            }
        }
    }

    // https://api.jellyfin.org/#tag/Playlists/operation/RemoveItemFromPlaylist
    /** {@inheritDoc} */
    @Override
    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in syncPlaylist");
            return;
        }
        logger.debug("Getting jellyfin playlist '" + playlist.getName() + "' to remove mismatches");
        String playlistId = this.getPlaylistId(playlist);
        if (playlistId == null) {
            return;
        }
        LinkedHashSet<String> localIds = new LinkedHashSet<>();
        for (Song song : playlist.getSongs()) {
            String songId = this.getSongId(song);
            if (songId != null) {
                localIds.add(songId);
            }
        }
        ArrayList<String> entryIds = new ArrayList<>();
        for (BaseItemDto entry : this.getPlaylistEntries(playlistId)) {
            // deletion asks for the playlist entry, not the item it points at
            if (entry.Id != null && entry.PlaylistItemId != null && !localIds.contains(entry.Id)) {
                entryIds.add(entry.PlaylistItemId);
            }
        }
        if (entryIds.isEmpty()) {
            logger.debug("Playlist '" + playlist.getName() + "' has no mismatches on jellyfin");
            return;
        }
        try (IPIterator<?> pb = IPIterator.manual("Removing from playlist", entryIds.size())) {
            for (int offset = 0; offset < entryIds.size(); offset += batchSize) {
                ArrayList<String> batch = new ArrayList<>(
                        entryIds.subList(offset, Math.min(offset + batchSize, entryIds.size())));
                WebTarget target = WebTools.target(Settings.jellyfinURL).path("Playlists").path(playlistId)
                        .path("Items").queryParam("userId", this.userId)
                        .queryParam("entryIds", String.join(",", batch));
                try {
                    WebTools.delete(target, this.headers, Void.class);
                } catch (QueryException e) {
                    logger.error("Exception while removing from jellyfin playlist '" + playlistId + "'", e);
                }
                pb.step(batch.size());
            }
        }
        logger.debug("Removed " + entryIds.size() + " mismatched songs from jellyfin playlist '"
                + playlist.getName() + "'");
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    /** {@inheritDoc} */
    @Override
    public ArrayList<Album> getAlbums() throws InterruptedException {
        ArrayList<Album> albums = new ArrayList<>();
        WebTarget target = WebTools.target(Settings.jellyfinURL).path("Items").queryParam("userId", this.userId)
                .queryParam("IncludeItemTypes", "MusicAlbum").queryParam("recursive", "true");
        try {
            BaseItemDtoQueryResult response = WebTools.get(target, this.headers, BaseItemDtoQueryResult.class);
            if (response != null && response.Items != null) {
                int total = response.TotalRecordCount != null ? response.TotalRecordCount : response.Items.size();
                for (BaseItemDto item : IPIterator.wrap(response.Items.iterator(), "Albums", total)) {
                    String artistName = null;
                    if (item.AlbumArtists != null && !item.AlbumArtists.isEmpty()) {
                        artistName = item.AlbumArtists.get(0).Name;
                    }
                    String mbid = this.musicBrainzId(item.ProviderIds, "release");
                    Album album = this.getAlbum(item.Id, item.Name, artistName, mbid);
                    if (album != null && !album.isEmpty()) {
                        albums.add(album);
                    }
                }
                if (!albums.isEmpty()) {
                    return albums;
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while getting jellyfin albums", e);
        }
        logger.debug("Unable to get ids of albums");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        return this.getAlbum(albumId, albumName, albumArtistName, null);
    }

    private Album getAlbum(String albumId, String albumName, String albumArtistName, String mbid)
            throws InterruptedException {
        if (albumId == null) {
            logger.debug("null album id provided in getAlbum");
            return null;
        }
        Album album = new Album(albumName);
        if (albumArtistName != null) {
            album.addArtist(new Artist(albumArtistName));
        }
        if (mbid != null) {
            album.addId("mbid", mbid);
        }
        ArrayList<Song> songs = this.getAlbumSongs(albumId);
        if (songs != null) {
            album.addSongs(songs);
        }
        if (library != null) {
            Album foundAlbum = library.getAlbum(album);
            if (foundAlbum != null) {
                album = foundAlbum;
            } else if (Settings.libraryVerified) {
                album = null;
            }
        }
        return album;
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    /**
     * <p>
     * getAlbumSongs. the query already answers with whole items, so no song is
     * refetched by id.
     * </p>
     *
     * @param albumId a {@link java.lang.String} object
     * @return a {@link java.util.ArrayList} object
     * @throws java.lang.InterruptedException if any.
     */
    public ArrayList<Song> getAlbumSongs(String albumId) throws InterruptedException {
        if (albumId == null) {
            logger.debug("empty or null albumId provided in getAlbumSongs");
            return null;
        }
        ArrayList<Song> songs = new ArrayList<>();
        WebTarget target = WebTools.target(Settings.jellyfinURL).path("Items").queryParam("userId", this.userId)
                .queryParam("IncludeItemTypes", "Audio").queryParam("albumIds", albumId)
                .queryParam("recursive", "true");
        try {
            BaseItemDtoQueryResult response = WebTools.get(target, this.headers, BaseItemDtoQueryResult.class);
            if (response != null && response.Items != null) {
                int total = response.TotalRecordCount != null ? response.TotalRecordCount : response.Items.size();
                try (IPIterator<?> pb = IPIterator.manual(albumId, total)) {
                    for (BaseItemDto item : response.Items) {
                        String id = item.Id;
                        if (id != null && !id.isEmpty()) {
                            Song song = this.matchedSong(this.songFrom(item));
                            if (song != null) {
                                songs.add(song);
                            }
                        }
                        pb.step(item.Name != null ? item.Name : id);
                    }
                }
                if (!songs.isEmpty()) {
                    return songs;
                }
            }
        } catch (QueryException e) {
            logger.error("Exception while getting jellyfin album songs: " + albumId, e);
        }
        logger.debug("Unable to get ids of album songs: " + albumId);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAlbums() {
        logger.debug("Unsupported method uploadAlbums called");
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAlbum(Album album) {
        logger.debug("Unsupported method uploadAlbum called");
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbums() {
        logger.debug("Unsupported method syncAlbums called");
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbum(Album Album) {
        logger.debug("Unsupported method syncAlbum called");
    }

    // https://api.jellyfin.org/#tag/UserLibrary/operation/GetItem
    /**
     * <p>
     * getSong. answered from the index once it exists, the single item query is
     * only sent before the first name had to be resolved.
     * </p>
     *
     * @param songId a {@link java.lang.String} object
     * @return a {@link ryzen.ownitall.classes.Song} object
     * @throws java.lang.InterruptedException if any.
     */
    public Song getSong(String songId) throws InterruptedException {
        if (songId == null) {
            logger.debug("empty or null songId provided in getSong");
            return null;
        }
        if (this.itemsById != null) {
            BaseItemDto indexed = this.itemsById.get(songId);
            if (indexed != null) {
                return this.matchedSong(this.songFrom(indexed));
            }
        }
        WebTarget target = WebTools.target(Settings.jellyfinURL).path("Items").path(songId)
                .queryParam("userId", this.userId);
        try {
            BaseItemDto response = WebTools.get(target, this.headers, BaseItemDto.class);
            if (response != null) {
                return this.matchedSong(this.songFrom(response));
            }
        } catch (QueryException e) {
            logger.error("Exception while getting jellyfin song '" + songId + "'", e);
        }
        logger.debug("Unable to find song '" + songId + "' in jellyfin");
        return null;
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    /**
     * <p>
     * getSongId. resolved from the library index instead of one search query
     * per song; the searchTerm query is only the fallback for names the index
     * does not hold.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException if any.
     */
    public String getSongId(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("null song provided in getSongId");
            return null;
        }
        if (song.getId("jellyfin") != null) {
            return song.getId("jellyfin");
        }
        this.ensureIndex();
        String songId = this.lookupId(song);
        if (songId == null) {
            songId = this.searchSongId(song);
        }
        if (songId != null) {
            song.addId("jellyfin", songId);
            return songId;
        }
        logger.debug("Could not find song '" + song.getName() + "' in jellyfin library");
        return null;
    }

    private String lookupId(Song song) {
        String name = this.normalize(song.getName());
        if (name.isEmpty()) {
            return null;
        }
        for (Artist artist : song.getArtists()) {
            String id = this.idByArtistAndName.get(this.normalize(artist.getName()) + "|" + name);
            if (id != null) {
                return id;
            }
        }
        return this.idByName.get(name);
    }

    private String searchSongId(Song song) {
        WebTarget target = WebTools.target(Settings.jellyfinURL).path("Items")
                .queryParam("userId", this.userId)
                .queryParam("searchTerm", song.getName());
        if (!song.getArtists().isEmpty()) {
            String artists = "";
            for (Artist artist : song.getArtists()) {
                artists += artist.getName() + " | ";
            }
            target = target.queryParam("artists", artists);
        }
        target = target.queryParam("mediaTypes", "Audio")
                .queryParam("recursive", "true")
                .queryParam("limit", 1);
        try {
            BaseItemDtoQueryResult response = WebTools.get(target, this.headers, BaseItemDtoQueryResult.class);
            if (response != null && response.Items != null && !response.Items.isEmpty()) {
                return response.Items.get(0).Id;
            }
        } catch (QueryException e) {
            logger.error("Exception while searching jellyfin for '" + song.getName() + "'", e);
        }
        return null;
    }
}
