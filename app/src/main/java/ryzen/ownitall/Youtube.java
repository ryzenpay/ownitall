package ryzen.ownitall;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

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
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.time.Duration;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Youtube extends YoutubeCredentials {
    private static final Logger logger = LogManager.getLogger(YoutubeCredentials.class);
    private static Settings settings = Settings.load();
    private com.google.api.services.youtube.YouTube youtubeApi;

    /**
     * default youtube constructor asking for user input
     */
    public Youtube() {
        super();
        this.youtubeApi = this.getService();
    }

    /**
     * youtube constructor with known values
     * 
     * @param applicationName - youtube api application name
     * @param clientId        - youtube api client id
     * @param clientSecret    - youtube api client secret
     */
    public Youtube(String applicationName, String clientId, String clientSecret) {
        super(applicationName, clientId, clientSecret);
        this.youtubeApi = this.getService();
    }

    /**
     * youtube constructor with known youtube credentials
     * 
     * @param youtubeCredentials - constructed YoutubeCredentials
     */
    public Youtube(YoutubeCredentials youtubeCredentials) {
        super(youtubeCredentials.getApplicationName(), youtubeCredentials.getClientId(),
                youtubeCredentials.getClientSecret());
        this.youtubeApi = this.getService();
    }

    /**
     * get constructed youtube credentials (for exporting)
     * 
     * @return - constructed YoutubeCredentials
     */
    public YoutubeCredentials getYoutubeCredentials() {
        return new YoutubeCredentials(this.getApplicationName(), this.getClientId(), this.getClientSecret());
    }

    /**
     * Build and return an authorized API client service.
     *
     * @return an authorized API client service
     */
    public com.google.api.services.youtube.YouTube getService() {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = this.authorize(httpTransport);
            return new com.google.api.services.youtube.YouTube.Builder(httpTransport, this.getJsonFactory(), credential)
                    .setApplicationName(this.getApplicationName())
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            logger.error("Error logging in with youtube api: " + e);
            return null;
        }
    }

    /**
     * get all youtube liked songs
     * 
     * @return - constructed LikedSongs
     */
    public LikedSongs getLikedSongs() {
        LikedSongs songs = new LikedSongs();
        String pageToken = null;
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
                            Song song = new Song(snippet.getTitle());
                            song.addArtist(new Artist(snippet.getChannelTitle()));
                            song.setDuration(Duration.parse(contentDetails.getDuration()));
                            songs.addSong(song);
                        }
                    }
                }
                songs.setYoutubePageToken(pageToken);
                pageToken = response.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            logger.error("Error obtaining liked songs: " + e);
        }
        return songs;
    }

    /**
     * get saved youtube albums
     * 
     * @return - linkedhashset of constructed Album
     */
    public LinkedHashSet<Album> getAlbums() { // TODO: currently not supported (no youtube music API)
        LinkedHashSet<Album> albums = new LinkedHashSet<>();
        return albums;
    }

    /**
     * get saved youtube playlists
     * current criteria:
     * - gets all vidoes from any playlist with category id 10
     * 
     * @return - linkedhashset of constructed Playlist
     */
    public LinkedHashSet<Playlist> getPlaylists() {
        LinkedHashSet<Playlist> playlists = new LinkedHashSet<>();
        String nextPageToken = null;
        try {
            do {
                YouTube.Playlists.List playlistRequest = youtubeApi.playlists()
                        .list("snippet,contentDetails")
                        .setMine(true)
                        .setMaxResults(settings.getYoutubePlaylistLimit())
                        .setPageToken(nextPageToken);

                PlaylistListResponse playlistResponse = playlistRequest.execute();

                for (com.google.api.services.youtube.model.Playlist currentPlaylist : playlistResponse.getItems()) {
                    ArrayList<Song> songs = this.getPlaylistSongs(currentPlaylist.getId());
                    if (!songs.isEmpty()) {
                        Playlist playlist = new Playlist(currentPlaylist.getSnippet().getTitle());
                        playlist.addSongs(songs);
                        if (!playlist.getSongs().isEmpty()) { // to filter out non music playlists
                            playlists.add(playlist);
                        }
                    }
                }

                nextPageToken = playlistResponse.getNextPageToken();
            } while (nextPageToken != null);
        } catch (IOException e) {
            logger.error("Error retrieving playlists: " + e);
        }
        return playlists;
    }

    /**
     * get all songs from a playlist
     * 
     * @param playlistId - youtube id of playlist
     * @return - arraylist of constructed Song
     */
    private ArrayList<Song> getPlaylistSongs(String playlistId) {
        ArrayList<Song> songs = new ArrayList<>();
        String nextPageToken = null;
        try {
            do {
                YouTube.PlaylistItems.List itemRequest = youtubeApi.playlistItems()
                        .list("snippet,contentDetails")
                        .setPlaylistId(playlistId)
                        .setMaxResults(settings.getYoutubeSongLimit())
                        .setPageToken(nextPageToken);

                PlaylistItemListResponse itemResponse = itemRequest.execute();

                for (PlaylistItem item : itemResponse.getItems()) {
                    String videoId = item.getContentDetails().getVideoId();
                    if (isMusicVideo(videoId)) {
                        PlaylistItemSnippet snippet = item.getSnippet();
                        Song song = new Song(snippet.getTitle());
                        String artistName = this.getVideoChannel(videoId);
                        if (artistName != null) {
                            song.addArtist(new Artist(artistName));
                        }
                        song.setDuration(this.getDuration(videoId));
                        songs.add(song);
                    }
                }
                nextPageToken = itemResponse.getNextPageToken();
            } while (nextPageToken != null);
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
