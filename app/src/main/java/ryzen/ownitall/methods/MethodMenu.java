package ryzen.ownitall.methods;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class MethodMenu {
    private static final Logger logger = LogManager.getLogger(MethodMenu.class);
    private static Collection collection = Collection.load();
    private Method method;

    public MethodMenu() throws InterruptedException {
        String choice = Menu.optionMenu(Method.methods.keySet(), "METHODS");
        if (choice.equals("Exit")) {
            throw new InterruptedException();
        }
        Class<? extends Method> methodClass = Method.methods.get(choice);
        try {
            this.method = methodClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
            logger.error("Error instantiating method '" + choice + "': " + e);
            throw new InterruptedException();
        }
    }

    public void importMenu() {
        // TODO: import all albums and playlists
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        options.put("Import liked songs", this::optionImportLikedSongs);
        options.put("Import Album(s)", this::optionImportAlbum);
        options.put("Import Playlist(s)", this::optionImportPlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT " + this.method.getClass().getName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + method.getClass().getName() + " import menu choice");
        }
    }

    private void optionImportCollection() {
        logger.debug("Importing '" + this.method.getClass().getName() + "' music...");
        try (ProgressBar pb = Progressbar.progressBar(this.method.getClass().getName() + " Import", 3)) {
            pb.setExtraMessage("Liked Songs");
            this.importLikedSongs();
            pb.setExtraMessage("Saved Albums").step();
            this.importAlbums();
            pb.setExtraMessage("Playlists").step();
            this.importPlaylists();
            pb.setExtraMessage("Done").step();
            logger.debug("done importing '" + this.method.getClass().getName() + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + this.method.getClass().getName() + "' music");
        }
    }

    private void optionImportLikedSongs() {
        try {
            this.importLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + this.method.getClass().getName() + "' liked songs");
        }
    }

    private void optionImportAlbum() {
        String albumId = null;
        try {
            while (albumId == null || albumId.isEmpty()) {
                System.out.print("*Enter '" + this.method.getClass().getName() + "' Album ID: ");
                albumId = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album id");
            return;
        }
        String albumName = null;
        try {
            System.out.print("Enter '" + this.method.getClass().getName() + "' Album name: ");
            albumName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album name");
        }
        String albumArtistName = null;
        try {
            System.out.print("Enter '" + this.method.getClass().getName() + "' Album artist name: ");
            albumArtistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album artist name");
        }
        try {
            this.importAlbum(albumId, albumName, albumArtistName);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + this.method.getClass().getName() + "' album");
        }
    }

    private void optionImportPlaylist() {
        String playlistId = null;
        try {
            while (playlistId == null || playlistId.isEmpty()) {
                System.out.print("*Enter '" + this.method.getClass().getName() + "' Playlist ID: ");
                playlistId = Input.request().getString();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist id");
            return;
        }
        String playlistName = null;
        try {
            System.out.print("Enter '" + this.method.getClass().getName() + "' Playlist Name: ");
            playlistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist name");
        }
        try {
            this.importPlaylist(playlistId, playlistName);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + this.method.getClass().getName() + "' playlist");
        }
    }

    private void importLikedSongs() throws InterruptedException {
        logger.info("Getting liked songs from '" + method.getClass().getName() + "'...");
        LikedSongs likedSongs = method.getLikedSongs();
        if (likedSongs != null) {
            collection.addLikedSongs(likedSongs);
            logger.info("Imported " + likedSongs.size() + " liked songs from '" + method.getClass().getName() + "'");
        }
    }

    private void importPlaylists() throws InterruptedException {
        logger.info("Getting playlists from '" + method.getClass().getName() + "'...");
        ArrayList<Playlist> playlists = method.getPlaylists();
        if (playlists != null) {
            collection.addPlaylists(playlists);
            logger.info("Imported " + playlists.size() + " playlists from '" + method.getClass() + "'");
        }
    }

    private void importPlaylist(String playlistId, String playlistName) throws InterruptedException {
        if (playlistId == null) {
            logger.debug("null playlist id provided in importPlaylist");
            return;
        }
        logger.info("Getting playlist '" + playlistId + "' from '" + method.getClass().getName() + "'...");
        Playlist playlist = method.getPlaylist(playlistId, playlistName);
        if (playlist != null) {
            collection.addPlaylist(playlist);
            logger.info("Imported playlist '" + playlist.getName() + "' (" + playlist.size() + ") from '"
                    + method.getClass().getName() + "'");
        }
    }

    private void importAlbums() throws InterruptedException {
        logger.info("Getting albums from '" + method.getClass().getName() + "'...");
        ArrayList<Album> albums = method.getAlbums();
        if (albums != null) {
            collection.addAlbums(albums);
            logger.info("Imported " + albums.size() + " albums from '" + method.getClass() + "'");
        }
    }

    private void importAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        if (albumId == null) {
            logger.debug("null album id provided in importAlbum");
            return;
        }
        logger.info("Getting album '" + albumId + "' from '" + method.getClass().getName() + "'...");
        Album album = method.getAlbum(albumId, albumName, albumArtistName);
        if (album != null) {
            collection.addAlbum(album);
            logger.info("Imported album '" + album.getName() + "' (" + album.size() + ") from '"
                    + method.getClass().getName() + "'");
        }
    }

    public void exportMenu() {
        // TODO: export individual album or playlist
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Export Library", this::optionExportCollection);
        options.put("Export Liked Songs", this::optionExportLikedSongs);
        options.put("Export Albums", this::optionExportAlbums);
        options.put("Export Playlists", this::optionExportPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "EXPORT " + this.method.getClass().getName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + this.method.getClass().getName() + " export menu choice");
        }
    }

    private void optionExportCollection() {
        logger.debug("Uploading '" + this.method.getClass().getName() + "' (" + collection.getTotalTrackCount()
                + ") music...");
        try (ProgressBar pb = Progressbar.progressBar(this.method.getClass().getName() + " Upload", 3)) {
            pb.setExtraMessage("Liked Songs");
            this.exportLikedSongs();
            pb.setExtraMessage("Saved Albums").step();
            this.exportAlbums();
            pb.setExtraMessage("Playlists").step();
            this.exportPlaylists();
            pb.setExtraMessage("Done").step();
            logger.debug("done uploading '" + this.method.getClass().getName() + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.method.getClass().getName() + "' music");
        }
    }

    private void optionExportLikedSongs() {
        try {
            method.uploadLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.method.getClass().getName() + "' liked songs");
        }
    }

    private void optionExportPlaylists() {
        try {
            this.exportPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.method.getClass().getName() + "' playlists");
        }
    }

    private void optionExportAlbums() {
        try {
            this.exportAlbums();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.method.getClass().getName() + "' albums");
        }
    }

    private void exportLikedSongs() throws InterruptedException {
        logger.info("Uploading " + collection.getLikedSongs().size() + " liked songs to '" + method.getClass().getName()
                + "'");
        method.uploadLikedSongs();
        logger.info("Exported " + collection.getLikedSongs().size() + " liked songs to '" + method.getClass().getName()
                + "'");
    }

    private void exportPlaylists() throws InterruptedException {
        logger.info("Uploading " + collection.getPlaylistCount() + " playlists to '" + method.getClass().getName()
                + "'");
        method.uploadPlaylists();
        logger.info("Exported " + collection.getPlaylistCount() + " playlists to '"
                + method.getClass().getName() + "'");
    }

    private void exportPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided to exportPlaylist");
            return;
        }
        logger.info("Uploading playlist '" + playlist.getName() + "' (" + playlist.size() + ") to '"
                + method.getClass().getName() + "'");
        method.uploadPlaylist(playlist);
        logger.info("Exported playlist '" + playlist.getName() + "' to '" + method.getClass().getName() + "'");
    }

    private void exportAlbums() throws InterruptedException {
        logger.info("Uploading " + collection.getAlbumCount() + " albums to '" + method.getClass().getName()
                + "'");
        method.uploadPlaylists();
        logger.info("Exported " + collection.getAlbumCount() + " albums to '"
                + method.getClass().getName() + "'");
    }

    private void exportAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided to exportPlaylist");
            return;
        }
        logger.info("Uploading album '" + album.getName() + "' (" + album.size() + ") to '"
                + method.getClass().getName() + "'");
        method.uploadAlbum(album);
        logger.info("Exported album '" + album.getName() + "' to '" + method.getClass().getName() + "'");
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
            logger.debug("Interrupted while syncing '" + this.method.getClass().getName() + "'");
        }
    }
}
