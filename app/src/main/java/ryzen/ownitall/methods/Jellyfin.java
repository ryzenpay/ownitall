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

import ryzen.ownitall.Credentials;
import ryzen.ownitall.classes.Song;

public class Jellyfin {
    // TODO: jellyfin integration
    // sync playlists
    // sync albums
    // sync liked songs
    private static final Logger logger = LogManager.getLogger(Jellyfin.class);
    private static final Credentials credentials = Credentials.load();
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
        JsonNode response = this.postQuery("/Users/AuthenticateByName", credsNode);
        if (response == null) {
            logger.error("Failed to authenticate with jellyfin");
        } else {
            logger.debug("Authenticated into jellyfin with user " + response.get("User").get("Name").asText());
            this.userId = response.get("User").get("Id").asText();
            this.accessToken = response.get("AccessToken").asText();
        }
    }

    // https://api.jellyfin.org/#tag/UserLibrary/operation/MarkFavoriteItem
    public void uploadLikedSongs(ArrayList<Song> songs) {
        if (songs == null) {
            logger.debug("null songs provided in uploadLikedSongs");
            return;
        }
        LinkedHashSet<String> songIds = new LinkedHashSet<>();
        for (Song song : songs) {
            songIds.add(this.getSongId(song));
        }
        if (!songIds.isEmpty()) {
            for (String songId : songIds) {
                this.postQuery("/UserFavoriteItems/" + songId, null);
            }
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
        JsonNode response = this.getQuery("/Items", params);
        if (response != null) {
            JsonNode responseNode = response.get("Items").get(0);
            if (responseNode != null) {
                return responseNode.path("Id").asText();
            }
        }
        logger.debug("Could not find song '" + song.getName() + "' in jellyfin library");
        return null;
    }

    private JsonNode getQuery(String type, LinkedHashMap<String, String> params) {
        if (type == null || type.isEmpty()) {
            logger.debug("null or invalid type provided in getQuery");
            return null;
        }
        if (params == null || params.isEmpty()) {
            logger.debug("null or invalid parameters provided in getQuery");
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
            connection.setRequestMethod("GET");
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
            logger.error("Exception while constructing jellyfin query: " + e);
            return null;
        } catch (IOException e) {
            logger.error("Exception while querying jellyfin: " + e);
            return null;
        }
    }

    private JsonNode postQuery(String type, JsonNode params) {
        if (type == null || type.isEmpty()) {
            logger.debug("null or invalid type provided in postQuery");
            return null;
        }
        if (params == null || params.isEmpty()) {
            logger.debug("null or invalid parameters provided in postQuery");
            return null;
        }
        try {
            URI url = new URI(credentials.getJellyfinUrl() + type);
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            if (this.accessToken != null && !this.accessToken.isEmpty()) {
                // https://gist.github.com/nielsvanvelzen/ea047d9028f676185832e51ffaf12a6f
                String authHeader = String.format(
                        "MediaBrowser Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\", Token=\"%s\"",
                        "ownitall",
                        "Java",
                        credentials.getJellyfinUsername() + "ownitall",
                        "10.10.6",
                        this.accessToken);
                connection.setRequestProperty("Authorization", authHeader);
            } else {
                String authHeader = String.format(
                        "MediaBrowser Client=\"%s\", Device=\"%s\", DeviceId=\"%s\", Version=\"%s\"",
                        "ownitall",
                        "Java",
                        credentials.getJellyfinUsername() + "ownitall",
                        "10.10.6");
                connection.setRequestProperty("Authorization", authHeader);
            }
            if (params != null) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = params.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
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
            logger.error("Exception while constructing jellyfin query: " + e);
            return null;
        } catch (IOException e) {
            logger.error("Exception while querying jellyfin: " + e);
            return null;
        }
    }
}
