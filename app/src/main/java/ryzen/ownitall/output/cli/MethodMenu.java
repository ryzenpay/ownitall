package ryzen.ownitall.output.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.method.MethodClass;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.ProgressBar;

public class MethodMenu {
    private static final Logger logger = LogManager.getLogger();

    public MethodMenu() throws InterruptedException {
        LibraryMenu.initializeLibrary();
        String choice = Menu.optionMenu(Method.methods.keySet(), "METHODS");
        if (choice.equals("Exit")) {
            throw new InterruptedException("Cancelled method selection");
        }
        Class<? extends MethodClass> methodClass = Method.methods.get(choice);
        this.initializeMethod(methodClass);
    }

    private void initializeMethod(Class<? extends MethodClass> methodClass) throws InterruptedException {
        if (Method.isCredentialsEmpty(methodClass)) {
            this.setCredentials(methodClass);
        }
        Method.setMethod(methodClass);
    }

    private void setCredentials(Class<? extends MethodClass> methodClass) throws InterruptedException {
        if (methodClass == null) {
            logger.debug("null methodClass provided in setCredentials");
            return;
        }
        if (!Method.isCredentialsEmpty(methodClass)) {
            return;
        }
        Credentials credentials = Credentials.load();
        LinkedHashMap<String, String> classCredentials = Method.credentialGroups.get(methodClass);
        if (classCredentials != null) {
            for (String name : classCredentials.keySet()) {
                System.out.print("Enter '" + name + "': ");
                Object value = Input.request().getValue(credentials.getType(classCredentials.get(name)));
                if (!credentials.set(classCredentials.get(name), value)) {
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
                        "IMPORT " + Method.getMethodName());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + Method.getMethodName() + " import menu choice");
        }
    }

    private void optionImportCollection() {
        logger.debug("Importing '" + Method.getMethodName() + "' music...");
        try (ProgressBar pb = new ProgressBar(Method.getMethodName() + " Import", 3)) {
            pb.step("Liked Songs");
            this.importLikedSongs();
            pb.step("Saved Albums");
            this.importAlbums();
            pb.step("Playlists");
            this.importPlaylists();
            logger.debug("done importing '" + Method.getMethodName() + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + Method.getMethodName() + "' music", e);
        }
    }

    private void optionImportLikedSongs() {
        try {
            logger.info("Getting liked songs from '" + Method.getMethodName() + "'...");
            int size = this.importLikedSongs();
            logger.info(
                    "Imported " + size + " liked songs from '" + Method.getMethodName() + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + Method.getMethodName() + "' liked songs", e);
        }
    }

    private void optionImportAlbumsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved albums", this::optionImportAlbums);
        options.put("Individual album", this::optionImportAlbum);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT ALBUM" + Method.getMethodName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while getting " + Method.getMethodName() + " import album menu choice");
        }
    }

    private void optionImportAlbums() {
        try {
            logger.info("Getting albums from '" + Method.getMethodName() + "'...");
            int size = this.importAlbums();
            logger.info("Imported " + size + " albums from '" + Method.getMethodName() + "'");
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
                System.out.print("*Enter '" + Method.getMethodName() + "' Album ID: ");
                albumId = Input.request().getString();
            }
            System.out.print("Enter '" + Method.getMethodName() + "' Album name: ");
            albumName = Input.request().getString();
            System.out.print("Enter '" + Method.getMethodName() + "' Album artist name: ");
            albumArtistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album details");
            return;
        }
        try {
            this.importAlbum(albumId, albumName, albumArtistName);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + Method.getMethodName() + "' album", e);
        }
    }

    private void optionImportPlaylistsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved playlists", this::optionImportPlaylists);
        options.put("Individual playlist", this::optionImportPlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT PlAYLIST" + Method.getMethodName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while getting " + Method.getMethodName() + " import playlist menu choice");
        }
    }

    private void optionImportPlaylists() {
        try {
            logger.info("Getting playlists from '" + Method.getMethodName() + "'...");
            int size = this.importPlaylists();
            logger.info("Imported " + size + " playlists from '" + Method.getMethodName() + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing playlists", e);
        }
    }

    private void optionImportPlaylist() {
        String playlistId = null;
        String playlistName = null;
        try {
            while (playlistId == null || playlistId.isEmpty()) {
                System.out.print("*Enter '" + Method.getMethodName() + "' Playlist ID: ");
                playlistId = Input.request().getString();
            }
            System.out.print("Enter '" + Method.getMethodName() + "' Playlist Name: ");
            playlistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist details");
            return;
        }
        try {
            this.importPlaylist(playlistId, playlistName);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + Method.getMethodName() + "' playlist", e);
        }
    }

    private int importLikedSongs() throws InterruptedException {
        LikedSongs likedSongs = Method.load().getLikedSongs();
        if (likedSongs != null) {
            Collection.addLikedSongs(likedSongs);
            return likedSongs.size();
        }
        return 0;
    }

    private int importPlaylists() throws InterruptedException {
        logger.info("Getting playlists from '" + Method.getMethodName() + "'...");
        ArrayList<Playlist> playlists = Method.load().getPlaylists();
        if (playlists != null) {
            Collection.addPlaylists(playlists);
            return playlists.size();
        }
        return 0;
    }

    private void importPlaylist(String playlistId, String playlistName) throws InterruptedException {
        if (playlistId == null) {
            logger.debug("null playlist id provided in importPlaylist");
            return;
        }
        logger.info("Getting playlist '" + playlistId + "' from '" + Method.getMethodName() + "'...");
        Playlist playlist = Method.load().getPlaylist(playlistId, playlistName);
        if (playlist != null) {
            Collection.addPlaylist(playlist);
            logger.info("Imported playlist '" + playlist.getName() + "' (" + playlist.size() + ") from '"
                    + Method.getMethodName() + "'");
        }
    }

    private int importAlbums() throws InterruptedException {
        logger.info("Getting albums from '" + Method.getMethodName() + "'...");
        ArrayList<Album> albums = Method.load().getAlbums();
        if (albums != null) {
            Collection.addAlbums(albums);
            return albums.size();
        }
        return 0;
    }

    private void importAlbum(String albumId, String albumName, String albumArtistName) throws InterruptedException {
        if (albumId == null) {
            logger.debug("null album id provided in importAlbum");
            return;
        }
        logger.info("Getting album '" + albumId + "' from '" + Method.getMethodName() + "'...");
        Album album = Method.load().getAlbum(albumId, albumName, albumArtistName);
        if (album != null) {
            Collection.addAlbum(album);
            logger.info("Imported album '" + album.getName() + "' (" + album.size() + ") from '"
                    + Method.getMethodName() + "'");
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
                        "EXPORT " + Method.getMethodName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + Method.getMethodName() + " export menu choice");
        }
    }

    private void optionExportCollection() {
        logger.debug("Uploading '" + Method.getMethodName() + "' (" + Collection.getTotalTrackCount()
                + ") music...");
        try (ProgressBar pb = new ProgressBar(Method.getMethodName() + " Upload", 3)) {
            pb.step("Liked Songs");
            this.exportLikedSongs();
            pb.step("Saved Albums");
            this.exportAlbums();
            pb.step("Playlists");
            this.exportPlaylists();
            logger.debug("done uploading '" + Method.getMethodName() + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + Method.getMethodName() + "' music", e);
        }
    }

    private void optionExportLikedSongs() {
        try {
            Method.load().uploadLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + Method.getMethodName() + "' liked songs", e);
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
            logger.debug("Interrupted while uploading '" + Method.getMethodName() + "' playlists", e);
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
            logger.debug("Interrupted while uploading '" + Method.getMethodName() + "' playlists", e);
        }
    }

    private void exportLikedSongs() throws InterruptedException {
        logger.info("Uploading " + Collection.getLikedSongs().size() + " liked songs to '"
                + Method.getMethodName()
                + "'");
        Method.load().uploadLikedSongs();
        logger.info("Exported " + Collection.getLikedSongs().size() + " liked songs to '"
                + Method.getMethodName()
                + "'");
    }

    private void exportPlaylists() throws InterruptedException {
        logger.info("Uploading " + Collection.getPlaylistCount() + " playlists to '" + Method.getMethodName()
                + "'");
        Method.load().uploadPlaylists();
        logger.info("Exported " + Collection.getPlaylistCount() + " playlists to '"
                + Method.getMethodName() + "'");
    }

    private void exportPlaylist(Playlist playlist) throws InterruptedException {
        if (playlist == null) {
            logger.debug("null playlist provided to exportPlaylist");
            return;
        }
        logger.info("Uploading playlist '" + playlist.getName() + "' (" + playlist.size() + ") to '"
                + Method.getMethodName() + "'");
        Method.load().uploadPlaylist(playlist);
        logger.info("Exported playlist '" + playlist.getName() + "' to '" + Method.getMethodName() + "'");
    }

    private void exportAlbums() throws InterruptedException {
        logger.info("Uploading " + Collection.getAlbumCount() + " albums to '" + Method.getMethodName()
                + "'");
        Method.load().uploadAlbums();
        logger.info("Exported " + Collection.getAlbumCount() + " albums to '"
                + Method.getMethodName() + "'");
    }

    private void exportAlbum(Album album) throws InterruptedException {
        if (album == null) {
            logger.debug("null album provided to exportPlaylist");
            return;
        }
        logger.info("Uploading album '" + album.getName() + "' (" + album.size() + ") to '"
                + Method.getMethodName() + "'");
        Method.load().uploadAlbum(album);
        logger.info("Exported album '" + album.getName() + "' to '" + Method.getMethodName() + "'");
    }

    public void sync() {
        try {
            MethodClass method = Method.load();
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
            logger.debug("Interrupted while syncing '" + Method.getMethodName() + "'", e);
        }
    }
}
