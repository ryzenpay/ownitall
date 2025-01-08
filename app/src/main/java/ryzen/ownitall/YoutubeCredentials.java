package ryzen.ownitall;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeCredentials {
    private String applicationName;
    private String clientId;
    private String clientSecret;

    /**
     * default youtube credentials constructor asking for input
     */
    public YoutubeCredentials() {
        System.out.println("Enter youtube application name: ");
        this.applicationName = Input.getInstance().getString();
        System.out.println("Enter youtube client id: ");
        this.clientId = Input.getInstance().getString();
        System.out.println("Enter youtube client secret: ");
        this.clientSecret = Input.getInstance().getString();
    }

    /**
     * youtube credentials constructor with known values
     * 
     * @param applicationName - youtube api application name
     * @param clientId        - youtube api client id
     * @param clientSecret    - youtube api client secret
     */
    public YoutubeCredentials(String applicationName, String clientId, String clientSecret) {
        this.applicationName = applicationName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * get youtube api client id
     * 
     * @return - String client id
     */
    public String getClientId() {
        return this.clientId;
    }

    /**
     * get youtube api client secret
     * 
     * @return - String client secret
     */
    public String getClientSecret() {
        return this.clientSecret;
    }

    /**
     * get youtube api application name
     * 
     * @return - String application name
     */
    public String getApplicationName() {
        return this.applicationName;
    }

    /**
     * check if youtube credentials successfully initialized
     * 
     * @return - true if successfully initialized, false if not
     */
    @JsonIgnore
    public boolean isNull() {
        if (this.getClientId() == null || this.getClientSecret() == null || this.getApplicationName() == null) {
            return true;
        }
        return false;
    }
}
