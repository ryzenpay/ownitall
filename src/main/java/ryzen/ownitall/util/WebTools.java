package ryzen.ownitall.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.QueryException;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.security.MessageDigest;

public class WebTools {
    private static final Logger logger = new Logger(WebTools.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static long retries = 5;
    public static long timeout = 20; // timeout in seconds
    private static long lastQueryTime = 0;

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

    /**
     * ensure requests are not sent faster than the query
     * 
     * @param queryDiff - milliseconds of timeout between requests
     * @throws InterruptedException
     */
    public static void queryPacer(long queryDiff) throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastQueryTime;
        if (elapsedTime < queryDiff) {
            TimeUnit.MILLISECONDS.sleep(queryDiff - elapsedTime);
        }
    }

    public static JsonNode query(HttpURLConnection connection) throws QueryException {
        return query(connection, 0);
    }

    public static JsonNode query(HttpURLConnection connection, int i) throws QueryException {
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
            lastQueryTime = System.currentTimeMillis();
            return rootNode;
        } catch (IOException e) {
            try {
                switch (connection.getResponseCode()) {
                    case 429: // too many requests
                        if (i > retries) {
                            throw new QueryException("Reached 5 retries");
                        }
                        logger.debug("Too many requests, trying again in 30 seconds...");
                        TimeUnit.SECONDS.sleep(timeout);
                        return query(connection, i++);
                    case 404:
                        logger.debug("Requested page '" + connection.getURL().toString() + "'does not exist");
                        return null;
                    default:
                        logger.error("Unknown exception processing: " + connection.getURL().toString(), e);
                        throw new QueryException(e);
                }
            } catch (IOException | InterruptedException f) {
                logger.error("Exception retreiving response code from request", e);
                throw new QueryException(e);
            }
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

    /**
     * Start an HTTP server to intercept the oauth code
     *
     * @throws java.lang.InterruptedException - if an error occurs
     */
    public static String getOauthCode(String authUrl, String clientID, ArrayList<String> scope,
            String codeChallenge) throws InterruptedException {
        String url = authUrl;
        if (url.contains("?")) {
            url += "&client_id=" + clientID;
        } else {
            url += "?client_id=" + clientID;
        }
        url += "&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Foauth";
        url += "&scope=" + String.join("%20", scope);
        url += "&code_challenge_method=S256&code_challenge=" + codeChallenge;
        URI authUri = URI.create(url);
        return getOauthCode(authUri);
    }

    public static String getOauthCode(URI authUri) throws InterruptedException {
        AtomicReference<String> codeRef = new AtomicReference<>();
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
            server.createContext("/oauth", exchange -> {
                logger.debug("request received on /oauth");
                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery();
                String code = null;
                if (query != null && query.contains("code=")) {
                    String[] queryParams = query.split("&");
                    for (String param : queryParams) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2 && "code".equals(keyValue[0])) {
                            code = keyValue[1];
                        }
                    }
                }

                String responseText;
                if (code != null) {
                    codeRef.set(code);
                    logger.info("Authorization code received: " + code);
                    responseText = "Code received, you can now close this tab";
                    exchange.sendResponseHeaders(200, responseText.getBytes().length);
                } else {
                    logger.warn("Failed to retrieve authorization code. Query: " + query);
                    responseText = "an error occurred, check logs for more";
                    exchange.sendResponseHeaders(404, responseText.getBytes().length);
                }

                OutputStream responseBody = exchange.getResponseBody();
                responseBody.write(responseText.getBytes());
                responseBody.close();
            });
            server.start();
            logger.info("Awaiting response at http://localhost/oauth");
            try {
                interactiveSetCode(authUri, codeRef);
            } catch (InterruptedException e) {
                logger.debug("Interrupted while interactively getting code");
            }
            server.stop(0);
        } catch (IOException e) {
            logger.error("Failed to start local server", e);
        }
        if (codeRef.get() == null) {
            logger.info("Unable to get code automatically, please provide it manually");
            System.out.println("Please open this url: " + authUri);
            System.out.print("Please provide the code found in response url: ");
            return Input.request().getString();
        } else {
            return codeRef.get();
        }
    }

    private static void interactiveSetCode(URI url, AtomicReference<String> codeRef) throws InterruptedException {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(url);
            } catch (IOException e) {
                logger.error("Exception opening web browser", e);
            }
        }
        logger.info("Waiting " + timeout + " seconds for code or interrupt to manually provide");
        for (int i = 0; i < timeout; i++) {
            if (codeRef.get() != null) {
                break;
            }
            Thread.sleep(1000);
        }
    }

    public static String getOauthToken(String tokenUrl, String codeUrl, String clientID,
            ArrayList<String> scope) throws AuthenticationException {
        String codeVerifier = WebTools.generateCodeVerifier();
        String codeChallenge = WebTools.generateCodeChallenge(codeVerifier);
        try {
            String code = getOauthCode(codeUrl, clientID, scope, codeChallenge);
            String url = tokenUrl;
            if (url.contains("?")) {
                url += "&client_id=" + clientID;
            } else {
                url += "?client_id=" + clientID;
            }
            url += "&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Foauth";
            url += "&code=" + code;
            url += "&code_verifier=" + codeVerifier;
            URI uri = URI.create(url);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            JsonNode response = WebTools.query(connection);
            if (response.has("access_token")) {
                return response.path("access_token").asText();
            } else {
                throw new AuthenticationException("no access token in response: " + response.toString());
            }
        } catch (IOException | QueryException | InterruptedException e) {
            throw new AuthenticationException(e);
        }
    }
}
