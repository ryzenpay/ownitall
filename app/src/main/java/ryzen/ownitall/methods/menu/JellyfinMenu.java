package ryzen.ownitall.methods.menu;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.methods.Jellyfin;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class JellyfinMenu {
    private static final Logger logger = LogManager.getLogger(JellyfinMenu.class);
    private static Collection collection = Collection.load();
    Jellyfin jellyfin;

    public JellyfinMenu() throws InterruptedException {
        jellyfin = new Jellyfin();
    }

    public void importMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        options.put("Import liked songs", this::optionImportLikedSongs);
        options.put("Import Album", this::optionImportAlbum);
        options.put("Import Playlist", this::optionImportPlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "IMPORT SPOTIFY");
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting spotify import menu choice");
        }
    }

    public void exportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Export Liked Songs (favorites)", this::optionExportLikedSongs);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "EXPORT JELLYFIN");
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting jellyfin export menu choice");
        }
    }

    private void optionImportCollection() {
        logger.debug("Importing jellyfin collection...");
        try (ProgressBar pb = Progressbar.progressBar("Jellyfin Import", 3)) {
            pb.setExtraMessage("Liked Songs");
            collection.addLikedSongs(jellyfin.getLikedSongs());
            pb.setExtraMessage("Saved Albums").step();
            collection.addAlbums(jellyfin.getAlbums());
            pb.setExtraMessage("Playlists").step();
            collection.addPlaylists(jellyfin.getPlaylists());
            pb.setExtraMessage("Done").step();
            logger.debug("done importing jellyfin music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing jellyfin music");
        }
    }

    private void optionImportLikedSongs() {
        logger.debug("Getting all jellyfin favorites...");
        try {
            collection.addLikedSongs(this.jellyfin.getLikedSongs());
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting all liked songs");
        }
    }

    private void optionImportAlbum() {
        String albumName = null;
        try {
            while (albumName == null || albumName.isEmpty()) {
                System.out.print("*Enter Jellyfin Album name: ");
                albumName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album name");
            return;
        }
        String albumId = null;
        try {
            while (albumId == null || albumId.isEmpty()) {
                System.out.print("*Enter Jellyfin Album ID: ");
                albumId = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album id");
            return;
        }
        logger.info("Importing Jellyfin Album...");
        try {
            Album album = jellyfin.getAlbum(albumId, albumName);
            if (album != null) {
                collection.addAlbum(album);
            }
            logger.info("Done importing Jellyfin album");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Jellyfin album");
        }
    }

    private void optionImportPlaylist() {
        String playlistName = null;
        try {
            while (playlistName == null || playlistName.isEmpty()) {
                System.out.print("*Enter Jellyfin Playlist name: ");
                playlistName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist Name");
            return;
        }
        String playlistId = null;
        try {
            while (playlistId == null || playlistId.isEmpty()) {
                System.out.print("*Enter Jellyfin Playlist ID: ");
                playlistId = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist info");
            return;
        }
        logger.info("Importing Jellyfin Playlist...");
        try {
            Playlist playlist = jellyfin.getPlaylist(playlistId, playlistName);
            if (playlist != null) {
                collection.addPlaylist(playlist);
            }
            logger.info("Done importing Jellyfin Playlist");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting Jellyfin playlist");
        }
    }

    private void optionExportLikedSongs() {
        logger.debug("Marking all liked songs as favorites...");
        try {
            this.jellyfin.uploadLikedSongs(collection.getLikedSongs().getSongs());
            logger.debug("successfully marked all liked songs as favorites");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while marking all liked songs as favorites");
        }
    }

    public void sync() {
        try {
            jellyfin.likedSongsCleanUp();
            jellyfin.uploadLikedSongs(collection.getLikedSongs().getSongs());
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncing jellyfin");
        }
    }
}
