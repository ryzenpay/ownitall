package ryzen.ownitall.output.web;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.methods.Method;

@Controller
public class MethodMenu {
    private static final Logger logger = LogManager.getLogger(MethodMenu.class);
    private Method method;
    private String methodName;

    @GetMapping("/method")
    public String methodMenu(Model model, @RequestParam(value = "method", required = false) String method,
            @RequestParam(value = "callback", required = true) String callback,
            @RequestParam(value = "notification", required = false) String notification) {
        if (method != null) {
            try {
                this.login(model, method, callback);
                this.setMethod(method);
            } catch (InterruptedException e) {
                notification = "Interrupted while setting up '" + method + "': " + e;
            }
        }
        if (this.method != null) {
            return "/method/" + callback;
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        for (String currMethod : Method.methods.keySet()) {
            options.put(currMethod, "/method?method=" + currMethod + "?callback=" + callback);
        }
        options.put("Cancel", "/method/" + callback);
        model.addAttribute("menuName", "Import Menu");
        model.addAttribute("menuOptions", options);
        if (notification != null) {
            model.addAttribute("notification", notification);
        }
        return "menu";
    }

    // TODO: get credentials as parameters and attempt to sign in
    @GetMapping("/method/login")
    public String login(Model model, @RequestParam(value = "method", required = true) String method,
            @RequestParam(value = "callback", required = false) String callback) {
        if (this.method == null) {
            LinkedHashSet<String> fields = new LinkedHashSet<>();
            // TODO: set all credential fields
            fields.add("Cancel");
            model.addAttribute("loginName", method);
            model.addAttribute("loginFields", fields);
            return "login";
        }
        if (this.method != null) {
            return methodMenu(model, method, callback, "Successfully signed into " + method);
        } else {
            return methodMenu(model, method, callback, "failed to sign into " + method);
        }
    }

    private void setMethod(String choice) throws InterruptedException {
        Class<? extends Method> methodClass = Method.methods.get(choice);
        try {
            this.method = methodClass.getDeclaredConstructor().newInstance();
            this.methodName = method.getClass().getSimpleName();
        } catch (InstantiationException e) {
            logger.debug("Interrupted while setting up method '" + choice + "'");
            throw new InterruptedException(e.getMessage());
        } catch (IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
            logger.error("Exception instantiating method '" + choice + "'", e);
            throw new InterruptedException(e.getMessage());
        }
    }

    @GetMapping("/collection/import")
    public String importMenu(Model model,
            @RequestParam(value = "notification", required = false) String notification) {
        if (this.method == null) {
            return methodMenu(model, null, "import", null);
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Import Library", "/collection/import/library");
        options.put("Import Liked Songs", "/collection/import/likedsongs");
        options.put("Import Album(s)", "/collection/import/album/choose");
        options.put("Import Playlist(s)", "/collection/import/playlist/choose");
        options.put("Return", "/method/return");
        model.addAttribute("menuName", "Import Menu");
        model.addAttribute("menuOptions", options);
        if (notification != null) {
            model.addAttribute("notification", notification);
        }
        return "menu";
    }

    @GetMapping("/collection/import/library")
    public String optionImportLibrary() {
        // TODO: library progress
        return "redirect:/collection/method/import";
    }

    @GetMapping("/collection/import/likedsongs")
    public String optionImportLikedSongs() {
        // TODO: liked songs progress
        return "redirect:/collection/method/import";
    }

    @GetMapping("/collection/import/album/choose")
    public String optionImportAlbumsMenu() {
        // TODO: albums menu
        return "redirect:/collection/method/import";
    }

    @GetMapping("/collection/import/playlist/choose")
    public String optionImportPlaylistsMenu() {
        // TODO: playlist menu
        return "redirect:/collection/method/import";
    }

    // TODO: export menu
    @GetMapping("/collection/export")
    public String exportMenu(Model model,
            @RequestParam(value = "notification", required = false) String notification) {
        if (this.method == null) {
            return methodMenu(model, null, "export", null);
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Return", "/method/return");
        model.addAttribute("menuName", "Export Menu");
        model.addAttribute("menuOptions", options);
        if (notification != null) {
            model.addAttribute("notification", notification);
        }
        return "menu";
    }

    @GetMapping("/collection/sync")
    public String sync(Model model) {
        if (this.method == null) {
            return methodMenu(model, null, "sync", null);
        }
        // TODO: sync
        return "redirect:/collection";
    }

    @GetMapping("/method/return")
    public String optionReturn(Model model) {
        return CollectionMenu.collectionMenu(model, null);
    }
}
