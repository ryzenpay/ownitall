package ryzen.ownitall.integrations;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

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
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Artist;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.classes.Song;
import ryzen.ownitall.integrations.Integration.Export;
import ryzen.ownitall.integrations.Integration.Import;
import ryzen.ownitall.integrations.Integration.Sync;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.IPIterator;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.WebTools.AuthenticationException;

/**
 * <p>
 * Youtube class.
 * </p>
 *
 * @author ryzen
 */
public class Youtube implements Import, Export, Sync {
    private static final Logger logger = new Logger(Youtube.class);
    private static final Library library = Library.load();
    private com.google.api.services.youtube.YouTube youtubeApi;
    private java.util.Collection<String> scopes = Arrays.asList("https://www.googleapis.com/auth/youtube");
    private JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * default youtube constructor asking for user input
     *
     * @throws ryzen.ownitall.Settings.MissingSettingException      if any.
     * @throws ryzen.ownitall.util.WebTools.AuthenticationException if any.
     */
    public Youtube() throws MissingSettingException, AuthenticationException {
        if (Settings.load().isGroupEmpty(Youtube.class)) {
            logger.debug("Empty youtube credentials");
            throw new MissingSettingException("empty youtube credentials");
        }
        this.youtubeApi = this.getService();
        logger.debug("Successfully authenticated into youtube");
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
                YouTube.Videos.List request = this.youtubeApi.videos()
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

    /** {@inheritDoc} */
    @Override
    public void uploadLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = new LikedSongs();
        likedSongs.addSongs(Collection.getLikedSongs().getSongs());
        logger.info("Getting current liked songs to prevent duplicates");
        LikedSongs currLikedSongs = this.getLikedSongs();
        if (currLikedSongs != null) {
            likedSongs.removeSongs(currLikedSongs.getSongs());
        }
        try (IPIterator<?> pb = IPIterator.manual("Liked Songs", likedSongs.size())) {
            for (Song song : likedSongs.getSongs()) {
                String videoId = this.getSongId(song);
                if (videoId != null) {
                    try {
                        this.youtubeApi.videos().rate(videoId, "like").execute();
                        logger.debug("Liked video '" + videoId + "' for song '" + song.getName() + "'");
                    } catch (IOException e) {
                        logger.warn("Exception liking song '" + song.getName() + "': " + e);
                    }
                }
                pb.step(song.getName());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncLikedSongs() throws InterruptedException {
        logger.debug("Getting youtube liked songs to remove mismatches");
        LikedSongs likedSongs = this.getLikedSongs();
        if (likedSongs == null || likedSongs.isEmpty()) {
            return;
        }
        // rate as dislike is youtube's unlike, there is no unlike endpoint
        likedSongs.removeSongs(Collection.getLikedSongs().getSongs());
        try (IPIterator<?> pb = IPIterator.manual("Liked Songs", likedSongs.size())) {
            for (Song song : likedSongs.getSongs()) {
                String videoId = song.getId("youtube");
                if (videoId != null) {
                    try {
                        this.youtubeApi.videos().rate(videoId, "dislike").execute();
                        logger.debug("Unliked video '" + videoId + "' for song '" + song.getName() + "'");
                    } catch (IOException e) {
                        logger.warn("Exception unliking song '" + song.getName() + "': " + e);
                    }
                }
                pb.step(song.getName());
            }
        }
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
                YouTube.Playlists.List playlistRequest = this.youtubeApi.playlists()
                        .list("snippet,contentDetails")
                        .setMine(true)
                        .setMaxResults(Settings.youtubePlaylistLimit)
                        .setPageToken(pageToken);

                PlaylistListResponse playlistResponse = playlistRequest.execute();
                for (com.google.api.services.youtube.model.Playlist currentPlaylist : playlistResponse.getItems()) {
                    Playlist playlist = new Playlist(currentPlaylist.getSnippet().getTitle());
                    playlist.addId("youtube", currentPlaylist.getId());
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
                YouTube.PlaylistItems.List itemRequest = this.youtubeApi.playlistItems()
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

    /**
     * get all playlist items of a playlist, keyed by their video id so exports
     * can deduplicate by id and syncs can delete by playlist item id
     * 
     * @param playlistId - youtube id of playlist
     * @return - linkedhashmap of video id to playlist item
     * @throws InterruptedException - when user interrupts
     */
    private LinkedHashMap<String, PlaylistItem> getPlaylistItems(String playlistId) throws InterruptedException {
        LinkedHashMap<String, PlaylistItem> items = new LinkedHashMap<>();
        if (playlistId == null) {
            logger.debug("null playlistID provided in getPlaylistItems");
            return items;
        }
        String pageToken = null;
        try {
            do {
                PlaylistItemListResponse itemResponse = this.youtubeApi.playlistItems()
                        .list("snippet,contentDetails")
                        .setPlaylistId(playlistId)
                        .setMaxResults(Settings.youtubeSongLimit)
                        .setPageToken(pageToken)
                        .execute();
                for (PlaylistItem item : itemResponse.getItems()) {
                    String videoId = item.getContentDetails() == null ? null : item.getContentDetails().getVideoId();
                    if (videoId != null) {
                        items.put(videoId, item);
                    }
                }
                pageToken = itemResponse.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            logger.error("Exception retrieving playlist items", e);
        }
        return items;
    }

    /** {@inheritDoc} */
    @Override
    public Playlist getPlaylist(String playlistId, String playlistName) throws InterruptedException {
        if (playlistId == null || playlistName == null) {
            logger.debug("null playlistID or playlistName provided in getPlaylist");
            return null;
        }
        Playlist playlist = new Playlist(playlistName);
        ArrayList<Song> songs = this.getPlaylistSongs(playlistId);
        if (songs != null) {
            playlist.addSongs(songs);
        }
        playlist.addId("youtube", playlistId);
        if (playlist.isEmpty()) {
            logger.debug("getPlaylist " + playlistId + " playlist is empty");
            return null;
        }
        return playlist;
    }

    /**
     * {@inheritDoc}
     *
     * unsupported, youtube's api has no concept of saved albums (youtube
     * music's library lives behind unofficial endpoints), albums are covered
     * by the library and lidarr integrations instead
     */
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

    /** {@inheritDoc} */
    @Override
    public void uploadPlaylists() throws InterruptedException {
        ArrayList<Playlist> playlists = Collection.getPlaylists();
        for (Playlist playlist : IPIterator.wrap(playlists, "Uploading Playlists", playlists.size())) {
            this.uploadPlaylist(playlist);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in uploadPlaylist");
            return;
        }
        String playlistId = this.getPlaylistId(playlist);
        if (playlistId == null) {
            playlistId = this.createPlaylist(playlist);
        }
        if (playlistId == null) {
            logger.debug("No playlist to upload '" + playlist.toString() + "' to");
            return;
        }
        // deduplicate by video id instead of by matched song: library
        // filtering could drop a remote song and cause a duplicate add
        LinkedHashMap<String, PlaylistItem> currentItems = this.getPlaylistItems(playlistId);
        try (IPIterator<?> pb = IPIterator.manual(playlist.getName(), playlist.size())) {
            for (Song song : playlist.getSongs()) {
                String videoId = this.getSongId(song);
                if (videoId != null && !currentItems.containsKey(videoId)) {
                    this.addPlaylistItem(playlistId, videoId);
                }
                pb.step(song.getName());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylists() throws InterruptedException {
        logger.debug("Getting youtube playlists to remove mismatches");
        ArrayList<Playlist> playlists = this.getPlaylists();
        if (playlists == null || playlists.isEmpty()) {
            return;
        }
        playlists.removeAll(Collection.getPlaylists());
        try (IPIterator<?> pb = IPIterator.manual("Playlists", playlists.size())) {
            for (Playlist playlist : playlists) {
                String playlistId = playlist.getId("youtube");
                if (playlistId != null) {
                    try {
                        this.youtubeApi.playlists().delete(playlistId).execute();
                        logger.debug("Deleted playlist '" + playlist.getName() + "'");
                    } catch (IOException e) {
                        logger.warn("Exception deleting playlist '" + playlist.getName() + "': " + e);
                    }
                }
                pb.step(playlist.getName());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void syncPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in syncPlaylist");
            return;
        }
        logger.debug("Getting youtube playlist '" + playlist.getName() + "' to remove mismatches");
        String playlistId = this.getPlaylistId(playlist);
        if (playlistId == null) {
            return;
        }
        LinkedHashMap<String, PlaylistItem> items = this.getPlaylistItems(playlistId);
        try (IPIterator<?> pb = IPIterator.manual(playlist.getName(), items.size())) {
            for (java.util.Map.Entry<String, PlaylistItem> entry : items.entrySet()) {
                Song song = this.getSong(entry.getValue());
                // unidentified videos are kept, only remove what we are sure
                // the collection does not contain
                if (song != null && !playlist.contains(song)) {
                    try {
                        this.youtubeApi.playlistItems().delete(entry.getValue().getId()).execute();
                        logger.debug("Removed song '" + song.getName() + "' from playlist '" + playlist.getName()
                                + "'");
                    } catch (IOException e) {
                        logger.warn("Exception removing song '" + song.getName() + "' from playlist '"
                                + playlist.getName() + "': " + e);
                    }
                }
                pb.step(1);
            }
        }
    }

    /**
     * construct a song from a playlist item the same way the import does, so
     * collection matching behaves the same, null when the library cannot
     * identify it and only library verified songs are trusted
     * 
     * @param item - youtube playlist item
     * @return - constructed Song (with its youtube id) or null
     * @throws InterruptedException - when user interrupts
     */
    private Song getSong(PlaylistItem item) throws InterruptedException {
        if (item == null || item.getSnippet() == null || item.getContentDetails() == null) {
            return null;
        }
        String videoId = item.getContentDetails().getVideoId();
        PlaylistItemSnippet snippet = item.getSnippet();
        Song song = new Song(snippet.getTitle());
        song.addArtist(new Artist(snippet.getChannelTitle()));
        if (library != null) {
            Song foundSong = library.getSong(song);
            if (foundSong != null) {
                song = foundSong;
            } else if (Settings.libraryVerified) {
                return null;
            }
        }
        song.addId("youtube", videoId);
        return song;
    }

    /**
     * find the youtube playlist matching this collection playlist by name,
     * playlists.insert gives no lookup by name and the collection might have
     * imported the playlist from another platform
     * 
     * @param playlist - collection playlist
     * @return - youtube playlist id or null
     * @throws InterruptedException - when user interrupts
     */
    private String getPlaylistId(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided in getPlaylistId");
            return null;
        }
        // do not use cached playlist id because the playlist might have been deleted
        String pageToken = null;
        try {
            do {
                PlaylistListResponse response = this.youtubeApi.playlists()
                        .list("snippet")
                        .setMine(true)
                        .setMaxResults(Settings.youtubePlaylistLimit)
                        .setPageToken(pageToken)
                        .execute();
                for (com.google.api.services.youtube.model.Playlist currentPlaylist : response.getItems()) {
                    if (currentPlaylist.getSnippet() != null
                            && currentPlaylist.getSnippet().getTitle() != null
                            && currentPlaylist.getSnippet().getTitle().equalsIgnoreCase(playlist.getName())) {
                        playlist.addId("youtube", currentPlaylist.getId());
                        return currentPlaylist.getId();
                    }
                }
                pageToken = response.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            logger.warn("Exception fetching playlists: " + e);
        }
        return null;
    }

    /**
     * create a private youtube playlist for this collection playlist
     * 
     * @param playlist - collection playlist
     * @return - youtube playlist id of the created playlist or null
     */
    private String createPlaylist(Playlist playlist) {
        com.google.api.services.youtube.model.Playlist created = new com.google.api.services.youtube.model.Playlist();
        PlaylistSnippet snippet = new PlaylistSnippet();
        snippet.setTitle(playlist.getName());
        // required credential, enforced by the constructor's group check
        snippet.setChannelId(Settings.youtubeChannelId);
        if (playlist.getDescription() != null) {
            snippet.setDescription(playlist.getDescription());
        }
        created.setSnippet(snippet);
        created.setStatus(new PlaylistStatus().setPrivacyStatus("private"));
        try {
            com.google.api.services.youtube.model.Playlist response = this.youtubeApi.playlists()
                    .insert("snippet,status", created)
                    .execute();
            if (response != null && response.getId() != null) {
                logger.debug("Created new playlist: " + playlist.getName());
                playlist.addId("youtube", response.getId());
                return response.getId();
            }
        } catch (IOException e) {
            logger.warn("Exception creating playlist '" + playlist.getName() + "': " + e);
        }
        return null;
    }

    /**
     * add a video to a youtube playlist, the resourceId form is used so the
     * video id is not exposed on the item to other users
     * 
     * @param playlistId - youtube playlist id
     * @param videoId    - youtube video id
     */
    private void addPlaylistItem(String playlistId, String videoId) {
        PlaylistItem item = new PlaylistItem();
        PlaylistItemSnippet snippet = new PlaylistItemSnippet();
        snippet.setPlaylistId(playlistId);
        snippet.setResourceId(new ResourceId()
                .setKind("youtube#video")
                .setVideoId(videoId));
        item.setSnippet(snippet);
        try {
            this.youtubeApi.playlistItems().insert("snippet", item).execute();
            logger.debug("Added video '" + videoId + "' to playlist '" + playlistId + "'");
        } catch (IOException e) {
            logger.warn("Exception adding video '" + videoId + "' to playlist '" + playlistId + "': " + e);
        }
    }

    // youtube has no saved albums endpoint (see getAlbums), so the album half
    // of Export and Sync is unsupported in every direction
    /** {@inheritDoc} */
    @Override
    public void uploadAlbums() {
        logger.debug("Unsupported method uploadAlbums called");
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAlbum(Album album) {
        logger.debug("Unsupported method uploadAlbum called");
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbums() {
        logger.debug("Unsupported method syncAlbums called");
    }

    /** {@inheritDoc} */
    @Override
    public void syncAlbum(Album album) {
        logger.debug("Unsupported method syncAlbum called");
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
            YouTube.Videos.List videoRequest = this.youtubeApi.videos()
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
            YouTube.Videos.List videoRequest = this.youtubeApi.videos()
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

    /**
     * <p>
     * getSongId.
     * </p>
     *
     * @param song a {@link ryzen.ownitall.classes.Song} object
     * @return a {@link java.lang.String} object
     * @throws java.lang.InterruptedException if any.
     */
    public String getSongId(Song song) throws InterruptedException {
        if (song == null) {
            logger.debug("null song provided in getSongId");
            return null;
        }
        if (song.getId("youtube") != null) {
            return song.getId("youtube");
        }
        // youtube search costs 100 quota units a call, only songs without a
        // cached video id pay for it
        try {
            YouTube.Search.List search = this.youtubeApi.search()
                    .list("snippet")
                    .setQ(song.toString()) // search query
                    .setType("video")
                    .setVideoCategoryId("10"); // music video
            if (!song.getDuration().isZero()) {
                // youtube filters by duration bucket, most songs fall in the
                // short (under 4 minutes) one
                search.setVideoDuration(song.getDuration().toMinutes() < 4 ? "short" : "medium");
            }
            SearchListResponse listResponse = search.setMaxResults(1L).execute();
            if (listResponse.getItems() != null && !listResponse.getItems().isEmpty()) {
                String videoId = listResponse.getItems().get(0).getId().getVideoId();
                song.addId("youtube", videoId);
                return videoId;
            }
        } catch (IOException e) {
            logger.error("Exception searching for song '" + song.getName() + "'", e);
        }
        logger.debug("Unable to find song '" + song.getName() + "' on youtube");
        return null;
    }
}
