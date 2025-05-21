package ryzen.ownitall.ui.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
import ryzen.ownitall.method.Method.Export;
import ryzen.ownitall.method.Method.Import;
import ryzen.ownitall.util.InterruptionHandler;
import ryzen.ownitall.util.LogConfig;
import ryzen.ownitall.util.Logger;
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
     * @param model           a {@link org.springframework.ui.Model} object
     * @param methodClassName a {@link java.lang.String} object
     * @param callback        a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/method")
    public String methodMenu(Model model,
            @RequestParam(value = "method", required = false) String method,
            @RequestParam(value = "callback", required = true) String callback) {
        if (LogConfig.isDebug()) {
            model.addAttribute("debug",
                    "method=" + method + ", callback=" + callback);
        }
        if (method != null) {
            Class<? extends Method> methodClass = Method.getMethod(method);
            if (methodClass != null) {
                try {
                    this.method = Method.initMethod(methodClass);
                    return "redirect:" + callback;
                } catch (MissingSettingException e) {
                    model.addAttribute("info", "Missing settings to set up '" + methodClass.getSimpleName() + "'");
                    return this.loginForm(model,
                            method,
                            "/method?method=" + method + "&callback=" + callback);
                } catch (AuthenticationException e) {
                    model.addAttribute("error",
                            "Failed to authenticate into method '" + method + "'");
                    Method.clearCredentials(methodClass);
                    return this.loginForm(model, methodClass
                            .getSimpleName(),
                            "/method?method=" + method + "&callback=" + callback);
                } catch (NoSuchMethodException e) {
                    model.addAttribute("error", "Invalid method '" + method + "' provided");
                }
            } else {
                model.addAttribute("error", "Unsupported method class '" + method + "'");
            }
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        if (callback.contains("import")) {
            for (String currMethod : Method.getMethods(Import.class).keySet()) {
                options.put(currMethod, "/method?method=" + currMethod + "&callback=" + callback);
            }
        } else {
            for (String currMethod : Method.getMethods(Export.class).keySet()) {
                options.put(currMethod, "/method?method=" + currMethod + "&callback=" + callback);
            }
        }
        model.addAttribute("menuName", "Method Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/method/return");
        return "menu";
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
    @GetMapping("/method/login")
    public String loginForm(Model model,
            @RequestParam(value = "method", required = true) String method,
            @RequestParam(value = "callback", required = true) String callback) {

        if (LogConfig.isDebug()) {
            model.addAttribute("debug",
                    "method=" + method + ", callback=" + callback);
        }

        Class<? extends Method> methodClass = Method.getMethod(method);
        if (methodClass == null) {
            model.addAttribute("error", "Unsupported method provided");
            return methodMenu(model, null, callback);
        }

        LinkedHashMap<String, String> classCredentials = Settings.load().getGroup(methodClass);
        if (classCredentials == null || classCredentials.isEmpty()) {
            model.addAttribute("info", "No credentials required");
            return methodMenu(model, methodClass.getSimpleName(), callback);
        }
        LinkedHashMap<String, String> currentCredentials = new LinkedHashMap<>();
        Settings credentials = Settings.load();
        for (String name : classCredentials.keySet()) {
            String settingName = classCredentials.get(name);
            String value = "";
            if (!credentials.isEmpty(settingName)) {
                value = credentials.get(settingName).toString();
            }
            currentCredentials.put(name, value);
        }
        model.addAttribute("formName", methodClass.getSimpleName() + " Credentials");
        model.addAttribute("loginFields", currentCredentials);
        model.addAttribute("postAction", "/method/login?method=" + method);
        model.addAttribute("callback", callback);
        return "form";
    }

    /**
     * <p>
     * login.
     * </p>
     *
     * @param model           a {@link org.springframework.ui.Model} object
     * @param methodClassName a {@link java.lang.String} object
     * @param callback        a {@link java.lang.String} object
     * @param params          a {@link java.util.LinkedHashMap} object
     * @return a {@link java.lang.String} object
     */
    @PostMapping("/method/login")
    public String login(Model model,
            @RequestParam(value = "method", required = true) String method,
            @RequestParam(value = "callback", required = true) String callback,
            @RequestParam(required = false) LinkedHashMap<String, String> params) {

        Class<? extends Method> methodClass = Method.getMethod(method);
        if (methodClass == null) {
            model.addAttribute("error", "Invalid method '" + method + "' provided");
            return loginForm(model, method, callback);
        }
        Settings settings = Settings.load();
        LinkedHashMap<String, String> classCredentials = settings.getGroup(methodClass);

        if (params != null) {
            for (String name : classCredentials.keySet()) {
                String value = params.get(name);
                if (value == null || value.trim().isEmpty()) {
                    model.addAttribute("error",
                            "Missing value for: '" + name + "' for '" + methodClass.getSimpleName() + "'");
                    return loginForm(model, method, callback);
                }
                if (!settings.set(classCredentials.get(name), value)) {
                    model.addAttribute("error",
                            "Failed to set credential: '" + name + "' for '" + methodClass.getSimpleName() + "'");
                    return loginForm(model, method, callback);
                }
            }
        }
        return "redirect:" + callback;
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
            return methodMenu(model, null, "/method/import");
        }
        model.addAttribute("info", "Current method: " + Method.getMethodName(this.method));
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
        return process(model, "Importing '" + Method.getMethodName(this.method)
                + "' collection", "/method/import/collection", "/method/import");
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
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/process?processName=" + processName + "&processFunction="
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
     * importCollection.
     * </p>
     */
    @PostMapping("/method/import/collection")
    public void importCollection() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/import/collection");
            return;
        }
        try {
            try (ProgressBar pb = new ProgressBar("Import Collection", 3)) {
                pb.step("Liked Songs");
                method.uploadLikedSongs();
                pb.step("Albums");
                method.uploadAlbums();
                pb.step("Playlists");
                method.uploadPlaylists();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + method.getClass().getSimpleName() + "'collection");
        }
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
        return process(model, "Importing '" + Method.getMethodName(this.method) + "' liked songs",
                "/method/import/likedsongs", "/method/import");
    }

    /**
     * <p>
     * importLikedSongs.
     * </p>
     */
    @PostMapping("/method/import/likedsongs")
    public void importLikedSongs() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/import/likedsongs");
        }
        try {
            LikedSongs likedSongs = method.getLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + method.getClass().getSimpleName() + "'liked songs");
        }
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
        return process(model, "Importing '" + Method.getMethodName(this.method) + "' albums", "/method/import/albums",
                "/method/import");
    }

    /**
     * <p>
     * importAlbums.
     * </p>
     */
    @PostMapping("/method/import/albums")
    public void importAlbums() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/import/albums");
        }
        try {
            ArrayList<Album> albums = method.getAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + method.getClass().getSimpleName() + "'albums");
        }
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
        return process(model, "Importing '" + Method.getMethodName(this.method) + "' playlists",
                "/method/import/playlists", "/method/import");
    }

    /**
     * <p>
     * importPlaylists.
     * </p>
     */
    @PostMapping("/method/import/playlists")
    public void importPlaylists() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/import/playlists");
        }
        try {
            ArrayList<Playlist> playlists = method.getPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + method.getClass().getSimpleName() + "'playlists");
        }
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
            return methodMenu(model, null, "/method/export");
        }
        model.addAttribute("info", "Current method: " + Method.getMethodName(this.method));
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
        return process(model, "Exporting '" + Method.getMethodName(this.method) + "' collection",
                "/method/export/collection", "/method/export");
    }

    /**
     * <p>
     * exportCollection.
     * </p>
     */
    @PostMapping("/method/export/collection")
    public void exportCollection() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/export/collection");
        }
        try {
            try (ProgressBar pb = new ProgressBar("Export Collection", 3)) {
                pb.step("Liked Songs");
                method.uploadLikedSongs();
                pb.step("Albums");
                method.uploadAlbums();
                pb.step("Playlists");
                method.uploadPlaylists();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + method.getClass().getSimpleName() + "' collection");
        }
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
        return process(model, "Exporting '" + Method.getMethodName(this.method) + "' liked songs",
                "/method/export/likedsongs", "/method/export");
    }

    /**
     * <p>
     * exportLikedSongs.
     * </p>
     */
    @PostMapping("/method/export/likedsongs")
    public void exportLikedSongs() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/import/likedsongs");
        }
        try {
            method.uploadLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + method.getClass().getSimpleName() + "'liked songs");
        }
    }

    // TODO: export individual album
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
        return process(model, "Exporting '" + Method.getMethodName(this.method) + "' albums", "/method/export/albums",
                "/method/export");
    }

    /**
     * <p>
     * exportAlbums.
     * </p>
     */
    @PostMapping("/method/export/albums")
    public void exportAlbums() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/export/albums");
        }
        try {
            method.uploadAlbums();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + method.getClass().getSimpleName() + "'albums");
        }
    }

    // TODO: export individual playlist
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
        return process(model, "Exporting '" + Method.getMethodName(this.method) + "' playlists",
                "/method/export/playlists", "/method/export");
    }

    /**
     * <p>
     * exportPlaylists.
     * </p>
     */
    @PostMapping("/method/export/playlists")
    public void exportPlaylists() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/export/playlists");
        }
        try {
            method.uploadPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + method.getClass().getSimpleName() + "'playlists");
        }
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
            return methodMenu(model, null, "/method/sync");
        }
        model.addAttribute("info", "Current method: " + Method.getMethodName(this.method));
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
        return process(model, "Syncronizing '" + Method.getMethodName(this.method) + "' collection",
                "/method/sync/collection", "/method/sync");
    }

    /**
     * <p>
     * syncCollection.
     * </p>
     */
    @PostMapping("/method/sync/collection")
    public void syncCollection() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/sync/collection");
            return;
        }
        try {
            try (ProgressBar pb = new ProgressBar("Sync Collection", 3)) {
                pb.step("Liked Songs");
                method.syncLikedSongs();
                pb.step("Albums");
                method.syncAlbums();
                pb.step("Playlists");
                method.syncPlaylists();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getClass().getSimpleName() + "'collection");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + Method.getMethodName(this.method) + "' library");
        } catch (AuthenticationException e) {
            logger.error(
                    "Failed to Authenticate while syncronizing '" + Method.getMethodName(this.method) + "' library", e);
        }
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
        return process(model, "Syncronizing '" + Method.getMethodName(this.method) + "' liked songs",
                "/method/sync/likedsongs", "/method/sync");
    }

    /**
     * <p>
     * syncLikedSongs.
     * </p>
     */
    @PostMapping("/method/sync/likedsongs")
    public void syncLikedSongs() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/sync/likedsongs");
            return;
        }
        try {
            method.syncLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getClass().getSimpleName() + "'liked songs");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + Method.getMethodName(this.method) + "' library");
        } catch (AuthenticationException e) {
            logger.error(
                    "Failed to Authenticate while syncronizing '" + Method.getMethodName(this.method) + "' library", e);
        }
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
        return process(model, "Syncronizing '" + Method.getMethodName(this.method) + "' albums", "/method/sync/albums",
                "/method/sync");
    }

    /**
     * <p>
     * syncAlbums.
     * </p>
     */
    @PostMapping("/method/sync/albums")
    public void syncAlbums() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/sync/albums");
            return;
        }
        try {
            method.syncAlbums();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getClass().getSimpleName() + "'albums");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + Method.getMethodName(this.method) + "' library");
        } catch (AuthenticationException e) {
            logger.error(
                    "Failed to Authenticate while syncronizing '" + Method.getMethodName(this.method) + "' library", e);
        }
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
        return process(model, "Exporting '" + Method.getMethodName(this.method) + "' playlists",
                "/method/sync/playlists", "/method/sync");
    }

    /**
     * <p>
     * syncPlaylists.
     * </p>
     */
    @PostMapping("/method/sync/playlists")
    public void syncPlaylists() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/sync/playlists");
            return;
        }
        try {
            method.syncPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getClass().getSimpleName() + "'playlists");
        } catch (MissingSettingException e) {
            logger.warn("Missing credentials while syncronizing '" + Method.getMethodName(this.method) + "' library");
        } catch (AuthenticationException e) {
            logger.error(
                    "Failed to Authenticate while syncronizing '" + Method.getMethodName(this.method) + "' library", e);
        }
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
        ProgressBar pb = ProgressBar.getCurrentInstance();
        if (pb != null) {
            rootNode.put("title", pb.getTitle());
            rootNode.put("step", pb.getStep());
            rootNode.put("message", pb.getMessage());
            rootNode.put("maxstep", pb.getMaxStep());
            return ResponseEntity.ok(rootNode.toPrettyString());
        } else {
            rootNode.put("title", "");
            rootNode.put("step", 0);
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

        for (Entry<Level, String> entity : LogConfig.getLogs()) {
            ObjectNode logNode = mapper.createObjectNode();
            logNode.put("level", entity.getKey().toString().toUpperCase());
            logNode.put("message", entity.getValue());
            logsArray.add(logNode);
        }
        LogConfig.clearLogs();
        return ResponseEntity.ok(rootNode.toPrettyString());
    }

    /**
     * <p>
     * cancel.
     * </p>
     */
    @PostMapping("/method/cancel")
    public void cancel() {
        InterruptionHandler.forceInterruption();
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
