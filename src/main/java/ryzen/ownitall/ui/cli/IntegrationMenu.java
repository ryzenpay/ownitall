package ryzen.ownitall.ui.cli;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.integrations.Integration;
import ryzen.ownitall.integrations.Integration.InvalidIntegrationException;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.Menu.MenuClosed;
import ryzen.ownitall.util.WebTools.AuthenticationException;

/**
 * <p>
 * IntegrationMeniu class.
 * </p>
 *
 * @author ryzen
 */
public class IntegrationMenu {
    private static final Logger logger = new Logger(IntegrationMenu.class);

    private Integration integration;

    /**
     * <p>
     * Constructor for IntegrationMenu.
     * </p>
     *
     * @throws java.lang.InterruptedException                  if any.
     * @throws ryzen.ownitall.Settings.MissingSettingException if any.
     * @param filter a {@link java.lang.Class} object
     */
    public IntegrationMenu(Class<?> filter)
            throws InterruptedException, MissingSettingException {
        LinkedHashMap<String, Class<?>> options = new LinkedHashMap<>();
        for (Class<?> integration : Integration.getIntegrations(filter)) {
            options.put(integration.getSimpleName(), integration);
        }
        try {
            String choice = Menu.optionMenu(options.keySet(), "INTEGRATIONS");
            Class<?> clazz = Integration.getIntegration(choice);
            while (true) {
                try {
                    this.integration = new Integration(clazz);
                    break;
                } catch (MissingSettingException e) {
                    logger.warn(
                            "Missing settings to set up integration '" + clazz.getSimpleName() + "': "
                                    + e.getMessage());
                    setCredentials(clazz);
                } catch (AuthenticationException e) {
                    logger.warn("Authentication exception setting up integration '" + clazz.getSimpleName()
                            + "': " + e.getMessage());
                    Integration.clearCredentials(clazz);
                    setCredentials(clazz);
                } catch (InvalidIntegrationException e) {
                    logger.error("integration '" + clazz.getSimpleName() + "' does not exist", e);
                    break;
                }
            }
        } catch (MenuClosed e) {
            throw new InterruptedException("Cancelled integration selection");
        }
    }

    private static void setCredentials(Class<?> clazz)
            throws MissingSettingException, InterruptedException {
        if (clazz == null) {
            logger.debug("null clazz provided in setCredentials");
            return;
        }
        Settings settings = Settings.load();
        LinkedHashSet<String> credentials = settings.getGroup(clazz);
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
        options.put("Import Library", this.integration::importCollection);
        options.put("Import liked songs", this.integration::importLikedSongs);
        options.put("Import Album(s)", this::optionImportAlbumsMenu);
        options.put("Import Playlist(s)", this::optionImportPlaylistsMenu);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT " + this.integration.getIntegrationName());
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
        }
    }

    private void optionImportAlbumsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved albums", this.integration::importAlbums);
        options.put("Individual album", this::optionImportAlbum);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT ALBUM" + this.integration.getIntegrationName().toUpperCase());
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
                System.out.print("*Enter '" + this.integration.getIntegrationName() + "' Album ID: ");
                id = Input.request().getString();
            }
            System.out.print("Enter '" + this.integration.getIntegrationName() + "' Album name: ");
            name = Input.request().getString();
            System.out.print("Enter '" + this.integration.getIntegrationName() + "' Album artist name: ");
            artistName = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting album details");
            return;
        }
        this.integration.importAlbum(id, name, artistName);
    }

    private void optionImportPlaylistsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("All usersaved playlists", this.integration::importPlaylists);
        options.put("Individual playlist", this::optionImportPlaylist);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "IMPORT PlAYLIST" + this.integration.getIntegrationName().toUpperCase());
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
                System.out.print("*Enter '" + this.integration.getIntegrationName() + "' Playlist ID: ");
                id = Input.request().getString();
            }
            System.out.print("Enter '" + this.integration.getIntegrationName() + "' Playlist Name: ");
            name = Input.request().getString();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist details");
            return;
        }
        this.integration.importPlaylist(id, name);
    }

    /**
     * <p>
     * exportMenu.
     * </p>
     */
    public void exportMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Export Library", this.integration::exportCollection);
        options.put("Export Liked Songs", this.integration::exportLikedSongs);
        options.put("Export Album(s)", this::optionExportAlbums);
        options.put("Export Playlist(s)", this::optionExportPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "EXPORT " + this.integration.getIntegrationName().toUpperCase());
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
                this.integration.exportPlaylists();
            } else {
                this.integration.exportPlaylist(options.get(choice));
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
                this.integration.exportAlbums();
            } else {
                this.integration.exportAlbum(options.get(choice));
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
        options.put("Sync Library", this.integration::syncCollection);
        options.put("Sync liked songs", this.integration::syncLikedSongs);
        options.put("Sync Album", this.integration::syncAlbums);
        options.put("Sync Playlists", this::optionSyncPlaylists);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(),
                        "SYNC " + this.integration.getIntegrationName());
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
                this.integration.syncPlaylists();
            } else {
                this.integration.syncPlaylist(options.get(choice));
            }
        } catch (MenuClosed e) {
        }
    }
}
