package ryzen.ownitall;

import java.net.URI;
import java.time.LocalDateTime;

public class SpotifyCredentials {
    private String clientId;
    private String clientSecret;
    private URI redirectUrl;
    private String code;
    private LocalDateTime codeExpiration;

    /**
     * constructer purely to keep and store spotify api credentials
     * 
     * @param client_id       - spotify api client id
     * @param client_secret   - spotify api client secret
     * @param redirect_url    - spotify api redirect url
     * @param code            - temporary spotify api oauth code
     * @param code_expiration - time when code expires (in seconds)
     */
    public SpotifyCredentials(String clientId, String clientSecret, URI redirectUrl, String code,
            int codeExpiration) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = redirectUrl;
        this.code = code;
        this.setCodeExpiration(codeExpiration);
    }

    private void setCodeExpiration(int codeExpiration) {
        this.codeExpiration = LocalDateTime.now().plusSeconds(codeExpiration);
    }

    /**
     * check the expiration of the current code
     * 
     * @return - true if expired, false if not expired
     */
    public boolean checkExpiration() {
        if (this.codeExpiration == null) {
            return true;
        }
        if (this.codeExpiration.isAfter(LocalDateTime.now())) {
            return true;
        }
        return false;
    }
}
