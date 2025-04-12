package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.methods.Method;

@Controller
public class MethodMenu {
    private static final Credentials credentials = Credentials.load();
    private Method method;
    private String methodName;

    @GetMapping("/method")
    public String methodMenu(Model model,
            @RequestParam(value = "methodClass", required = false) String methodClassName,
            @RequestParam(value = "callback", required = true) String callback) {
        if (methodClassName != null) {
            Class<? extends Method> methodClass = Method.methods.get(methodClassName);
            if (methodClass != null) {
                try {
                    if (Method.isCredentialsEmpty(methodClass)) {
                        return this.loginForm(model, methodClassName, callback);
                    } else {
                        this.method = Method.load(methodClass);
                        this.methodName = method.getClass().getSimpleName();
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
            String uri = UriComponentsBuilder.fromPath("/method")
                    .queryParam("methodClassName", currMethod)
                    .queryParam("callback", callback)
                    .toUriString();
            options.put(currMethod, uri);
        }
        options.put("Cancel", callback);
        model.addAttribute("menuName", "Import Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/method/login")
    public String loginForm(Model model,
            @RequestParam(value = "methodClass", required = true) String methodClassName,
            @RequestParam(value = "callback", required = true) String callback) {
        Class<? extends Method> methodClass = Method.methods.get(methodClassName);
        if (methodClass == null) {
            model.addAttribute("error", "Unsupported method provided");
            return methodMenu(model, methodClassName, callback);
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
        model.addAttribute("loginName", methodClass.getSimpleName());
        model.addAttribute("loginFields", classCredentials.keySet());
        model.addAttribute("methodClass", methodClass.getName());
        model.addAttribute("callback", callback);
        return "login";
    }

    @PostMapping("/method/login")
    public String login(Model model, HttpServletRequest request,
            @RequestParam(value = "methodClass", required = true) String methodClassName,
            @RequestParam(value = "callback", required = true) String callback) {
        Class<? extends Method> methodClass = Method.methods.get(methodClassName);
        if (methodClass == null) {
            model.addAttribute("error", "Unsupported method '" + methodClassName + "' provided in login");
            return methodMenu(model, null, callback);
        }
        LinkedHashMap<String, String> classCredentials = Method.credentialGroups.get(methodClass);

        for (Map.Entry<String, String> entry : classCredentials.entrySet()) {
            String value = request.getParameter(entry.getKey());
            if (value == null || value.trim().isEmpty()) {
                model.addAttribute("error", "Missing value for: " + entry.getKey());
                return loginForm(model, methodClassName, callback);
            }
            if (!credentials.change(entry.getValue(), value)) {
                model.addAttribute("error", "Failed to set credential: " + entry.getKey());
                return loginForm(model, methodClassName, callback);
            }
        }

        if (Method.isCredentialsEmpty(methodClass)) {
            model.addAttribute("error", "Failed to set credentials");
            return loginForm(model, methodClassName, callback);
        }
        model.addAttribute("info", "Successfully signed in");
        return methodMenu(model, methodClassName, callback);
    }

    @GetMapping("/collection/import")
    public String importMenu(Model model) {
        if (this.method == null) {
            return methodMenu(model, null, "/collection/import");
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Import Library", "/collection/import/library");
        options.put("Import Liked Songs", "/collection/import/likedsongs");
        options.put("Import Album(s)", "/collection/import/album/choose");
        options.put("Import Playlist(s)", "/collection/import/playlist/choose");
        options.put("Return", "/method/return");
        model.addAttribute("menuName", "Import Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/collection/import/library")
    public String optionImportLibrary() {
        // TODO: library progress
        return "redirect:/collection";
    }

    @GetMapping("/collection/import/likedsongs")
    public String optionImportLikedSongs() {
        // TODO: liked songs progress
        return "redirect:/collection";
    }

    @GetMapping("/collection/import/album/choose")
    public String optionImportAlbumsMenu() {
        // TODO: albums menu
        return "redirect:/collection";
    }

    @GetMapping("/collection/import/playlist/choose")
    public String optionImportPlaylistsMenu() {
        // TODO: playlist menu
        return "redirect:/collection";
    }

    // TODO: export menu
    @GetMapping("/collection/export")
    public String exportMenu(Model model) {
        if (this.method == null) {
            return methodMenu(model, null, "/collection/export");
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Return", "/method/return");
        model.addAttribute("menuName", "Export Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/collection/sync")
    public String sync(Model model) {
        if (this.method == null) {
            return methodMenu(model, null, "/collection/sync");
        }
        // TODO: sync
        return "redirect:/collection";
    }

    @GetMapping("/method/return")
    public String optionReturn(Model model) {
        return "redirect:/collection";
    }
}
