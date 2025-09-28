package ryzen.ownitall.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ryzen.ownitall.util.exceptions.QueryException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.security.MessageDigest;

public class WebTools {
    // TODO: error and input handling
    private static final Logger logger = new Logger(WebTools.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String generateCodeVerifier() {
        SecureRandom sr = new SecureRandom();
        byte[] codeVerifierBytes = new byte[32];
        sr.nextBytes(codeVerifierBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifierBytes);
    }

    public static String generateCodeChallenge(String codeVerifier) {
        try {
            byte[] bytes = codeVerifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes, 0, bytes.length);
            byte[] digest = md.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode query(HttpURLConnection connection) throws QueryException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            JsonNode rootNode = objectMapper.readTree(response.toString());
            if (rootNode.has("error") || rootNode.has("failed")) {
                throw new QueryException(rootNode.toString());
            }
            return rootNode;
        } catch (IOException e) {
            logger.error("Query exception processing " + connection.getURL().toString(), e);
            throw new QueryException(e);
        }
    }

    /**
     * download an image from the web
     *
     * @param url  - URI to fetch image from
     * @param file - file to download the image to (will make a new one)
     * @throws java.io.IOException - java.io.IOException while downloading
     */
    public static void downloadImage(URI url, File file) throws IOException {
        if (url == null || file == null) {
            logger.debug("null url or file passed in downloadImage");
            return;
        }
        if (file.exists()) {
            logger.debug("coverimage already found: '" + file.getAbsolutePath() + "'");
            return;
        }
        try (InputStream in = url.toURL().openStream()) {
            Files.copy(in, file.toPath());
        } catch (FileNotFoundException e) {
            logger.debug("Image at url '" + url + "' not found");
        }
    }
}
