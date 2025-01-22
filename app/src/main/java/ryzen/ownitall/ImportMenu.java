package ryzen.ownitall;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.tools.Menu;

public class ImportMenu {
    private static final Logger logger = LogManager.getLogger(ImportMenu.class);
    private static Settings settings = Settings.load();
    private static Credentials credentials = Credentials.load();
    private Collection collection;
    private LinkedHashMap<String, Runnable> options;

    /**
     * constructor for Import which also prompts user for import options
     * 
     */
    public ImportMenu() {
        if (settings.useLibrary && credentials.lastFMIsEmpty()) {
            logger.info("No local LastFM API key found");
            credentials.setLastFMCredentials();
        }
        this.collection = new Collection();
        this.options = new LinkedHashMap<>();
        options.put("Youtube", this::importYoutube);
        options.put("Spotify", this::importSpotify);
        options.put("Local", this::importLocal);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "IMPORT");
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
        this.collection.mergeLikedSongs(youtube.getLikedSongs());
        pb.setExtraMessage("Saved Albums").step();
        this.collection.mergeAlbums(youtube.getAlbums());
        pb.setExtraMessage("Playlists").step();
        this.collection.mergePlaylists(youtube.getPlaylists());
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
        this.collection.mergeLikedSongs(spotify.getLikedSongs());
        pb.setExtraMessage("Saved Albums").step();
        this.collection.mergeAlbums(spotify.getAlbums());
        pb.setExtraMessage("Playlists").step();
        this.collection.mergePlaylists(spotify.getPlaylists());
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
        pb.setExtraMessage("Saved Albums").step();
        LinkedHashSet<Album> localAlbums = local.getAlbums();
        this.collection.mergeAlbums(localAlbums);
        pb.setExtraMessage("Playlists").step();
        LinkedHashSet<Playlist> localPlaylists = local.getPlaylists();
        Iterator<Playlist> iterator = localPlaylists.iterator();
        while (iterator.hasNext()) { // filter out singles
            Playlist playlist = iterator.next();
            if (playlist.size() <= 1) {
                localLikedSongs.addSongs(playlist.getSongs());
                iterator.remove();
            }
        }
        this.collection.mergePlaylists(localPlaylists);
        this.collection.mergeLikedSongs(localLikedSongs);
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("done importing local music");
    }

    public Collection getCollection() {
        return this.collection;
    }
}
