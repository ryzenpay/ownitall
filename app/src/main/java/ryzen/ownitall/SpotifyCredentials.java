package ryzen.ownitall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.time.LocalDateTime;

import se.michaelthelin.spotify.SpotifyHttpManager;

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
    public boolean isNull() {
        if (this.getClientId() == null || this.getClientSecret() == null || this.getRedirectUrl() == null) {
            return true;
        }
        return false;
    }
}
