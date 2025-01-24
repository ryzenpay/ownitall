package ryzen.ownitall.library.menu;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.library.Spotify;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class SpotifyMenu {
    private static final Logger logger = LogManager.getLogger(SpotifyMenu.class);
    private Spotify spotify;

    public SpotifyMenu() {
        this.spotify = new Spotify();
    }

    public void SpotifyImportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "SPOTIFY");
            if (choice.equals("Exit")) {
                break;
            } else {
                options.get(choice).run();
            }
        }
    }

    private void optionImportCollection() {
        logger.info("Importing Spotify music...");
        ProgressBar pb = Progressbar.progressBar("Spotify Import", 3);
        pb.setExtraMessage("Liked Songs");
        spotify.getLikedSongs();
        pb.setExtraMessage("Saved Albums").step();
        spotify.getAlbums();
        pb.setExtraMessage("Playlists").step();
        spotify.getPlaylists();
        pb.setExtraMessage("Done").step();
        pb.close();
        logger.info("done importing Spotify music");
    }

    /**
     * TODO: allow import of album by passing album id
     * ^ requires redesign of spotify class to return album rather than array of
     * songs
     */
}
