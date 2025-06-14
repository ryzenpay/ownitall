package ryzen.ownitall.method;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;

import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.method.interfaces.Import;
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * <p>
 * Youtube class.
 * </p>
 *
 * @author ryzen
 */
public class Youtube implements Import {
    private static final Logger logger = new Logger(Youtube.class);
    private static final Library library = Library.load();
    private com.google.api.services.youtube.YouTube youtubeApi;
    private java.util.Collection<String> scopes = Arrays.asList("https://www.googleapis.com/auth/youtube.readonly");
    private JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * default youtube constructor asking for user input
     *
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public Youtube() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Youtube.class)) {
            logger.debug("Empty youtube credentials");
            throw new MissingSettingException("empty youtube credentials");
        }
        this.youtubeApi = this.getService();
    }

    /**
     * Build and return an authorized API client service.
     *
     * @return an authorized API client service
     * @throws AuthenticationException - when exception logging in
     */
    private com.google.api.services.youtube.YouTube getService() throws AuthenticationException {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = this.authorize(httpTransport);
            return new com.google.api.services.youtube.YouTube.Builder(httpTransport, this.JSON_FACTORY, credential)
                    .setApplicationName(
                            Settings.youtubeApplicatioName)
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            logger.error("Exception logging in with youtube api", e);
            throw new AuthenticationException(e);
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
                        .setClientId(
                                Settings.youtubeClientID)
                        .setClientSecret(Settings.youtubeClientSecret));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, this.JSON_FACTORY, clientSecrets, this.scopes)
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    /**
     * {@inheritDoc}
     *
     * save all youtube liked songs to collection
     */
    @Override
    public LikedSongs getLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        String pageToken = null;
        try (IPIterator<?> pb = IPIterator.manual("Liked Songs", -1)) {
            do {
                YouTube.Videos.List request = youtubeApi.videos()
                        .list("snippet,contentDetails");
                VideoListResponse response = request.setMyRating("like")
                        .setVideoCategoryId("10") // Category ID 10 is for Music
                        .setMaxResults(Settings.youtubeSongLimit)
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
                            song.setDuration(Duration.parse(contentDetails.getDuration()).toSeconds(),
                                    ChronoUnit.SECONDS);
                            if (library != null) {
                                Song foundSong = library.getSong(song);
                                if (foundSong != null) {
                                    song = foundSong;
                                } else if (Settings.libraryVerified) {
                                    song = null;
                                }
                            }
                            if (song != null) {
                                pb.step(song.getName());
                                song.addId("youtube", video.getId());
                                likedSongs.addSong(song);
                            }
                        }
                    }
                }
                pageToken = response.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            logger.error("Exception obtaining liked songs", e);
        }
        return likedSongs;
    }

    /**
     * {@inheritDoc}
     *
     * get saved youtube playlists
     * current criteria:
     * - gets all videos from any playlist with category id 10
     */
    @Override
    public ArrayList<Playlist> getPlaylists() throws InterruptedException {
        String pageToken = null;
        ArrayList<Playlist> playlists = new ArrayList<>();
        try (IPIterator<?> pb = IPIterator.manual("Playlists", -1)) {
            do {
                YouTube.Playlists.List playlistRequest = youtubeApi.playlists()
                        .list("snippet,contentDetails")
                        .setMine(true)
                        .setMaxResults(Settings.youtubePlaylistLimit)
                        .setPageToken(pageToken);

                PlaylistListResponse playlistResponse = playlistRequest.execute();
                for (com.google.api.services.youtube.model.Playlist currentPlaylist : playlistResponse.getItems()) {
                    Playlist playlist = new Playlist(currentPlaylist.getSnippet().getTitle());
                    ArrayList<Song> songs = this.getPlaylistSongs(currentPlaylist.getId());
                    if (songs != null) {
                        pb.step(playlist.getName());
                        playlist.addSongs(songs);
                        playlists.add(playlist);
                    }
                }
                pageToken = playlistResponse.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            logger.error("Exception retrieving playlists", e);
        }
        return playlists;
    }

    /**
     * get all songs from a playlist
     * 
     * @param playlistId - youtube id of playlist
     * @param pageToken  - optional token to continue from (default to 0)
     * @return - arraylist of constructed Song
     * @throws InterruptedException - when user interrupts
     */
    private ArrayList<Song> getPlaylistSongs(String playlistId) throws InterruptedException {
        if (playlistId == null) {
            logger.debug("null playlistID provided in getPlaylistSongs");
            return null;
        }
        ArrayList<Song> songs = new ArrayList<>();
        String pageToken = null;
        try (IPIterator<?> pb = IPIterator.manual(playlistId, -1)) {
            do {
                YouTube.PlaylistItems.List itemRequest = youtubeApi.playlistItems()
                        .list("snippet,contentDetails")
                        .setPlaylistId(playlistId)
                        .setMaxResults(Settings.youtubeSongLimit)
                        .setPageToken(pageToken);
                PlaylistItemListResponse itemResponse = itemRequest.execute();
                for (PlaylistItem item : itemResponse.getItems()) {
                    String videoId = item.getContentDetails().getVideoId();
                    if (isMusicVideo(videoId)) {
                        PlaylistItemSnippet snippet = item.getSnippet();
                        Song song = new Song(snippet.getTitle());
                        song.addArtist(new Artist(this.getVideoChannel(videoId)));
                        if (library != null) {
                            Song foundSong = library.getSong(song);
                            if (foundSong != null) {
                                song = foundSong;
                            } else if (Settings.libraryVerified) {
                                song = null;
                            }
                        }
                        if (song != null) {
                            song.addId("youtube", item.getContentDetails().getVideoId());
                            songs.add(song);
                            pb.step(song.getName());
                        }
                    }
                }
                pageToken = itemResponse.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            logger.error("Exception retrieving playlist songs", e);
        }
        return songs;
    }

    /** {@inheritDoc} */
    @Override
    public Playlist getPlaylist(String playlistId, String playlistName) {
        logger.debug("Unsupported method getPlaylists called");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Album> getAlbums() {
        logger.debug("Unsupported method getAlbums called");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Album getAlbum(String albumId, String albumName, String albumArtistName) {
        logger.debug("Unsupported method getAlbum called");
        return null;
    }

    /**
     * check if youtube video is a music video
     * done by checking category id to be 10
     * 
     * @param videoId - youtube video id
     * @return - true if song, false if not
     */
    private boolean isMusicVideo(String videoId) {
        if (videoId == null) {
            logger.debug("null videoID passed in isMusicVideo");
            return false;
        }
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
            logger.error("Exception checking if video is music", e);
        }
        return false;
    }

    /**
     * get video artist (needed because playlist snippet doesnt include)
     * 
     * @param videoId - video id of artist to find
     * @return - name of the channel
     */
    private String getVideoChannel(String videoId) {
        if (videoId == null) {
            logger.debug("null videoID provided in getVideoChannel");
            return null;
        }
        try {
            YouTube.Videos.List videoRequest = youtubeApi.videos()
                    .list("snippet")
                    .setId(videoId);
            VideoListResponse videoResponse = videoRequest.execute();
            if (!videoResponse.getItems().isEmpty()) {
                return videoResponse.getItems().get(0).getSnippet().getChannelTitle();
            }
        } catch (IOException e) {
            logger.error("Exception retrieving video details for '" + videoId + "'", e);
        }
        return null;
    }
}
