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
import ryzen.ownitall.util.exceptions.MenuClosed;
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

    /**
     * <p>
     * Constructor for MethodMenu.
     * </p>
     *
     * @throws java.lang.InterruptedException                         if any.
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @param filter a {@link java.lang.Class} object
     */
    public MethodMenu(Class<?> filter)
            throws InterruptedException, MissingSettingException {
        LinkedHashMap<String, Class<?>> options = new LinkedHashMap<>();
        for (Class<?> method : Method.getMethods(filter)) {
            options.put(method.getSimpleName(), method);
        }
        try {
            String choice = Menu.optionMenu(options.keySet(), "METHODS");
            Class<?> methodClass = Method.getMethod(choice);
            while (true) {
                try {
                    method = new Method(methodClass);
                    break;
                } catch (MissingSettingException e) {
                    logger.warn(
                            "Missing settings to set up method '" + methodClass.getSimpleName() + "': "
                                    + e.getMessage());
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
        } catch (MenuClosed e) {
            throw new InterruptedException("Cancelled method selection");
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
     */
    public void importMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Import Library", method::importCollection);
        options.put("Import liked songs", method::importLikedSongs);
        options.put("Import Album(s)", this::optionImportAlbumsMenu);
        options.put("Import Playlist(s)", this::optionImportPlaylistsMenu);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT " + method.getMethodName());
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    private void optionImportAlbumsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved albums", method::importAlbums);
        options.put("Individual album", this::optionImportAlbum);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT ALBUM" + method.getMethodName().toUpperCase());
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    private void optionImportAlbum() {
        String id = null;
        String name = null;
        String artistName = null;
        try {
            while (id == null || id.isEmpty()) {
                System.out.print("*Enter '" + method.getMethodName() + "' Album ID: ");
                id = Input.request().getString();
            }
            System.out.print("Enter '" + method.getMethodName() + "' Album name: ");
            name = Input.request().getString();
            System.out.print("Enter '" + method.getMethodName() + "' Album artist name: ");
            artistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album details");
            return;
        }
        this.method.importAlbum(id, name, artistName);
    }

    private void optionImportPlaylistsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved playlists", method::importPlaylists);
        options.put("Individual playlist", this::optionImportPlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT PlAYLIST" + method.getMethodName().toUpperCase());
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    private void optionImportPlaylist() {
        String id = null;
        String name = null;
        try {
            while (id == null || id.isEmpty()) {
                System.out.print("*Enter '" + method.getMethodName() + "' Playlist ID: ");
                id = Input.request().getString();
            }
            System.out.print("Enter '" + method.getMethodName() + "' Playlist Name: ");
            name = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist details");
            return;
        }
        method.importPlaylist(id, name);
    }

    /**
     * <p>
     * exportMenu.
     * </p>
     */
    public void exportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Export Library", method::exportCollection);
        options.put("Export Liked Songs", method::exportLikedSongs);
        options.put("Export Album(s)", this::optionExportAlbums);
        options.put("Export Playlist(s)", this::optionExportPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "EXPORT " + method.getMethodName().toUpperCase());
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    private void optionExportPlaylists() {
        LinkedHashMap<String, Playlist> options = new LinkedHashMap<>();
        options.put("All", null);
        options.put("Liked Songs", Collection.getLikedSongs());
        for (Playlist playlist : Collection.getPlaylists()) {
            options.put(playlist.toString(), playlist);
        }
        try {
            String choice = Menu.optionMenu(options.keySet(), "PLAYLIST EXPORT MENU");
            if (choice.equals("All")) {
                method.exportPlaylists();
            } else {
                method.exportPlaylist(options.get(choice));
            }
        } catch (MenuClosed e) {
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
            if (choice.equals("All")) {
                method.exportAlbums();
            } else {
                method.exportAlbum(options.get(choice));
            }
        } catch (MenuClosed e) {
        }
    }

    /**
     * <p>
     * syncMenu.
     * </p>
     */
    public void syncMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Sync Library", method::syncCollection);
        options.put("Sync liked songs", method::syncLikedSongs);
        options.put("Sync Album", method::syncAlbums);
        options.put("Sync Playlists", this::optionSyncPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "SYNC " + method.getMethodName());
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
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
            if (choice.equals("All")) {
                method.syncPlaylists();
            } else {
                method.syncPlaylist(options.get(choice));
            }
        } catch (MenuClosed e) {
        }
    }
}
