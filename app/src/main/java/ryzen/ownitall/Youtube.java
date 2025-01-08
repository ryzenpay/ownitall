package ryzen.ownitall;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
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
                            Song song = new Song(snippet.getTitle());
                            song.addArtist(new Artist(snippet.getChannelTitle()));
                            song.setDuration(Duration.parse(contentDetails.getDuration()));// TODO: this is not
                                                                                           // working correctly
                            songs.addSong(song);
                        }
                    }
                }
                songs.setYoutubePageToken(pageToken);
                pageToken = response.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            System.err.println("Error obtaining liked songs: " + e.getMessage());
            e.printStackTrace();
        }
        return songs;
    }

    public LinkedHashSet<Album> getAlbums() { // TODO: currently not supported (no youtube music API)
        LinkedHashSet<Album> albums = new LinkedHashSet<>();
        return albums;
    }

    public LinkedHashSet<Playlist> getPlaylists() {
        LinkedHashSet<Playlist> playlists = new LinkedHashSet<>();
        String nextPageToken = null;
        try {
            do {
                YouTube.Playlists.List playlistRequest = youtubeApi.playlists()
                        .list("snippet,contentDetails")
                        .setMine(true)
                        .setMaxResults(50L)
                        .setPageToken(nextPageToken);

                PlaylistListResponse playlistResponse = playlistRequest.execute();

                for (com.google.api.services.youtube.model.Playlist currentPlaylist : playlistResponse.getItems()) {
                    ArrayList<Song> songs = this.getPlaylistSongs(currentPlaylist.getId());
                    if (!songs.isEmpty()) {
                        Playlist playlist = new Playlist(currentPlaylist.getSnippet().getTitle());
                        playlist.addSongs(songs);
                        playlists.add(playlist);
                    }
                }

                nextPageToken = playlistResponse.getNextPageToken();
            } while (nextPageToken != null);
        } catch (IOException e) {
            System.err.println("Error retrieving playlists: " + e.getMessage());
            e.printStackTrace();
        }
        return playlists;
    }

    private ArrayList<Song> getPlaylistSongs(String playlistId) {
        ArrayList<Song> songs = new ArrayList<>();
        String nextPageToken = null;
        try {
            do {
                YouTube.PlaylistItems.List itemRequest = youtubeApi.playlistItems()
                        .list("snippet,contentDetails")
                        .setPlaylistId(playlistId)
                        .setMaxResults(50L)
                        .setPageToken(nextPageToken);

                PlaylistItemListResponse itemResponse = itemRequest.execute();

                for (PlaylistItem item : itemResponse.getItems()) {
                    String videoId = item.getContentDetails().getVideoId();
                    if (isMusicVideo(videoId)) {
                        PlaylistItemSnippet snippet = item.getSnippet();
                        Song song = new Song(snippet.getTitle());
                        song.addArtist(new Artist(snippet.getChannelTitle()));
                        song.setDuration(this.getDuration(videoId));
                        songs.add(song);
                    }
                }
                nextPageToken = itemResponse.getNextPageToken();
            } while (nextPageToken != null);
        } catch (IOException e) {
            System.err.println("Error retrieving playlist songs: " + e.getMessage());
            e.printStackTrace();
        }
        return songs;
    }

    private boolean isMusicVideo(String videoId) {
        try {
            YouTube.Videos.List videoRequest = youtubeApi.videos()
                    .list("snippet")
                    .setId(videoId);

            VideoListResponse videoResponse = videoRequest.execute();
            if (!videoResponse.getItems().isEmpty()) {
                Video video = videoResponse.getItems().get(0);
                return "10".equals(video.getSnippet().getCategoryId());
            }
        } catch (IOException e) {
            System.err.println("Error checking if video is music: " + e.getMessage());
        }
        return false;
    }

    private Duration getDuration(String videoId) {
        try {
            YouTube.Videos.List videoRequest = youtubeApi.videos()
                    .list("contentDetails")
                    .setId(videoId);

            VideoListResponse videoResponse = videoRequest.execute();
            if (!videoResponse.getItems().isEmpty()) {
                Video video = videoResponse.getItems().get(0);
                return Duration.parse(video.getContentDetails().getDuration());
            }
        } catch (IOException e) {
            System.err.println("Error getting video duration: " + e.getMessage());
        }
        return Duration.ZERO;
    }
}
