package ryzen.ownitall.method;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import ryzen.ownitall.output.cli.ProgressBar;
import ryzen.ownitall.util.InterruptionHandler;

public class Jellyfin extends Method {
    private static final Logger logger = LogManager.getLogger();
    private static final Library library = Library.load();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private String userId;
    private String accessToken;

    // https://api.jellyfin.org/
    public Jellyfin() throws InterruptedException {
        if (Method.isCredentialsEmpty(Jellyfin.class)) {
            throw new InterruptedException("empty jellyfin credentials");
        }
        this.authenticate();
    }

    // https://api.jellyfin.org/#tag/User/operation/AuthenticateUserByName
    private void authenticate() throws InterruptedException {
        ObjectNode credsNode = objectMapper.createObjectNode();
        credsNode.put("Username", Settings.jellyfinUsername);
        credsNode.put("Pw", Settings.jellyfinPassword);
        JsonNode response = this.payloadQuery("post", "/Users/AuthenticateByName", credsNode);
        if (response == null) {
            logger.error("Failed to authenticate with jellyfin");
            throw new InterruptedException("Failed to authenticate with jellyfin");
        } else {
            logger.debug("Authenticated into jellyfin with user " + response.get("User").get("Name").asText());
            this.userId = response.get("User").get("Id").asText();
            this.accessToken = response.get("AccessToken").asText();
        }
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    @Override
    public LikedSongs getLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        try (ProgressBar pb = new ProgressBar("Liked Songs", -1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            params.put("mediaTypes", "Audio");
            params.put("recursive", "true");
            params.put("isFavorite", "true");
            JsonNode response = this.paramQuery("get", "/Items", params);
            if (response != null) {
                JsonNode itemsNode = response.get("Items");
                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode itemNode : itemsNode) {
                        interruptionHandler.throwInterruption();
                        String id = itemNode.get("Id").asText();
                        if (id != null && !id.isEmpty()) {
                            Song song = this.getSong(id);
                            if (song != null) {
                                likedSongs.addSong(song);
                                pb.step(song.getName());
                            }
                        }
                    }
                }
            } else {
                logger.debug("Unable to get ids of favorite items");
                return null;
            }
            return likedSongs;
        }
    }

    @Override
    public void syncLikedSongs() throws InterruptedException {
        logger.debug("Getting jellyfin liked songs to remove mismatches");
        LikedSongs likedSongs = this.getLikedSongs();
        try (InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            if (likedSongs != null && !likedSongs.isEmpty()) {
                likedSongs.removeSongs(Collection.getLikedSongs().getSongs());
                for (Song song : likedSongs.getSongs()) {
                    interruptionHandler.throwInterruption();
                    String songId = this.getSongId(song);
                    if (songId != null) {
                        this.paramQuery("delete", "/UserFavoriteItems/" + songId, new LinkedHashMap<>());
                        logger.debug("removed '" + song.toString() + "' favorite from jellyfin");
                    }
                }
            }
        }
    }

    // https://api.jellyfin.org/#tag/UserLibrary/operation/MarkFavoriteItem
    @Override
    public void uploadLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = Collection.getLikedSongs();
        try (ProgressBar pb = new ProgressBar("Liked Songs", likedSongs.size());
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (Song song : likedSongs.getSongs()) {
                String songId = this.getSongId(song);
                if (songId != null) {
                    interruptionHandler.throwInterruption();
                    this.paramQuery("post", "/UserFavoriteItems/" + songId, new LinkedHashMap<>());
                    pb.step(song.getName());
                }
            }
        }
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    @Override
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = new ArrayList<>();
        try (ProgressBar pb = new ProgressBar("Playlists", -1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            params.put("IncludeItemTypes", "Playlist");
            params.put("recursive", "true");
            JsonNode response = this.paramQuery("get", "/Items", params);
            if (response != null) {
                JsonNode itemsNode = response.get("Items");
                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode itemNode : itemsNode) {
                        interruptionHandler.throwInterruption();
                        Playlist playlist = this.getPlaylist(itemNode.get("Id").asText(),
                                itemNode.get("Name").asText());
                        if (playlist != null && !playlist.isEmpty()) {
                            playlists.add(playlist);
                            pb.step(playlist.getName());
                        }
                    }
                }
            } else {
                logger.debug("Unable to get ids of playlists");
                return null;
            }
            return playlists;
        }
    }

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
        return playlist;
    }

    // https://api.jellyfin.org/#tag/Playlists/operation/GetPlaylist
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
                for (JsonNode itemIdNode : itemIdsNode) {
                    Song song = this.getSong(itemIdNode.asText());
                    if (song != null) {
                        songs.add(song);
                    }
                }
            }
            return songs;
        }
        logger.debug("Unable to find playlist songs '" + playlistId + "' in jellyfin");
        return null;
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    @Override
    public ArrayList<Album> getAlbums() throws InterruptedException {
        ArrayList<Album> albums = new ArrayList<>();
        try (ProgressBar pb = new ProgressBar("Playlists", -1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            params.put("IncludeItemTypes", "MusicAlbum");
            params.put("recursive", "true");
            JsonNode response = this.paramQuery("get", "/Items", params);
            if (response != null) {
                JsonNode itemsNode = response.get("Items");
                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode itemNode : itemsNode) {
                        interruptionHandler.throwInterruption();
                        String artistName = null;
                        JsonNode artistNode = response.get("Artists").get(0);
                        if (artistNode != null) {
                            artistName = artistNode.asText();
                        }
                        Album album = this.getAlbum(itemNode.get("Id").asText(), itemNode.get("Name").asText(),
                                artistName);
                        if (album != null && !album.isEmpty()) {
                            albums.add(album);
                            pb.step(album.getName());
                        }
                    }
                }
            } else {
                logger.debug("Unable to get ids of albums");
                return null;
            }
            return albums;
        }
    }

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
            }
            return songs;
        } else {
            logger.debug("Unable to get ids of album songs: " + albumId);
            return null;
        }
    }

    // https://api.jellyfin.org/#tag/UserLibrary/operation/GetItem
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
    public String getSongId(Song song) {
        if (song == null) {
            logger.debug("null song provided in getSongId");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("searchTerm", song.getName());
        if (song.getMainArtist() != null) {
            params.put("artists", song.getMainArtist().toString());
        }
        if (song.getAlbumName() != null) {
            params.put("albums", song.getAlbumName());
        }
        params.put("mediaTypes", "Audio");
        params.put("recursive", "true");
        params.put("limit", "1");
        JsonNode response = this.paramQuery("get", "/Items", params);
        if (response != null) {
            JsonNode responseNode = response.get("Items").get(0);
            if (responseNode != null) {
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            JsonNode rootNode = objectMapper.readTree(response.toString());
            if (connection.getResponseCode() != 200) {
                logger.debug("Query error: " + rootNode.toString());
                return null;
            }
            return rootNode;
        } catch (URISyntaxException e) {
            logger.error("Exception while constructing jellyfin paramQuery", e);
            return null;
        } catch (IOException e) {
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            JsonNode rootNode = objectMapper.readTree(response.toString());
            if (connection.getResponseCode() != 200) {
                logger.debug("Query error: " + rootNode.toString());
                return null;
            }
            return rootNode;
        } catch (URISyntaxException e) {
            logger.error("Exception while constructing jellyfin payloadQuery", e);
            return null;
        } catch (IOException e) {
            logger.error("Exception while payloadQuery jellyfin", e);
            return null;
        }
    }
}
