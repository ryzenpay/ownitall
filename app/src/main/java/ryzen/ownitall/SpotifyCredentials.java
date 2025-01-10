package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.tools.Input;
import se.michaelthelin.spotify.SpotifyHttpManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyCredentials {
    private static final Logger logger = LogManager.getLogger(SpotifyCredentials.class);
    private String clientId;
    private String clientSecret;
    private URI redirectUrl;
    private String code;

    /**
     * Default spotify constructor checking for saved or asking for user input
     * 
     */
    public SpotifyCredentials() {
        System.out.println("A guide to obtaining the following variables is in the readme");
        System.out.println("Please provide your client id: ");
        this.clientId = Input.getInstance().getString();
        System.out.println("Please provide your client secret: ");
        this.clientSecret = Input.getInstance().getString();
        System.out.println("Please provide redirect url:");
        this.redirectUrl = SpotifyHttpManager.makeUri(Input.getInstance().getString());
    }

    /**
     * Spotify API credential constructor with known values
     * 
     * @param clientId     - spotify api client id
     * @param clientSecret - spotify api client secret
     * @param redirectUrl  - spotify api redirect url
     */
    @JsonCreator
    public SpotifyCredentials(@JsonProperty("clientId") String clientId,
            @JsonProperty("clientSecret") String clientSecret, @JsonProperty("redirectUrl") String redirectUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = SpotifyHttpManager.makeUri(redirectUrl);
    }

    /**
     * get spotify api client id
     * 
     * @return - string client id
     */
    public String getClientId() {
        if (this.clientId == null) {
            return null;
        }
        return this.clientId;
    }

    /**
     * set spotify api client id
     * 
     * @return - string client id
     */
    public String getClientSecret() {
        return this.clientSecret;
    }

    /**
     * get spotify api redirect url (String)
     * 
     * @return - string spotify api redirect url
     */
    @JsonIgnore
    public String getRedirectUrlString() {
        return this.redirectUrl.toString();
    }

    /**
     * get spotify api redirect url (URI)
     * 
     * @return - URI spotify api redirect url
     */
    public URI getRedirectUrl() {
        return this.redirectUrl;
    }

    /**
     * set spotifyAPI temoporary authentication code
     * 
     * @param code - String code
     */
    @JsonIgnore
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * get spotifyAPI temporary authentication code
     * 
     * @return - String code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * true if spotify credentials is empty (if successfully initialized)
     * 
     * @return - true if empty, false if not
     */
    @JsonIgnore
    public boolean isNull() {
        if (this.getClientId() == null || this.getClientSecret() == null || this.getRedirectUrl() == null) {
            return true;
        }
        return false;
    }

    /**
     * start temporary local server to "intercept" spotify api code
     */
    public void startLocalServer() { // TODO: make this work (cors error)
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            logger.info("Waiting for the authorization code...");
            Socket clientSocket = serverSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            StringBuilder request = new StringBuilder();
            while ((inputLine = in.readLine()) != null && !inputLine.isEmpty()) {
                request.append(inputLine).append("\n");
                if (inputLine.contains("code=")) {
                    break; // Stop reading after we've found the code
                }
            }

            String code = extractCodeFromRequest(request.toString());
            if (code != null) {
                this.setCode(code);
                logger.info("Authorization code received: " + code); // TODO: jframe force window on top
                                                                     // (frame.toFront(); frame.repaint();)
                sendResponse(clientSocket, 200, "Authorization code received successfully.");
            } else {
                logger.error("Failed to retrieve authorization code. Request: " + request.toString());
                sendResponse(clientSocket, 404, "Failed to retrieve authorization code.");
                System.out.println("Please provide the code it provides (in url)");
                this.setCode(Input.getInstance().getString());
            }

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Please provide the code it provides (in url)");
            this.setCode(Input.getInstance().getString());
        }
    }

    /**
     * extract code from the "intercepted" spotify api
     * 
     * @param request - raw response data
     * @return - String code
     */
    private String extractCodeFromRequest(String request) {
        int codeIndex = request.indexOf("code=");
        if (codeIndex != -1) {
            int endIndex = request.indexOf("&", codeIndex);
            if (endIndex == -1) {
                endIndex = request.indexOf(" ", codeIndex);
            }
            if (endIndex == -1) {
                endIndex = request.length();
            }
            return request.substring(codeIndex + 5, endIndex);
        }
        return null;
    }

    /**
     * send response to website if received code
     * 
     * @param clientSocket - socket to send through
     * @param statusCode   - additional success message (200 = success, 404 =
     *                     failed)
     * @param message      - additional message to present on site
     * @throws IOException
     */
    private void sendResponse(Socket clientSocket, int statusCode, String message) throws IOException {
        String statusLine = "HTTP/1.1 " + statusCode + " " + (statusCode == 200 ? "OK" : "Not Found") + "\r\n";
        String contentType = "Content-Type: text/plain\r\n";
        String contentLength = "Content-Length: " + message.length() + "\r\n";
        String response = statusLine + contentType + contentLength + "\r\n" + message;
        clientSocket.getOutputStream().write(response.getBytes());
    }
}
