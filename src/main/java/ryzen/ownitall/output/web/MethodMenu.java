package ryzen.ownitall.output.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Settings;
import ryzen.ownitall.classes.Album;
import ryzen.ownitall.classes.LikedSongs;
import ryzen.ownitall.classes.Playlist;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.util.Logs;

@Controller
public class MethodMenu {
    private static final Logger logger = LogManager.getLogger(MethodMenu.class);
    private Method method;

    @GetMapping("/method")
    public String methodMenu(Model model,
            @RequestParam(value = "methodClass", required = false) String methodClassName,
            @RequestParam(value = "callback", required = true) String callback) {
        if (Logs.isDebug()) {
            model.addAttribute("debug",
                    "methodclass=" + methodClassName + ", callback=" + callback);
        }
        if (methodClassName != null) {
            Class<? extends Method> methodClass = Method.methods.get(methodClassName);
            if (methodClass != null) {
                try {
                    if (Method.isCredentialsEmpty(methodClass)) {
                        return this.loginForm(model, methodClassName, callback);
                    } else {
                        this.method = Method.initMethod(methodClass);
                    }
                } catch (InterruptedException e) {
                    model.addAttribute("error", "Interrupted while setting up '" + methodClassName + "': " + e);
                    if (Method.clearCredentials(methodClass)) {
                        return this.loginForm(model, methodClassName, callback);
                    } else {
                        model.addAttribute("error", "Interrupted while setting up '" + methodClassName
                                + ", and unable to clear credentials");
                    }
                }
            } else {
                model.addAttribute("error", "Unsupported method class '" + methodClassName + "'");
            }
        }
        if (this.method != null) {
            return "redirect:" + callback;
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        for (String currMethod : Method.methods.keySet()) {
            options.put(currMethod, "/method?methodClass=" + currMethod + "&callback=" + callback);
        }
        model.addAttribute("menuName", "Method Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/method/return");
        return "menu";
    }

    @GetMapping("/method/login")
    public String loginForm(Model model,
            @RequestParam(value = "methodClass", required = true) String methodClassName,
            @RequestParam(value = "callback", required = true) String callback) {

        if (Logs.isDebug()) {
            model.addAttribute("debug",
                    "methodclass=" + methodClassName + ", callback=" + callback);
        }

        Class<? extends Method> methodClass = Method.methods.get(methodClassName);
        if (methodClass == null) {
            model.addAttribute("error", "Unsupported method provided");
            return methodMenu(model, null, callback);
        }

        if (!Method.isCredentialsEmpty(methodClass)) {
            model.addAttribute("info", "Found existing credentials");
            return methodMenu(model, methodClassName, callback);
        }

        LinkedHashMap<String, String> classCredentials = Method.credentialGroups.get(methodClass);
        if (classCredentials == null || classCredentials.isEmpty()) {
            model.addAttribute("info", "No credentials required");
            return methodMenu(model, methodClassName, callback);
        }
        LinkedHashMap<String, String> currentCredentials = new LinkedHashMap<>();
        Settings settings = Settings.load();
        for (String name : classCredentials.keySet()) {
            currentCredentials.put(name, settings.get(classCredentials.get(name)).toString());
        }
        model.addAttribute("loginName", methodClass.getSimpleName());
        model.addAttribute("loginFields", currentCredentials);
        model.addAttribute("methodClass", methodClassName);
        model.addAttribute("callback", callback);
        return "login";
    }

    @PostMapping("/method/login")
    public String login(Model model,
            @RequestParam(value = "methodClass", required = true) String methodClassName,
            @RequestParam(value = "callback", required = true) String callback,
            @RequestParam(required = false) LinkedHashMap<String, String> params) {

        Class<? extends Method> methodClass = Method.methods.get(methodClassName);
        LinkedHashMap<String, String> classCredentials = Method.credentialGroups.get(methodClass);

        if (params != null) {
            Settings settings = Settings.load();
            for (String name : classCredentials.keySet()) {
                String value = params.get(name);
                if (value == null || value.trim().isEmpty()) {
                    model.addAttribute("error",
                            "Missing value for: '" + name + "' for '" + methodClassName + "'");
                    break;
                }
                if (!settings.set(classCredentials.get(name), value)) {
                    model.addAttribute("error",
                            "Failed to set credential: '" + name + "' for '" + methodClassName + "'");
                    break;
                }
            }
            if (Method.isCredentialsEmpty(methodClass)) {
                model.addAttribute("error", "Missing credentials for '" + methodClassName + "'");
            } else {
                model.addAttribute("info", "Successfully signed into '" + methodClassName + "'");
                return methodMenu(model, methodClassName, callback);
            }
        }

        model.addAttribute("loginName", methodClass.getSimpleName());
        model.addAttribute("loginFields", classCredentials.keySet());
        model.addAttribute("methodClass", methodClassName);
        model.addAttribute("callback", callback);

        return "login";
    }

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

    @GetMapping("/method/import/collection")
    public String optionImportCollection(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/import");
        }
        model.addAttribute("processName", "Importing '" + Method.getMethodName(this.method) + "' collection");
        model.addAttribute("processFunction", "/method/import/collection");
        model.addAttribute("redirect", "/method/import");
        return "process";
    }

    @PostMapping("/method/import/collection")
    public void importCollection() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/import/collection");
            return;
        }
        try {
            method.uploadLikedSongs();
            method.uploadAlbums();
            method.uploadPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + method.getClass().getSimpleName() + "'collection: ", e);
        }
    }

    @GetMapping("/method/import/likedsongs")
    public String optionImportLikedSongs(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/import");
        }
        model.addAttribute("processName", "Importing '" + Method.getMethodName(this.method) + "' liked songs");
        model.addAttribute("processFunction", "/method/import/likedsongs");
        model.addAttribute("redirect", "/method/import");
        return "process";
    }

    @PostMapping("/method/import/likedsongs")
    // TODO: use response int
    public int importLikedSongs() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/import/likedsongs");
            return -1;
        }
        try {
            LikedSongs likedSongs = method.getLikedSongs();
            if (likedSongs != null) {
                Collection.addLikedSongs(likedSongs);
                return likedSongs.size();
            }
            return 0;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + method.getClass().getSimpleName() + "'liked songs: ", e);
            return -1;
        }
    }

    // TODO: import individual album
    @GetMapping("/method/import/albums")
    public String optionImportAlbums(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/import");
        }
        model.addAttribute("processName", "Importing '" + Method.getMethodName(this.method) + "' albums");
        model.addAttribute("processFunction", "/method/import/albums");
        model.addAttribute("redirect", "/method/import");
        return "process";
    }

    @PostMapping("/method/import/albums")
    public int importAlbums() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/import/albums");
            return -1;
        }
        try {
            ArrayList<Album> albums = method.getAlbums();
            if (albums != null) {
                Collection.addAlbums(albums);
                return albums.size();
            }
            return 0;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + method.getClass().getSimpleName() + "'albums: ", e);
            return -1;
        }
    }

    // TODO: import individual playlist
    @GetMapping("/method/import/playlists")
    public String optionImportPlaylists(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/import");
        }
        model.addAttribute("processName", "Importing '" + Method.getMethodName(this.method) + "' playlists");
        model.addAttribute("processFunction", "/method/import/playlists");
        model.addAttribute("redirect", "/method/import");
        return "process";
    }

    @PostMapping("/method/import/playlists")
    public int importPlaylists() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/import/playlists");
            return -1;
        }
        try {
            ArrayList<Playlist> playlists = method.getPlaylists();
            if (playlists != null) {
                Collection.addPlaylists(playlists);
                return playlists.size();
            }
            return 0;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while importing '" + method.getClass().getSimpleName() + "'playlists: ", e);
            return -1;
        }
    }

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

    @GetMapping("/method/export/collection")
    public String optionExportCollection(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/export");
        }
        model.addAttribute("processName", "Exporting '" + Method.getMethodName(this.method) + "' collection");
        model.addAttribute("processFunction", "/method/export/collection");
        model.addAttribute("redirect", "/method/export");
        return "process";
    }

    @PostMapping("/method/export/collection")
    public boolean exportCollection() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/export/collection");
            return false;
        }
        try {
            method.uploadLikedSongs();
            method.uploadAlbums();
            method.uploadPlaylists();
            return true;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + method.getClass().getSimpleName() + "'collection: ", e);
            return false;
        }
    }

    @GetMapping("/method/export/likedsongs")
    public String optionExportLikedSongs(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/export");
        }
        model.addAttribute("processName", "Exporting '" + Method.getMethodName(this.method) + "' liked songs");
        model.addAttribute("processFunction", "/method/export/likedsongs");
        model.addAttribute("redirect", "/method/export");
        return "process";
    }

    @PostMapping("/method/export/likedsongs")
    public boolean exportLikedSongs() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/import/likedsongs");
            return false;
        }
        try {
            method.uploadLikedSongs();
            return true;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + method.getClass().getSimpleName() + "'liked songs: ", e);
            return false;
        }
    }

    // TODO: export individual album
    @GetMapping("/method/export/albums")
    public String optionExportAlbums(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/export");
        }
        model.addAttribute("processName", "Exporting '" + Method.getMethodName(this.method) + "' albums");
        model.addAttribute("processFunction", "/method/export/albums");
        model.addAttribute("redirect", "/method/export");
        return "process";
    }

    @PostMapping("/method/export/albums")
    public boolean exportAlbums() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/export/albums");
            return false;
        }
        try {
            method.uploadAlbums();
            return true;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + method.getClass().getSimpleName() + "'albums: ", e);
            return false;
        }
    }

    // TODO: export individual playlist
    @GetMapping("/method/export/playlists")
    public String optionExportPlaylists(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/export");
        }
        model.addAttribute("processName", "Exporting '" + Method.getMethodName(this.method) + "' playlists");
        model.addAttribute("processFunction", "/method/export/playlists");
        model.addAttribute("redirect", "/method/export");
        return "process";
    }

    @PostMapping("/method/export/playlists")
    public boolean exportPlaylists() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/export/playlists");
            return false;
        }
        try {
            method.uploadPlaylists();
            return true;
        } catch (InterruptedException e) {
            logger.debug("Interrupted while exporting '" + method.getClass().getSimpleName() + "'playlists: ", e);
            return false;
        }
    }

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

    @GetMapping("/method/sync/collection")
    public String optionSyncCollection(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/sync");
        }
        model.addAttribute("processName", "Syncronizing '" + Method.getMethodName(this.method) + "' collection");
        model.addAttribute("processFunction", "/method/sync/collection");
        model.addAttribute("redirect", "/method/sync");
        return "process";
    }

    @PostMapping("/method/sync/collection")
    public void syncCollection() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/sync/collection");
            return;
        }
        try {
            method.syncLikedSongs();
            method.syncAlbums();
            method.syncPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getClass().getSimpleName() + "'collection: ", e);
        }
    }

    @GetMapping("/method/sync/likedsongs")
    public String optionSyncLikedSongs(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/sync");
        }
        model.addAttribute("processName", "Syncronizing '" + Method.getMethodName(this.method) + "' liked songs");
        model.addAttribute("processFunction", "/method/sync/likedsongs");
        model.addAttribute("redirect", "/method/sync");
        return "process";
    }

    @PostMapping("/method/sync/likedsongs")
    public void syncLikedSongs() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/sync/likedsongs");
            return;
        }
        try {
            method.syncLikedSongs();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getClass().getSimpleName() + "'liked songs: ", e);
        }
    }

    @GetMapping("/method/sync/albums")
    public String optionSyncAlbums(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/sync");
        }
        model.addAttribute("processName", "Syncronizing '" + Method.getMethodName(this.method) + "' albums");
        model.addAttribute("processFunction", "/method/sync/albums");
        model.addAttribute("redirect", "/method/sync");
        return "process";
    }

    @PostMapping("/method/sync/albums")
    public void syncAlbums() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/sync/albums");
            return;
        }
        try {
            method.syncAlbums();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getClass().getSimpleName() + "'albums: ", e);
        }
    }

    @GetMapping("/method/sync/playlists")
    public String optionSyncPlaylists(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/sync");
        }
        model.addAttribute("processName", "Exporting '" + Method.getMethodName(this.method) + "' playlists");
        model.addAttribute("processFunction", "/method/sync/playlists");
        model.addAttribute("redirect", "/method/sync");
        return "process";
    }

    @PostMapping("/method/sync/playlists")
    public void syncPlaylists() {
        if (this.method == null) {
            logger.debug("method was not initialized before /method/sync/playlists");
            return;
        }
        try {
            method.syncPlaylists();
        } catch (InterruptedException e) {
            logger.debug("Interrupted while syncronizing '" + method.getClass().getSimpleName() + "'playlists: ", e);
        }
    }

    @GetMapping("/method/return")
    public String optionReturn() {
        if (this.method != null) {
            // reset so they can change method as they back out (its persistent due to
            // springboot)
            this.method = null;
        }
        return "redirect:/collection";
    }
}
