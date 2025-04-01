package ryzen.ownitall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
public class Credentials extends ryzen.ownitall.util.Settings {
    @JsonIgnore
    private static Credentials instance;
    @JsonIgnore
    private static final Logger logger = LogManager.getLogger(Credentials.class);
    @JsonIgnore
    private final static String credentialsFilePath = "credentials.json";
    /**
     * spotify credentials
     */
    protected String spotifyClientId = "";
    protected String spotifyClientSecret = "";
    protected String spotifyRedirectUrl = "";

    /**
     * youtube credentials
     * 
     */
    protected String youtubeApplicationName = "";
    protected String youtubeClientId = "";
    protected String youtubeClientSecret = "";

    /**
     * soundcloud credentials
     * 
     */
    protected String soundCloudClientId = "";
    protected String soundCloudClientSecret = "";

    /**
     * Last FM Credentials
     * 
     */
    protected String lastFMApiKey = "";

    /**
     * Jellyfin Credentials
     * 
     */
    protected String jellyfinUrl = "";
    protected String jellyfinUsername = "";
    protected String jellyfinPassword = "";

    @JsonIgnore
    public static Credentials load() {
        if (instance == null) {
            instance = new Credentials();
            try {
                instance.importSettings(Credentials.class, credentialsFilePath);
            } catch (IOException e) {
                logger.error("exception importing credentials: " + e);
                logger.warn("If this persists, delete the file: '" + credentialsFilePath + "'");
            }
            logger.debug("New instance created");
        }
        return instance;
    }

    @JsonIgnore
    public void clear() {
        instance = null;
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

    public URI getSpotifyRedirectUrl() {
        try {
            return new URI(spotifyRedirectUrl);
        } catch (URISyntaxException e) {
            logger.error("Unable to convert spotify redirect url: " + e);
            return null;
        }
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

    public String getSoundCloudClientId() {
        return this.soundCloudClientId;
    }

    public String getSoundCloudClientSecret() {
        return this.soundCloudClientSecret;
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

    public void setYoutubeApplicationName(String name) {
        this.youtubeApplicationName = name;
    }

    public void setYoutubeClientId(String id) {
        this.youtubeClientId = id;
    }

    public void setYoutubeClientSecret(String secret) {
        this.youtubeClientSecret = secret;
    }

    public void setSpotifyClientId(String id) {
        this.spotifyClientId = id;
    }

    public void setSpotifyClientSecret(String secret) {
        this.spotifyClientSecret = secret;
    }

    public void setSpotifyRedirectUrl(String url) {
        this.spotifyRedirectUrl = url;
    }

    public void setLastFMApiKey(String key) {
        this.lastFMApiKey = key;
    }

    public void setJellyFinUrl(String url) {
        this.jellyfinUrl = url;
    }

    public void setJellyFinUsername(String username) {
        this.jellyfinUsername = username;
    }

    public void setJellyFinPassword(String password) {
        this.jellyfinPassword = password;
    }
}
