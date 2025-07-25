package ryzen.ownitall.method;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.method.interfaces.Export;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

// https://developer.tidal.com/documentation
// https://developer.tidal.com/apiref
//TODO: only support import
public class Tidal {
    private static final Logger logger = new Logger(Tidal.class);
    private String token;
    public static final String baseUrl = "https://openapi.tidal.com/v2/";
    public static final String countryCode = "US";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Tidal() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Tidal.class)) {
            logger.debug("Empty tidal credentials");
            throw new MissingSettingException(Tidal.class);
        }
        this.setToken();
    }

    private void setToken() {
        String idsecret = Settings.tidalClientID + ":" + Settings.tidalClientSecret;
        this.token = Base64.getEncoder().encodeToString(idsecret.getBytes());
    }

    private JsonNode query(String type, String query) {
        if (type == null) {
            logger.debug("null or invalid type provided in query");
            return null;
        }
        if (query == null) {
            logger.debug("null or invalid query provided in query");
            return null;
        }
        try {
            URI url = new URI(baseUrl + type + "/" + query);
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestProperty("Accept", "application/vnd.tidal.v1+json");
            connection.setRequestProperty("Authorization", "Basic " + this.token);
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            JsonNode rootNode = objectMapper.readTree(response.toString());

            if (rootNode.has("error") || rootNode.has("failed")) {
                logger.debug("Query error: " + rootNode.toString());
                int errorCode = rootNode.path("error").asInt();
                if (errorCode != 0) {
                    String errorMessage = rootNode.path("message").asText();
                    logger.debug("Received error code on query: " + url.toString());
                    this.queryErrorHandle(errorCode, errorMessage);
                    return null;
                }
            }
            return rootNode;
        } catch (URISyntaxException e) {
            logger.error("Exception while constructing tidal query", e);
            return null;
        } catch (IOException e) {
            logger.warn("Exception while query tidal: " + e);
            return null;
        }
    }

    private void queryErrorHandle(int code, String message) {
        logger.warn("Exception querying tidal, code=" + code + ", message=" + message);
        // TODO: tidal error code handling
    }
}
