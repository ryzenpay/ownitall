package ryzen.ownitall.library;

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

import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.Library;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Youtube {
    private static final Logger logger = LogManager.getLogger(Youtube.class);
    private static final Settings settings = Settings.load();
    private static final Credentials credentials = Credentials.load();
    private static Library library = Library.load();
    private static Collection collection = Collection.load();
    private com.google.api.services.youtube.YouTube youtubeApi;
    private java.util.Collection<String> scopes = Arrays.asList("https://www.googleapis.com/auth/youtube.readonly");
    private JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * default youtube constructor asking for user input
     */
    public Youtube() {
        if (credentials.youtubeIsEmpty()) {
            credentials.setYoutubeCredentials();
        }
        this.youtubeApi = this.getService();
    }

    /**
     * Build and return an authorized API client service.
     *
     * @return an authorized API client service
     */
    private com.google.api.services.youtube.YouTube getService() {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = this.authorize(httpTransport);
            return new com.google.api.services.youtube.YouTube.Builder(httpTransport, this.JSON_FACTORY, credential)
                    .setApplicationName(credentials.getYoutubeApplicationName())
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            logger.error("Error logging in with youtube api: " + e);
            return null;
        }
    }

    /**
     * Create an authorized Credential object.
     *
     * @param httpTransport - idk
     * @return an authorized Credential object.
     * @throws IOException - standard IOException
     */
    private Credential authorize(final NetHttpTransport httpTransport) throws IOException {
        // Create GoogleClientSecrets from the stored client secret
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setInstalled(new GoogleClientSecrets.Details()
                        .setClientId(credentials.getYoutubeClientId())
                        .setClientSecret(credentials.getYoutubeClientSecret()));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, this.JSON_FACTORY, clientSecrets, this.scopes)
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    /**
     * get all youtube liked songs
     * 
     * @param pageToken - token to continue from, default to null
     * @return - constructed LikedSongs
     */
    public void getLikedSongs() {
        if (youtubeApi == null) {
            return;
        }
        String pageToken = collection.getLikedSongs().getYoutubePageToken();
        try {
            do {
                YouTube.Videos.List request = youtubeApi.videos()
                        .list("snippet,contentDetails");
                VideoListResponse response = request.setMyRating("like")
                        .setVideoCategoryId("10") // Category ID 10 is for Music
                        .setMaxResults(settings.getYoutubeSongLimit())
                        .setPageToken(pageToken)
                        .execute();

                List<Video> items = response.getItems();
                for (Video video : items) {
                    VideoSnippet snippet = video.getSnippet();
                    VideoContentDetails contentDetails = video.getContentDetails();
                    if (snippet != null && contentDetails != null) {
                        // Check if the video is in the Music category
                        if ("10".equals(snippet.getCategoryId())) {
                            Song song = null;
                            if (settings.isUseLibrary()) {
                                song = library.getSong(snippet.getTitle(), snippet.getChannelTitle());
                            }
                            if (song == null && !settings.isLibraryVerified()) {
                                song = new Song(snippet.getTitle());
                                song.setArtist(new Artist(snippet.getChannelTitle()));
                            }
                            if (song != null) {
                                song.setDuration(Duration.parse(contentDetails.getDuration()).toSeconds(),
                                        ChronoUnit.SECONDS);
                                String videoLink = "https://www.youtube.com/watch?v=" + video.getId();
                                song.addLink("youtube", videoLink);
                                collection.addLikedSong(song);
                            }
                        }
                    }
                }
                collection.getLikedSongs().setYoutubePageToken(pageToken);
                pageToken = response.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            logger.error("Error obtaining liked songs: " + e);
        }
    }

    /**
     * get saved youtube albums
     * 
     * @return - linkedhashset of constructed Album
     */
    public void getAlbums() { // currently not supported (no youtube music API)
        if (youtubeApi == null) {
            return;
        }
    }

    /**
     * get saved youtube playlists
     * current criteria:
     * - gets all vidoes from any playlist with category id 10
     * 
     * @return - linkedhashset of constructed Playlist
     */
    public void getPlaylists() {
        String pageToken = null;
        if (youtubeApi == null) {
            return;
        }
        try {
            do {
                YouTube.Playlists.List playlistRequest = youtubeApi.playlists()
                        .list("snippet,contentDetails")
                        .setMine(true)
                        .setMaxResults(settings.getYoutubePlaylistLimit())
                        .setPageToken(pageToken);

                PlaylistListResponse playlistResponse = playlistRequest.execute();

                for (com.google.api.services.youtube.model.Playlist currentPlaylist : playlistResponse.getItems()) {
                    Playlist playlist = new Playlist(currentPlaylist.getSnippet().getTitle());
                    Playlist foundPlaylist = collection.getPlaylist(playlist);
                    LinkedHashSet<Song> songs;
                    if (foundPlaylist != null) {
                        songs = this.getPlaylistSongs(currentPlaylist.getId(),
                                foundPlaylist.getYoutubePageToken());
                    } else {
                        songs = this.getPlaylistSongs(currentPlaylist.getId(), null);
                    }
                    if (!songs.isEmpty()) {
                        playlist.addSongs(songs);
                        collection.addPlaylist(playlist);
                    }
                }
                pageToken = playlistResponse.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            logger.error("Error retrieving playlists: " + e);
        }
    }

    /**
     * get all songs from a playlist
     * 
     * @param playlistId - youtube id of playlist
     * @return - arraylist of constructed Song
     */
    private LinkedHashSet<Song> getPlaylistSongs(String playlistId, String pageToken) {
        LinkedHashSet<Song> songs = new LinkedHashSet<>();
        try {
            do {
                YouTube.PlaylistItems.List itemRequest = youtubeApi.playlistItems()
                        .list("snippet,contentDetails")
                        .setPlaylistId(playlistId)
                        .setMaxResults(settings.getYoutubeSongLimit())
                        .setPageToken(pageToken);

                PlaylistItemListResponse itemResponse = itemRequest.execute();

                for (PlaylistItem item : itemResponse.getItems()) {
                    String videoId = item.getContentDetails().getVideoId();
                    if (isMusicVideo(videoId)) {
                        PlaylistItemSnippet snippet = item.getSnippet();
                        Song song = null;
                        String artistName = this.getVideoChannel(videoId);
                        if (settings.isUseLibrary()) {
                            song = library.getSong(snippet.getTitle(), artistName);
                        }
                        if (song == null && !settings.isUseLibrary()) {
                            song = new Song(snippet.getTitle());
                            song.setArtist(new Artist(artistName));
                        }
                        if (song != null) {
                            song.setDuration(this.getDuration(videoId).getSeconds(), ChronoUnit.SECONDS);
                            String videoLink = "https://www.youtube.com/watch?v="
                                    + item.getContentDetails().getVideoId();
                            song.addLink("youtube", videoLink);
                            songs.add(song);
                        }
                    }
                }
                pageToken = itemResponse.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            logger.error("Error retrieving playlist songs: " + e);
        }
        return songs;
    }

    /**
     * check if youtube video is a music video
     * done by checking category id to be 10
     * 
     * @param videoId - youtube video id
     * @return - true if song, false if not
     */
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
            logger.error("Error checking if video is music: " + e);
        }
        return false;
    }

    /**
     * get video duration
     * 
     * @param videoId - spotify video id
     * @return - constructed Duration
     */
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
            logger.error("Error getting video duration: " + e);
        }
        return Duration.ZERO;
    }

    /**
     * get video artist (needed because playlist snippet doesnt include)
     * 
     * @param videoId - video id of artist to find
     * @return - name of the channel
     */
    private String getVideoChannel(String videoId) {
        try {
            YouTube.Videos.List videoRequest = youtubeApi.videos()
                    .list("snippet")
                    .setId(videoId);
            VideoListResponse videoResponse = videoRequest.execute();
            if (!videoResponse.getItems().isEmpty()) {
                return videoResponse.getItems().get(0).getSnippet().getChannelTitle();
            }
        } catch (IOException e) {
            logger.error("Error retrieving video details for " + videoId + ": " + e);
        }
        return null;
    }
}
