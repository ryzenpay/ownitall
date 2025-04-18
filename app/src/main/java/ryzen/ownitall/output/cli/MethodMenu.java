package ryzen.ownitall.output.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.tongfei.progressbar.ProgressBar;
import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.methods.Method;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Progressbar;

public class MethodMenu {
    private static final Logger logger = LogManager.getLogger();
    private static final Credentials credentials = Credentials.load();
    private Method method;
    private String methodName;

    public MethodMenu() throws InterruptedException {
        LibraryMenu.initializeLibrary();
        String choice = Menu.optionMenu(Method.methods.keySet(), "METHODS");
        if (choice.equals("Exit")) {
            throw new InterruptedException("Cancelled method selection");
        }
        Class<? extends Method> methodClass = Method.methods.get(choice);
        this.initializeMethod(methodClass);
    }

    private void initializeMethod(Class<? extends Method> methodClass) throws InterruptedException {
        if (Method.isCredentialsEmpty(methodClass)) {
            this.setCredentials(methodClass);
        }
        this.method = Method.load(methodClass);
        this.methodName = method.getClass().getSimpleName();
    }

    private void setCredentials(Class<? extends Method> methodClass) throws InterruptedException {
        if (methodClass == null) {
            logger.debug("null methodClass provided in setCredentials");
            return;
        }
        if (!Method.isCredentialsEmpty(methodClass)) {
            return;
        }
        LinkedHashMap<String, String> classCredentials = Method.credentialGroups.get(methodClass);
        if (classCredentials != null) {
            for (String name : classCredentials.keySet()) {
                System.out.print("Enter '" + name + "': ");
                String value = Input.request().getString();
                if (!credentials.change(classCredentials.get(name), value)) {
                    throw new InterruptedException(
                            "Unable to set credential '" + name + "' for '" + methodClass.getSimpleName() + "'");
                }
            }
        }
        if (Method.isCredentialsEmpty(methodClass)) {
            throw new InterruptedException("Unable to set credentials for '" + methodClass.getSimpleName() + "'");
        }
    }

    public void importMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        options.put("Import liked songs", this::optionImportLikedSongs);
        options.put("Import Album(s)", this::optionImportAlbumsMenu);
        options.put("Import Playlist(s)", this::optionImportPlaylistsMenu);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT " + this.methodName.toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + methodName + " import menu choice");
        }
    }

    private void optionImportCollection() {
        logger.debug("Importing '" + this.methodName + "' music...");
        try (ProgressBar pb = Progressbar.progressBar(this.methodName + " Import", 3)) {
            pb.setExtraMessage("Liked Songs");
            this.importLikedSongs();
            pb.setExtraMessage("Saved Albums").step();
            this.importAlbums();
            pb.setExtraMessage("Playlists").step();
            this.importPlaylists();
            pb.setExtraMessage("Done").step();
            logger.debug("done importing '" + this.methodName + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + this.methodName + "' music", e);
        }
    }

    private void optionImportLikedSongs() {
        try {
            this.importLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + this.methodName + "' liked songs", e);
        }
    }

    private void optionImportAlbumsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved albums", this::optionImportAlbums);
        options.put("Individual album", this::optionImportAlbum);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT ALBUM" + this.methodName.toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while getting " + methodName + " import album menu choice");
        }
    }

    private void optionImportAlbums() {
        try {
            this.importAlbums();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing albums", e);
        }
    }

    private void optionImportAlbum() {
        String albumId = null;
        String albumName = null;
        String albumArtistName = null;
        try {
            while (albumId == null || albumId.isEmpty()) {
                System.out.print("*Enter '" + this.methodName + "' Album ID: ");
                albumId = Input.request().getString();
            }
            System.out.print("Enter '" + this.methodName + "' Album name: ");
            albumName = Input.request().getString();
            System.out.print("Enter '" + this.methodName + "' Album artist name: ");
            albumArtistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album details");
            return;
        }
        try {
            this.importAlbum(albumId, albumName, albumArtistName);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + this.methodName + "' album", e);
        }
    }

    private void optionImportPlaylistsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved playlists", this::optionImportPlaylists);
        options.put("Individual playlist", this::optionImportPlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT PlAYLIST" + this.methodName.toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while getting " + methodName + " import playlist menu choice");
        }
    }

    private void optionImportPlaylists() {
        try {
            this.importPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing playlists", e);
        }
    }

    private void optionImportPlaylist() {
        String playlistId = null;
        String playlistName = null;
        try {
            while (playlistId == null || playlistId.isEmpty()) {
                System.out.print("*Enter '" + this.methodName + "' Playlist ID: ");
                playlistId = Input.request().getString();
            }
            System.out.print("Enter '" + this.methodName + "' Playlist Name: ");
            playlistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist details");
            return;
        }
        try {
            this.importPlaylist(playlistId, playlistName);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + this.methodName + "' playlist", e);
        }
    }

    private void importLikedSongs() throws InterruptedException {
        logger.info("Getting liked songs from '" + methodName + "'...");
        LikedSongs likedSongs = method.getLikedSongs();
        if (likedSongs != null) {
            Collection.addLikedSongs(likedSongs);
            logger.info(
                    "Imported " + likedSongs.size() + " liked songs from '" + methodName + "'");
        }
    }

    private void importPlaylists() throws InterruptedException {
        logger.info("Getting playlists from '" + methodName + "'...");
        ArrayList<Playlist> playlists = method.getPlaylists();
        if (playlists != null) {
            Collection.addPlaylists(playlists);
            logger.info("Imported " + playlists.size() + " playlists from '" + methodName + "'");
        }
    }

    private void importPlaylist(String playlistId, String playlistName) throws InterruptedException {
        if (playlistId == null) {
            logger.debug("null playlist id provided in importPlaylist");
            return;
        }
        logger.info("Getting playlist '" + playlistId + "' from '" + methodName + "'...");
        Playlist playlist = method.getPlaylist(playlistId, playlistName);
        if (playlist != null) {
            Collection.addPlaylist(playlist);
            logger.info("Imported playlist '" + playlist.getName() + "' (" + playlist.size() + ") from '"
                    + methodName + "'");
        }
    }

    private void importAlbums() throws InterruptedException {
        logger.info("Getting albums from '" + methodName + "'...");
        ArrayList<Album> albums = method.getAlbums();
        if (albums != null) {
            Collection.addAlbums(albums);
            logger.info("Imported " + albums.size() + " albums from '" + methodName + "'");
        }
    }

    private void importAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        if (albumId == null) {
            logger.debug("null album id provided in importAlbum");
            return;
        }
        logger.info("Getting album '" + albumId + "' from '" + methodName + "'...");
        Album album = method.getAlbum(albumId, albumName, albumArtistName);
        if (album != null) {
            Collection.addAlbum(album);
            logger.info("Imported album '" + album.getName() + "' (" + album.size() + ") from '"
                    + methodName + "'");
        }
    }

    public void exportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Export Library", this::optionExportCollection);
        options.put("Export Liked Songs", this::optionExportLikedSongs);
        options.put("Export Album(s)", this::optionExportAlbums);
        options.put("Export Playlist(s)", this::optionExportPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "EXPORT " + this.methodName.toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + this.methodName + " export menu choice");
        }
    }

    private void optionExportCollection() {
        logger.debug("Uploading '" + this.methodName + "' (" + Collection.getTotalTrackCount()
                + ") music...");
        try (ProgressBar pb = Progressbar.progressBar(this.methodName + " Upload", 3)) {
            pb.setExtraMessage("Liked Songs");
            this.exportLikedSongs();
            pb.setExtraMessage("Saved Albums").step();
            this.exportAlbums();
            pb.setExtraMessage("Playlists").step();
            this.exportPlaylists();
            pb.setExtraMessage("Done").step();
            logger.debug("done uploading '" + this.methodName + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.methodName + "' music", e);
        }
    }

    private void optionExportLikedSongs() {
        try {
            method.uploadLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.methodName + "' liked songs", e);
        }
    }

    private void optionExportPlaylists() {
        try {
            LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
            options.put("All", null);
            for (Playlist playlist : Collection.getPlaylists()) {
                options.put(playlist.toString(), playlist);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "PLAYLIST EXPORT MENU");
                if (choice.equals("Exit")) {
                    return;
                }
                if (choice.equals("All")) {
                    this.exportPlaylists();
                } else {
                    this.exportPlaylist(options.get(choice));
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting export playlist choice");
            }
            this.exportPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.methodName + "' playlists", e);
        }
    }

    private void optionExportAlbums() {
        try {
            LinkedHashMap<String, Album> options = new LinkedHashMap<>();
            options.put("All", null);
            for (Album album : Collection.getAlbums()) {
                options.put(album.toString(), album);
            }
            try {
                String choice = Menu.optionMenu(options.keySet(), "ALBUM EXPORT MENU");
                if (choice.equals("Exit")) {
                    return;
                }
                if (choice.equals("All")) {
                    this.exportAlbums();
                } else {
                    this.exportAlbum(options.get(choice));
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted while getting export playlist choice");
            }
            this.exportPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + this.methodName + "' playlists", e);
        }
    }

    private void exportLikedSongs() throws InterruptedException {
        logger.info("Uploading " + Collection.getLikedSongs().size() + " liked songs to '"
                + methodName
                + "'");
        method.uploadLikedSongs();
        logger.info("Exported " + Collection.getLikedSongs().size() + " liked songs to '"
                + methodName
                + "'");
    }

    private void exportPlaylists() throws InterruptedException {
        logger.info("Uploading " + Collection.getPlaylistCount() + " playlists to '" + methodName
                + "'");
        method.uploadPlaylists();
        logger.info("Exported " + Collection.getPlaylistCount() + " playlists to '"
                + methodName + "'");
    }

    private void exportPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided to exportPlaylist");
            return;
        }
        logger.info("Uploading playlist '" + playlist.getName() + "' (" + playlist.size() + ") to '"
                + methodName + "'");
        method.uploadPlaylist(playlist);
        logger.info("Exported playlist '" + playlist.getName() + "' to '" + methodName + "'");
    }

    private void exportAlbums() throws InterruptedException {
        logger.info("Uploading " + Collection.getAlbumCount() + " albums to '" + methodName
                + "'");
        method.uploadAlbums();
        logger.info("Exported " + Collection.getAlbumCount() + " albums to '"
                + methodName + "'");
    }

    private void exportAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided to exportPlaylist");
            return;
        }
        logger.info("Uploading album '" + album.getName() + "' (" + album.size() + ") to '"
                + methodName + "'");
        method.uploadAlbum(album);
        logger.info("Exported album '" + album.getName() + "' to '" + methodName + "'");
    }

    public void sync() {
        try {
            method.syncLikedSongs();
            method.uploadLikedSongs();
            method.syncPlaylists();
            for (Playlist playlist : Collection.getPlaylists()) {
                method.syncPlaylist(playlist);
                method.uploadPlaylist(playlist);
            }
            method.syncAlbums();
            for (Album album : Collection.getAlbums()) {
                method.syncAlbum(album);
                method.uploadAlbum(album);
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncing '" + this.methodName + "'", e);
        }
    }
}
