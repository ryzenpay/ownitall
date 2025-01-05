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
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.time.Duration;
import java.util.List;

public class Youtube extends YoutubeCredentials {
    private Collection<String> scopes = Arrays.asList("https://www.googleapis.com/auth/youtube.readonly");
    private com.google.api.services.youtube.YouTube youtubeApi;
    private JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public Youtube() {
        super();
        this.youtubeApi = this.getService();
    }

    public Youtube(String applicationName, String clientId, String clientSecret) {
        super(applicationName, clientId, clientSecret);
        this.youtubeApi = this.getService();
    }

    public Youtube(YoutubeCredentials youtubeCredentials) {
        super(youtubeCredentials.getApplicationName(), youtubeCredentials.getClientId(),
                youtubeCredentials.getClientSecret());
        this.youtubeApi = this.getService();
    }

    public YoutubeCredentials getYoutubeCredentials() {
        return new YoutubeCredentials(this.getApplicationName(), this.getClientId(), this.getClientSecret());
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
     * Build and return an authorized API client service.
     *
     * @return an authorized API client service
     */
    public com.google.api.services.youtube.YouTube getService() {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = authorize(httpTransport);
            return new com.google.api.services.youtube.YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(this.getApplicationName())
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

    public LikedSongs getLikedSongs() {
        LikedSongs songs = new LikedSongs();
        String pageToken = null;
        try {
            do {
                YouTube.Videos.List request = youtubeApi.videos()
                        .list("snippet,contentDetails");
                VideoListResponse response = request.setMyRating("like")
                        .setVideoCategoryId("10") // Category ID 10 is for Music
                        .setMaxResults(50L)
                        .setPageToken(pageToken)
                        .execute();

                List<Video> items = response.getItems();
                for (Video video : items) {
                    VideoSnippet snippet = video.getSnippet();
                    VideoContentDetails contentDetails = video.getContentDetails();
                    if (snippet != null && contentDetails != null) {
                        // Check if the video is in the Music category
                        if ("10".equals(snippet.getCategoryId())) {
                            Duration duration = Duration.parse(contentDetails.getDuration()); // TODO: this is not
                                                                                              // working correctly
                            ArrayList<Artist> artists = new ArrayList<>();
                            artists.add(new Artist(snippet.getChannelTitle()));
                            songs.addSong(new Song(snippet.getTitle(), artists, duration));
                        }
                    }
                }

                pageToken = response.getNextPageToken();
            } while (pageToken != null);
            songs.setYoutubePageToken(pageToken);
        } catch (IOException e) {
            System.err.println("Error obtaining liked songs: " + e.getMessage());
            e.printStackTrace();
        }
        return songs;
    }

}
