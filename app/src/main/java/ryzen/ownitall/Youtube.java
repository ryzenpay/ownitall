package ryzen.ownitall;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistListResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;

public class Youtube {
    private String application_name;
    private String client_id;
    private String client_secret;
    private Collection<String> scopes = Arrays.asList("https://www.googleapis.com/auth/youtube.readonly");
    private com.google.api.services.youtube.YouTube youtubeApi;
    private JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public Youtube() {
        System.out.println("Enter youtube application name: ");
        this.application_name = Input.getInstance().getString();
        System.out.println("Enter youtube client id: ");
        this.client_id = Input.getInstance().getString();
        System.out.println("Enter youtube client secret: ");
        this.client_secret = Input.getInstance().getString();
        this.youtubeApi = this.getService();
    }

    public Youtube(String application_name, String client_id, String client_secret) {
        this.application_name = application_name;
        this.client_id = client_id;
        this.client_secret = client_secret;
        this.youtubeApi = this.getService();
    }

    /**
     * Create an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    public Credential authorize(final NetHttpTransport httpTransport) throws IOException {
        // Create GoogleClientSecrets from the stored client secret
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setInstalled(new GoogleClientSecrets.Details()
                        .setClientId(this.client_id)
                        .setClientSecret(this.client_secret));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, this.JSON_FACTORY, clientSecrets, this.scopes)
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    /**
     * Build and return an authorized API client service.
     *
     * @return an authorized API client service
     */
    public com.google.api.services.youtube.YouTube getService() {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = authorize(httpTransport);
            return new com.google.api.services.youtube.YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(this.application_name)
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("Error logging in with youtube api: " + e);
            return null;
        }
    }

    /**
     * check if youtubeapi successfully set up
     * 
     * @return - true if working and logged in, false if not working
     */
    public boolean checkLogin() {
        if (this.youtubeApi == null) {
            return false;
        }
        return true;
    }
}
