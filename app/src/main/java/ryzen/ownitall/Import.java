package ryzen.ownitall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Import {
    private static final Logger logger = LogManager.getLogger(Import.class);
    private static final Settings settings = Settings.load();
    private boolean status;

    private LinkedHashSet<Album> albums;
    private LinkedHashSet<Playlist> playlists;
    private LikedSongs likedSongs;
    private LinkedHashMap<String, Runnable> options;

    /**
     * constructor for Import which also prompts user for import options
     * 
     * @param dataFolder - datafolder of where to get presaved credentials
     */
    public Import(String dataFolder) {
        this.status = false;
        this.albums = new LinkedHashSet<>();
        this.playlists = new LinkedHashSet<>();
        this.likedSongs = new LikedSongs();
        this.options = new LinkedHashMap<>();
        options.put("Youtube", this::importYoutube);
        options.put("Spotify", this::importSpotify);
        options.put("Local", this::importLocal);
        while (!this.status) {
            String choice = promptImport();
            if (choice != null) {
                if (choice.equals("Exit")) {
                    this.exit();
                } else {
                    this.options.get(choice).run();
                }
            }
        }
    }

    /**
     * prompt user for import options from the options array
     * 
     * @return - String key value of the options hashmap
     */
    private String promptImport() {
        System.out.println("Choose an import option from the following: ");
        int i = 1;
        for (String option : this.options.keySet()) {
            System.out.println("[" + i + "] " + option);
            i++;
        }
        System.out.println("[0] Exit");
        System.out.print("Enter your choice: ");
        int choice = Input.getInstance().getInt();
        if (choice < 0 || choice > options.size()) {
            System.err.println("Incorrect option, try again");
            return null;
        }
        ArrayList<String> options = new ArrayList<>(this.options.keySet());
        options.add(0, "Exit");
        return options.get(choice);
    }

    /**
     * import music from youtube, getting or setting credentials as needed
     */
    private void importYoutube() {
        Sync sync = Sync.load();
        Youtube youtube;
        if (settings.saveCredentials) {
            YoutubeCredentials youtubeCredentials = sync.importYoutubeCredentials();
            if (youtubeCredentials == null) {
                logger.info("No saved youtube credential records");
                youtube = new Youtube();
            } else if (youtubeCredentials.isNull()) {
                logger.error("Incorrect youtube credentials found");
                youtube = new Youtube();
            } else {
                youtube = new Youtube(youtubeCredentials);
            }
            sync.exportYoutubeCredentials(youtube.getYoutubeCredentials());
        } else {
            youtube = new Youtube();
        }
        logger.info("Getting all Youtube liked songs, albums and playlists: ");
        LikedSongs youtubeLikedSongs = youtube.getLikedSongs();
        logger.info("   Processed " + youtubeLikedSongs.size() + " liked songs");
        likedSongs.addSongs(youtubeLikedSongs.getSongs());
        likedSongs.setYoutubePageToken(youtubeLikedSongs.getYoutubePageToken());
        LinkedHashSet<Album> youtubeAlbums = youtube.getAlbums();
        albums.addAll(youtubeAlbums);
        logger.info("   Processed " + youtubeAlbums.size() + " albums");
        LinkedHashSet<Playlist> youtubePlaylists = youtube.getPlaylists();
        logger.info("   Processed " + youtubePlaylists.size() + " playlists");
        playlists.addAll(youtubePlaylists);
        this.status = true;
    }

    /**
     * import music from spotify, getting or setting credentials as needed
     */
    private void importSpotify() {
        Sync sync = new Sync();
        Spotify spotify;
        if (settings.saveCredentials) {
            SpotifyCredentials spotifyCredentials = sync.importSpotifyCredentials();
            if (spotifyCredentials == null) {
                logger.info("No saved spotify credential records");
                spotify = new Spotify();
            } else if (spotifyCredentials.isNull()) {
                logger.error("Incorrect spotify credentials found");
                spotify = new Spotify();
            } else {
                spotify = new Spotify(spotifyCredentials);
            }
            sync.exportSpotifyCredentials(spotify.getSpotifyCredentials());
        } else {
            spotify = new Spotify();
        }
        logger.info("Getting all spotify Playlists, Albums and liked songs: ");
        LikedSongs spotifyLikedSongs = spotify.getLikedSongs();
        logger.info("   Processed " + spotifyLikedSongs.size() + " liked songs");
        likedSongs.addSongs(spotifyLikedSongs.getSongs());
        likedSongs.setSpotifyPageOffset(spotifyLikedSongs.getSpotifyPageOffset());
        LinkedHashSet<Album> spotifyAlbums = spotify.getAlbums();
        logger.info("   Processed " + spotifyAlbums.size() + " albums");
        albums.addAll(spotifyAlbums);
        LinkedHashSet<Playlist> spotifyPlaylists = spotify.getPlaylists();
        logger.info("   Processed " + spotifyPlaylists.size() + " playlists");
        playlists.addAll(spotifyPlaylists);
        this.status = true;
    }

    /**
     * import music from a local music library, prompting for location
     */
    private void importLocal() {
        Local local = new Local();
        logger.info("Getting all music from your local library");
        LikedSongs localLikedSongs = local.getLikedSongs();
        logger.info("   Processed " + localLikedSongs.size() + " liked songs");
        likedSongs.addSongs(localLikedSongs.getSongs());
        LinkedHashSet<Album> localAlbums = local.getAlbums();
        logger.info("    Processed " + localAlbums.size() + " albums");
        albums.addAll(localAlbums);
        LinkedHashSet<Playlist> localPlaylists = local.getPlaylists();
        logger.info("    Processed " + localPlaylists.size() + " playlists");
        playlists.addAll(playlists);
        this.status = true;
    }

    private void exit() {
        this.status = true;
    }

    /**
     * get imported albums
     * 
     * @return - linkedhashset of constructed Album
     */
    public LinkedHashSet<Album> getAlbums() {
        return this.albums;
    }

    /**
     * get imported playlists
     * 
     * @return - linkedhashset of constructed Playlist
     */
    public LinkedHashSet<Playlist> getPlaylists() {
        return this.playlists;
    }

    /**
     * get liked songs
     * 
     * @return - linkedhashset of constructed Song
     */
    public LinkedHashSet<Song> getLikedSongs() {
        return new LinkedHashSet<>(this.likedSongs.getSongs());
    }

    /**
     * print overview of imported music
     * similar to Main inventory print with recursion 1
     */
    public void printOverview() {
        int trackCount = 0;
        for (Playlist playlist : this.playlists) {
            trackCount += playlist.size();
        }
        for (Album album : this.albums) {
            trackCount += album.size();
        }
        System.out.println("Imported " + this.albums.size() + " albums");
        System.out.println("Imported " + this.playlists.size() + " playlists");
        System.out.println("Imported " + this.likedSongs.size() + " liked songs");
        System.out.println("With a total of: " + trackCount + " songs");
    }
}
