package ryzen.ownitall.ui.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.ProgressBar;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>
 * MethodMenu class.
 * </p>
 *
 * @author ryzen
 */
public class MethodMenu {
    private static final Logger logger = new Logger(MethodMenu.class);

    private Method method;

    private String getMethodName() {
        if (method == null) {
            return "";
        }
        return method.getClass().getSimpleName();
    }

    /**
     * <p>
     * Constructor for MethodMenu.
     * </p>
     *
     * @param annotation a {@link java.lang.Class} object
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     */
    public MethodMenu(Class<?> filter)
            throws InterruptedException, MissingSettingException {
        LinkedHashMap<String, Class<?>> options = new LinkedHashMap<>();
        for (Class<?> method : Method.getMethods(filter)) {
            options.put(method.getSimpleName(), method);
        }
        String choice = Menu.optionMenu(options.keySet(), "METHODS");
        if (choice.equals("Exit")) {
            throw new InterruptedException("Cancelled method selection");
        }
        Class<?> methodClass = Method.getMethod(choice);
        while (true) {
            try {
                method = new Method(methodClass);
                break;
            } catch (MissingSettingException e) {
                logger.warn(
                        "Missing settings to set up method '" + methodClass.getSimpleName() + "': " + e.getMessage());
                setCredentials(methodClass);
            } catch (AuthenticationException e) {
                logger.warn("Authentication exception setting up method '" + methodClass.getSimpleName()
                        + "': " + e.getMessage());
                Method.clearCredentials(methodClass);
                setCredentials(methodClass);
            } catch (NoSuchMethodException e) {
                logger.error("method '" + methodClass.getSimpleName() + "' does not exist", e);
                break;
            }
        }
    }

    private static void setCredentials(Class<?> methodClass)
            throws MissingSettingException, InterruptedException {
        if (methodClass == null) {
            logger.debug("null methodClass provided in setCredentials");
            return;
        }
        Settings settings = Settings.load();
        LinkedHashSet<String> credentials = settings.getGroup(methodClass);
        if (credentials != null) {
            try {
                SettingsMenu.changeSettings(credentials);
            } catch (NoSuchFieldException e) {
                logger.error("Unable to find setting to change", e);
            }
        }
    }

    /**
     * <p>
     * importMenu.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void importMenu() throws InterruptedException {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", this::optionImportCollection);
        options.put("Import liked songs", this::optionImportLikedSongs);
        options.put("Import Album(s)", this::optionImportAlbumsMenu);
        options.put("Import Playlist(s)", this::optionImportPlaylistsMenu);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT " + getMethodName());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + getMethodName() + " import menu choice");
        }
    }

    private void optionImportCollection() {
        logger.debug("Importing '" + getMethodName() + "' library...");
        try (ProgressBar pb = new ProgressBar(getMethodName() + " Import", 3)) {
            pb.step("Liked Songs");
            LikedSongs likedSongs = method.getImport().getLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
                logger.info("Imported " + likedSongs.size() + " liked songs from '" + getMethodName()
                        + "'");
            }
            pb.step("Saved Albums");
            ArrayList<Album> albums = method.getImport().getAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
                logger.info("Imported " + albums.size() + " albums from '" + getMethodName() + "'");
            }
            pb.step("Playlists");
            ArrayList<Playlist> playlists = method.getImport().getPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
                logger.info(
                        "Imported " + playlists.size() + " playlists from '" + getMethodName() + "'");
            }
            logger.debug("done importing '" + getMethodName() + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + getMethodName() + "' library");
        }
    }

    private void optionImportLikedSongs() {
        try {
            logger.info("Getting liked songs from '" + getMethodName() + "'...");
            LikedSongs likedSongs = method.getImport().getLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
                logger.info(
                        "Imported " + likedSongs.size() + " liked songs from '" + getMethodName()
                                + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + getMethodName() + "' liked songs");
        }
    }

    private void optionImportAlbumsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved albums", this::optionImportAlbums);
        options.put("Individual album", this::optionImportAlbum);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT ALBUM" + getMethodName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while getting " + getMethodName() + " import album menu choice");
        }
    }

    private void optionImportAlbums() {
        try {
            logger.info("Getting albums from '" + getMethodName() + "'...");
            ArrayList<Album> albums = method.getImport().getAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
                logger.info("Imported " + albums.size() + " albums from '" + getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing albums");
        }
    }

    private void optionImportAlbum() {
        String albumId = null;
        String albumName = null;
        String albumArtistName = null;
        try {
            while (albumId == null || albumId.isEmpty()) {
                System.out.print("*Enter '" + getMethodName() + "' Album ID: ");
                albumId = Input.request().getString();
            }
            System.out.print("Enter '" + getMethodName() + "' Album name: ");
            albumName = Input.request().getString();
            System.out.print("Enter '" + getMethodName() + "' Album artist name: ");
            albumArtistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album details");
            return;
        }
        try {
            logger.info("Getting album '" + albumId + "' from '" + getMethodName() + "'...");
            Album album = method.getImport().getAlbum(albumId, albumName, albumArtistName);
            if (album != null) {
                Collection.addAlbum(album);
                logger.info("Imported album '" + album.getName() + "' (" + album.size() + ") from '"
                        + getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + getMethodName() + "' album");
        }
    }

    private void optionImportPlaylistsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved playlists", this::optionImportPlaylists);
        options.put("Individual playlist", this::optionImportPlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT PlAYLIST" + getMethodName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while getting " + getMethodName() + " import playlist menu choice");
        }
    }

    private void optionImportPlaylists() {
        try {
            logger.info("Getting playlists from '" + getMethodName() + "'...");
            ArrayList<Playlist> playlists = method.getImport().getPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
                logger.info(
                        "Imported " + playlists.size() + " playlists from '" + getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing playlists");
        }
    }

    private void optionImportPlaylist() {
        String playlistId = null;
        String playlistName = null;
        try {
            while (playlistId == null || playlistId.isEmpty()) {
                System.out.print("*Enter '" + getMethodName() + "' Playlist ID: ");
                playlistId = Input.request().getString();
            }
            System.out.print("Enter '" + getMethodName() + "' Playlist Name: ");
            playlistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist details");
            return;
        }
        try {
            logger.info("Getting playlist '" + playlistId + "' from '" + getMethodName() + "'...");
            Playlist playlist = method.getImport().getPlaylist(playlistId, playlistName);
            if (playlist != null) {
                Collection.addPlaylist(playlist);
                logger.info("Imported playlist '" + playlist.getName() + "' (" + playlist.size() + ") from '"
                        + getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting '" + getMethodName() + "' playlist");
        }
    }

    /**
     * <p>
     * exportMenu.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void exportMenu() throws InterruptedException {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Export Library", this::optionExportCollection);
        options.put("Export Liked Songs", this::optionExportLikedSongs);
        options.put("Export Album(s)", this::optionExportAlbums);
        options.put("Export Playlist(s)", this::optionExportPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "EXPORT " + getMethodName().toUpperCase());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + getMethodName() + " export menu choice");
        }
    }

    private void optionExportCollection() {
        logger.debug("Exporting '" + getMethodName() + "' (" + Collection.getTotalTrackCount()
                + ") library...");
        try (ProgressBar pb = new ProgressBar(getMethodName() + " Upload", 3)) {
            pb.step("Liked Songs");
            this.method.getExport().uploadLikedSongs();
            logger.debug("Exported " + Collection.getLikedSongs().size() + " liked songs to '"
                    + getMethodName() + "'");
            pb.step("Saved Albums");
            this.method.getExport().uploadAlbums();
            logger.debug("Exported " + Collection.getAlbumCount() + " albums to '"
                    + getMethodName() + "'");
            pb.step("Playlists");
            this.method.getExport().uploadPlaylists();
            logger.debug(
                    "Exported " + Collection.getPlaylistCount() + " playlists to '" + getMethodName()
                            + "'");
            logger.debug("done uploading '" + getMethodName() + "' music");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + getMethodName() + "' music");
        }
    }

    private void optionExportLikedSongs() {
        try {
            logger.info("Exporting " + Collection.getLikedSongs().size() + " liked songs to '"
                    + getMethodName()
                    + "'...");
            this.method.getExport().uploadLikedSongs();
            logger.info("Exported " + Collection.getLikedSongs().size() + " liked songs to '"
                    + getMethodName() + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while uploading '" + getMethodName() + "' liked songs");
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
                logger.info("Exporting " + Collection.getPlaylistCount() + " playlists to '"
                        + getMethodName()
                        + "'");
                this.method.getExport().uploadPlaylists();
                logger.info("Exported " + Collection.getPlaylistCount() + " playlists to '"
                        + getMethodName() + "'");
            } else {
                Playlist playlist = options.get(choice);
                logger.info("Uploading playlist '" + playlist.getName() + "' (" + playlist.size() + ") to '"
                        + getMethodName() + "'...");
                this.method.getExport().uploadPlaylist(playlist);
                logger.info("Exported playlist '" + playlist.getName() + "' to '" + getMethodName()
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
                logger.info("Exporting " + Collection.getAlbumCount() + " albums to '"
                        + getMethodName() + "'...");
                this.method.getExport().uploadAlbums();
                logger.info("Exported " + Collection.getAlbumCount() + " albums to '"
                        + getMethodName() + "'");
            } else {
                Album album = options.get(choice);
                logger.info("Uploading album '" + album.getName() + "' (" + album.size() + ") to '"
                        + getMethodName() + "'...");
                this.method.getExport().uploadAlbum(album);
                logger.info("Exported album '" + album.getName() + "' to '" + getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting export album choice");
        }
    }

    /**
     * <p>
     * syncMenu.
     * </p>
     *
     * @throws java.lang.InterruptedException if any.
     */
    public void syncMenu() throws InterruptedException {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Sync Library", this::optionSyncCollection);
        options.put("Sync liked songs", this::optionSyncLikedSongs);
        options.put("Sync Album", this::optionSyncAlbums);
        options.put("Sync Playlists", this::optionSyncPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "SYNC " + getMethodName());
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting " + getMethodName() + " import menu choice");
        }
    }

    private void optionSyncCollection() {
        logger.debug("Syncronizing '" + getMethodName() + "' library...");
        try (ProgressBar pb = new ProgressBar(getMethodName() + " Sync", 3)) {
            pb.step("Liked Songs");
            this.method.getSync().syncLikedSongs();
            logger.debug("Syncronized " + Collection.getLikedSongCount() + " liked songs from '"
                    + getMethodName() + "'");
            pb.step("Saved Albums");
            this.method.getSync().syncAlbums();
            logger.debug("Syncronized " + Collection.getAlbumCount() + " albums from '"
                    + getMethodName() + "'");
            pb.step("Playlists");
            this.method.getSync().syncPlaylists();
            logger.debug("Syncronized " + Collection.getPlaylistCount() + " playlists from '"
                    + getMethodName() + "'");
            logger.debug("done syncronizing '" + getMethodName() + "' library");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + getMethodName() + "' library");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
        }
    }

    private void optionSyncLikedSongs() {
        try {
            logger.info("Syncronizing " + Collection.getLikedSongCount() + " liked songs to '"
                    + getMethodName()
                    + "'...");
            this.method.getSync().syncLikedSongs();
            logger.info("Syncronized " + Collection.getLikedSongCount() + " liked songs to '"
                    + getMethodName()
                    + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + getMethodName() + "' liked songs");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
        }
    }

    private void optionSyncAlbums() {
        try {
            logger.info(
                    "Syncronizing " + Collection.getAlbumCount() + " albums to '" + getMethodName()
                            + "'...");
            this.method.getSync().syncAlbums();
            logger.info("Syncronized " + Collection.getAlbumCount() + " albums to '" + getMethodName()
                    + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + getMethodName() + "' albums");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
        }
    }

    private void optionSyncPlaylists() {
        try {
            logger.info("Syncronizing " + Collection.getPlaylistCount() + " playlists to '"
                    + getMethodName()
                    + "'...");
            this.method.getSync().syncPlaylists();
            logger.info("Syncronized " + Collection.getPlaylistCount() + " playlists to '"
                    + getMethodName()
                    + "'");
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + getMethodName() + "' playlists");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
        }
    }
}
