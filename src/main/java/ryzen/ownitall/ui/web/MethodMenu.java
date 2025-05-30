package ryzen.ownitall.ui.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import ryzen.ownitall.classes.LikedSongs;
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
     * @param model           a {@link org.springframework.ui.Model} object
     * @param methodClassName a {@link java.lang.String} object
     * @param callback        a {@link java.lang.String} object
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
     * @param model           a {@link org.springframework.ui.Model} object
     * @param methodClassName a {@link java.lang.String} object
     * @param callback        a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
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
     */
    @PostMapping("/method/import/collection")
    public ResponseEntity<Void> importCollection() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            try (ProgressBar pb = new ProgressBar("Import Collection", 3)) {
                pb.step("Liked Songs");
                this.method.getImport().getLikedSongs();
                pb.step("Albums");
                this.method.getImport().getAlbums();
                pb.step("Playlists");
                this.method.getImport().getPlaylists();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + getMethodName() + "' collection");
        }
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
     */
    @PostMapping("/method/import/likedsongs")
    public ResponseEntity<Void> importLikedSongs() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            LikedSongs likedSongs = this.method.getImport().getLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + getMethodName() + "' liked songs");
        }
        return ResponseEntity.ok().build();
    }

    // TODO: import individual album
    // browse with just albums, option to do all or individual albums?
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
     */
    @PostMapping("/method/import/albums")
    public ResponseEntity<Void> importAlbums() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            ArrayList<Album> albums = this.method.getImport().getAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
                logger.info("Successfully added '" + getMethodName() + "' albums to collection");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + getMethodName() + "' albums");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/method/import/album/{id}")
    public ResponseEntity<Void> importAlbum(@PathVariable(value = "id") String id) {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Album album = this.method.getImport().getAlbum(id, null, null);
            if (album != null) {
                Collection.addAlbum(album);
                logger.info("Successfully added album '" + album.toString() + "' to collection");
                return ResponseEntity.ok().build();
            } else {
                logger.warn(
                        "Unable to find album '" + id + "' for method '" + getMethodName() + "'");
            }
        } catch (InterruptedException e) {
            logger.debug(
                    "Interrupted while importing '" + getMethodName() + "' album '" + id + "'");
        }
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

    @PostMapping("/method/import/playlists")
    public ResponseEntity<Void> importPlaylists() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            ArrayList<Playlist> playlists = this.method.getImport().getPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
                logger.info("Successfully added " + playlists.size() + "'" + getMethodName() + "' playlists");
                return ResponseEntity.ok().build();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + getMethodName() + "' playlists");
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * <p>
     * importPlaylists.
     * </p>
     */
    @PostMapping("/method/import/playlist/{id}")
    public ResponseEntity<Void> importPlaylist(@PathVariable(value = "id") String id) {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Playlist playlist = this.method.getImport().getPlaylist(id, null);
            if (playlist != null) {
                Collection.addPlaylist(playlist);
                logger.info("Successfully added playlist '" + playlist.toString() + "' to collection");
                return ResponseEntity.ok().build();
            } else {
                logger.warn("Unable to find playlist '" + id + "' in " + getMethodName());
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting playlist '" + id + "' from '" + getMethodName() + "'");
        }
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
     */
    @PostMapping("/method/export/collection")
    public ResponseEntity<Void> exportCollection() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            try (ProgressBar pb = new ProgressBar("Export Collection", 3)) {
                pb.step("Liked Songs");
                this.method.getExport().uploadLikedSongs();
                pb.step("Albums");
                this.method.getExport().uploadAlbums();
                pb.step("Playlists");
                this.method.getExport().uploadPlaylists();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + getMethodName() + "' collection");
        }
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
     */
    @PostMapping("/method/export/likedsongs")
    public ResponseEntity<Void> exportLikedSongs() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            this.method.getExport().uploadLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + getMethodName() + "' liked songs");
        }
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

    @PostMapping("/method/export/albums")
    public ResponseEntity<Void> exportAlbums() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            this.method.getExport().uploadAlbums();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + getMethodName() + "' albums");
        }
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * exportAlbums.
     * </p>
     */
    @PostMapping("/method/export/album/{name}")
    public ResponseEntity<Void> exportAlbum(
            @PathVariable(value = "name") String name) {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        Album album = Collection.getAlbum(name);
        if (album != null) {
            try {
                this.method.getExport().uploadAlbum(album);
            } catch (InterruptedException e) {
                logger.debug("Interrupted while exporting '" + getMethodName() + "' album '"
                        + album.getName() + "'");
            }
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

    @PostMapping("/method/export/playlists")
    public ResponseEntity<Void> exportPlaylists() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            this.method.getExport().uploadPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + getMethodName() + "' playlists");
        }
        return ResponseEntity.ok().build();
    }

    /**
     * <p>
     * exportPlaylists.
     * </p>
     */
    @PostMapping("/method/export/playlist/{name}")
    public ResponseEntity<Void> exportPlaylist(
            @PathVariable(value = "name") String name) {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        Playlist playlist = Collection.getPlaylist(name);
        if (playlist != null) {
            try {
                this.method.getExport().uploadPlaylist(playlist);
            } catch (InterruptedException e) {
                logger.debug("Interrupted while exporting '" + getMethodName() + "' playlist '"
                        + playlist.getName() + "'");
            }
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
     */
    @PostMapping("/method/sync/collection")
    public ResponseEntity<Void> syncCollection() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            try (ProgressBar pb = new ProgressBar("Sync Collection", 3)) {
                pb.step("Liked Songs");
                this.method.getSync().syncLikedSongs();
                pb.step("Albums");
                this.method.getSync().syncAlbums();
                pb.step("Playlists");
                this.method.getSync().syncPlaylists();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + getMethodName() + "'collection");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
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
     */
    @PostMapping("/method/sync/likedsongs")
    public ResponseEntity<Void> syncLikedSongs() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            this.method.getSync().syncLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + getMethodName() + "'liked songs");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
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
     */
    @PostMapping("/method/sync/albums")
    public ResponseEntity<Void> syncAlbums() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            this.method.getSync().syncAlbums();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + getMethodName() + "'albums");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
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
     */
    @PostMapping("/method/sync/playlists")
    public ResponseEntity<Void> syncPlaylists() {
        if (this.method == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            this.method.getSync().syncPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + getMethodName() + "'playlists");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (AuthenticationException e) {
            logger.warn(
                    "Failed to Authenticate while syncronizing '" + getMethodName() + "' library: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
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
    @PostMapping("/method/progress")
    @ResponseBody
    public ResponseEntity<String> methodProgress() {
        ObjectNode rootNode = mapper.createObjectNode();
        ProgressBar pb = ProgressBar.getRootInstance();
        if (pb != null) {
            rootNode.put("title", pb.getTitle());
            rootNode.put("step", pb.getStep());
            rootNode.put("time", MusicTools.musicTime(pb.getElapsedTime()));
            rootNode.put("message", pb.getMessage());
            rootNode.put("maxstep", pb.getMaxStep());
            return ResponseEntity.ok(rootNode.toPrettyString());
        } else {
            rootNode.put("title", "");
            rootNode.put("step", 0);
            rootNode.put("time", 0);
            rootNode.put("message", "");
            rootNode.put("maxstep", 0);
            return ResponseEntity.ok(rootNode.toPrettyString());
        }
    }

    /**
     * <p>
     * methodLogs.
     * </p>
     *
     * @return a {@link org.springframework.http.ResponseEntity} object
     */
    @PostMapping("/method/logs")
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
