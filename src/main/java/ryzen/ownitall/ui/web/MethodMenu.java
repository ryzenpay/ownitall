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

    private String getMethodName() {
        if (method == null) {
            return "";
        }
        return method.getClass().getSimpleName();
    }

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
        model.addAttribute("menuName", "Method Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/method/return");
        return "menu";
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
        logger.info(model, "Current method: " + getMethodName());
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Import Library", "/method/import/collection");
        options.put("Import Liked Songs", "/method/import/likedsongs");
        options.put("Import Album(s)", "/method/import/albums");
        options.put("Import Playlist(s)", "/method/import/playlists");
        model.addAttribute("menuName", "Import Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/method/return");
        return "menu";
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
        return process(model, "Importing '" + getMethodName()
                + "' collection", "/method/import/collection", "/method/import");
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
        return process(model, "Importing '" + getMethodName() + "' liked songs",
                "/method/import/likedsongs", "/method/import");
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

    // TODO: import individual album
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
        return process(model, "Importing '" + getMethodName() + "' albums", "/method/import/albums",
                "/method/import");
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

    // TODO: use a form to get id, name and album artist name
    // also update for playlist
    /**
     * <p>
     * importAlbum.
     * </p>
     *
     * @param id a {@link java.lang.String} object
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/import/album")
    public ResponseEntity<Void> importAlbum(@RequestParam(value = "id", required = true) String id,
            @RequestParam(value = "albumName", required = false) String albumName,
            @RequestParam(value = "artistName", required = false) String artistName) {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.importAlbum(id, albumName, artistName);
        return ResponseEntity.badRequest().build();
    }

    // TODO: import individual playlist
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
        return process(model, "Importing '" + getMethodName() + "' playlists",
                "/method/import/playlists", "/method/import");
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
        return ResponseEntity.badRequest().build();
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
    public ResponseEntity<Void> importPlaylist(@RequestParam(value = "id", required = true) String id,
            @RequestParam(value = "name", required = false) String name) {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        this.method.importPlaylist(id, name);
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
        logger.info(model, "Current method: " + getMethodName());
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Export Library", "/method/export/collection");
        options.put("Export Liked Songs", "/method/export/likedsongs");
        options.put("Export Album(s)", "/method/export/albums");
        options.put("Export Playlist(s)", "/method/export/playlists");
        model.addAttribute("menuName", "Export Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/method/return");
        return "menu";
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
        return process(model, "Exporting '" + getMethodName() + "' collection",
                "/method/export/collection", "/method/export");
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
        return process(model, "Exporting '" + getMethodName() + "' liked songs",
                "/method/export/likedsongs", "/method/export");
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
                + getMethodName() + "&processFunction=/method/export/albums&callback=/method/export");
        for (Album album : Collection.getAlbums()) {
            options.put(album.toString(), "/method/process?processName=Exporting '" + album.getName() + "' from "
                    + getMethodName() + "&processFunction=/method/export/album/" + album.getName()
                    + "&callback=/method/export");
        }
        model.addAttribute("menuName", "Album Export Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/method/export");
        return "menu";
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
                + getMethodName() + "&processFunction=/method/export/playlists&callback=/method/export");
        for (Playlist playlist : Collection.getPlaylists()) {
            options.put(playlist.toString(), "/method/process?processName=Exporting '" + playlist.getName() + "' from "
                    + getMethodName() + "&processFunction=/method/export/playlist/" + playlist.getName()
                    + "&callback=/method/export");
        }
        model.addAttribute("menuName", "Playlist Export Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/method/export");
        return "menu";
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
        logger.info(model, "Current method: " + getMethodName());
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Sync Library", "/method/sync/collection");
        options.put("Sync Liked Songs", "/method/sync/likedsongs");
        options.put("Sync Albums", "/method/sync/albums");
        options.put("Sync Playlists", "/method/sync/playlists");
        model.addAttribute("menuName", "Sync Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/method/return");
        return "menu";
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
        return process(model, "Syncronizing '" + getMethodName() + "' collection",
                "/method/sync/collection", "/method/sync");
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
        return process(model, "Syncronizing '" + getMethodName() + "' liked songs",
                "/method/sync/likedsongs", "/method/sync");
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
        return process(model, "Syncronizing '" + getMethodName() + "' albums", "/method/sync/albums",
                "/method/sync");
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
        return process(model, "Exporting '" + getMethodName() + "' playlists",
                "/method/sync/playlists", "/method/sync");
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
     * process.
     * </p>
     *
     * @param model           a {@link org.springframework.ui.Model} object
     * @param processName     a {@link java.lang.String} object
     * @param processFunction a {@link java.lang.String} object
     * @param callback        a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method/process")
    public String process(Model model,
            @RequestParam(value = "processName", required = true) String processName,
            @RequestParam(value = "processFunction", required = true) String processFunction,
            @RequestParam(value = "callback", required = true) String callback) {
        if (this.method == null) {
            logger.warn(model, "Method was not initialized");
            return methodMenu(model, "/method/process?processName=" + processName + "&processFunction="
                    + processFunction + "&callback=" + callback);
        }
        model.addAttribute("processName", processName);
        model.addAttribute("processFunction", processFunction);
        model.addAttribute("processProgress", "/method/progress");
        model.addAttribute("processLogs", "/method/logs");
        model.addAttribute("callback", callback);
        return "process";
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
