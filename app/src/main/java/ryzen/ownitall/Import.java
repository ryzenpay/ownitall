package ryzen.ownitall;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.tools.Menu;

public class Import {
    private static final Logger logger = LogManager.getLogger(Import.class);
    private static Settings settings = Settings.load();
    private static Credentials credentials = Credentials.load();
    private LinkedHashSet<Album> albums;
    private LinkedHashSet<Playlist> playlists;
    private LikedSongs likedSongs;
    private LinkedHashMap<String, Runnable> options;

    /**
     * constructor for Import which also prompts user for import options
     * 
     */
    public Import() {
        if (settings.useLibrary && credentials.lastFMIsEmpty()) {
            logger.info("No local LastFM API key found");
            Library.setCredentials();
        }
        this.albums = new LinkedHashSet<>();
        this.playlists = new LinkedHashSet<>();
        this.likedSongs = new LikedSongs();
        this.options = new LinkedHashMap<>();
        options.put("Youtube", this::importYoutube);
        options.put("Spotify", this::importSpotify);
        options.put("Local", this::importLocal);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "IMPORT"); // TODO: add shutdownhook handling (pressing
                                                                         // cntr c)
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    /**
     * import music from youtube, getting or setting credentials as needed
     */
    private void importYoutube() {
        Youtube youtube = new Youtube();
        logger.info("Importing youtube music");
        ProgressBar pb = Main.progressBar("Youtube Import", 3);
        pb.setExtraMessage("Liked songs");
        LikedSongs youtubeLikedSongs = youtube
                .getLikedSongs();
        likedSongs.addSongs(youtubeLikedSongs.getSongs());
        likedSongs.setYoutubePageToken(youtubeLikedSongs.getYoutubePageToken());
        pb.setExtraMessage("Saved Albums").step();
        LinkedHashSet<Album> youtubeAlbums = youtube
                .getAlbums();
        albums.addAll(youtubeAlbums);
        pb.setExtraMessage("Playlists").step();
        LinkedHashSet<Playlist> youtubePlaylists = youtube
                .getPlaylists();
        playlists.addAll(youtubePlaylists);
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("Done importing youtube music");
    }

    /**
     * import music from spotify, getting or setting credentials as needed
     */
    private void importSpotify() {
        logger.info("Importing Spotify music");
        Spotify spotify = new Spotify();
        ProgressBar pb = Main.progressBar("Spotify Import", 3);
        pb.setExtraMessage("Liked Songs");
        LikedSongs spotifyLikedSongs = spotify.getLikedSongs();
        likedSongs.addSongs(spotifyLikedSongs.getSongs());
        likedSongs.setSpotifyPageOffset(spotifyLikedSongs.getSpotifyPageOffset());
        pb.setExtraMessage("Saved Albums").step();
        LinkedHashSet<Album> spotifyAlbums = spotify.getAlbums();
        albums.addAll(spotifyAlbums);
        pb.setExtraMessage("Playlists").step();
        LinkedHashSet<Playlist> spotifyPlaylists = spotify.getPlaylists();
        playlists.addAll(spotifyPlaylists);
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("done importing Spotify music");
    }

    /**
     * import music from a local music library, prompting for location
     */
    private void importLocal() {
        logger.info("Importing local music");
        Local local = new Local();
        ProgressBar pb = Main.progressBar("Local Import", 3);
        pb.setExtraMessage("Liked Songs");
        LikedSongs localLikedSongs = local.getLikedSongs();
        likedSongs.addSongs(localLikedSongs.getSongs());
        pb.setExtraMessage("Saved Albums").step();
        LinkedHashSet<Album> localAlbums = local.getAlbums();
        albums.addAll(localAlbums);
        pb.setExtraMessage("Playlists").step();
        LinkedHashSet<Playlist> localPlaylists = local.getPlaylists();
        Iterator<Playlist> iterator = localPlaylists.iterator();
        while (iterator.hasNext()) { // filter out singles
            Playlist playlist = iterator.next();
            if (playlist.size() <= 1) {
                likedSongs.addSongs(playlist.getSongs());
                iterator.remove();
            }
        }
        playlists.addAll(playlists);
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("done importing local music");
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
    public LikedSongs getLikedSongs() {
        return likedSongs;
    }

    /**
     * print overview of imported music
     * similar to Collection inventory print with recursion 1
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
