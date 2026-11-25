package ryzen.ownitall.integrations;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;

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
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.WebTools.AuthenticationException;
import ryzen.ownitall.util.WebTools.QueryException;
import ryzen.ownitall.util.OAuth;
import ryzen.superbeans.tidal.Albums;
import ryzen.superbeans.tidal.Artists;
import ryzen.superbeans.tidal.Commons;
import ryzen.superbeans.tidal.Playlists;
import ryzen.superbeans.tidal.Search;
import ryzen.superbeans.tidal.Tracks;
import ryzen.superbeans.tidal.UserCollections;
import ryzen.superbeans.tidal.Users;

// https://developer.tidal.com/documentation
// https://developer.tidal.com/apiref
public class Tidal implements Import, Export, Sync {
    private static final Logger logger = new Logger(Tidal.class);
    private String token;
    private String userID;
    private Map<String, String> headers;
    private static final String baseUrl = "https://openapi.tidal.com/v2";
    private static final String jsonApi = "application/vnd.api+json";
    // the mutation payloads all cap their data list at fifty items
    private static final int batchLimit = 50;
    private static final ArrayList<String> scope = new ArrayList<>(
            Arrays.asList("collection.read", "collection.write", "playlists.read", "playlists.write"));

    public Tidal() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Tidal.class)) {
            logger.debug("Empty tidal credentials");
            throw new MissingSettingException(Tidal.class);
        }
        WebTarget target = WebTools.target("https://auth.tidal.com").path("v1").path("oauth2").path("token");
        WebTarget authorizeTarget = WebTools.target("https://login.tidal.com").path("authorize");
        this.token = OAuth.getOauthToken(target, authorizeTarget, Settings.tidalClientID, null, scope);
        this.headers = Map.of("Accept", "application/vnd.api+json", "Authorization", "Bearer " + this.token);
        this.userID = this.getUserID();
        logger.debug("Successfully authenticated into tidal as " + this.userID);
    }

    private String getUserID() throws AuthenticationException {
        WebTarget target = WebTools.target(baseUrl).path("users").path("me").queryParam("countryCode", "US");
        try {
            Users.Users_Single_Resource_Data_Document response = WebTools.get(target, this.headers,
                    Users.Users_Single_Resource_Data_Document.class);
            if (response != null && response.data != null && response.data.id != null) {
                return response.data.id;
            }
        } catch (QueryException e) {
            logger.warn("Exception while querying tidal: " + e);
        }
        throw new AuthenticationException("no user id in users/me response");
    }

    private String nextCursor(Commons.Links links) {
        if (links == null || links.next == null || links.meta == null) {
            return null;
        }
        return links.meta.nextCursor;
    }

    private ArrayList<Artist> getArtists(List<Artists.Artists_Resource_Object> artistNodes) {
        ArrayList<Artist> artists = new ArrayList<>();
        if (artistNodes == null) {
            return artists;
        }
        for (Artists.Artists_Resource_Object artistNode : artistNodes) {
            if (artistNode == null || artistNode.attributes == null) {
                continue;
            }
            Artist artist = new Artist(artistNode.attributes.name);
            artist.addId("tidal", artistNode.id);
            artists.add(artist);
        }
        return artists;
    }

    private Song getSong(Tracks.Tracks_Resource_Object songItem) {
        if (songItem == null || songItem.attributes == null) {
            logger.debug("null songItem provided in getSong");
            return null;
        }
        String id = songItem.id;
        String title = songItem.attributes.title;
        Song song = new Song(title);
        song.addId("tidal", id);
        if (songItem.attributes.duration != null) {
            song.setDuration(Duration.parse(songItem.attributes.duration));
        }
        WebTarget target = WebTools.target(baseUrl).path("tracks").path(id).path("relationships").path("artists")
                .queryParam("countryCode", "US").queryParam("include", "artists");
        try {
            Tracks.Tracks_Artists_Multi_Relationship_Data_Document artistsResponse = WebTools.get(target, this.headers,
                    Tracks.Tracks_Artists_Multi_Relationship_Data_Document.class);
            if (artistsResponse != null) {
                song.addArtists(this.getArtists(artistsResponse.included));
            }
        } catch (QueryException e) {
            logger.warn("Exception while querying tidal: " + e);
        }
        return song;
    }

    /**
     * songs out of an items sideload: the union members arrive as maps and are
     * rebound per member, skipping the videos this importer does not model
     */
    private ArrayList<Song> songsFromItems(List<Object> included) {
        ArrayList<Song> songs = new ArrayList<>();
        if (included == null) {
            return songs;
        }
        for (Object member : included) {
            Tracks.Tracks_Resource_Object item = WebTools.convert(member, Tracks.Tracks_Resource_Object.class);
            if (item == null || item.attributes == null || item.attributes.title == null) {
                continue;
            }
            Song song = this.getSong(item);
            if (song != null) {
                songs.add(song);
            }
        }
        return songs;
    }

    private ArrayList<Song> getAlbumItems(String albumId, int amount) {
        ArrayList<Song> songs = new ArrayList<>();
        String pageCursor = null;
        WebTarget target = WebTools.target(baseUrl).path("albums").path(albumId).path("relationships").path("items")
                .queryParam("countryCode", "US").queryParam("include", "items");
        try (IPIterator<?> pb = IPIterator.manual(target.getUri().getPath(), -1)) {
            while (true) {
                WebTarget page = pageCursor == null ? target : target.queryParam("page[cursor]", pageCursor);
                try {
                    Albums.Albums_Items_Multi_Relationship_Data_Document response = WebTools.get(page, this.headers,
                            Albums.Albums_Items_Multi_Relationship_Data_Document.class);
                    if (response == null) {
                        logger.debug("null response received in getAlbumItems");
                        break;
                    }
                    for (Song song : this.songsFromItems(response.included)) {
                        pb.step(song.getName());
                        songs.add(song);
                    }
                    pageCursor = this.nextCursor(response.links);
                    if (pageCursor == null) {
                        break;
                    }
                    // this is needed because tidal likes to loop playlists, YAY
                    if (amount != -1 && songs.size() >= amount) {
                        logger.debug("songs capita reached");
                        break;
                    }
                } catch (QueryException e) {
                    logger.warn("Exception while querying tidal: " + e);
                    break;
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting songs from " + target.getUri().getPath());
            return null;
        }
        return songs;
    }

    private ArrayList<Song> getPlaylistItems(String playlistId, int amount) {
        ArrayList<Song> songs = new ArrayList<>();
        String pageCursor = null;
        WebTarget target = WebTools.target(baseUrl).path("playlists").path(playlistId).path("relationships")
                .path("items").queryParam("countryCode", "US").queryParam("include", "items");
        try (IPIterator<?> pb = IPIterator.manual(target.getUri().getPath(), -1)) {
            while (true) {
                WebTarget page = pageCursor == null ? target : target.queryParam("page[cursor]", pageCursor);
                try {
                    Playlists.Playlists_Items_Multi_Relationship_Data_Document response = WebTools.get(page,
                            this.headers, Playlists.Playlists_Items_Multi_Relationship_Data_Document.class);
                    if (response == null) {
                        logger.debug("null response received in getPlaylistItems");
                        break;
                    }
                    for (Song song : this.songsFromItems(response.included)) {
                        pb.step(song.getName());
                        songs.add(song);
                    }
                    pageCursor = this.nextCursor(response.links);
                    if (pageCursor == null) {
                        break;
                    }
                    // this is needed because tidal likes to loop playlists, YAY
                    if (amount != -1 && songs.size() >= amount) {
                        logger.debug("songs capita reached");
                        break;
                    }
                } catch (QueryException e) {
                    logger.warn("Exception while querying tidal: " + e);
                    break;
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting songs from " + target.getUri().getPath());
            return null;
        }
        return songs;
    }

    public LikedSongs getLikedSongs() {
        LikedSongs likedSongs = new LikedSongs();
        String pageCursor = null;
        WebTarget target = WebTools.target(baseUrl).path("userCollections").path(this.userID).path("relationships")
                .path("tracks").queryParam("countryCode", "US").queryParam("include", "tracks");
        try (IPIterator<?> pb = IPIterator.manual("Liked Songs", -1)) {
            while (true) {
                WebTarget page = pageCursor == null ? target : target.queryParam("page[cursor]", pageCursor);
                try {
                    UserCollections.UserCollectionTracks_Items_Multi_Relationship_Data_Document response = WebTools.get(
                            page, this.headers,
                            UserCollections.UserCollectionTracks_Items_Multi_Relationship_Data_Document.class);
                    if (response == null) {
                        logger.warn("null response received in tidal likedsongs query");
                        break;
                    }
                    if (response.included != null) {
                        for (Tracks.Tracks_Resource_Object songItem : response.included) {
                            Song song = this.getSong(songItem);
                            if (song != null) {
                                pb.step(song.toString());
                                likedSongs.addSong(song);
                            }
                        }
                    } else {
                        logger.debug("No songs in tidal likedsongs query");
                    }
                    pageCursor = this.nextCursor(response.links);
                    if (pageCursor == null) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.warn("Exception while querying tidal: " + e);
                    break;
                }
            }
            return likedSongs;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing liked songs");
            return null;
        }
    }

    public Album getAlbum(String albumId, String albumName, String albumArtistName) {
        if (albumId == null) {
            logger.debug("null albumID provided in getAlbum");
            return null;
        }
        WebTarget target = WebTools.target(baseUrl).path("albums").path(albumId).queryParam("countryCode", "US");
        try {
            Albums.Albums_Single_Resource_Data_Document response = WebTools.get(target, this.headers,
                    Albums.Albums_Single_Resource_Data_Document.class);
            if (response == null || response.data == null || response.data.attributes == null) {
                logger.debug("null response received in getAlbum");
                return null;
            }
            String id = response.data.id;
            Album album = new Album(response.data.attributes.title);
            album.addId("tidal", id);

            WebTarget target2 = WebTools.target(baseUrl).path("albums").path(id).path("relationships").path("artists")
                    .queryParam("countryCode", "US").queryParam("include", "artists");
            try {
                Albums.Albums_Artists_Multi_Relationship_Data_Document artistsResponse = WebTools.get(target2,
                        this.headers, Albums.Albums_Artists_Multi_Relationship_Data_Document.class);
                if (artistsResponse != null) {
                    album.addArtists(this.getArtists(artistsResponse.included));
                }
            } catch (QueryException e) {
                logger.warn("Exception while querying tidal: " + e);
            }
            WebTarget target3 = WebTools.target(baseUrl).path("albums").path(id).path("relationships").path("coverArt")
                    .queryParam("countryCode", "US").queryParam("include", "coverArt");
            try {
                Albums.Albums_CoverArt_Multi_Relationship_Data_Document coverResponse = WebTools.get(target3,
                        this.headers, Albums.Albums_CoverArt_Multi_Relationship_Data_Document.class);
                if (coverResponse != null && coverResponse.included != null && !coverResponse.included.isEmpty()) {
                    album.setCoverImage(this.artworkHref(coverResponse.included.get(0)));
                }
            } catch (QueryException e) {
                logger.warn("Exception while querying tidal: " + e);
            }
            ArrayList<Song> songs = this.getAlbumItems(id, -1);
            if (songs != null) {
                album.addSongs(songs);
            }
            return album;
        } catch (QueryException e) {
            logger.warn("Exception while querying tidal: " + e);
            logger.debug("null response received in getAlbum");
        }
        return null;
    }

    public ArrayList<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<>();
        String pageCursor = null;
        WebTarget target = WebTools.target(baseUrl).path("userCollections").path(this.userID).path("relationships")
                .path("albums").queryParam("countryCode", "US").queryParam("include", "albums");
        try (IPIterator<?> pb = IPIterator.manual("Albums", -1)) {
            while (true) {
                WebTarget page = pageCursor == null ? target : target.queryParam("page[cursor]", pageCursor);
                try {
                    UserCollections.UserCollectionAlbums_Items_Multi_Relationship_Data_Document response = WebTools.get(
                            page, this.headers,
                            UserCollections.UserCollectionAlbums_Items_Multi_Relationship_Data_Document.class);
                    if (response == null) {
                        logger.debug("null response received in getAlbums");
                        break;
                    }
                    if (response.included != null) {
                        for (Albums.Albums_Resource_Object albumEntry : response.included) {
                            Album album = this.getAlbum(albumEntry.id, albumEntry.attributes.title, null);
                            if (album != null) {
                                pb.step(album.toString());
                                albums.add(album);
                            }
                        }
                    } else {
                        logger.debug("No albums in tidal albums query");
                    }
                    pageCursor = this.nextCursor(response.links);
                    if (pageCursor == null) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.warn("Exception while querying tidal: " + e);
                    break;
                }
            }
            return albums;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing albums");
            return null;
        }
    }

    private String artworkHref(ryzen.superbeans.tidal.Catalog.Artworks_Resource_Object artwork) {
        if (artwork == null || artwork.attributes == null || artwork.attributes.files == null
                || artwork.attributes.files.isEmpty() || artwork.attributes.files.get(0).href == null) {
            return null;
        }
        return artwork.attributes.files.get(0).href;
    }

    public Playlist getPlaylist(String playlistId, String playlistName) {
        if (playlistId == null) {
            logger.debug("null playlistId provided in getPlaylist");
            return null;
        }
        WebTarget target = WebTools.target(baseUrl).path("playlists").path(playlistId)
                .queryParam("countryCode", "US");
        try {
            Playlists.Playlists_Single_Resource_Data_Document response = WebTools.get(target, this.headers,
                    Playlists.Playlists_Single_Resource_Data_Document.class);
            if (response == null || response.data == null || response.data.attributes == null) {
                logger.debug("null response received in getPlaylist");
                return null;
            }
            String id = response.data.id;
            Playlist playlist = new Playlist(response.data.attributes.name);
            playlist.addId("tidal", id);

            WebTarget target2 = WebTools.target(baseUrl).path("playlists").path(id).path("relationships").path("coverArt")
                    .queryParam("countryCode", "US").queryParam("include", "coverArt");
            try {
                Playlists.Playlists_CoverArt_Multi_Relationship_Data_Document coverResponse = WebTools.get(target2,
                        this.headers, Playlists.Playlists_CoverArt_Multi_Relationship_Data_Document.class);
                if (coverResponse != null && coverResponse.included != null && !coverResponse.included.isEmpty()) {
                    playlist.setCoverImage(this.artworkHref(coverResponse.included.get(0)));
                }
            } catch (QueryException e) {
                logger.warn("Exception while querying tidal: " + e);
            }
            int size = response.data.attributes.numberOfItems != null ? response.data.attributes.numberOfItems : -1;
            ArrayList<Song> songs = this.getPlaylistItems(id, size);
            if (songs != null) {
                playlist.addSongs(songs);
            }
            if (playlist.isEmpty()) {
                logger.debug("getPlaylist " + playlistId + " playlist is empty");
                return null;
            }
            return playlist;
        } catch (QueryException e) {
            logger.warn("Exception while querying tidal: " + e);
            return null;
        }
    }

    public ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<>();
        String pageCursor = null;
        WebTarget target = WebTools.target(baseUrl).path("userCollections").path(this.userID).path("relationships")
                .path("playlists").queryParam("countryCode", "US").queryParam("include", "playlists");
        try (IPIterator<?> pb = IPIterator.manual("Playlists", -1)) {
            while (true) {
                WebTarget page = pageCursor == null ? target : target.queryParam("page[cursor]", pageCursor);
                try {
                    UserCollections.UserCollectionPlaylists_Items_Multi_Relationship_Data_Document response = WebTools.get(
                            page, this.headers,
                            UserCollections.UserCollectionPlaylists_Items_Multi_Relationship_Data_Document.class);
                    if (response == null) {
                        logger.debug("null response received in getPlaylists");
                        break;
                    }
                    if (response.included != null) {
                        for (Object member : response.included) {
                            Playlists.Playlists_Resource_Object playlistEntry = WebTools.convert(member,
                                    Playlists.Playlists_Resource_Object.class);
                            if (playlistEntry == null || playlistEntry.attributes == null) {
                                continue;
                            }
                            Playlist playlist = this.getPlaylist(playlistEntry.id, playlistEntry.attributes.name);
                            if (playlist != null) {
                                pb.step(playlist.toString());
                                playlists.add(playlist);
                            }
                        }
                    } else {
                        logger.debug("No playlists in tidal playlists query");
                    }
                    pageCursor = this.nextCursor(response.links);
                    if (pageCursor == null) {
                        break;
                    }
                } catch (QueryException e) {
                    logger.warn("Exception while querying tidal: " + e);
                    break;
                }
            }
            return playlists;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing playlists");
            return null;
        }
    }

    /**
     * search. tidal answers a query with one searchResults resource per query;
     * the ids of the matches hang off that resource as relationships, so a
     * lookup is the query followed by one relationship read of the type wanted
     *
     * @param query what to search for
     * @return the searchResults id, null when the query matched nothing
     */
    private String search(String query) {
        WebTarget target = WebTools.target(baseUrl).path("searchResults")
                .queryParam("filter[query]", query).queryParam("countryCode", "US");
        try {
            Search.SearchResults_Multi_Resource_Data_Document response = WebTools.get(target, this.headers,
                    Search.SearchResults_Multi_Resource_Data_Document.class);
            if (response == null || response.data == null || response.data.isEmpty()) {
                logger.debug("Nothing found searching tidal for '" + query + "'");
                return null;
            }
            return response.data.get(0).id;
        } catch (QueryException e) {
            logger.warn("Exception searching tidal for '" + query + "': " + e);
            return null;
        }
    }

    /**
     * <p>
     * getTrackId. a collection song without a tidal id on it yet takes the
     * first track the search answers with
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
        if (song.getId("tidal") != null) {
            return song.getId("tidal");
        }
        String searchId = this.search(song.toString());
        if (searchId == null) {
            return null;
        }
        WebTarget target = WebTools.target(baseUrl).path("searchResults").path(searchId).path("relationships")
                .path("tracks").queryParam("countryCode", "US");
        try {
            Search.SearchResults_Tracks_Multi_Relationship_Data_Document response = WebTools.get(target, this.headers,
                    Search.SearchResults_Tracks_Multi_Relationship_Data_Document.class);
            if (response == null || response.data == null || response.data.isEmpty()) {
                logger.debug("Song '" + song.toString() + "' not found");
                return null;
            }
            String id = response.data.get(0).id;
            song.addId("tidal", id);
            return id;
        } catch (QueryException e) {
            logger.warn("Exception searching for song: " + e);
            return null;
        }
    }

    /**
     * <p>
     * getAlbumId. a collection album without a tidal id on it yet takes the
     * first album the search answers with
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
        if (album.getId("tidal") != null) {
            return album.getId("tidal");
        }
        String searchId = this.search(album.toString());
        if (searchId == null) {
            return null;
        }
        WebTarget target = WebTools.target(baseUrl).path("searchResults").path(searchId).path("relationships")
                .path("albums").queryParam("countryCode", "US");
        try {
            Search.SearchResults_Albums_Multi_Relationship_Data_Document response = WebTools.get(target, this.headers,
                    Search.SearchResults_Albums_Multi_Relationship_Data_Document.class);
            if (response == null || response.data == null || response.data.isEmpty()) {
                logger.debug("Album '" + album.toString() + "' not found");
                return null;
            }
            String id = response.data.get(0).id;
            album.addId("tidal", id);
            return id;
        } catch (QueryException e) {
            logger.warn("Exception searching for album: " + e);
            return null;
        }
    }

    /**
     * <p>
     * getPlaylistId. tidal gives playlists no lookup by name, so the own
     * collection's playlist relationships are paged until one of them answers
     * to the name
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
        String pageCursor = null;
        WebTarget target = WebTools.target(baseUrl).path("userCollections").path(this.userID).path("relationships")
                .path("playlists").queryParam("countryCode", "US").queryParam("include", "playlists");
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            while (true) {
                interruptionHandler.checkInterruption();
                WebTarget page = pageCursor == null ? target : target.queryParam("page[cursor]", pageCursor);
                try {
                    UserCollections.UserCollectionPlaylists_Items_Multi_Relationship_Data_Document response = WebTools.get(
                            page, this.headers,
                            UserCollections.UserCollectionPlaylists_Items_Multi_Relationship_Data_Document.class);
                    if (response == null) {
                        break;
                    }
                    if (response.included != null) {
                        for (Object member : response.included) {
                            Playlists.Playlists_Resource_Object playlistEntry = WebTools.convert(member,
                                    Playlists.Playlists_Resource_Object.class);
                            if (playlistEntry == null || playlistEntry.attributes == null) {
                                continue;
                            }
                            if (playlistEntry.attributes.name != null
                                    && playlistEntry.attributes.name.equalsIgnoreCase(playlist.getName())) {
                                playlist.addId("tidal", playlistEntry.id);
                                return playlistEntry.id;
                            }
                        }
                    }
                    pageCursor = this.nextCursor(response.links);
                    if (pageCursor == null) {
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
     * send a batch of tidal ids against one collection relationship: post adds
     * them to the collection, delete takes them back out, both carrying the
     * same document of item identifiers
     */
    private void sendIds(WebTarget target, IdsVerb verb, String label, ArrayList<String> ids,
            Function<List<String>, Object> request) throws InterruptedException {
        try (IPIterator<?> pb = IPIterator.manual(label, ids.size())) {
            for (int offset = 0; offset < ids.size(); offset += batchLimit) {
                List<String> currentIds = ids.subList(offset, Math.min(offset + batchLimit, ids.size()));
                try {
                    verb.send(target, this.headers, Entity.entity(WebTools.json(request.apply(currentIds)), jsonApi),
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
     * the collection mutation documents, one per route and direction: the add
     * ones stamp when the item joined, the remove ones leave that out and
     * json() drops the null meta, which is exactly the remove payloads ask for
     */
    private static UserCollections.UserCollectionTracks_Items_Add_Multi_Relationship_Data_Document saveTracks(
            List<String> ids) {
        UserCollections.UserCollectionTracks_Items_Add_Multi_Relationship_Data_Document request = new UserCollections.UserCollectionTracks_Items_Add_Multi_Relationship_Data_Document();
        List<UserCollections.UserCollectionTracks_Items_Add_Resource_Identifier> data = new ArrayList<>();
        for (String id : ids) {
            UserCollections.UserCollectionTracks_Items_Add_Resource_Identifier item = new UserCollections.UserCollectionTracks_Items_Add_Resource_Identifier();
            item.id = id;
            item.type = "tracks";
            UserCollections.UserCollectionTracks_Items_Add_Resource_Identifier_Meta meta = new UserCollections.UserCollectionTracks_Items_Add_Resource_Identifier_Meta();
            meta.addedAt = Instant.now();
            item.meta = meta;
            data.add(item);
        }
        request.data = data;
        return request;
    }

    private static UserCollections.UserCollectionTracks_Items_Add_Multi_Relationship_Data_Document removeTracks(
            List<String> ids) {
        UserCollections.UserCollectionTracks_Items_Add_Multi_Relationship_Data_Document request = new UserCollections.UserCollectionTracks_Items_Add_Multi_Relationship_Data_Document();
        List<UserCollections.UserCollectionTracks_Items_Add_Resource_Identifier> data = new ArrayList<>();
        for (String id : ids) {
            UserCollections.UserCollectionTracks_Items_Add_Resource_Identifier item = new UserCollections.UserCollectionTracks_Items_Add_Resource_Identifier();
            item.id = id;
            item.type = "tracks";
            data.add(item);
        }
        request.data = data;
        return request;
    }

    private static UserCollections.UserCollectionAlbums_Items_Add_Multi_Relationship_Data_Document saveAlbums(
            List<String> ids) {
        UserCollections.UserCollectionAlbums_Items_Add_Multi_Relationship_Data_Document request = new UserCollections.UserCollectionAlbums_Items_Add_Multi_Relationship_Data_Document();
        List<UserCollections.UserCollectionAlbums_Items_Add_Resource_Identifier> data = new ArrayList<>();
        for (String id : ids) {
            UserCollections.UserCollectionAlbums_Items_Add_Resource_Identifier item = new UserCollections.UserCollectionAlbums_Items_Add_Resource_Identifier();
            item.id = id;
            item.type = "albums";
            UserCollections.UserCollectionAlbums_Items_Add_Resource_Identifier_Meta meta = new UserCollections.UserCollectionAlbums_Items_Add_Resource_Identifier_Meta();
            meta.addedAt = Instant.now();
            item.meta = meta;
            data.add(item);
        }
        request.data = data;
        return request;
    }

    private static UserCollections.UserCollectionAlbums_Items_Add_Multi_Relationship_Data_Document removeAlbums(
            List<String> ids) {
        UserCollections.UserCollectionAlbums_Items_Add_Multi_Relationship_Data_Document request = new UserCollections.UserCollectionAlbums_Items_Add_Multi_Relationship_Data_Document();
        List<UserCollections.UserCollectionAlbums_Items_Add_Resource_Identifier> data = new ArrayList<>();
        for (String id : ids) {
            UserCollections.UserCollectionAlbums_Items_Add_Resource_Identifier item = new UserCollections.UserCollectionAlbums_Items_Add_Resource_Identifier();
            item.id = id;
            item.type = "albums";
            data.add(item);
        }
        request.data = data;
        return request;
    }

    private static Playlists.PlaylistsItemsRelationshipAddOperation_Payload addPlaylistTracks(List<String> ids) {
        Playlists.PlaylistsItemsRelationshipAddOperation_Payload request = new Playlists.PlaylistsItemsRelationshipAddOperation_Payload();
        List<Playlists.PlaylistsItemsRelationshipAddOperation_Payload_Data> data = new ArrayList<>();
        for (String id : ids) {
            Playlists.PlaylistsItemsRelationshipAddOperation_Payload_Data item = new Playlists.PlaylistsItemsRelationshipAddOperation_Payload_Data();
            item.id = id;
            item.type = "tracks";
            Playlists.PlaylistsItemsRelationshipAddOperation_Payload_Data_Meta meta = new Playlists.PlaylistsItemsRelationshipAddOperation_Payload_Data_Meta();
            meta.addedAt = Instant.now();
            item.meta = meta;
            data.add(item);
        }
        request.data = data;
        return request;
    }

    private static Playlists.PlaylistsItemsRelationshipAddOperation_Payload removePlaylistTracks(List<String> ids) {
        Playlists.PlaylistsItemsRelationshipAddOperation_Payload request = new Playlists.PlaylistsItemsRelationshipAddOperation_Payload();
        List<Playlists.PlaylistsItemsRelationshipAddOperation_Payload_Data> data = new ArrayList<>();
        for (String id : ids) {
            Playlists.PlaylistsItemsRelationshipAddOperation_Payload_Data item = new Playlists.PlaylistsItemsRelationshipAddOperation_Payload_Data();
            item.id = id;
            item.type = "tracks";
            data.add(item);
        }
        request.data = data;
        return request;
    }

    /**
     * <p>
     * IdsVerb, the one webtools verb that carries the batch: {@link WebTools#post}
     * or {@link WebTools#delete}, whichever way the ids are meant to move.
     * </p>
     */
    @FunctionalInterface
    private interface IdsVerb {
        <T> T send(WebTarget target, Map<String, ?> headers, Entity<?> body, Class<T> responseType)
                throws QueryException;
    }

    /** {@inheritDoc} */
    @Override
    public void uploadLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        likedSongs.addSongs(Collection.getLikedSongs().getSongs());
        logger.info("Getting current liked songs to prevent duplicates");
        LikedSongs currentLikedSongs = this.getLikedSongs();
        if (currentLikedSongs != null) {
            likedSongs.removeSongs(currentLikedSongs.getSongs());
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
        WebTarget target = WebTools.target(baseUrl).path("userCollectionTracks").path("me").path("relationships")
                .path("items");
        this.sendIds(target, WebTools::post, "Liked Songs", songIds, Tidal::saveTracks);
    }

    /** {@inheritDoc} */
    @Override
    public void syncLikedSongs() throws InterruptedException {
        logger.debug("Getting tidal liked songs to remove mismatches");
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
            WebTarget target = WebTools.target(baseUrl).path("userCollectionTracks").path("me").path("relationships")
                    .path("items");
            this.sendIds(target, WebTools::delete, "Liked Songs", songIds, Tidal::removeTracks);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAlbums() throws InterruptedException {
        ArrayList<String> albumIds = new ArrayList<>();
        for (Album album : Collection.getAlbums()) {
            String id = this.getAlbumId(album);
            if (id != null) {
                albumIds.add(id);
            }
        }
        if (albumIds.isEmpty()) {
            logger.debug("No saved albums in collection");
            return;
        }
        WebTarget target = WebTools.target(baseUrl).path("userCollectionAlbums").path("me").path("relationships")
                .path("items");
        this.sendIds(target, WebTools::post, "Albums", albumIds, Tidal::saveAlbums);
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided in uploadAlbum");
            return;
        }
        String id = this.getAlbumId(album);
        if (id == null) {
            logger.debug("Album '" + album.toString() + "' not found on tidal");
            return;
        }
        ArrayList<String> albumIds = new ArrayList<>();
        albumIds.add(id);
        WebTarget target = WebTools.target(baseUrl).path("userCollectionAlbums").path("me").path("relationships")
                .path("items");
        this.sendIds(target, WebTools::post, album.getName(), albumIds, Tidal::saveAlbums);
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbums() throws InterruptedException {
        logger.debug("Getting tidal album ids to remove mismatches");
        ArrayList<String> savedIds = this.getSavedAlbumIds();
        Set<String> collectionIds = new HashSet<>();
        for (Album album : Collection.getAlbums()) {
            String id = this.getAlbumId(album);
            if (id != null) {
                collectionIds.add(id);
            }
        }
        savedIds.removeAll(collectionIds);
        if (savedIds.isEmpty()) {
            return;
        }
        WebTarget target = WebTools.target(baseUrl).path("userCollectionAlbums").path("me").path("relationships")
                .path("items");
        this.sendIds(target, WebTools::delete, "Albums", savedIds, Tidal::removeAlbums);
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbum(Album album) throws InterruptedException {
        // saving an already saved album is a no-op on tidal, and removals of
        // whole albums are syncAlbums' business
        this.uploadAlbum(album);
    }

    /**
     * the ids of the saved albums alone, paged out of the collection
     * relationship without any of the album details the importer would load
     */
    private ArrayList<String> getSavedAlbumIds() throws InterruptedException {
        ArrayList<String> ids = new ArrayList<>();
        String pageCursor = null;
        WebTarget target = WebTools.target(baseUrl).path("userCollectionAlbums").path("me").path("relationships")
                .path("items").queryParam("countryCode", "US");
        while (true) {
            WebTarget page = pageCursor == null ? target : target.queryParam("page[cursor]", pageCursor);
            try {
                UserCollections.UserCollectionAlbums_Items_Multi_Relationship_Data_Document response = WebTools.get(page,
                        this.headers,
                        UserCollections.UserCollectionAlbums_Items_Multi_Relationship_Data_Document.class);
                if (response == null) {
                    break;
                }
                if (response.data != null) {
                    for (UserCollections.UserCollectionAlbums_Items_Resource_Identifier item : response.data) {
                        if (item != null && item.id != null) {
                            ids.add(item.id);
                        }
                    }
                }
                pageCursor = this.nextCursor(response.links);
                if (pageCursor == null) {
                    break;
                }
            } catch (QueryException e) {
                logger.warn("Exception while querying tidal: " + e);
                break;
            }
        }
        return ids;
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
    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in uploadPlaylist");
            return;
        }
        String playlistId = this.getPlaylistId(playlist);
        ArrayList<Song> currentSongs = new ArrayList<>();
        if (playlistId != null) {
            currentSongs = this.getPlaylistItems(playlistId, -1);
        }
        ArrayList<Song> songs = new ArrayList<>(playlist.getSongs());
        if (currentSongs == null) {
            currentSongs = new ArrayList<>();
        }
        if (currentSongs.isEmpty()) {
            Playlists.PlaylistsCreateOperation_Payload body = new Playlists.PlaylistsCreateOperation_Payload();
            Playlists.PlaylistsCreateOperation_Payload_Data data = new Playlists.PlaylistsCreateOperation_Payload_Data();
            data.type = "playlists";
            Playlists.PlaylistsCreateOperation_Payload_Data_Attributes attributes = new Playlists.PlaylistsCreateOperation_Payload_Data_Attributes();
            attributes.name = playlist.getName();
            if (playlist.getDescription() != null) {
                attributes.description = playlist.getDescription();
            }
            data.attributes = attributes;
            body.data = data;
            WebTarget target = WebTools.target(baseUrl).path("playlists");
            try {
                Playlists.Playlists_Create_Single_Resource_Data_Document created = WebTools.post(target, this.headers,
                        Entity.entity(WebTools.json(body), jsonApi),
                        Playlists.Playlists_Create_Single_Resource_Data_Document.class);
                if (created != null && created.data != null && created.data.id != null) {
                    playlistId = created.data.id;
                    logger.debug("Created new playlist: " + playlist.getName());
                    playlist.addId("tidal", playlistId);
                }
            } catch (QueryException | JsonProcessingException e) {
                logger.warn("Exception creating tidal playlist '" + playlist.getName() + "': " + e);
            }
        } else {
            // filter out the existing playlist songs
            songs.removeAll(currentSongs);
        }
        if (playlistId == null) {
            logger.debug("No playlist to upload '" + playlist.toString() + "' to");
            return;
        }
        ArrayList<String> trackIds = new ArrayList<>();
        for (Song song : songs) {
            String id = this.getTrackId(song);
            if (id != null) {
                trackIds.add(id);
            }
        }
        if (trackIds.isEmpty()) {
            logger.debug("Playlist '" + playlist.toString() + "' is empty / no update");
            return;
        }
        WebTarget target = WebTools.target(baseUrl).path("playlists").path(playlistId).path("relationships")
                .path("items");
        this.sendIds(target, WebTools::post, playlist.getName(), trackIds, Tidal::addPlaylistTracks);
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylists() throws InterruptedException {
        logger.debug("Getting tidal playlists to remove mismatches");
        ArrayList<Playlist> playlists = this.getPlaylists();
        if (playlists != null && !playlists.isEmpty()) {
            playlists.removeAll(Collection.getPlaylists());
            for (Playlist playlist : playlists) {
                String playlistId = playlist.getId("tidal");
                if (playlistId == null) {
                    continue;
                }
                WebTarget target = WebTools.target(baseUrl).path("playlists").path(playlistId);
                try {
                    WebTools.delete(target, this.headers, Object.class);
                    logger.info("Deleted tidal playlist '" + playlist.getName()
                            + "' found on tidal but not in the collection");
                } catch (QueryException e) {
                    logger.warn("Exception deleting playlist '" + playlist.getName() + "': " + e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in syncPlaylist");
            return;
        }
        logger.debug("Getting tidal playlist '" + playlist.getName() + "' to remove mismatches");
        String playlistId = this.getPlaylistId(playlist);
        if (playlistId == null) {
            return;
        }
        ArrayList<Song> songs = this.getPlaylistItems(playlistId, -1);
        if (songs == null || songs.isEmpty()) {
            return;
        }
        // only delete one when there are duplicate songs
        songs.removeIf(playlist::contains);
        ArrayList<String> trackIds = new ArrayList<>();
        for (Song song : songs) {
            String id = song.getId("tidal");
            if (id != null) {
                trackIds.add(id);
            }
        }
        if (trackIds.isEmpty()) {
            return;
        }
        WebTarget target = WebTools.target(baseUrl).path("playlists").path(playlistId).path("relationships")
                .path("items");
        this.sendIds(target, WebTools::delete, playlist.getName(), trackIds, Tidal::removePlaylistTracks);
    }
}
