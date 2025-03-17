package ryzen.ownitall.methods;

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
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.Progressbar;

public class Jellyfin {
    // TODO: jellyfin integration
    // sync playlists
    // sync albums
    // sync liked songs
    private static final Logger logger = LogManager.getLogger(Jellyfin.class);
    private static final Credentials credentials = Credentials.load();
    private static final Settings settings = Settings.load();
    private static Library library = Library.load();
    private ObjectMapper objectMapper;
    private String userId;
    private String accessToken;

    // https://api.jellyfin.org/
    public Jellyfin() throws InterruptedException {
        super();
        if (credentials.jellyfinIsEmpty()) {
            credentials.setJellyfinCredentials();
        }
        objectMapper = new ObjectMapper();
        this.authenticate();
    }

    // https://api.jellyfin.org/#tag/User/operation/AuthenticateUserByName
    private void authenticate() {
        ObjectNode credsNode = objectMapper.createObjectNode();
        credsNode.put("Username", credentials.getJellyfinUsername());
        credsNode.put("Pw", credentials.getJellyfinPassword());
        JsonNode response = this.payloadQuery("post", "/Users/AuthenticateByName", credsNode);
        if (response == null) {
            logger.error("Failed to authenticate with jellyfin");
        } else {
            logger.debug("Authenticated into jellyfin with user " + response.get("User").get("Name").asText());
            this.userId = response.get("User").get("Id").asText();
            this.accessToken = response.get("AccessToken").asText();
        }
    }

    // https://api.jellyfin.org/#tag/UserLibrary/operation/MarkFavoriteItem
    public void uploadLikedSongs(ArrayList<Song> songs) throws InterruptedException {
        if (songs == null) {
            logger.debug("null songs provided in uploadLikedSongs");
            return;
        }
        LinkedHashSet<String> songIds = new LinkedHashSet<>();
        try (ProgressBar pb = Progressbar.progressBar("Liked Songs", songs.size() * 2);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
            for (Song song : songs) {
                interruptionHandler.throwInterruption();
                String songId = this.getSongId(song);
                if (songId != null) {
                    songIds.add(songId);
                }
                pb.setExtraMessage("id: " + song.getName()).step();
            }
            if (!songIds.isEmpty()) {
                for (String songId : songIds) {
                    interruptionHandler.throwInterruption();
                    this.paramQuery("post", "/UserFavoriteItems/" + songId, new LinkedHashMap<>());
                    pb.setExtraMessage("added: " + songId).step();
                }
            }
        }
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    // https://api.jellyfin.org/#tag/UserLibrary/operation/GetItem
    public LikedSongs getLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        ArrayList<String> songIds = new ArrayList<>();
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        try (ProgressBar pb = Progressbar.progressBar("Liked Songs", -1);
                InterruptionHandler interruptionHandler = new InterruptionHandler()) {
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
                            songIds.add(itemNode.get("Id").asText());
                            pb.setExtraMessage(id).step();
                        }
                    }
                }
            } else {
                logger.debug("Unable to get ids of favorite items");
                return null;
            }
            for (String songId : songIds) {
                interruptionHandler.throwInterruption();
                JsonNode idResponse = this.paramQuery("get", "/Items/" + songId, new LinkedHashMap<>());
                if (idResponse != null) {
                    Song song = new Song(idResponse.get("Name").asText());
                    JsonNode artistNode = idResponse.get("Artists").get(0);
                    if (artistNode != null) {
                        song.setArtist(new Artist(artistNode.asText()));
                    }
                    String albumName = idResponse.get("Album").asText();
                    if (albumName != null && !albumName.isEmpty()) {
                        song.setAlbumName(albumName);
                    }
                    if (library != null) {
                        Song foundSong = library.getSong(song);
                        if (foundSong != null) {
                            song = foundSong;
                        } else if (settings.isLibraryVerified()) {
                            song = null;
                        }
                    }
                    if (song != null) {
                        likedSongs.addSong(song);
                        pb.setExtraMessage(song.getName()).step();
                    }
                }
            }
            return likedSongs;
        }
    }

    // https://api.jellyfin.org/#tag/Items/operation/GetItems
    public String getSongId(Song song) {
        if (song == null) {
            logger.debug("null song provided in getSongId");
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("searchTerm", song.getName());
        if (song.getArtist() != null) {
            params.put("artists", song.getArtist().toString());
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
        if (method == null || method.isEmpty()) {
            logger.debug("null or invalid method provided in paramQuery");
            return null;
        }
        if (type == null || type.isEmpty()) {
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
            logger.error("Exception while encoding query: " + e);
            return null;
        }
        try {
            URI url = new URI(credentials.getJellyfinUrl() + type + builder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestMethod(method.toUpperCase());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            // https://gist.github.com/nielsvanvelzen/ea047d9028f676185832e51ffaf12a6f
            String authHeader = String.format(
                    "MediaBrowser Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\", Token=\"%s\"",
                    "ownitall",
                    "Java",
                    credentials.getJellyfinUsername() + "ownitall",
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
            logger.error("Exception while constructing jellyfin paramQuery: " + e);
            return null;
        } catch (IOException e) {
            logger.error("Exception while paramQuery jellyfin: " + e);
            return null;
        }
    }

    private JsonNode payloadQuery(String method, String type, JsonNode payload) {
        if (method == null || method.isEmpty()) {
            logger.debug("null or invalid method provided in payloadQuery");
            return null;
        }
        if (type == null || type.isEmpty()) {
            logger.debug("null or invalid type provided in payloadQuery");
            return null;
        }
        if (payload == null || payload.isEmpty()) {
            logger.debug("null or invalid parameters provided in payloadQuery");
            return null;
        }
        try {
            URI url = new URI(credentials.getJellyfinUrl() + type);
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestMethod(method.toUpperCase());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            // https://gist.github.com/nielsvanvelzen/ea047d9028f676185832e51ffaf12a6f
            String authHeader = String.format(
                    "MediaBrowser Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\", Token=\"%s\"",
                    "ownitall",
                    "Java",
                    credentials.getJellyfinUsername() + "ownitall",
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
            logger.error("Exception while constructing jellyfin payloadQuery: " + e);
            return null;
        } catch (IOException e) {
            logger.error("Exception while payloadQuery jellyfin: " + e);
            return null;
        }
    }
}
