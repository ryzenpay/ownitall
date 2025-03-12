package ryzen.ownitall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ryzen.ownitall.util.Input;

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
    protected String jellyfinApiKey = "";
    protected String jellyfinUrl = "";

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

    public void setYoutubeCredentials() throws InterruptedException {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("Enter youtube application name: ");
            youtubeApplicationName = Input.request().getString();
            System.out.print("Enter youtube client id: ");
            youtubeClientId = Input.request().getString(72);
            System.out.print("Enter youtube client secret: ");
            youtubeClientSecret = Input.request().getString(35);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting youtube credentials");
            throw e;
        }
    }

    public void setSpotifyCredentials() throws InterruptedException {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("Client id: ");
            spotifyClientId = Input.request().getString(32);
            System.out.print("Client secret: ");
            spotifyClientSecret = Input.request().getString(32);
            System.out.print("Redirect url:");
            spotifyRedirectUrl = Input.request().getURL().toString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting spotify credentials");
            throw e;
        }
    }

    public void setLastFMCredentials() throws InterruptedException {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("LastFM API key: ");
            lastFMApiKey = Input.request().getString(32);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting lastfm credentials");
            throw e;
        }
    }

    public void setJellyfinCredentials() throws InterruptedException {
        logger.info("A guide to obtaining the following variables is in the readme");
        try {
            System.out.print("instance url: ");
            jellyfinUrl = Input.request().getURL().toString();
            System.out.print("api key: ");
            jellyfinApiKey = Input.request().getString(32);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while setting jellyfin credentials");
            throw e;
        }
    }

    /**
     * check if youtube credentials empty (if successfully initialized)
     * 
     * @return - true if empty, false if not
     */
    @JsonIgnore
    public boolean youtubeIsEmpty() {
        if (this.youtubeClientId.isEmpty()) {
            return true;
        }
        if (this.youtubeApplicationName.isEmpty()) {
            return true;
        }
        if (this.youtubeClientSecret.isEmpty()) {
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
        if (this.spotifyClientId.isEmpty()) {
            return true;
        }
        if (this.spotifyClientSecret.isEmpty()) {
            return true;
        }
        if (this.spotifyRedirectUrl.isEmpty()) {
            return true;
        }
        return false;
    }

    @JsonIgnore
    public boolean soundCloudIsEmpty() {
        if (this.soundCloudClientId.isEmpty()) {
            return true;
        }
        if (this.soundCloudClientSecret.isEmpty()) {
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
    public boolean jellyfinIsEmpty() {
        if (this.jellyfinApiKey.isEmpty()) {
            return true;
        }
        if (this.jellyfinUrl.isEmpty()) {
            return true;
        }
        return false;
    }
}
