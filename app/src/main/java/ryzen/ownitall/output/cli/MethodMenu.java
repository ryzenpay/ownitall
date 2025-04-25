package ryzen.ownitall.output.cli;

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

public class MethodMenu {
    private static final Logger logger = LogManager.getLogger();
    private Method method;

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
        if (method != null) {
            if (method.getMethodName().equals(methodClass.getSimpleName())) {
                return;
            }
        }
        if (Method.isCredentialsEmpty(methodClass)) {
            this.setCredentials(methodClass);
        }
        method = new Method(methodClass);
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
                        "IMPORT " + method.getMethodName());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + method.getMethodName() + " import menu choice");
        }
    }

    private void optionImportCollection() {
        logger.debug("Importing '" + method.getMethodName() + "' library...");
        try (ProgressBar pb = new ProgressBar(method.getMethodName() + " Import", 3)) {
            pb.step("Liked Songs");
            LikedSongs likedSongs = method.importLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
                logger.info("Imported " + likedSongs.size() + " liked songs from '" + method.getMethodName() + "'");
            }
            pb.step("Saved Albums");
            ArrayList<Album> albums = method.importAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
                logger.info("Imported " + albums.size() + " albums from '" + method.getMethodName() + "'");
            }
            pb.step("Playlists");
            ArrayList<Playlist> playlists = method.importPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
                logger.info("Imported " + playlists.size() + " playlists from '" + method.getMethodName() + "'");
            }
            logger.debug("done importing '" + method.getMethodName() + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + method.getMethodName() + "' library", e);
        }
    }

    private void optionImportLikedSongs() {
        try {
            logger.info("Getting liked songs from '" + method.getMethodName() + "'...");
            LikedSongs likedSongs = method.importLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
                logger.info(
                        "Imported " + likedSongs.size() + " liked songs from '" + method.getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + method.getMethodName() + "' liked songs", e);
        }
    }

    private void optionImportAlbumsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved albums", this::optionImportAlbums);
        options.put("Individual album", this::optionImportAlbum);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT ALBUM" + method.getMethodName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while getting " + method.getMethodName() + " import album menu choice");
        }
    }

    private void optionImportAlbums() {
        try {
            logger.info("Getting albums from '" + method.getMethodName() + "'...");
            ArrayList<Album> albums = method.importAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
                logger.info("Imported " + albums.size() + " albums from '" + method.getMethodName() + "'");
            }
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
                System.out.print("*Enter '" + method.getMethodName() + "' Album ID: ");
                albumId = Input.request().getString();
            }
            System.out.print("Enter '" + method.getMethodName() + "' Album name: ");
            albumName = Input.request().getString();
            System.out.print("Enter '" + method.getMethodName() + "' Album artist name: ");
            albumArtistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album details");
            return;
        }
        try {
            logger.info("Getting album '" + albumId + "' from '" + method.getMethodName() + "'...");
            Album album = method.importAlbum(albumId, albumName, albumArtistName);
            if (album != null) {
                Collection.addAlbum(album);
                logger.info("Imported album '" + album.getName() + "' (" + album.size() + ") from '"
                        + method.getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + method.getMethodName() + "' album", e);
        }
    }

    private void optionImportPlaylistsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved playlists", this::optionImportPlaylists);
        options.put("Individual playlist", this::optionImportPlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT PlAYLIST" + method.getMethodName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while getting " + method.getMethodName() + " import playlist menu choice");
        }
    }

    private void optionImportPlaylists() {
        try {
            logger.info("Getting playlists from '" + method.getMethodName() + "'...");
            ArrayList<Playlist> playlists = method.importPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
                logger.info("Imported " + playlists.size() + " playlists from '" + method.getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing playlists", e);
        }
    }

    private void optionImportPlaylist() {
        String playlistId = null;
        String playlistName = null;
        try {
            while (playlistId == null || playlistId.isEmpty()) {
                System.out.print("*Enter '" + method.getMethodName() + "' Playlist ID: ");
                playlistId = Input.request().getString();
            }
            System.out.print("Enter '" + method.getMethodName() + "' Playlist Name: ");
            playlistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist details");
            return;
        }
        try {
            logger.info("Getting playlist '" + playlistId + "' from '" + method.getMethodName() + "'...");
            Playlist playlist = method.importPlaylist(playlistId, playlistName);
            if (playlist != null) {
                Collection.addPlaylist(playlist);
                logger.info("Imported playlist '" + playlist.getName() + "' (" + playlist.size() + ") from '"
                        + method.getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + method.getMethodName() + "' playlist", e);
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
                        "EXPORT " + method.getMethodName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + method.getMethodName() + " export menu choice");
        }
    }

    private void optionExportCollection() {
        logger.debug("Uploading '" + method.getMethodName() + "' (" + Collection.getTotalTrackCount()
                + ") library...");
        try (ProgressBar pb = new ProgressBar(method.getMethodName() + " Upload", 3)) {
            pb.step("Liked Songs");
            method.exportLikedSongs();
            logger.debug("Exported " + Collection.getLikedSongs().size() + " liked songs to '"
                    + method.getMethodName() + "'");
            pb.step("Saved Albums");
            method.exportAlbums();
            logger.debug("Exported " + Collection.getAlbumCount() + " albums to '"
                    + method.getMethodName() + "'");
            pb.step("Playlists");
            method.exportPlaylists();
            logger.debug(
                    "Exported " + Collection.getPlaylistCount() + " playlists to '" + method.getMethodName() + "'");
            logger.debug("done uploading '" + method.getMethodName() + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + method.getMethodName() + "' music", e);
        }
    }

    private void optionExportLikedSongs() {
        try {
            logger.info("Uploading " + Collection.getLikedSongs().size() + " liked songs to '" + method.getMethodName()
                    + "'");
            method.exportLikedSongs();
            logger.info("Exported " + Collection.getLikedSongs().size() + " liked songs to '"
                    + method.getMethodName() + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + method.getMethodName() + "' liked songs", e);
        }
    }

    private void optionExportPlaylists() {
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
                logger.info("Uploading " + Collection.getPlaylistCount() + " playlists to '" + method.getMethodName()
                        + "'");
                method.exportPlaylists();
                logger.info("Exported " + Collection.getPlaylistCount() + " playlists to '"
                        + method.getMethodName() + "'");
            } else {
                Playlist playlist = options.get(choice);
                logger.info("Uploading playlist '" + playlist.getName() + "' (" + playlist.size() + ") to '"
                        + method.getMethodName() + "'");
                method.exportPlaylist(playlist);
                logger.info("Exported playlist '" + playlist.getName() + "' to '" + method.getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting export playlist choice");
        }
    }

    private void optionExportAlbums() {
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
                logger.info("Exported " + Collection.getAlbumCount() + " albums to '"
                        + method.getMethodName() + "'");
                method.exportAlbums();
                logger.info("Exported " + Collection.getAlbumCount() + " albums to '"
                        + method.getMethodName() + "'");
            } else {
                Album album = options.get(choice);
                logger.info("Uploading album '" + album.getName() + "' (" + album.size() + ") to '"
                        + method.getMethodName() + "'");
                method.exportAlbum(album);
                logger.info("Exported album '" + album.getName() + "' to '" + method.getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting export album choice");
        }
    }

    public void syncMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Sync Library", this::optionSyncCollection);
        options.put("Sync liked songs", this::optionSyncLikedSongs);
        options.put("Sync Album", this::optionSyncAlbums);
        options.put("Sync Playlists", this::optionSyncPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "SYNC " + method.getMethodName());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + method.getMethodName() + " import menu choice");
        }
    }

    private void optionSyncCollection() {
        logger.debug("Syncronizing '" + method.getMethodName() + "' library...");
        try (ProgressBar pb = new ProgressBar(method.getMethodName() + " Sync", 3)) {
            pb.step("Liked Songs");
            method.syncLikedSongs();
            logger.debug("Syncronized " + Collection.getLikedSongCount() + " liked songs from '"
                    + method.getMethodName() + "'");
            pb.step("Saved Albums");
            method.syncAlbums();
            logger.debug("Syncronized " + Collection.getAlbumCount() + " albums from '"
                    + method.getMethodName() + "'");
            pb.step("Playlists");
            method.syncPlaylists();
            logger.debug("Syncronized " + Collection.getPlaylistCount() + " playlists from '"
                    + method.getMethodName() + "'");
            logger.debug("done syncronizing '" + method.getMethodName() + "' library");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getMethodName() + "' library");
        }
    }

    private void optionSyncLikedSongs() {
        try {
            logger.info("Syncronizing " + Collection.getLikedSongCount() + " liked songs to '" + method.getMethodName()
                    + "'");
            method.syncLikedSongs();
            logger.info("Syncronized " + Collection.getLikedSongCount() + " liked songs to '" + method.getMethodName()
                    + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getMethodName() + "' liked songs");
        }
    }

    private void optionSyncAlbums() {
        try {
            logger.info("Syncronizing " + Collection.getAlbumCount() + " albums to '" + method.getMethodName()
                    + "'");
            method.syncAlbums();
            logger.info("Syncronized " + Collection.getAlbumCount() + " albums to '" + method.getMethodName()
                    + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getMethodName() + "' albums");
        }
    }

    private void optionSyncPlaylists() {
        try {
            logger.info("Syncronizing " + Collection.getPlaylistCount() + " playlists to '" + method.getMethodName()
                    + "'");
            method.syncPlaylists();
            logger.info("Syncronized " + Collection.getPlaylistCount() + " playlists to '" + method.getMethodName()
                    + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getMethodName() + "' playlists");
        }
    }
}
