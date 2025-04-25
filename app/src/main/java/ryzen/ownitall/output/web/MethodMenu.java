package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.method.Method;
import ryzen.ownitall.method.MethodClass;
import ryzen.ownitall.util.Logs;

@Controller
public class MethodMenu {
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
            Class<? extends MethodClass> methodClass = Method.methods.get(methodClassName);
            if (methodClass != null) {
                try {
                    if (Method.isCredentialsEmpty(methodClass)) {
                        return this.login(model, methodClassName, callback, null);
                    } else {
                        this.method = new Method(methodClass);
                    }
                } catch (InterruptedException e) {
                    model.addAttribute("error", "Interrupted while setting up '" + methodClassName + "': " + e);
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
        options.put("Cancel", "/method/return");
        model.addAttribute("menuName", "Method Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/method/login")
    public String login(Model model,
            @RequestParam(value = "methodClass", required = true) String methodClassName,
            @RequestParam(value = "callback", required = true) String callback,
            @RequestParam(required = false) LinkedHashMap<String, String> params) {
        if (Logs.isDebug()) {
            model.addAttribute("debug",
                    "methodclass=" + methodClassName + ", callback=" + callback + ", params=" + params);
        }
        Class<? extends MethodClass> methodClass = Method.methods.get(methodClassName);
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
        if (params != null) {
            Credentials credentials = Credentials.load();
            for (String name : classCredentials.keySet()) {
                String value = params.get(name);
                if (value == null || value.trim().isEmpty()) {
                    model.addAttribute("error",
                            "Missing value for: '" + name + "' for '" + methodClassName + "'");
                    break;
                }
                if (!credentials.set(classCredentials.get(name), value)) {
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
        model.addAttribute("info", "Current method: " + this.method.getMethodName());
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Import Library", "/method/import/library");
        options.put("Import Liked Songs", "/method/import/likedsongs");
        options.put("Import Album(s)", "/method/import/album/choose");
        options.put("Import Playlist(s)", "/method/import/playlist/choose");
        options.put("Return", "/method/return");
        model.addAttribute("menuName", "Import Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/method/import/library")
    public String optionImportLibrary(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/import");
        }
        model.addAttribute("processName", "Importing '" + this.method.getMethodName() + "' music");
        model.addAttribute("redirect", "/method/import");
        // try (ProgressBar pb = new ProgressBar(Method.getMethodName() + " Import", 3))
        // {
        // pb.step("Liked Songs");
        // this.importLikedSongs();
        // pb.step("Saved Albums");
        // this.importAlbums();
        // pb.step("Playlists");
        // this.importPlaylists();
        // } catch (InterruptedException e) {
        // model.addAttribute("debug",
        // "Interrupted while importing '" + method.getClass().getSimpleName() + "'
        // music: " + e);
        // return importMenu(model);
        // }
        return "process";
    }

    @GetMapping("/method/import/likedsongs")
    public String optionImportLikedSongs() {
        // TODO: liked songs progress
        return "redirect:/collection";
    }

    @GetMapping("/method/import/album/choose")
    public String optionImportAlbumsMenu() {
        // TODO: albums menu
        return "redirect:/collection";
    }

    @GetMapping("/method/import/playlist/choose")
    public String optionImportPlaylistsMenu() {
        // TODO: playlist menu
        return "redirect:/collection";
    }

    // TODO: export menu
    @GetMapping("/method/export")
    public String exportMenu(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/export");
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Return", "/method/return");
        model.addAttribute("menuName", "Export Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/method/sync")
    public String sync(Model model) {
        if (this.method == null) {
            model.addAttribute("error", "Method was not initialized");
            return methodMenu(model, null, "/method/sync");
        }
        // TODO: sync
        return "redirect:/collection";
    }

    @GetMapping("/method/return")
    public String optionReturn(Model model) {
        if (this.method != null) {
            // reset so they can change method as they back out (its persistent due to
            // springboot)
            this.method = null;
        }
        return "redirect:/collection";
    }
}
