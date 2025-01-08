package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import se.michaelthelin.spotify.SpotifyHttpManager;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyCredentials {
    private String clientId;
    private String clientSecret;
    private URI redirectUrl;
    private String code;
    private LocalDateTime codeExpiration;

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
    public SpotifyCredentials(String clientId, String clientSecret, String redirectUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = SpotifyHttpManager.makeUri(redirectUrl);
    }

    public String getClientId() {
        if (this.clientId == null) {
            return null;
        }
        return this.clientId;
    }

    public String getClientSecret() {
        return this.clientSecret;
    }

    public String getRedirectUrlString() {
        return this.redirectUrl.toString();
    }

    public URI getRedirectUrl() {
        return this.redirectUrl;
    }

    @JsonIgnore
    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public void setCodeExpiration(int codeExpiration) {
        this.codeExpiration = LocalDateTime.now().plusSeconds(codeExpiration);
    }

    /**
     * check the expiration of the current code, but also fix if close
     * 
     * @return - true if expired, false if not expired
     */
    public boolean checkExpiration() {
        if (this.codeExpiration == null) {
            return true;
        }
        if (this.codeExpiration.isBefore(LocalDateTime.now())) {
            return true;
        }
        return false;
    }

    /**
     * true if spotify credentials is empty
     * 
     * @return
     */
    @JsonIgnore
    public boolean isNull() {
        if (this.getClientId() == null || this.getClientSecret() == null || this.getRedirectUrl() == null) {
            return true;
        }
        return false;
    }

    public void startLocalServer() {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            System.out.println("Waiting for the authorization code...");
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
                System.out.println("Authorization code received: " + code);
                sendResponse(clientSocket, "Authorization successful. You can close this window.");
            } else {
                System.err.println("Failed to retrieve authorization code. Request: " + request.toString());
                sendResponse(clientSocket, "Failed to retrieve authorization code. Please try again.");
                System.out.println("Please provide the code it provides (in url)");
                this.setCode(Input.getInstance().getString());// TODO: gui would help this so much
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private void sendResponse(Socket clientSocket, String message) throws IOException { // TODO: fix web page to show
                                                                                        // this
        String response = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html\r\n"
                + "\r\n"
                + "<html><body>"
                + "<h1>" + message + "</h1>"
                + "</body></html>";
        clientSocket.getOutputStream().write(response.getBytes());
    }
}
