package ryzen.ownitall.ui.web;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Stack;

import org.apache.logging.log4j.Level;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.Settings.MissingSettingException;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.integrations.Integration;
import ryzen.ownitall.integrations.Integration.Export;
import ryzen.ownitall.integrations.Integration.Import;
import ryzen.ownitall.integrations.Integration.InvalidIntegrationException;
import ryzen.ownitall.integrations.Integration.Sync;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.MusicTools;
import ryzen.ownitall.util.ProgressBar;
import ryzen.ownitall.util.WebTools.AuthenticationException;

/**
 * <p>
 * IntegrationMenu class.
 * </p>
 *
 * @author ryzen
 */
@Controller
public class IntegrationMenu {
    private static final Logger logger = new Logger(IntegrationMenu.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    // read by the interceptor and the progress poll from other request threads
    private static volatile Integration integration;

    public static Integration getIntegration() {
        return integration;
    }

    /**
     * <p>
     * IntegrationMenu.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration")
    public String integrationMenu(Model model,
            @RequestParam(value = "callback", required = true) String callback) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        Class<?> filter;
        if (callback.contains("import")) {
            filter = Import.class;
        } else if (callback.contains("export")) {
            filter = Export.class;
        } else {
            filter = Sync.class;
        }
        for (Class<?> clazz : Integration.getIntegrations(filter)) {
            String name = clazz.getSimpleName();
            options.put(name, "/integration/" + name + "?callback=" + callback);
        }
        return Templates.menu(model, "Integration Menu", options, "/integration/return");
    }

    /**
     * <p>
     * Setter for the field <code>integration</code>.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param name     a {@link java.lang.String} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/{integration}")
    public String setIntegration(Model model, @PathVariable(value = "integration") String name,
            @RequestParam(value = "callback", required = true) String callback) {
        Class<?> clazz = Integration.getIntegration(name);
        if (clazz != null) {
            try {
                integration = new Integration(clazz);
                return "redirect:" + callback;
            } catch (MissingSettingException e) {
                logger.warn(model, "Missing settings to set up '" + clazz.getSimpleName() + "': "
                        + e.getMessage());
                return this.loginForm(model, name, "/integration/" + name + "?callback=" + callback);
            } catch (AuthenticationException e) {
                logger.warn(model, "Failed to authenticate into integration '" + integration + "': " + e.getMessage());
                return this.loginForm(model, clazz
                        .getSimpleName(),
                        "/integration/" + integration + "?callback=" + callback);
            } catch (InvalidIntegrationException e) {
                logger.error(model, "Invalid integration '" + integration + "' provided", e);
            }
        } else {
            logger.warn(model, "Unsupported integration class '" + integration + "'");
        }
        return integrationMenu(model, callback);
    }

    /**
     * <p>
     * loginForm.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param name     a {@link java.lang.String} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/login/{integration}")
    public String loginForm(Model model,
            @PathVariable(value = "integration") String name,
            @RequestParam(value = "callback", required = true) String callback) {
        Class<?> clazz = Integration.getIntegration(name);
        if (clazz == null) {
            logger.warn(model, "Unsupported integration '" + name + "'provided");
            return integrationMenu(model, callback);
        }
        Settings settings = Settings.load();
        LinkedHashSet<String> credentials = settings.getGroup(clazz);
        if (credentials == null || credentials.isEmpty()) {
            logger.info(model, "Integration '" + clazz.getSimpleName() + "' does not have credentials");
            return "redirect:" + callback;
        }
        return SettingsMenu.changeSettingForm(model, credentials, callback);
    }

    /**
     * <p>
     * importMenu.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/import")
    public String importMenu(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/return") String callback) {
        logger.info(model, "Current integration: " + integration.getIntegrationName());
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Import Library", "/integration/import/collection");
        options.put("Import Liked Songs", "/integration/import/likedsongs");
        options.put("Import Album(s)", "/integration/import/albums/menu");
        options.put("Import Playlist(s)", "/integration/import/playlists/menu");
        return Templates.menu(model, "Import Menu", options, callback);
    }

    /**
     * <p>
     * optionImportCollection.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/import/collection")
    public String optionImportCollection(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/import") String callback) {
        return Templates.process(model, "Importing '" + integration.getIntegrationName() + "' collection",
                "/integration/import/collection", "/integration/progress", "/integration/logs", callback);
    }

    /**
     * <p>
     * importCollection.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/import/collection")
    public ResponseEntity<Void> importCollection() {
        integration.importCollection();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionImportLikedSongs.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/import/likedsongs")
    public String optionImportLikedSongs(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/import") String callback) {
        return Templates.process(model, "Importing '" + integration.getIntegrationName() + "' liked songs",
                "/integration/import/likedsongs", "/integration/progress", "/integration/logs", callback);
    }

    /**
     * <p>
     * importLikedSongs.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/import/likedsongs")
    public ResponseEntity<Void> importLikedSongs() {
        integration.importLikedSongs();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/integration/import/albums/menu")
    public String importAlbumsMenu(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/import") String callback) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("All " + integration.getIntegrationName() + " Albums", "/integration/import/albums");
        options.put("Single " + integration.getIntegrationName() + " Album",
                "/integration/import/album/form?callback=/integration/import");
        return Templates.menu(model, "Import Album(s) Menu", options, callback);
    }

    /**
     * <p>
     * optionImportAlbums.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/import/albums")
    public String optionImportAlbums(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/import") String callback) {
        return Templates.process(model, "Importing '" + integration.getIntegrationName() + "' albums",
                "/integration/import/albums", "/integration/progress", "/integration/logs", callback);
    }

    /**
     * <p>
     * importAlbums.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/import/albums")
    public ResponseEntity<Void> importAlbums() {
        integration.importAlbums();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/integration/import/album/form")
    public String importAlbumForm(Model model,
            @RequestParam(value = "callback", required = true) String callback) {
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable id = new FormVariable("id");
        id.setName(integration.getIntegrationName() + " Album ID");
        id.setRequired(true);
        fields.add(id);
        FormVariable name = new FormVariable("name");
        name.setName("Album Name");
        fields.add(name);
        FormVariable artistName = new FormVariable("artistName");
        artistName.setName("Album Main Artist Name");
        fields.add(artistName);
        return Templates.form(model, "Get Album", fields, "/integration/import/album", callback);
    }

    /**
     * <p>
     * importAlbum.
     * </p>
     *
     * @param model     a {@link org.springframework.ui.Model} object
     * @param variables a {@link java.util.LinkedHashMap} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/import/album")
    public ResponseEntity<Void> importAlbum(Model model, @RequestBody LinkedHashMap<String, String> variables) {
        if (variables.get("id") == null) {
            logger.warn(model, "Missing album id in variables");
            return ResponseEntity.badRequest().build();
        }
        integration.importAlbum(variables.get("id"), variables.get("name"), variables.get("artistName"));
        logger.info(model,
                "Successfully imported " + integration.getIntegrationName() + " album '" + variables.get("id") + "'");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/integration/import/playlists/menu")
    public String importPlaylistsMenu(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/import") String callback) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("All " + integration.getIntegrationName() + " Playlists", "/integration/import/playlists");
        options.put("Single " + integration.getIntegrationName() + " Playlist",
                "/integration/import/playlist/form?callback=/integration/import");
        return Templates.menu(model, "Import Album(s) Menu", options, callback);
    }

    /**
     * <p>
     * optionImportPlaylists.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/import/playlists")
    public String optionImportPlaylists(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/import") String callback) {
        return Templates.process(model, "Importing '" + integration.getIntegrationName() + "' playlists",
                "/integration/import/playlists", "/integration/progress", "/integration/logs", callback);
    }

    /**
     * <p>
     * importPlaylists.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/import/playlists")
    public ResponseEntity<Void> importPlaylists() {
        integration.importPlaylists();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/integration/import/playlist/form")
    public String importPlaylistForm(Model model,
            @RequestParam(value = "callback", required = true) String callback) {
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable id = new FormVariable("id");
        id.setName(integration.getIntegrationName() + " Playlist ID");
        id.setRequired(true);
        fields.add(id);
        FormVariable name = new FormVariable("name");
        name.setName("Playlist Name");
        fields.add(name);
        return Templates.form(model, "Get Playlist", fields, "/integration/import/playlist", callback);
    }

    /**
     * <p>
     * importPlaylists.
     * </p>
     *
     * @param model     a {@link org.springframework.ui.Model} object
     * @param variables a {@link java.util.LinkedHashMap} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/import/playlist")
    public ResponseEntity<Void> importPlaylist(Model model, @RequestBody LinkedHashMap<String, String> variables) {
        if (variables.get("id") == null) {
            logger.warn(model, "missing playlist id in variables");
            return ResponseEntity.badRequest().build();
        }
        integration.importPlaylist(variables.get("id"), variables.get("name"));
        logger.info(model,
                "Successfully imported " + integration.getIntegrationName() + " playlist '" + variables.get("id")
                        + "'");
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * exportMenu.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/export")
    public String exportMenu(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/return") String callback) {
        logger.info(model, "Current Integration: " + integration.getIntegrationName());
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Export Library", "/integration/export/collection");
        options.put("Export Liked Songs", "/integration/export/likedsongs");
        options.put("Export Album(s)", "/integration/export/albums");
        options.put("Export Playlist(s)", "/integration/export/playlists");
        return Templates.menu(model, "Export Menu", options, callback);
    }

    /**
     * <p>
     * optionExportCollection.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/export/collection")
    public String optionExportCollection(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/export") String callback) {
        return Templates.process(model, "Exporting '" + integration.getIntegrationName() + "' collection",
                "/integration/export/collection", "/integration/progress", "/integration/logs", callback);
    }

    /**
     * <p>
     * exportCollection.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/export/collection")
    public ResponseEntity<Void> exportCollection() {
        integration.exportCollection();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionExportLikedSongs.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/export/likedsongs")
    public String optionExportLikedSongs(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/export") String callback) {
        return Templates.process(model, "Exporting '" + integration.getIntegrationName() + "' liked songs",
                "/integration/export/likedsongs", "/integration/progress", "/integration/logs", callback);
    }

    /**
     * <p>
     * exportLikedSongs.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/export/likedsongs")
    public ResponseEntity<Void> exportLikedSongs() {
        integration.exportLikedSongs();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionExportAlbums.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/export/albums")
    public String optionExportAlbums(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/export") String callback) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("All", "/integration/process?processName=Exporting all Albums from "
                + integration.getIntegrationName() + "&processFunction=/integration/export/albums&callback="
                + callback);
        for (Album album : Collection.getAlbums()) {
            options.put(album.toString(), "/integration/process?processName=Exporting '" + album.getName() + "' from "
                    + integration.getIntegrationName()
                    + "&processFunction=/integration/export/album/" + album.hashCode()
                    + "&callback=" + callback);
        }
        return Templates.menu(model, "Album Export Menu", options, callback);
    }

    /**
     * <p>
     * exportAlbums.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/export/albums")
    public ResponseEntity<Void> exportAlbums() {
        integration.exportAlbums();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * exportAlbums.
     * </p>
     *
     * @param hashCode a int
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/export/album/{album}")
    public ResponseEntity<Void> exportAlbum(
            @PathVariable(value = "album") int hashCode) {
        Album album = Collection.getAlbum(hashCode);
        if (album != null) {
            integration.exportAlbum(album);
        } else {
            logger.warn("Unable to find album '" + hashCode + "' in collection");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionExportPlaylists.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/export/playlists")
    public String optionExportPlaylists(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/return") String callback) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("All", "/integration/process?processName=Exporting all playlists from "
                + integration.getIntegrationName() + "&processFunction=/integration/export/playlists&callback="
                + callback);
        for (Playlist playlist : Collection.getPlaylists()) {
            options.put(playlist.toString(),
                    "/integration/process?processName=Exporting '" + playlist.getName() + "' from "
                            + integration.getIntegrationName()
                            + "&processFunction=/integration/export/playlist/" + playlist.hashCode()
                            + "&callback=" + callback);
        }
        return Templates.menu(model, "Playlist Export Menu", options, callback);
    }

    /**
     * <p>
     * exportPlaylists.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/export/playlists")
    public ResponseEntity<Void> exportPlaylists() {
        integration.exportPlaylists();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * exportPlaylists.
     * </p>
     *
     * @param hashCode a int
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/export/playlist/{playlist}")
    public ResponseEntity<Void> exportPlaylist(
            @PathVariable(value = "playlist") int hashCode) {
        Playlist playlist = Collection.getPlaylist(hashCode);
        if (playlist != null) {
            integration.exportPlaylist(playlist);
        } else {
            logger.warn("Unable to find playlist '" + hashCode + "' in collection");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * syncMenu.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/sync")
    public String syncMenu(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/return") String callback) {
        logger.info(model, "Current Integration: " + integration.getIntegrationName());
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Sync Library", "/integration/sync/collection");
        options.put("Sync Liked Songs", "/integration/sync/likedsongs");
        options.put("Sync Albums", "/integration/sync/albums");
        options.put("Sync Playlists", "/integration/sync/playlists/menu");
        return Templates.menu(model, "Sync Menu", options, callback);
    }

    /**
     * <p>
     * optionSyncCollection.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/sync/collection")
    public String optionSyncCollection(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/sync") String callback) {
        return Templates.process(model, "Syncronizing '" + integration.getIntegrationName() + "' collection",
                "/integration/sync/collection", "/integration/progress", "/integration/logs", callback);
    }

    /**
     * <p>
     * syncCollection.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/sync/collection")
    public ResponseEntity<Void> syncCollection() {
        integration.syncCollection();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionSyncLikedSongs.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/sync/likedsongs")
    public String optionSyncLikedSongs(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/sync") String callback) {
        return Templates.process(model, "Syncronizing '" + integration.getIntegrationName() + "' liked songs",
                "/integration/sync/likedsongs", "/integration/progress", "/integration/logs", callback);
    }

    /**
     * <p>
     * syncLikedSongs.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/sync/likedsongs")
    public ResponseEntity<Void> syncLikedSongs() {
        integration.syncLikedSongs();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionSyncAlbums.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/sync/albums")
    public String optionSyncAlbums(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/sync") String callback) {
        return Templates.process(model, "Syncronizing '" + integration.getIntegrationName() + "' albums",
                "/integration/sync/albums", "/integration/progress", "/integration/logs", callback);
    }

    /**
     * <p>
     * syncAlbums.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/sync/albums")
    public ResponseEntity<Void> syncAlbums() {
        integration.syncAlbums();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/integration/sync/playlists/menu")
    public String syncPlaylistsMenu(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/sync") String callback) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("All", "/integration/sync/playlists");
        for (Playlist playlist : Collection.getPlaylists()) {
            options.put(playlist.getName(), "/integration/sync/playlist/" + playlist.hashCode());
        }
        return Templates.menu(model, "Sync Playlist(s)", options, callback);
    }

    /**
     * <p>
     * optionSyncPlaylists.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/sync/playlists")
    public String optionSyncPlaylists(Model model,
            @RequestParam(value = "callback", defaultValue = "/integration/sync") String callback) {
        return Templates.process(model, "Exporting '" + integration.getIntegrationName() + "' playlists",
                "/integration/sync/playlists", "/integration/progress", "/integration/logs", callback);
    }

    @GetMapping("/integration/sync/playlists/{playlist}")
    public String optionSyncPlaylist(Model model,
            @PathVariable(value = "playlist") int hashCode,
            @RequestParam(value = "callback", defaultValue = "/integration/sync") String callback) {
        Playlist playlist = Collection.getPlaylist(hashCode);
        if (playlist != null) {
            return Templates.process(model,
                    "Exporting '" + integration.getIntegrationName() + "' playlist '" + playlist.getName() + "'",
                    "/integration/sync/playlist/" + playlist.hashCode(), "/integration/progress", "/integration/logs",
                    callback);
        } else {
            logger.warn(model, "Unable to find playlist '" + hashCode + "' in collection");
            return "redirect:" + callback;
        }
    }

    /**
     * <p>
     * syncPlaylists.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/sync/playlists")
    public ResponseEntity<Void> syncPlaylists() {
        integration.syncPlaylists();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/integration/sync/playlist/{playlist}")
    public ResponseEntity<Void> syncPlaylist(@PathVariable(value = "playlist") int hashCode) {
        Playlist playlist = Collection.getPlaylist(hashCode);
        if (playlist != null) {
            integration.syncPlaylist(playlist);
        } else {
            logger.warn("Unable to find playlist '" + hashCode + "' in collection");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * integrationProgress.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @GetMapping("/integration/progress")
    @ResponseBody
    public ResponseEntity<String> integrationProgress() {
        ObjectNode rootNode = mapper.createObjectNode();
        Stack<ProgressBar> pbs = ProgressBar.getInstances();
        if (!pbs.isEmpty()) {
            ArrayNode pbNodes = mapper.createArrayNode();
            for (ProgressBar pb : pbs) {
                ObjectNode pbNode = mapper.createObjectNode();
                pbNode.put("id", pb.hashCode());
                pbNode.put("title", pb.getTitle());
                pbNode.put("step", pb.getStep());
                pbNode.put("time", MusicTools.musicTime(pb.getElapsedTime()));
                pbNode.put("message", pb.getMessage());
                pbNode.put("maxstep", pb.getMaxStep());
                pbNodes.add(pbNode);
            }
            rootNode.set("bars", pbNodes);
            return ResponseEntity.ok(rootNode.toPrettyString());
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * <p>
     * integrationLogs.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @GetMapping("/integration/logs")
    @ResponseBody
    public ResponseEntity<String> integrationLogs() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode logsArray = rootNode.putArray("logs");

        for (Entry<Level, String> entity : Logger.getGlobalLogs()) {
            ObjectNode logNode = mapper.createObjectNode();
            logNode.put("level", entity.getKey().toString().toUpperCase());
            logNode.put("message", entity.getValue());
            logsArray.add(logNode);
        }
        Logger.clearLogs();
        return ResponseEntity.ok(rootNode.toPrettyString());
    }

    /**
     * <p>
     * cancel.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/integration/cancel")
    public ResponseEntity<Void> cancel() {
        InterruptionHandler.forceInterruption();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionReturn.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/integration/return")
    public String optionReturn() {
        integration = null;
        logger.debug("Done cleaning albums");
        return "redirect:/collection";
    }
}
