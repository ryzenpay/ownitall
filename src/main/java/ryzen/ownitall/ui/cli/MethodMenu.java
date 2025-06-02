package ryzen.ownitall.ui.cli;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.Menu;
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
        options.put("Import Library", method::importCollection);
        options.put("Import liked songs", method::importLikedSongs);
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

    private void optionImportAlbumsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved albums", method::importAlbums);
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
        this.method.importAlbum(albumId, albumName, albumArtistName);
    }

    private void optionImportPlaylistsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved playlists", method::importPlaylists);
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
        method.importPlaylist(playlistId, playlistName);
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
        options.put("Export Library", method::exportCollection);
        options.put("Export Liked Songs", method::exportLikedSongs);
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
                method.exportPlaylists();
            } else {
                method.exportPlaylist(options.get(choice));
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
                method.exportAlbums();
            } else {
                method.exportAlbum(options.get(choice));
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
        options.put("Sync Library", method::syncCollection);
        options.put("Sync liked songs", method::syncLikedSongs);
        options.put("Sync Album", method::syncAlbums);
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

    private void optionSyncPlaylists() {
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        options.put("All", null);
        for (Playlist playlist : Collection.getPlaylists()) {
            options.put(playlist.toString(), playlist);
        }
        try {
            String choice = Menu.optionMenu(options.keySet(), "PLAYLIST SYNC MENU");
            if (choice.equals("Exit")) {
                return;
            }
            if (choice.equals("All")) {
                method.syncPlaylists();
            } else {
                method.syncPlaylist(options.get(choice));
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting sync playlist choice");
        }
    }
}
