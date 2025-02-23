package ryzen.ownitall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ryzen.ownitall.util.Input;
import java.net.URI;

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
     * Last FM credentials
     * 
     */
    protected String lastFMApiKey = "";

    /**
     * JellyFin credentials
     * 
     */
    protected String jellyFinUrl = "";
    protected String jellyFinApiKey = "";

    @JsonIgnore
    public static Credentials load() {
        if (instance == null) {
            instance = new Credentials();
            try {
                instance.importSettings(Credentials.class, credentialsFilePath);
            } catch (Exception e) {
                logger.error("exception importing credentials: " + e);
                logger.info("If this persists, delete the file: " + credentialsFilePath);
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
        try {
            super.save(credentialsFilePath);
        } catch (Exception e) {
            logger.error("exception saving Credentials: " + e);
        }
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
        } catch (Exception e) {
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

    public String getJellyFinApiKey() {
        return jellyFinApiKey;
    }

    public String getJellyFinUrl() {
        return jellyFinUrl;
    }

    public void setYoutubeCredentials() {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("Enter youtube application name: ");
            youtubeApplicationName = Input.request().getString();
            System.out.print("Enter youtube client id: ");
            youtubeClientId = Input.request().getString();
            System.out.print("Enter youtube client secret: ");
            youtubeClientSecret = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting youtube credentials");
        }
    }

    public void setSpotifyCredentials() {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("Please provide your client id: ");
            spotifyClientId = Input.request().getString();
            System.out.print("Please provide your client secret: ");
            spotifyClientSecret = Input.request().getString();
            System.out.print("Please provide redirect url:");
            spotifyRedirectUrl = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting spotify credentials");
        }
    }

    public void setLastFMCredentials() {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("Please enter LastFM API key: ");
            lastFMApiKey = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting lastFM credentials");
        }
    }

    public void setJellyFinCredentials() {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("Enter JellyFin instance URL: ");
            jellyFinUrl = Input.request().getURL().toString();
            System.out.print("Enter JellyFin API Key: ");
            jellyFinApiKey = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting JellyFin credentials");
        }
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

    @JsonIgnore
    public boolean soundCloudIsEmpty() {
        if (this.soundCloudClientId.isEmpty() || this.soundCloudClientSecret.isEmpty()) {
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

    @JsonIgnore
    public boolean jellyFinisEmpty() {
        if (this.jellyFinApiKey.isEmpty() || this.jellyFinUrl.isEmpty()) {
            return true;
        }
        return false;
    }
}
