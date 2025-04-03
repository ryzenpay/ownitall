package ryzen.ownitall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
public class Credentials extends ryzen.ownitall.util.Settings {
    @JsonIgnore
    private static final Logger logger = LogManager.getLogger(Credentials.class);
    @JsonIgnore
    private final static String credentialsFilePath = "credentials.json";
    @JsonIgnore
    private static Credentials instance;
    /**
     * spotify credentials
     */
    protected String spotifyClientId = "";
    protected String spotifyClientSecret = "";
    protected String spotifyRedirectUrl = "";

    @JsonIgnore
    public LinkedHashMap<String, String> getSpotifyCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("Spotify Client ID", "spotifyClientId");
        credentials.put("Spotify Client Secret", "spotifyClientSecret");
        credentials.put("Spotify Redirect URL", "spotifyRedirectUrl");
        return credentials;
    }

    /**
     * youtube credentials
     * 
     */
    protected String youtubeApplicationName = "";
    protected String youtubeClientId = "";
    protected String youtubeClientSecret = "";

    @JsonIgnore
    public LinkedHashMap<String, String> getYoutubeCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("Youtube Application Name", "youtubeApplicationName");
        credentials.put("Youtube Client ID", "youtubeClientId");
        credentials.put("Youtube Client Secret", "youtubeClientSecret");
        return credentials;
    }

    /**
     * Last FM Credentials
     * 
     */
    protected String lastFMApiKey = "";

    @JsonIgnore
    public LinkedHashMap<String, String> getLastFMCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("LastFM API Key", "lastFMApiKey");
        return credentials;
    }

    /**
     * Jellyfin Credentials
     * 
     */
    protected String jellyfinUrl = "";
    protected String jellyfinUsername = "";
    protected String jellyfinPassword = "";

    @JsonIgnore
    public LinkedHashMap<String, String> getJellyfinCredentials() {
        LinkedHashMap<String, String> credentials = new LinkedHashMap<>();
        credentials.put("JellyFin URL", "jellyfinUrl");
        credentials.put("JellyFin Username", "jellyfinUsername");
        credentials.put("JellyFin Password", "jellyfinPassword");
        return credentials;
    }

    @JsonIgnore
    public static Credentials load() {
        if (instance == null) {
            instance = new Credentials();
            try {
                instance.importSettings(Credentials.class, credentialsFilePath);
            } catch (IOException e) {
                logger.error("exception importing credentials", e);
                logger.warn("If this persists, delete the file: '" + credentialsFilePath + "'");
            }
            logger.debug("New instance created");
        }
        return instance;
    }

    @JsonIgnore
    public void clear() {
        this.clearSettings(credentialsFilePath);
    }

    public void save() {
        super.save(credentialsFilePath);
    }

    public String getSpotifyClientId() {
        return spotifyClientId;
    }

    public String getSpotifyClientSecret() {
        return spotifyClientSecret;
    }

    public String getSpotifyRedirectUrl() {
        return spotifyRedirectUrl;
    }

    public String getYoutubeApplicationName() {
        return youtubeApplicationName;
    }

    public String getYoutubeClientId() {
        return youtubeClientId;
    }

    public String getYoutubeClientSecret() {
        return youtubeClientSecret;
    }

    public String getLastFMApiKey() {
        return lastFMApiKey;
    }

    public String getJellyfinUrl() {
        return jellyfinUrl;
    }

    public String getJellyfinUsername() {
        return jellyfinUsername;
    }

    public String getJellyfinPassword() {
        return jellyfinPassword;
    }

    public boolean isJellyFinCredentialsEmpty() {
        for (String fieldName : this.getJellyfinCredentials().values()) {
            if (super.isEmpty(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSpotifyCredentialsEmpty() {
        for (String fieldName : this.getSpotifyCredentials().values()) {
            if (super.isEmpty(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isYoutubeCredentialsEmpty() {
        for (String fieldName : this.getYoutubeCredentials().values()) {
            if (super.isEmpty(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isLastFMCredentialsEmpty() {
        for (String fieldName : this.getLastFMCredentials().values()) {
            if (super.isEmpty(fieldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param spotifyClientId the spotifyClientId to set
     */
    public void setSpotifyClientId(String spotifyClientId) {
        this.spotifyClientId = spotifyClientId;
    }

    /**
     * @param spotifyClientSecret the spotifyClientSecret to set
     */
    public void setSpotifyClientSecret(String spotifyClientSecret) {
        this.spotifyClientSecret = spotifyClientSecret;
    }

    /**
     * @param spotifyRedirectUrl the spotifyRedirectUrl to set
     */
    public void setSpotifyRedirectUrl(String spotifyRedirectUrl) {
        this.spotifyRedirectUrl = spotifyRedirectUrl;
    }

    /**
     * @param youtubeApplicationName the youtubeApplicationName to set
     */
    public void setYoutubeApplicationName(String youtubeApplicationName) {
        this.youtubeApplicationName = youtubeApplicationName;
    }

    /**
     * @param youtubeClientId the youtubeClientId to set
     */
    public void setYoutubeClientId(String youtubeClientId) {
        this.youtubeClientId = youtubeClientId;
    }

    /**
     * @param youtubeClientSecret the youtubeClientSecret to set
     */
    public void setYoutubeClientSecret(String youtubeClientSecret) {
        this.youtubeClientSecret = youtubeClientSecret;
    }

    /**
     * @param lastFMApiKey the lastFMApiKey to set
     */
    public void setLastFMApiKey(String lastFMApiKey) {
        this.lastFMApiKey = lastFMApiKey;
    }

    /**
     * @param jellyfinUrl the jellyfinUrl to set
     */
    public void setJellyfinUrl(String jellyfinUrl) {
        this.jellyfinUrl = jellyfinUrl;
    }

    /**
     * @param jellyfinUsername the jellyfinUsername to set
     */
    public void setJellyfinUsername(String jellyfinUsername) {
        this.jellyfinUsername = jellyfinUsername;
    }

    /**
     * @param jellyfinPassword the jellyfinPassword to set
     */
    public void setJellyfinPassword(String jellyfinPassword) {
        this.jellyfinPassword = jellyfinPassword;
    }
}
