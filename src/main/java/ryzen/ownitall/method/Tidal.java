package ryzen.ownitall.method;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.method.interfaces.Import;
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;
import ryzen.ownitall.util.exceptions.QueryException;

// https://developer.tidal.com/documentation
// https://developer.tidal.com/apiref
//TODO: export and sync
//TODO: progress bars
public class Tidal implements Import {
    private static final Logger logger = new Logger(Tidal.class);
    private String token;
    private String userID;
    private static final String baseUrl = "https://openapi.tidal.com/v2";
    private static final ArrayList<String> scope = new ArrayList<>(
            Arrays.asList("collection.read", "collection.write", "playlists.read", "playlists.write"));

    public Tidal() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Tidal.class)) {
            logger.debug("Empty tidal credentials");
            throw new MissingSettingException(Tidal.class);
        }
        this.token = WebTools.getOauthToken("https://auth.tidal.com/v1/oauth2/token?grant_type=authorization_code",
                "https://login.tidal.com/authorize?response_type=code",
                Settings.tidalClientID, scope);
        this.userID = this.getUserID();
        logger.debug("Successfully authenticated into tidal as " + this.userID);
    }

    private String getUserID() throws AuthenticationException {
        try {
            URI uri = new URI(baseUrl + "/users/me");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("accept", "application/vnd.api+json");
            connection.setRequestProperty("Authorization", "Bearer " + this.token);
            JsonNode response = WebTools.query(connection);
            if (response.has("data")) {
                return response.path("data").path("id").asText();
            } else {
                throw new AuthenticationException("no user id in response: " + response.toString());
            }
        } catch (IOException | QueryException e) {
            throw new AuthenticationException(e);
        } catch (URISyntaxException e) {
            logger.error("Invalid tidal oauth url constructed", e);
            return null;
        }
    }

    private JsonNode query(String path, String pageCursor, ArrayList<String> include) {
        String flags = "?countryCode=US";
        if (pageCursor != null) {
            flags += "&page[cursor]=" + pageCursor;
        }
        if (include != null) {
            for (String includeEntry : include) {
                flags += "&include=" + includeEntry;
            }
        }
        try {
            URI url = new URI(baseUrl + path + flags);
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestProperty("Accept", "application/vnd.api+json");
            connection.setRequestProperty("Authorization", "Bearer " + this.token);

            WebTools.queryPacer(500);
            return WebTools.query(connection);
        } catch (URISyntaxException e) {
            logger.error("Exception while constructing tidal query", e);
            return null;
        } catch (QueryException | IOException | InterruptedException e) {
            logger.warn("Exception while querying tidal: " + e);
            return null;
        }
    }

    private ArrayList<Artist> getArtists(JsonNode artistResponse) {
        if (artistResponse == null) {
            logger.debug("null artistResponse provided");
            return null;
        }
        ArrayList<Artist> artists = new ArrayList<>();
        JsonNode artistNodes = artistResponse.path("included");
        if (artistNodes != null && artistNodes.isArray()) {
            for (JsonNode artistNode : artistNodes) {
                String artistID = artistNode.path("id").asText();
                JsonNode artistAttributes = artistNode.path("attributes");
                String artistName = artistAttributes.path("name").asText();
                Artist artist = new Artist(artistName);
                artist.addId("tidal", artistID);
                artists.add(artist);
            }
        }
        return artists;
    }

    private Song getSong(JsonNode songItem) {
        if (songItem == null) {
            logger.debug("null songItem provided in getSong");
            return null;
        }
        String id = songItem.path("id").asText();
        JsonNode attributes = songItem.path("attributes");
        String title = attributes.path("title").asText();
        Song song = new Song(title);
        song.addId("tidal", id);
        Duration duration = Duration.parse(attributes.path("duration").asText());
        if (duration != null) {
            song.setDuration(duration);
        }
        JsonNode artistsResponse = this.query("/tracks/" + id + "/relationships/artists", null,
                new ArrayList<>(Arrays.asList("artists")));
        if (artistsResponse != null) {
            song.addArtists(this.getArtists(artistsResponse));
        }
        return song;
    }

    public LikedSongs getLikedSongs() {
        LikedSongs likedSongs = new LikedSongs();
        String pageCursor = null;
        try (IPIterator<?> pb = IPIterator.manual("Liked Songs", -1)) {
            while (true) {
                JsonNode response = this.query("/userCollections/" + userID + "/relationships/tracks", pageCursor,
                        new ArrayList<>(Arrays.asList("tracks")));
                if (response == null) {
                    logger.warn("null response received in tidal likedsongs query");
                    break;
                }
                JsonNode songs = response.path("included");
                if (songs != null && songs.isArray()) {
                    for (JsonNode songItem : songs) {
                        Song song = this.getSong(songItem);
                        if (song != null) {
                            pb.step(song.toString());
                            likedSongs.addSong(song);
                        }
                    }
                } else {
                    logger.debug("No songs in tidal likedsongs query: " + response);
                }
                JsonNode links = response.path("links");
                if (links.has("next")) {
                    pageCursor = links.path("meta").path("nextCursor").asText();
                } else {
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
        JsonNode response = this.query("/albums/" + albumId, null,
                new ArrayList<>(Arrays.asList("artists", "coverArt", "items")));
        if (response == null) {
            logger.debug("null response received in getAlbum");
            return null;
        }
        JsonNode albumData = response.path("data");
        String id = albumData.path("id").asText();
        JsonNode attributes = albumData.path("attributes");
        String title = attributes.path("title").asText();
        Album album = new Album(title);
        album.addId("tidal", id);

        JsonNode albumItems = response.path("included");
        if (albumItems != null && albumItems.isArray()) {
            for (JsonNode albumItem : albumItems) {
                String itemType = albumItem.path("type").asText();
                if (itemType.equals("artworks")) { // cover art
                    String coverArtUrl = albumItem.path("attributes").path("files").path("href").asText();
                    album.setCoverImage(coverArtUrl);
                } else if (itemType.equals("artists")) {
                    String artistID = albumItem.path("id").asText();
                    String artistName = albumItem.path("attributes").path("name").asText();
                    Artist artist = new Artist(artistName);
                    artist.addId("tidal", artistID);
                    album.addArtist(artist);
                } else { // tracks
                    album.addSong(this.getSong(albumItem));
                }
            }
        } else {
            logger.debug("getAlbum response is missing data");
            return null;
        }
        return album;
    }

    public ArrayList<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<>();
        String pageCursor = null;
        try (IPIterator<?> pb = IPIterator.manual("Albums", -1)) {
            while (true) {
                JsonNode response = this.query("/userCollections/" + userID + "/relationships/albums", pageCursor,
                        new ArrayList<>(Arrays.asList("albums")));
                if (response == null) {
                    logger.debug("null response received in getAlbums");
                    break;
                }
                JsonNode albumEntries = response.path("included");
                if (albumEntries != null && albumEntries.isArray()) {
                    for (JsonNode albumEntry : albumEntries) {
                        String id = albumEntry.path("id").asText();
                        JsonNode attributes = albumEntry.path("attributes");
                        String title = attributes.path("title").asText();
                        Album album = this.getAlbum(id, title, null);
                        if (album != null) {
                            pb.step(album.toString());
                            albums.add(album);
                        }
                    }
                } else {
                    logger.debug("No albums in tidal albums query: " + response);
                }
                JsonNode links = response.path("links");
                if (links.has("next")) {
                    pageCursor = links.path("meta").path("nextCursor").asText();
                } else {
                    break;
                }
            }
            return albums;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing albums");
            return null;
        }
    }

    public Playlist getPlaylist(String playlistId, String playlistName) {
        if (playlistId == null) {
            logger.debug("null playlistId provided in getPlaylist");
            return null;
        }
        JsonNode response = this.query("/playlists/" + playlistId, null,
                new ArrayList<>(Arrays.asList("coverArt", "items")));
        if (response == null) {
            logger.debug("null response received in getPlaylist");
            return null;
        }
        JsonNode playlistData = response.path("data");
        String id = playlistData.path("id").asText();
        JsonNode attributes = playlistData.path("attributes");
        String title = attributes.path("title").asText();
        Playlist playlist = new Playlist(title);
        playlist.addId("tidal", id);

        JsonNode playlistItems = response.path("included");
        if (playlistItems != null && playlistItems.isArray()) {
            for (JsonNode playlistItem : playlistItems) {
                String itemType = playlistItem.path("type").asText();
                if (itemType.equals("artworks")) { // cover art
                    String coverArtUrl = playlistItem.path("attributes").path("files").path("href").asText();
                    playlist.setCoverImage(coverArtUrl);
                } else { // tracks
                    playlist.addSong(this.getSong(playlistItem));
                }
            }
        } else {
            logger.debug("getAlbum response is missing data");
            return null;
        }
        return playlist;
    }

    public ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<>();
        String pageCursor = null;
        try (IPIterator<?> pb = IPIterator.manual("Playlists", -1)) {
            while (true) {
                JsonNode response = this.query("/userCollections/" + userID + "/relationships/playlists", pageCursor,
                        new ArrayList<>(Arrays.asList("playlists")));
                if (response == null) {
                    logger.debug("null response received in getPlaylists");
                    break;
                }
                JsonNode playlistEntries = response.path("included");
                if (playlistEntries != null && playlistEntries.isArray()) {
                    for (JsonNode playlistEntry : playlistEntries) {
                        String id = playlistEntry.path("id").asText();
                        JsonNode attributes = playlistEntry.path("attributes");
                        String name = attributes.path("name").asText();
                        Playlist playlist = this.getPlaylist(id, name);
                        if (playlist != null) {
                            pb.step(playlist.toString());
                            playlists.add(playlist);
                        }
                    }
                } else {
                    logger.debug("No playlists in tidal playlists query: " + response);
                }
                JsonNode links = response.path("links");
                if (links.has("next")) {
                    pageCursor = links.path("meta").path("nextCursor").asText();
                } else {
                    break;
                }
            }
            return playlists;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing playlists");
            return null;
        }
    }
}
