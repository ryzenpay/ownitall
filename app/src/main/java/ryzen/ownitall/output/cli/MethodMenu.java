package ryzen.ownitall.output.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;

public class MethodMenu {
    private static final Logger logger = LogManager.getLogger(MethodMenu.class);
    private Method method;

    public MethodMenu() throws InterruptedException {
        LibraryMenu.initializeLibrary();
        String choice = Menu.optionMenu(Method.methods.keySet(), "METHODS");
        if (choice.equals("Exit")) {
            throw new InterruptedException("Cancelled method selection");
        }
        Class<? extends Method> methodClass = Method.methods.get(choice);
        if (Method.isCredentialsEmpty(methodClass)) {
            this.setCredentials(methodClass);
        }
        try {
            method = Method.initMethod(methodClass);
        } catch (InterruptedException e) {
            logger.info("Interrupted while setting up method, could be due to invalid credentials", e);
            Method.clearCredentials(methodClass);
            this.setCredentials(methodClass);
        } finally {
            method = Method.initMethod(methodClass);
        }
    }

    private void setCredentials(Class<? extends Method> methodClass) throws InterruptedException {
        if (methodClass == null) {
            logger.debug("null methodClass provided in setCredentials");
            return;
        }
        if (!Method.isCredentialsEmpty(methodClass)) {
            return;
        }
        Settings settings = Settings.load();
        LinkedHashMap<String, String> classCredentials = Method.credentialGroups.get(methodClass);
        if (classCredentials != null) {
            for (String name : classCredentials.keySet()) {
                if (!settings.isEmpty(classCredentials.get(name))) {
                    // skip already set values
                    continue;
                }
                System.out.print("Enter '" + name + "': ");
                Object value = Input.request().getValue(settings.getType(classCredentials.get(name)));
                if (!settings.set(classCredentials.get(name), value)) {
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
                        "IMPORT " + Method.getMethodName(this.method));
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + Method.getMethodName(this.method) + " import menu choice");
        }
    }

    private void optionImportCollection() {
        logger.debug("Importing '" + Method.getMethodName(this.method) + "' library...");
        try (ProgressBar pb = new ProgressBar(Method.getMethodName(this.method) + " Import", 3)) {
            pb.step("Liked Songs");
            LikedSongs likedSongs = method.getLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
                logger.info("Imported " + likedSongs.size() + " liked songs from '" + Method.getMethodName(this.method)
                        + "'");
            }
            pb.step("Saved Albums");
            ArrayList<Album> albums = method.getAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
                logger.info("Imported " + albums.size() + " albums from '" + Method.getMethodName(this.method) + "'");
            }
            pb.step("Playlists");
            ArrayList<Playlist> playlists = method.getPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
                logger.info(
                        "Imported " + playlists.size() + " playlists from '" + Method.getMethodName(this.method) + "'");
            }
            logger.debug("done importing '" + Method.getMethodName(this.method) + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + Method.getMethodName(this.method) + "' library", e);
        }
    }

    private void optionImportLikedSongs() {
        try {
            logger.info("Getting liked songs from '" + Method.getMethodName(this.method) + "'...");
            LikedSongs likedSongs = method.getLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
                logger.info(
                        "Imported " + likedSongs.size() + " liked songs from '" + Method.getMethodName(this.method)
                                + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + Method.getMethodName(this.method) + "' liked songs", e);
        }
    }

    private void optionImportAlbumsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved albums", this::optionImportAlbums);
        options.put("Individual album", this::optionImportAlbum);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT ALBUM" + Method.getMethodName(this.method).toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while getting " + Method.getMethodName(this.method) + " import album menu choice");
        }
    }

    private void optionImportAlbums() {
        try {
            logger.info("Getting albums from '" + Method.getMethodName(this.method) + "'...");
            ArrayList<Album> albums = method.getAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
                logger.info("Imported " + albums.size() + " albums from '" + Method.getMethodName(this.method) + "'");
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
                System.out.print("*Enter '" + Method.getMethodName(this.method) + "' Album ID: ");
                albumId = Input.request().getString();
            }
            System.out.print("Enter '" + Method.getMethodName(this.method) + "' Album name: ");
            albumName = Input.request().getString();
            System.out.print("Enter '" + Method.getMethodName(this.method) + "' Album artist name: ");
            albumArtistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album details");
            return;
        }
        try {
            logger.info("Getting album '" + albumId + "' from '" + Method.getMethodName(this.method) + "'...");
            Album album = method.getAlbum(albumId, albumName, albumArtistName);
            if (album != null) {
                Collection.addAlbum(album);
                logger.info("Imported album '" + album.getName() + "' (" + album.size() + ") from '"
                        + Method.getMethodName(this.method) + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + Method.getMethodName(this.method) + "' album", e);
        }
    }

    private void optionImportPlaylistsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved playlists", this::optionImportPlaylists);
        options.put("Individual playlist", this::optionImportPlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT PlAYLIST" + Method.getMethodName(this.method).toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while getting " + Method.getMethodName(this.method) + " import playlist menu choice");
        }
    }

    private void optionImportPlaylists() {
        try {
            logger.info("Getting playlists from '" + Method.getMethodName(this.method) + "'...");
            ArrayList<Playlist> playlists = method.getPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
                logger.info(
                        "Imported " + playlists.size() + " playlists from '" + Method.getMethodName(this.method) + "'");
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
                System.out.print("*Enter '" + Method.getMethodName(this.method) + "' Playlist ID: ");
                playlistId = Input.request().getString();
            }
            System.out.print("Enter '" + Method.getMethodName(this.method) + "' Playlist Name: ");
            playlistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist details");
            return;
        }
        try {
            logger.info("Getting playlist '" + playlistId + "' from '" + Method.getMethodName(this.method) + "'...");
            Playlist playlist = method.getPlaylist(playlistId, playlistName);
            if (playlist != null) {
                Collection.addPlaylist(playlist);
                logger.info("Imported playlist '" + playlist.getName() + "' (" + playlist.size() + ") from '"
                        + Method.getMethodName(this.method) + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + Method.getMethodName(this.method) + "' playlist", e);
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
                        "EXPORT " + Method.getMethodName(this.method).toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + Method.getMethodName(this.method) + " export menu choice");
        }
    }

    private void optionExportCollection() {
        logger.debug("Uploading '" + Method.getMethodName(this.method) + "' (" + Collection.getTotalTrackCount()
                + ") library...");
        try (ProgressBar pb = new ProgressBar(Method.getMethodName(this.method) + " Upload", 3)) {
            pb.step("Liked Songs");
            method.uploadLikedSongs();
            logger.debug("Exported " + Collection.getLikedSongs().size() + " liked songs to '"
                    + Method.getMethodName(this.method) + "'");
            pb.step("Saved Albums");
            method.uploadAlbums();
            logger.debug("Exported " + Collection.getAlbumCount() + " albums to '"
                    + Method.getMethodName(this.method) + "'");
            pb.step("Playlists");
            method.uploadPlaylists();
            logger.debug(
                    "Exported " + Collection.getPlaylistCount() + " playlists to '" + Method.getMethodName(this.method)
                            + "'");
            logger.debug("done uploading '" + Method.getMethodName(this.method) + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + Method.getMethodName(this.method) + "' music", e);
        }
    }

    private void optionExportLikedSongs() {
        try {
            logger.info("Uploading " + Collection.getLikedSongs().size() + " liked songs to '"
                    + Method.getMethodName(this.method)
                    + "'");
            method.uploadLikedSongs();
            logger.info("Exported " + Collection.getLikedSongs().size() + " liked songs to '"
                    + Method.getMethodName(this.method) + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + Method.getMethodName(this.method) + "' liked songs", e);
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
                logger.info("Uploading " + Collection.getPlaylistCount() + " playlists to '"
                        + Method.getMethodName(this.method)
                        + "'");
                method.uploadPlaylists();
                logger.info("Exported " + Collection.getPlaylistCount() + " playlists to '"
                        + Method.getMethodName(this.method) + "'");
            } else {
                Playlist playlist = options.get(choice);
                logger.info("Uploading playlist '" + playlist.getName() + "' (" + playlist.size() + ") to '"
                        + Method.getMethodName(this.method) + "'");
                method.uploadPlaylist(playlist);
                logger.info("Exported playlist '" + playlist.getName() + "' to '" + Method.getMethodName(this.method)
                        + "'");
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
                        + Method.getMethodName(this.method) + "'");
                method.uploadAlbums();
                logger.info("Exported " + Collection.getAlbumCount() + " albums to '"
                        + Method.getMethodName(this.method) + "'");
            } else {
                Album album = options.get(choice);
                logger.info("Uploading album '" + album.getName() + "' (" + album.size() + ") to '"
                        + Method.getMethodName(this.method) + "'");
                method.uploadAlbum(album);
                logger.info("Exported album '" + album.getName() + "' to '" + Method.getMethodName(this.method) + "'");
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
                        "SYNC " + Method.getMethodName(this.method));
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + Method.getMethodName(this.method) + " import menu choice");
        }
    }

    private void optionSyncCollection() {
        logger.debug("Syncronizing '" + Method.getMethodName(this.method) + "' library...");
        try (ProgressBar pb = new ProgressBar(Method.getMethodName(this.method) + " Sync", 3)) {
            pb.step("Liked Songs");
            method.syncLikedSongs();
            logger.debug("Syncronized " + Collection.getLikedSongCount() + " liked songs from '"
                    + Method.getMethodName(this.method) + "'");
            pb.step("Saved Albums");
            method.syncAlbums();
            logger.debug("Syncronized " + Collection.getAlbumCount() + " albums from '"
                    + Method.getMethodName(this.method) + "'");
            pb.step("Playlists");
            method.syncPlaylists();
            logger.debug("Syncronized " + Collection.getPlaylistCount() + " playlists from '"
                    + Method.getMethodName(this.method) + "'");
            logger.debug("done syncronizing '" + Method.getMethodName(this.method) + "' library");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + Method.getMethodName(this.method) + "' library");
        }
    }

    private void optionSyncLikedSongs() {
        try {
            logger.info("Syncronizing " + Collection.getLikedSongCount() + " liked songs to '"
                    + Method.getMethodName(this.method)
                    + "'");
            method.syncLikedSongs();
            logger.info("Syncronized " + Collection.getLikedSongCount() + " liked songs to '"
                    + Method.getMethodName(this.method)
                    + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + Method.getMethodName(this.method) + "' liked songs");
        }
    }

    private void optionSyncAlbums() {
        try {
            logger.info(
                    "Syncronizing " + Collection.getAlbumCount() + " albums to '" + Method.getMethodName(this.method)
                            + "'");
            method.syncAlbums();
            logger.info("Syncronized " + Collection.getAlbumCount() + " albums to '" + Method.getMethodName(this.method)
                    + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + Method.getMethodName(this.method) + "' albums");
        }
    }

    private void optionSyncPlaylists() {
        try {
            logger.info("Syncronizing " + Collection.getPlaylistCount() + " playlists to '"
                    + Method.getMethodName(this.method)
                    + "'");
            method.syncPlaylists();
            logger.info("Syncronized " + Collection.getPlaylistCount() + " playlists to '"
                    + Method.getMethodName(this.method)
                    + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + Method.getMethodName(this.method) + "' playlists");
        }
    }
}
