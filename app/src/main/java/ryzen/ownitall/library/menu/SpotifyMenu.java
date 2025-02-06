package ryzen.ownitall.library.menu;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.library.Spotify;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class SpotifyMenu {
    private static final Logger logger = LogManager.getLogger(SpotifyMenu.class);
    private static final Collection collection = Collection.load();
    private Spotify spotify;

    public SpotifyMenu() {
        this.spotify = new Spotify();
    }

    public void SpotifyImportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        options.put("Import liked songs", this::optionImportLikedSongs);
        options.put("Import Album", this::optionImportAlbum);
        options.put("Import Playlist", this::optionImportPlaylist);
        while (true) {
            String choice = Menu.optionMenu(options.keySet(), "IMPORT SPOTIFY");
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

    private void optionImportLikedSongs() {
        logger.info("Importing Spotify Liked Songs...");
        spotify.getLikedSongs();
        logger.info("done importing Spotify Liked songs");
    }

    private void optionImportAlbum() {
        String albumId;
        String albumName;
        try {
            System.out.print("Enter Spotify Album name: ");
            albumName = Input.request().getString();
            System.out.print("Enter Spotify Album ID: ");
            albumId = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album info");
            return;
        }
        logger.info("Importing Spotify Album...");
        Album album = spotify.getAlbum(albumId, albumName, null, null);
        collection.addAlbum(album);
        logger.info("Done importing Spotify album");
    }

    private void optionImportPlaylist() {
        String playlistId;
        String playlistName;
        try {
            System.out.print("Enter Spotify Playlist name: ");
            playlistName = Input.request().getString();
            System.out.print("Enter Spotify Playlist ID: ");
            playlistId = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist info");
            return;
        }
        logger.info("Importing Spotify Playlist...");
        Playlist playlist = spotify.getPlaylist(playlistId, playlistName, null);
        collection.addPlaylist(playlist);
        logger.info("Done importing Spotify Playlist");
    }
}
