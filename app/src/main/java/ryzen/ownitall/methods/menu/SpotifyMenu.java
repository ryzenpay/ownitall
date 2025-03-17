package ryzen.ownitall.methods.menu;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.methods.Spotify;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class SpotifyMenu {
    private static final Logger logger = LogManager.getLogger(SpotifyMenu.class);
    private static Collection collection = Collection.load();
    private Spotify spotify;

    public SpotifyMenu() throws InterruptedException {
        this.spotify = new Spotify();
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
        options.put("Export Library", this::optionExportCollection);
        options.put("Export Liked Songs", this::optionExportLikedSongs);
        options.put("Export Albums", this::optionExportAlbums);
        options.put("Export Playlists", this::optionExportPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "EXPORT SPOTIFY");
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting spotify export menu choice");
        }
    }

    private void optionImportCollection() {
        logger.debug("Importing Spotify music...");
        try (ProgressBar pb = Progressbar.progressBar("Spotify Import", 3)) {
            pb.setExtraMessage("Liked Songs");
            collection.addLikedSongs(spotify.getLikedSongs());
            pb.setExtraMessage("Saved Albums").step();
            collection.addAlbums(spotify.getAlbums());
            pb.setExtraMessage("Playlists").step();
            collection.addPlaylists(spotify.getPlaylists());
            pb.setExtraMessage("Done").step();
            logger.debug("done importing Spotify music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing spotify music");
        }
    }

    private void optionImportLikedSongs() {
        logger.debug("Importing Spotify Liked Songs...");
        try {
            collection.addLikedSongs(spotify.getLikedSongs());
            logger.debug("done importing Spotify Liked songs");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing spotify liked songs");
        }
    }

    private void optionImportAlbum() {
        String albumName = null;
        try {
            while (albumName == null || albumName.isEmpty()) {
                System.out.print("*Enter Spotify Album name: ");
                albumName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album name");
            return;
        }
        String albumId = null;
        try {
            while (albumId == null || albumId.isEmpty()) {
                System.out.print("*Enter Spotify Album ID: ");
                albumId = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album id");
            return;
        }
        logger.info("Importing Spotify Album...");
        try {
            Album album = spotify.getAlbum(albumId, albumName, null);
            if (album != null) {
                collection.addAlbum(album);
            }
            logger.info("Done importing Spotify album");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting spotify album");
        }
    }

    private void optionImportPlaylist() {
        String playlistName = null;
        try {
            while (playlistName == null || playlistName.isEmpty()) {
                System.out.print("*Enter Spotify Playlist name: ");
                playlistName = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist Name");
            return;
        }
        String playlistId = null;
        try {
            while (playlistId == null || playlistId.isEmpty()) {
                System.out.print("*Enter Spotify Playlist ID: ");
                playlistId = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist info");
            return;
        }
        logger.info("Importing Spotify Playlist...");
        try {
            Playlist playlist = spotify.getPlaylist(playlistId, playlistName, null);
            if (playlist != null) {
                collection.addPlaylist(playlist);
            }
            logger.info("Done importing Spotify Playlist");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting spotify playlist");
        }
    }

    private void optionExportCollection() {
        logger.debug("Uploading Spotify music...");
        try (ProgressBar pb = Progressbar.progressBar("Spotify Upload", 3)) {
            pb.setExtraMessage("Liked Songs");
            spotify.uploadLikedSongs(collection.getLikedSongs().getSongs());
            pb.setExtraMessage("Saved Albums").step();
            spotify.uploadAlbums(collection.getAlbums());
            pb.setExtraMessage("Playlists").step();
            spotify.uploadPlaylists(collection.getPlaylists());
            pb.setExtraMessage("Done").step();
            logger.debug("done uploading Spotify music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading spotify music");
        }
    }

    private void optionExportLikedSongs() {
        logger.debug("Uploading Spotify Liked Songs...");
        try {
            spotify.uploadLikedSongs(collection.getLikedSongs().getSongs());
            logger.debug("done uploading Spotify Liked songs");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading spotify liked songs");
        }
    }

    private void optionExportPlaylists() {
        logger.debug("Uploading Spotify Playlists...");
        try {
            spotify.uploadPlaylists(collection.getPlaylists());
            logger.debug("done uploading Spotify Playlists");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading spotify playlists");
        }
    }

    private void optionExportAlbums() {
        logger.debug("Uploading Spotify Albums...");
        try {
            spotify.uploadAlbums(collection.getAlbums());
            logger.debug("done uploading Spotify Albums");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading spotify albums");
        }
    }

    public void sync() {
        try {
            Spotify spotify = new Spotify();
            spotify.likedSongsCleanUp();
            spotify.uploadLikedSongs(collection.getLikedSongs().getSongs());
            spotify.playlistsCleanUp();
            for (Playlist playlist : collection.getPlaylists()) {
                spotify.playlistCleanUp(playlist);
                spotify.uploadPlaylist(playlist);
            }
            spotify.albumsCleanUp();
            spotify.uploadAlbums(collection.getAlbums());
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncing spotify");
        }
    }
}
