package ryzen.ownitall;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import ryzen.ownitall.tools.Input;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeCredentials {
    private String applicationName;
    private String clientId;
    private String clientSecret;
    @JsonIgnore
    private Collection<String> scopes = Arrays.asList("https://www.googleapis.com/auth/youtube.readonly");
    @JsonIgnore
    private JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

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
    @JsonCreator
    public YoutubeCredentials(@JsonProperty("applicationName") String applicationName,
            @JsonProperty("clientId") String clientId,
            @JsonProperty("clientSecret") String clientSecret) {
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

    @JsonIgnore
    public JsonFactory getJsonFactory() {
        return this.JSON_FACTORY;
    }

    /**
     * Create an authorized Credential object.
     *
     * @param httpTransport - idk
     * @return an authorized Credential object.
     * @throws IOException - standard IOException
     */
    @JsonIgnore
    public Credential authorize(final NetHttpTransport httpTransport) throws IOException { // TODO: jframe force window
                                                                                           // on top
                                                                                           // (frame.toFront();
                                                                                           // frame.repaint();)
        // Create GoogleClientSecrets from the stored client secret
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setInstalled(new GoogleClientSecrets.Details()
                        .setClientId(this.getClientId())
                        .setClientSecret(this.getClientSecret()));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, this.JSON_FACTORY, clientSecrets, this.scopes)
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    /**
     * check if youtube credentials empty (if successfully initialized)
     * 
     * @return - true if empty, false if not
     */
    @JsonIgnore
    public boolean isNull() {
        if (this.getClientId() == null || this.getClientSecret() == null || this.getApplicationName() == null) {
            return true;
        }
        return false;
    }
}
