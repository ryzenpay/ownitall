package ryzen.ownitall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.File;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
public class Credentials extends ryzen.ownitall.tools.Settings {
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
     * Last FM credentials
     * 
     */
    protected String lastFMApiKey = "";

    @JsonIgnore
    public static Credentials load() {
        if (instance == null) {
            instance = new Credentials();
            try {
                instance.importSettings(Credentials.class, credentialsFilePath);
            } catch (Exception e) {
                logger.error(e);
                logger.info("If this persists, delete the file: " + credentialsFilePath);
            }
        }
        return instance;
    }

    @JsonIgnore
    public void clear() {
        instance = null;
        File credentialsFile = new File(credentialsFilePath);
        credentialsFile.delete();
    }

    public void save() {
        try {
            super.saveSettings(credentialsFilePath);
        } catch (Exception e) {
            logger.error("Error saving Credentials: " + e);
        }
    }

    public static Credentials getInstance() {
        return instance;
    }

    public static void setInstance(Credentials instance) {
        Credentials.instance = instance;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static String getCredentialsfilepath() {
        return credentialsFilePath;
    }

    public String getSpotifyClientId() {
        return spotifyClientId;
    }

    public void setSpotifyClientId(String spotifyClientId) {
        this.spotifyClientId = spotifyClientId;
    }

    public String getSpotifyClientSecret() {
        return spotifyClientSecret;
    }

    public void setSpotifyClientSecret(String spotifyClientSecret) {
        this.spotifyClientSecret = spotifyClientSecret;
    }

    public URI getSpotifyRedirectUrl() {
        try {
            return new URI(spotifyRedirectUrl);
        } catch (Exception e) {
            logger.error("Unable to convert spotify redirect url: " + e);
            return null;
        }
    }

    public void setSpotifyRedirectUrl(String spotifyRedirectUrl) {
        this.spotifyRedirectUrl = spotifyRedirectUrl;
    }

    public String getYoutubeApplicationName() {
        return youtubeApplicationName;
    }

    public void setYoutubeApplicationName(String youtubeApplicationName) {
        this.youtubeApplicationName = youtubeApplicationName;
    }

    public String getYoutubeClientId() {
        return youtubeClientId;
    }

    public void setYoutubeClientId(String youtubeClientId) {
        this.youtubeClientId = youtubeClientId;
    }

    public String getYoutubeClientSecret() {
        return youtubeClientSecret;
    }

    public void setYoutubeClientSecret(String youtubeClientSecret) {
        this.youtubeClientSecret = youtubeClientSecret;
    }

    public String getLastFMApiKey() {
        return lastFMApiKey;
    }

    public void setLastFMApiKey(String lastFMApiKey) {
        this.lastFMApiKey = lastFMApiKey;
    }

    /**
     * check if youtube credentials empty (if successfully initialized)
     * 
     * @return - true if empty, false if not
     */
    @JsonIgnore
    public boolean youtubeIsEmpty() {
        if (this.youtubeClientId.isEmpty() || this.youtubeClientSecret.isEmpty()
                || this.youtubeApplicationName.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * check if spotify credentials empty (if successfully initialized)
     * 
     * @return - true if empty, false if not
     */
    @JsonIgnore
    public boolean spotifyIsEmpty() {
        if (this.spotifyClientId.isEmpty() || this.spotifyClientSecret.isEmpty()
                || this.spotifyRedirectUrl.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * check if Last FM credentials empty (if successfully initialized)
     * 
     * @return - true if empty, false if not
     */
    @JsonIgnore
    public boolean lastFMIsEmpty() {
        if (this.lastFMApiKey.isEmpty()) {
            return true;
        }
        return false;
    }

}
