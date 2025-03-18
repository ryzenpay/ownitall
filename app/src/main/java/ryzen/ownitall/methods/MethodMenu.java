package ryzen.ownitall.methods;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class MethodMenu {
    private static final Logger logger = LogManager.getLogger(MethodMenu.class);
    private static Collection collection = Collection.load();
    private Method method;
    private String choice;

    public MethodMenu() throws InterruptedException {
        LinkedHashSet<String> options = new LinkedHashSet<>();
        options.add("Spotify");
        options.add("Youtube");
        options.add("Local");
        options.add("Manual");
        options.add("Jellyfin");
        while (true) {
            String choice = Menu.optionMenu(options, "METHODS");
            if (choice.equals("Exit")) {
                throw new InterruptedException();
            } else {
                this.choice = choice;
                break;
            }
        }
        this.method = Method.load(choice);
    }

    public void importMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        options.put("Import liked songs", this::optionImportLikedSongs);
        options.put("Import Album", this::optionImportAlbum);
        options.put("Import Playlist", this::optionImportPlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "IMPORT " + this.choice.toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + choice + " import menu choice");
        }
    }

    private void optionImportCollection() {
        logger.debug("Importing '" + this.choice + "' music...");
        try (ProgressBar pb = Progressbar.progressBar(this.choice + " Import", 3)) {
            pb.setExtraMessage("Liked Songs");
            collection.addLikedSongs(method.getLikedSongs());
            pb.setExtraMessage("Saved Albums").step();
            collection.addAlbums(method.getAlbums());
            pb.setExtraMessage("Playlists").step();
            collection.addPlaylists(method.getPlaylists());
            pb.setExtraMessage("Done").step();
            logger.debug("done importing '" + this.choice + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + this.choice + "' music");
        }
    }

    private void optionImportLikedSongs() {
        logger.debug("Importing '" + this.choice + "' Liked Songs...");
        try {
            collection.addLikedSongs(method.getLikedSongs());
            logger.debug("done importing '" + this.choice + "' Liked songs");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + this.choice + "' liked songs");
        }
    }

    private void optionImportAlbum() {
        String albumId = null;
        try {
            while (albumId == null || albumId.isEmpty()) {
                System.out.print("*Enter '" + this.choice + "' Album ID: ");
                albumId = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album id");
            return;
        }
        String albumName = null;
        try {
            System.out.print("Enter '" + this.choice + "' Album name: ");
            albumName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album name");
        }
        String albumArtistName = null;
        try {
            System.out.print("Enter '" + this.choice + "' Album artist name: ");
            albumArtistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album artist name");
        }
        logger.info("Importing '" + this.choice + "' Album...");
        try {
            Album album = method.getAlbum(albumId, albumName, albumArtistName);
            if (album != null) {
                collection.addAlbum(album);
            }
            logger.info("Done importing '" + this.choice + "' album");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + this.choice + "' album");
        }
    }

    private void optionImportPlaylist() {
        String playlistId = null;
        try {
            while (playlistId == null || playlistId.isEmpty()) {
                System.out.print("*Enter '" + this.choice + "' Playlist ID: ");
                playlistId = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist id");
            return;
        }
        String playlistName = null;
        try {
            System.out.print("Enter '" + this.choice + "' Playlist Name: ");
            playlistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist name");
        }
        logger.info("Importing '" + this.choice + "' Playlist...");
        try {
            Playlist playlist = method.getPlaylist(playlistId, playlistName);
            if (playlist != null) {
                collection.addPlaylist(playlist);
            }
            logger.info("Done importing '" + this.choice + "' Playlist");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + this.choice + "' playlist");
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
                String choice = Menu.optionMenu(options.keySet(), "EXPORT " + this.choice.toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                } else {
                    options.get(choice).run();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + this.choice + " export menu choice");
        }
    }

    private void optionExportCollection() {
        logger.debug("Uploading '" + this.choice + "' music...");
        try (ProgressBar pb = Progressbar.progressBar(this.choice + " Upload", 3)) {
            pb.setExtraMessage("Liked Songs");
            method.uploadLikedSongs();
            pb.setExtraMessage("Saved Albums").step();
            method.uploadAlbums();
            pb.setExtraMessage("Playlists").step();
            method.uploadPlaylists();
            pb.setExtraMessage("Done").step();
            logger.debug("done uploading '" + this.choice + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.choice + "' music");
        }
    }

    private void optionExportLikedSongs() {
        logger.debug("Uploading '" + this.choice + "' Liked Songs...");
        try {
            method.uploadLikedSongs();
            logger.debug("done uploading '" + this.choice + "' Liked songs");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.choice + "' liked songs");
        }
    }

    private void optionExportPlaylists() {
        logger.debug("Uploading '" + this.choice + "' Playlists...");
        try {
            method.uploadPlaylists();
            logger.debug("done uploading '" + this.choice + "' Playlists");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.choice + "' playlists");
        }
    }

    private void optionExportAlbums() {
        logger.debug("Uploading '" + this.choice + "' Albums...");
        try {
            method.uploadAlbums();
            logger.debug("done uploading '" + this.choice + "' Albums");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.choice + "' albums");
        }
    }

    public void sync() {
        try {
            method.syncLikedSongs();
            method.uploadLikedSongs();
            method.syncPlaylists();
            for (Playlist playlist : collection.getPlaylists()) {
                method.syncPlaylist(playlist);
                method.uploadPlaylist(playlist);
            }
            method.syncAlbums();
            for (Album album : collection.getAlbums()) {
                method.syncAlbum(album);
                method.uploadAlbum(album);
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncing '" + this.choice + "'");
        }
    }

}
