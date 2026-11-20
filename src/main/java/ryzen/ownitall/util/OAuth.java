package ryzen.ownitall.util;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import ryzen.ownitall.util.WebTools.AuthenticationException;
import ryzen.ownitall.util.WebTools.QueryException;

/**
 * <p>
 * OAuth, the pieces every integration's login flow repeats: pkce pair,
 * browser redirect for the code, code for token.
 * </p>
 *
 * @author ryzen
 */
public class OAuth {
    private static final Logger logger = new Logger(OAuth.class);
    private static final String redirectUri = "http://localhost:8081/oauth";

    /**
     * <p>
     * generateCodeVerifier.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public static String generateCodeVerifier() {
        SecureRandom sr = new SecureRandom();
        byte[] codeVerifierBytes = new byte[32];
        sr.nextBytes(codeVerifierBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifierBytes);
    }

    /**
     * <p>
     * generateCodeChallenge.
     * </p>
     *
     * @param codeVerifier a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
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
     * Start an HTTP server to intercept the oauth code
     *
     * @param authUrl       a {@link java.lang.String} object
     * @param clientID      a {@link java.lang.String} object
     * @param scope         a {@link java.util.ArrayList} object
     * @param codeChallenge a {@link java.lang.String} object
     * @throws java.lang.InterruptedException - if an error occurs
     */
    public static String getOauthCode(String authUrl, String clientID, ArrayList<String> scope,
            String codeChallenge) throws InterruptedException {
        WebTarget target = WebTools.target(authUrl)
                .queryParam("client_id", clientID)
                .queryParam("redirect_uri", redirectUri);
        if (scope != null) {
            target = target.queryParam("scope", String.join(" ", scope));
        }
        target = target.queryParam("code_challenge_method", "S256")
                .queryParam("code_challenge", codeChallenge);
        return getOauthCode(target.getUri());
    }

    /**
     * <p>
     * getOauthCode. opens the browser and catches the redirect, falling back
     * to asking for the code by hand when nothing arrives.
     * </p>
     *
     * @param authUri a {@link java.net.URI} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException - if an error occurs
     */
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
                    logger.debug("Authorization code received: " + code);
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
            logger.debug("Awaiting response at " + redirectUri);
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
            logger.info("Unable to get code automatically, provide it manually");
            System.out.println("Open this url: " + authUri);
            System.out.print("Provide the code found in response url: ");
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
        int timeout = WebTools.timeout();
        logger.debug("Waiting " + timeout + " seconds for code or interrupt to manually provide");
        for (int i = 0; i < timeout; i++) {
            if (codeRef.get() != null) {
                break;
            }
            Thread.sleep(1000);
        }
    }

    /**
     * <p>
     * getOauthToken. the full pkce dance: verifier, code through the browser,
     * code exchanged for a token.
     * </p>
     *
     * @param tokenUrl     a {@link java.lang.String} object
     * @param codeUrl      a {@link java.lang.String} object
     * @param clientID     a {@link java.lang.String} object
     * @param clientSecret a {@link java.lang.String} object
     * @param scope        a {@link java.util.ArrayList} object
     * @return a {@link java.lang.String} object
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public static String getOauthToken(String tokenUrl, String codeUrl, String clientID, String clientSecret,
            ArrayList<String> scope) throws AuthenticationException {
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        try {
            String code = getOauthCode(codeUrl, clientID, scope, codeChallenge);
            // the code and secret ride in the form body, not the url, so they
            // never end up in server or proxy access logs
            Form form = new Form()
                    .param("grant_type", "authorization_code")
                    .param("client_id", clientID)
                    .param("redirect_uri", redirectUri)
                    .param("code", code)
                    .param("code_verifier", codeVerifier);
            if (clientSecret != null) {
                form.param("client_secret", clientSecret);
            }
            WebTarget target = WebTools.target(tokenUrl);
            TokenResponse token = WebTools.query(target, "post", null,
                    Entity.form(form), TokenResponse.class);
            if (token == null || token.access_token == null) {
                throw new AuthenticationException("no access token in response from: " + target.getUri());
            }
            return token.access_token;
        } catch (QueryException | InterruptedException e) {
            throw new AuthenticationException(e);
        }
    }

    /**
     * <p>
     * TokenResponse, the answer to the code for token exchange, fields named
     * exactly as the endpoints send them, the way the other beans mirror the
     * documents they bind.
     * </p>
     */
    public static class TokenResponse {
        public String access_token;
        public String token_type;
        public long expires_in;
        public String refresh_token;
        public String scope;

        @Override
        public String toString() {
            return "TokenResponse [token_type=" + token_type + ", expires_in=" + expires_in + ", scope=" + scope + "]";
        }
    }
}
