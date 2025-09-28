package ryzen.ownitall.method;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.method.interfaces.Export;
import ryzen.ownitall.method.interfaces.Import;
import ryzen.ownitall.method.interfaces.Sync;
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;
import ryzen.ownitall.util.exceptions.QueryException;

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
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private String userId;
    private String accessToken;

    // https://api.jellyfin.org/
    /**
     * <p>
     * Constructor for Jellyfin.
     * </p>
     *
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public Jellyfin() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Jellyfin.class)) {
            logger.debug("Empty jellyfin credentials");
            throw new MissingSettingException(Jellyfin.class);
        }
        this.authenticate();
    }

    // https://api.jellyfin.org/#tag/User/operation/AuthenticateUserByName
    private void authenticate() throws AuthenticationException {
        ObjectNode credsNode = objectMapper.createObjectNode();
        credsNode.put("Username", Settings.jellyfinUsername);
        credsNode.put("Pw", Settings.jellyfinPassword);
        JsonNode response = this.payloadQuery("post", "/Users/AuthenticateByName", credsNode);
        if (response == null) {
            throw new AuthenticationException("Failed to authenticate with jellyfin");
        } else {
            logger.debug("Authenticated into jellyfin with user " + response.get("User").get("Name").asText());
            this.userId = response.get("User").get("Id").asText();
            this.accessToken = response.get("AccessToken").asText();
        }
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    /** {@inheritDoc} */
    @Override
    public LikedSongs getLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("mediaTypes", "Audio");
        params.put("recursive", "true");
        params.put("isFavorite", "true");
        JsonNode response = this.paramQuery("get", "/Items", params);
        if (response != null) {
            JsonNode itemsNode = response.get("Items");
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : IPIterator.wrap(itemsNode.iterator(), "Liked Songs", -1)) {
                    String id = itemNode.get("Id").asText();
                    if (id != null && !id.isEmpty()) {
                        Song song = this.getSong(id);
                        if (song != null) {
                            likedSongs.addSong(song);
                        }
                    }
                }
                if (!likedSongs.isEmpty()) {
                    return likedSongs;
                }
            }
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
                    this.paramQuery("delete", "/UserFavoriteItems/" + songId, new LinkedHashMap<>());
                    logger.debug("removed '" + song.toString() + "' favorite from jellyfin");
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
                this.paramQuery("post", "/UserFavoriteItems/" + songId, new LinkedHashMap<>());
            }
        }
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    /** {@inheritDoc} */
    @Override
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = new ArrayList<>();
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("IncludeItemTypes", "Playlist");
        params.put("recursive", "true");
        JsonNode response = this.paramQuery("get", "/Items", params);
        if (response != null) {
            JsonNode itemsNode = response.get("Items");
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : IPIterator.wrap(itemsNode.iterator(), "Playlists", -1)) {
                    Playlist playlist = this.getPlaylist(itemNode.get("Id").asText(),
                            itemNode.get("Name").asText());
                    if (playlist != null && !playlist.isEmpty()) {
                        playlists.add(playlist);
                    }
                }
                if (!playlists.isEmpty()) {
                    return playlists;
                }
            }
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
            return playlist;
        }
        return null;
    }

    // https://api.jellyfin.org/#tag/Playlists/operation/GetPlaylist
    /**
     * <p>
     * getPlaylistSongs.
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
        JsonNode response = this.paramQuery("get", "/Playlists/" + playlistId, new LinkedHashMap<>());
        if (response != null) {
            JsonNode itemIdsNode = response.get("ItemIds");
            if (itemIdsNode != null && itemIdsNode.isArray()) {
                for (JsonNode itemIdNode : IPIterator.wrap(itemIdsNode.iterator(), playlistId, -1)) {
                    Song song = this.getSong(itemIdNode.asText());
                    if (song != null) {
                        songs.add(song);
                    }
                }
            }
            if (!songs.isEmpty()) {
                return songs;
            }
        }
        logger.debug("Unable to find playlist songs '" + playlistId + "' in jellyfin");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void uploadPlaylists() {
        logger.debug("Unsupported method uploadPlaylists called");
    }

    /** {@inheritDoc} */
    @Override
    public void uploadPlaylist(Playlist playlist) {
        logger.debug("Unsupported method uploadPlaylist called");
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylists() {
        logger.debug("Unsupported method syncPlaylists called");
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylist(Playlist playlist) {
        logger.debug("Unsupported method syncPlaylist called");
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    /** {@inheritDoc} */
    @Override
    public ArrayList<Album> getAlbums() throws InterruptedException {
        ArrayList<Album> albums = new ArrayList<>();
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("IncludeItemTypes", "MusicAlbum");
        params.put("recursive", "true");
        JsonNode response = this.paramQuery("get", "/Items", params);
        if (response != null) {
            JsonNode itemsNode = response.get("Items");
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : IPIterator.wrap(itemsNode.iterator(), "Albums", -1)) {
                    String artistName = null;
                    JsonNode artistNode = response.get("Artists").get(0);
                    if (artistNode != null) {
                        artistName = artistNode.asText();
                    }
                    Album album = this.getAlbum(itemNode.get("Id").asText(), itemNode.get("Name").asText(),
                            artistName);
                    if (album != null && !album.isEmpty()) {
                        albums.add(album);
                    }
                }
                if (!albums.isEmpty()) {
                    return albums;
                }
            }
        }
        logger.debug("Unable to get ids of albums");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Album getAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        if (albumId == null) {
            logger.debug("null album id provided in getAlbum");
            return null;
        }
        Album album = new Album(albumName);
        if (albumArtistName != null) {
            album.addArtist(new Artist(albumArtistName));
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
     * getAlbumSongs.
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
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("IncludeItemTypes", "Audio");
        params.put("albumIds", albumId);
        params.put("recursive", "true");
        JsonNode response = this.paramQuery("get", "/Items", params);
        if (response != null) {
            JsonNode itemsNode = response.get("Items");
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    String id = itemNode.get("Id").asText();
                    if (id != null && !id.isEmpty()) {
                        Song song = this.getSong(id);
                        if (song != null) {
                            songs.add(song);
                        }
                    }
                }
                if (!songs.isEmpty()) {
                    return songs;
                }
            }
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
     * getSong.
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
        JsonNode response = this.paramQuery("get", "/Items/" + songId, new LinkedHashMap<>());
        if (response != null) {
            Song song = new Song(response.get("Name").asText());
            JsonNode artistNode = response.get("Artists").get(0);
            if (artistNode != null) {
                song.addArtist(new Artist(artistNode.asText()));
            }
            JsonNode albumNode = response.get("Album");
            if (albumNode != null) {
                String albumName = albumNode.asText();
                if (albumName != null && !albumName.isEmpty()) {
                    song.setAlbumName(albumName);
                }
            }
            if (library != null) {
                Song foundSong = library.getSong(song);
                if (foundSong != null) {
                    song = foundSong;
                } else if (Settings.libraryVerified) {
                    song = null;
                }
            }
            return song;
        }
        logger.debug("Unable to find song '" + songId + "' in jellyfin");
        return null;
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    /**
     * <p>
     * getSongId.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @return a {@link java.lang.String} object
     */
    public String getSongId(Song song) {
        if (song == null) {
            logger.debug("null song provided in getSongId");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("searchTerm", song.getName());
        if (!song.getArtists().isEmpty()) {
            String artists = "";
            for (Artist artist : song.getArtists()) {
                artists += artist.getName() + " | ";
            }
            params.put("artists", artists);
        }
        // this limits songs being found?
        // if (song.getAlbumName() != null) {
        // params.put("albums", song.getAlbumName());
        // }
        params.put("mediaTypes", "Audio");
        params.put("recursive", "true");
        params.put("limit", "1");
        JsonNode response = this.paramQuery("get", "/Items", params);
        if (response != null) {
            JsonNode responseNode = response.get("Items").get(0);
            if (responseNode != null) {
                // add id to song's id's?
                // not going to implement as lots of files are moved and their id's might not be
                // persistant
                // and might turn out being 2 queries (checking if song still same id, and then
                // performing action)
                return responseNode.path("Id").asText();
            }
        }
        logger.debug("Could not find song '" + song.getName() + "' in jellyfin library");
        return null;
    }

    private JsonNode paramQuery(String method, String type, LinkedHashMap<String, String> params) {
        if (method == null) {
            logger.debug("null or invalid method provided in paramQuery");
            return null;
        }
        if (type == null) {
            logger.debug("null or invalid type provided in paramQuery");
            return null;
        }
        if (params == null) {
            logger.debug("null or invalid parameters provided in paramQuery");
            return null;
        }
        StringBuilder builder = new StringBuilder();
        try {
            builder.append('?').append("userId").append('=').append(this.userId);
            for (String key : params.keySet()) {
                builder.append("&").append(key).append('=')
                        .append(URLEncoder.encode(params.get(key), StandardCharsets.UTF_8.toString()));
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Exception while encoding query", e);
            return null;
        }
        try {
            URI url = new URI(Settings.jellyfinURL + type + builder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestMethod(method.toUpperCase());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            // https://gist.github.com/nielsvanvelzen/ea047d9028f676185832e51ffaf12a6f
            String authHeader = String.format(
                    "MediaBrowser Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\", Token=\"%s\"",
                    "ownitall",
                    "Java",
                    Settings.jellyfinUsername + "ownitall",
                    "10.10.6",
                    this.accessToken);
            connection.setRequestProperty("Authorization", authHeader);
            return WebTools.query(connection);
        } catch (URISyntaxException e) {
            logger.error("Exception while constructing jellyfin paramQuery", e);
            return null;
        } catch (QueryException | IOException e) {
            logger.error("Exception while paramQuery jellyfin", e);
            return null;
        }
    }

    private JsonNode payloadQuery(String method, String type, JsonNode payload) {
        if (method == null) {
            logger.debug("null or invalid method provided in payloadQuery");
            return null;
        }
        if (type == null) {
            logger.debug("null or invalid type provided in payloadQuery");
            return null;
        }
        if (payload == null) {
            logger.debug("null or invalid parameters provided in payloadQuery");
            return null;
        }
        try {
            URI url = new URI(Settings.jellyfinURL + type);
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestMethod(method.toUpperCase());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            // https://gist.github.com/nielsvanvelzen/ea047d9028f676185832e51ffaf12a6f
            String authHeader = String.format(
                    "MediaBrowser Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\", Token=\"%s\"",
                    "ownitall",
                    "Java",
                    Settings.jellyfinUsername + "ownitall",
                    "10.10.6",
                    this.accessToken);
            connection.setRequestProperty("Authorization", authHeader);
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            return WebTools.query(connection);
        } catch (URISyntaxException e) {
            logger.error("Exception while constructing jellyfin payloadQuery", e);
            return null;
        } catch (QueryException | IOException e) {
            logger.warn("Exception while payloadQuery jellyfin: " + e);
            return null;
        }
    }
}
