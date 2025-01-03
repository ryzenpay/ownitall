package ryzen.ownitall;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;

import org.apache.hc.core5.http.ParseException;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

public class SpotifyCredentials {
    private String clientId;
    private String clientSecret;
    private URI redirectUrl;
    private String code;
    private LocalDateTime codeExpiration;
    public transient SpotifyApi spotifyApi;

    /**
     * Default spotify constructor checking for saved or asking for user input
     * 
     */
    public SpotifyCredentials() {
        this.getCredentials();
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(this.clientId)
                .setClientSecret(this.clientSecret)
                .setRedirectUri(this.redirectUrl)
                .build();
        this.setCode();
        this.setToken();
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
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(this.clientId)
                .setClientSecret(this.clientSecret)
                .setRedirectUri(this.redirectUrl)
                .build();
        this.setCode();
        this.setToken();
    }

    /**
     * constructor with an existing constructed, used for importing
     * 
     * @param spotifyCredentials - constructed with values to copy from
     */
    public SpotifyCredentials(SpotifyCredentials spotifyCredentials) {
        this.clientId = spotifyCredentials.clientId;
        this.clientSecret = spotifyCredentials.clientSecret;
        this.redirectUrl = spotifyCredentials.redirectUrl;
        this.code = spotifyCredentials.code;
        this.codeExpiration = spotifyCredentials.codeExpiration;
        this.spotifyApi = spotifyCredentials.spotifyApi;
    }

    private void getCredentials() {
        System.out.println("A guide to obtaining the following variables is in the readme");
        System.out.println("Please provide your client id: ");
        this.clientId = Input.getInstance().getString();
        System.out.println("Please provide your client secret: ");
        this.clientSecret = Input.getInstance().getString();
        System.out.println("Please provide redirect url:");
        this.redirectUrl = SpotifyHttpManager.makeUri(Input.getInstance().getString());
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

    public String getRedirectUrl() {
        return this.redirectUrl.toString();
    }

    /**
     * set the spotifyApi access token
     * 
     * @param code - the authentication code provided in the oauth
     */
    private void setToken() {
        AuthorizationCodeRequest authorizationCodeRequest = this.spotifyApi.authorizationCode(this.code).build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
            this.spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            this.spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
            this.setCodeExpiration(authorizationCodeCredentials.getExpiresIn());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.err.println("Error logging in: " + e);
        }
    }

    /**
     * refresh the spotify api token incase of expiring
     * 
     * @return - true if succesfully refreshed, false if not
     */
    public boolean refreshToken() {
        AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = this.spotifyApi.authorizationCodeRefresh()
                .build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            this.setCodeExpiration(authorizationCodeCredentials.getExpiresIn());
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * obtaining the oauth code to set the token
     * 
     * @return - the oauth code with permissions
     */
    private void setCode() {
        AuthorizationCodeUriRequest authorizationCodeUriRequest = this.spotifyApi.authorizationCodeUri()
                .scope("user-library-read,playlist-read-private")
                .show_dialog(true)
                .build();
        URI auth_uri = authorizationCodeUriRequest.execute();
        System.out.println("Open this link:\n" + auth_uri.toString());
        System.out.println("Please provide the code it provides (in url)");
        this.code = Input.getInstance().getString(); // TODO: gui would help this so much
    }

    private void setCodeExpiration(int codeExpiration) {
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
        if (this.codeExpiration.isAfter(LocalDateTime.now().minusSeconds(60))) { // 60 second buffer period to refresh -
                                                                                 // TODO: ongoing checking (only helps
                                                                                 // if constantly using)
            if (!this.refreshToken()) {
                return true;
            }
        }
        return false;
    }
}
