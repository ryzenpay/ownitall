package ryzen.ownitall.ui.web;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Stack;
import java.util.Map.Entry;

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
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.method.interfaces.Export;
import ryzen.ownitall.method.interfaces.Import;
import ryzen.ownitall.method.interfaces.Sync;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.MusicTools;

import org.apache.logging.log4j.Level;
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
@Controller
public class MethodMenu {
    private static final Logger logger = new Logger(MethodMenu.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private Method method;

    /**
     * <p>
     * methodMenu.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method")
    public String methodMenu(Model model,
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
        for (Class<?> currMethod : Method.getMethods(filter)) {
            String methodName = currMethod.getSimpleName();
            options.put(methodName, "/method/" + methodName + "?callback=" + callback);
        }
        return Templates.menu(model, "Method Menu", options, "/method/return");
    }

    /**
     * <p>
     * Setter for the field <code>method</code>.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param method   a {@link java.lang.String} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/{method}")
    public String setMethod(Model model, @PathVariable(value = "method") String method,
            @RequestParam(value = "callback", required = true) String callback) {
        Class<?> methodClass = Method.getMethod(method);
        if (methodClass != null) {
            try {
                this.method = new Method(methodClass);
                return "redirect:" + callback;
            } catch (MissingSettingException e) {
                logger.warn(model, "Missing settings to set up '" + methodClass.getSimpleName() + "': "
                        + e.getMessage());
                return this.loginForm(model, method, "/method/" + method + "?callback=" + callback);
            } catch (AuthenticationException e) {
                logger.warn(model, "Failed to authenticate into method '" + method + "': " + e.getMessage());
                return this.loginForm(model, methodClass
                        .getSimpleName(),
                        "/method/" + method + "?callback=" + callback);
            } catch (NoSuchMethodException e) {
                logger.error(model, "Invalid method '" + method + "' provided", e);
            }
        } else {
            logger.warn(model, "Unsupported method class '" + method + "'");
        }
        return methodMenu(model, callback);
    }

    /**
     * <p>
     * loginForm.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     * @param method a {@link java.lang.String} object
     */
    @GetMapping("/method/login/{method}")
    public String loginForm(Model model,
            @PathVariable(value = "method") String method,
            @RequestParam(value = "callback", required = true) String callback) {
        logger.debug(model, "method=" + method + ", callback=" + callback);

        Class<?> methodClass = Method.getMethod(method);
        if (methodClass == null) {
            logger.warn(model, "Unsupported method '" + method + "'provided");
            return methodMenu(model, callback);
        }
        Settings settings = Settings.load();
        LinkedHashSet<String> credentials = settings.getGroup(methodClass);
        if (credentials == null || credentials.isEmpty()) {
            logger.info(model, "Method '" + methodClass.getSimpleName() + "' does not have credentials");
            return "redirect:" + callback;
        }
        return SettingsMenu.changeSettingForm(model, credentials, callback);
    }

    /**
     * <p>
     * importMenu.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/import")
    public String importMenu(Model model) {
        if (this.method == null) {
            return methodMenu(model, "/method/import");
        }
        logger.info(model, "Current method: " + this.method.getMethodName());
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Import Library", "/method/import/collection");
        options.put("Import Liked Songs", "/method/import/likedsongs");
        options.put("Import Album(s)", "/method/import/albums/menu");
        options.put("Import Playlist(s)", "/method/import/playlists/menu");
        return Templates.menu(model, "Import Menu", options, "/method/return");
    }

    /**
     * <p>
     * optionImportCollection.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/import/collection")
    public String optionImportCollection(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in import/collection");
            return methodMenu(model, "/method/import/collection");
        }
        return Templates.process(model, "Importing '" + this.method.getMethodName() + "' collection",
                "/method/import/collection", "/method/progress", "/method/logs", "/method/import");
    }

    /**
     * <p>
     * importCollection.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/import/collection")
    public ResponseEntity<Void> importCollection() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.importCollection();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionImportLikedSongs.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/import/likedsongs")
    public String optionImportLikedSongs(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in import/likedsongs");
            return methodMenu(model, "/method/import/likedsongs");
        }
        return Templates.process(model, "Importing '" + this.method.getMethodName() + "' liked songs",
                "/method/import/likedsongs", "/method/progress", "/method/logs", "/method/import");
    }

    /**
     * <p>
     * importLikedSongs.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/import/likedsongs")
    public ResponseEntity<Void> importLikedSongs() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.importLikedSongs();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/method/import/albums/menu")
    public String importAlbumsMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("All " + this.method.getMethodName() + " Albums", "/method/import/albums");
        options.put("Single " + this.method.getMethodName() + " Album",
                "/method/import/album/form?callback=/method/import");
        return Templates.menu(model, "Import Album(s) Menu", options, "/method/import");
    }

    /**
     * <p>
     * optionImportAlbums.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/import/albums")
    public String optionImportAlbums(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in import/albums");
            return methodMenu(model, "/method/import/albums");
        }
        return Templates.process(model, "Importing '" + this.method.getMethodName() + "' albums",
                "/method/import/albums", "/method/progress", "/method/logs", "/method/import");
    }

    /**
     * <p>
     * importAlbums.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/import/albums")
    public ResponseEntity<Void> importAlbums() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.importAlbums();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/method/import/album/form")
    public String importAlbumForm(Model model,
            @RequestParam(value = "callback", required = true) String callback) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in import/album");
            return methodMenu(model, "/method/import/album");
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable id = new FormVariable("id");
        id.setName(this.method.getMethodName() + " Album ID");
        id.setRequired(true);
        fields.add(id);
        FormVariable name = new FormVariable("name");
        name.setName("Album Name");
        fields.add(name);
        FormVariable artistName = new FormVariable("artistName");
        artistName.setName("Album Main Artist Name");
        fields.add(artistName);
        return Templates.form(model, "Get Album", fields, "/method/import/album", callback);
    }

    /**
     * <p>
     * importAlbum.
     * </p>
     *
     * @param id a {@link java.lang.String} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/import/album")
    public ResponseEntity<Void> importAlbum(Model model, @RequestBody LinkedHashMap<String, String> variables) {
        if (variables.get("id") == null) {
            return ResponseEntity.badRequest().build();
        }
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.importAlbum(variables.get("id"), variables.get("name"), variables.get("artistName"));
        logger.info(model,
                "Successfully imported " + this.method.getMethodName() + " album '" + variables.get("id") + "'");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/method/import/playlists/menu")
    public String importPlaylistsMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("All " + this.method.getMethodName() + " Playlists", "/method/import/playlists");
        options.put("Single " + this.method.getMethodName() + " Playlist",
                "/method/import/playlist/form?callback=/method/import");
        return Templates.menu(model, "Import Album(s) Menu", options, "/method/import");
    }

    /**
     * <p>
     * optionImportPlaylists.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/import/playlists")
    public String optionImportPlaylists(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in import/playlists");
            return methodMenu(model, "/method/import/playlists");
        }
        return Templates.process(model, "Importing '" + this.method.getMethodName() + "' playlists",
                "/method/import/playlists", "/method/progress", "/method/logs", "/method/import");
    }

    /**
     * <p>
     * importPlaylists.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/import/playlists")
    public ResponseEntity<Void> importPlaylists() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.importPlaylists();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/method/import/playlist/form")
    public String importPlaylistForm(Model model,
            @RequestParam(value = "callback", required = true) String callback) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in import/playlist");
            return methodMenu(model, "/method/import/playlist");
        }
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        FormVariable id = new FormVariable("id");
        id.setName(this.method.getMethodName() + " Playlist ID");
        id.setRequired(true);
        fields.add(id);
        FormVariable name = new FormVariable("name");
        name.setName("Playlist Name");
        fields.add(name);
        return Templates.form(model, "Get Playlist", fields, "/method/import/playlist", callback);
    }

    /**
     * <p>
     * importPlaylists.
     * </p>
     *
     * @param id a {@link java.lang.String} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/import/playlist")
    public ResponseEntity<Void> importPlaylist(Model model, @RequestBody LinkedHashMap<String, String> variables) {
        if (variables.get("id") == null) {
            return ResponseEntity.badRequest().build();
        }
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.importPlaylist(variables.get("id"), variables.get("name"));
        logger.info(model,
                "Successfully imported " + this.method.getMethodName() + " playlist '" + variables.get("id") + "'");
        return ResponseEntity.badRequest().build();
    }

    /**
     * <p>
     * exportMenu.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/export")
    public String exportMenu(Model model) {
        if (this.method == null) {
            return methodMenu(model, "/method/export");
        }
        logger.info(model, "Current method: " + this.method.getMethodName());
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Export Library", "/method/export/collection");
        options.put("Export Liked Songs", "/method/export/likedsongs");
        options.put("Export Album(s)", "/method/export/albums");
        options.put("Export Playlist(s)", "/method/export/playlists");
        return Templates.menu(model, "Export Menu", options, "/method/return");
    }

    /**
     * <p>
     * optionExportCollection.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/export/collection")
    public String optionExportCollection(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in export/collection");
            return methodMenu(model, "/method/export/collection");
        }
        return Templates.process(model, "Exporting '" + this.method.getMethodName() + "' collection",
                "/method/export/collection", "/method/progress", "/method/logs", "/method/export");
    }

    /**
     * <p>
     * exportCollection.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/export/collection")
    public ResponseEntity<Void> exportCollection() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.exportCollection();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionExportLikedSongs.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/export/likedsongs")
    public String optionExportLikedSongs(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in export/likedsongs");
            return methodMenu(model, "/method/export/likedsongs");
        }
        return Templates.process(model, "Exporting '" + this.method.getMethodName() + "' liked songs",
                "/method/export/likedsongs", "/method/progress", "/method/logs", "/method/export");
    }

    /**
     * <p>
     * exportLikedSongs.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/export/likedsongs")
    public ResponseEntity<Void> exportLikedSongs() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.exportLikedSongs();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionExportAlbums.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/export/albums")
    public String optionExportAlbums(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in export/albums");
            return methodMenu(model, "/method/export/albums");
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("All", "/method/process?processName=Exporting all Albums from "
                + this.method.getMethodName() + "&processFunction=/method/export/albums&callback=/method/export");
        for (Album album : Collection.getAlbums()) {
            options.put(album.toString(), "/method/process?processName=Exporting '" + album.getName() + "' from "
                    + this.method
                            .getMethodName()
                    + "&processFunction=/method/export/album/" + album.getName()
                    + "&callback=/method/export");
        }
        return Templates.menu(model, "Album Export Menu", options, "/method/export");
    }

    /**
     * <p>
     * exportAlbums.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/export/albums")
    public ResponseEntity<Void> exportAlbums() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.exportAlbums();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * exportAlbums.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/export/album/{name}")
    public ResponseEntity<Void> exportAlbum(
            @PathVariable(value = "name") String name) {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        Album album = Collection.getAlbum(name);
        if (album != null) {
            this.method.exportAlbum(album);
        } else {
            logger.warn("Unable to find album '" + name + "' in collection");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionExportPlaylists.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/export/playlists")
    public String optionExportPlaylists(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in export/playlists");
            return methodMenu(model, "/method/export/playlists");
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("All", "/method/process?processName=Exporting all playlists from "
                + this.method.getMethodName() + "&processFunction=/method/export/playlists&callback=/method/export");
        for (Playlist playlist : Collection.getPlaylists()) {
            options.put(playlist.toString(), "/method/process?processName=Exporting '" + playlist.getName() + "' from "
                    + this.method
                            .getMethodName()
                    + "&processFunction=/method/export/playlist/" + playlist.getName()
                    + "&callback=/method/export");
        }
        return Templates.menu(model, "Playlist Export Menu", options, "/method/export");
    }

    /**
     * <p>
     * exportPlaylists.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/export/playlists")
    public ResponseEntity<Void> exportPlaylists() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.exportPlaylists();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * exportPlaylists.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/export/playlist/{name}")
    public ResponseEntity<Void> exportPlaylist(
            @PathVariable(value = "name") String name) {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        Playlist playlist = Collection.getPlaylist(name);
        if (playlist != null) {
            this.method.exportPlaylist(playlist);
        } else {
            logger.warn("Unable to find playlist '" + name + "' in collection");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * syncMenu.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/sync")
    public String syncMenu(Model model) {
        if (this.method == null) {
            return methodMenu(model, "/method/sync");
        }
        logger.info(model, "Current method: " + this.method.getMethodName());
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Sync Library", "/method/sync/collection");
        options.put("Sync Liked Songs", "/method/sync/likedsongs");
        options.put("Sync Albums", "/method/sync/albums");
        options.put("Sync Playlists", "/method/sync/playlists");
        return Templates.menu(model, "Sync Menu", options, "/method/return");
    }

    /**
     * <p>
     * optionSyncCollection.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/sync/collection")
    public String optionSyncCollection(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in sync/collection");
            return methodMenu(model, "/method/sync/collection");
        }
        return Templates.process(model, "Syncronizing '" + this.method.getMethodName() + "' collection",
                "/method/sync/collection", "/method/progress", "/method/logs", "/method/sync");
    }

    /**
     * <p>
     * syncCollection.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/sync/collection")
    public ResponseEntity<Void> syncCollection() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.syncCollection();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionSyncLikedSongs.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/sync/likedsongs")
    public String optionSyncLikedSongs(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in sync/likedsongs");
            return methodMenu(model, "/method/sync/likedsongs");
        }
        return Templates.process(model, "Syncronizing '" + this.method.getMethodName() + "' liked songs",
                "/method/sync/likedsongs", "/method/progress", "/method/logs", "/method/sync");
    }

    /**
     * <p>
     * syncLikedSongs.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/sync/likedsongs")
    public ResponseEntity<Void> syncLikedSongs() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.syncLikedSongs();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionSyncAlbums.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/sync/albums")
    public String optionSyncAlbums(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in sync/albums");
            return methodMenu(model, "/method/sync/albums");
        }
        return Templates.process(model, "Syncronizing '" + this.method.getMethodName() + "' albums",
                "/method/sync/albums", "/method/progress", "/method/logs", "/method/sync");
    }

    /**
     * <p>
     * syncAlbums.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/sync/albums")
    public ResponseEntity<Void> syncAlbums() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.syncAlbums();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * optionSyncPlaylists.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/sync/playlists")
    public String optionSyncPlaylists(Model model) {
        if (this.method == null) {
            logger.warn(model, "Method not initialized in sync/playlists");
            return methodMenu(model, "/method/sync/playlists");
        }
        return Templates.process(model, "Exporting '" + this.method.getMethodName() + "' playlists",
                "/method/sync/playlists", "/method/progress", "/method/logs", "/method/sync");
    }

    /**
     * <p>
     * syncPlaylists.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    // TODO: sync individual playlists
    @PostMapping("/method/sync/playlists")
    public ResponseEntity<Void> syncPlaylists() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.syncPlaylists();
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * methodProgress.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @GetMapping("/method/progress")
    @ResponseBody
    public ResponseEntity<String> methodProgress() {
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
     * methodLogs.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @GetMapping("/method/logs")
    @ResponseBody
    public ResponseEntity<String> methodLogs() {
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
    @PostMapping("/method/cancel")
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
    @GetMapping("/method/return")
    public String optionReturn() {
        this.method = null;
        return "redirect:/collection";
    }
}
