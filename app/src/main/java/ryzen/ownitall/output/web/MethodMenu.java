package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.methods.Method;

@Controller
@SessionAttributes({ "method" })
public class MethodMenu {
    private static final Credentials credentials = Credentials.load();

    @ModelAttribute("method")
    public Method setMethod() {
        return null;
    }

    // TODO: sessionattribute method is not persistant :(
    @GetMapping("/method")
    public String methodMenu(Model model,
            @RequestParam(value = "methodClass", required = false) String methodClassName,
            @RequestParam(value = "callback", required = true) String callback,
            @ModelAttribute(value = "method") Method method) {
        if (methodClassName != null) {
            Class<? extends Method> methodClass = Method.methods.get(methodClassName);
            if (methodClass != null) {
                try {
                    if (Method.isCredentialsEmpty(methodClass)) {
                        return this.loginForm(model, methodClassName, callback, method);
                    } else {
                        method = Method.load(methodClass);
                        model.addAttribute("method", method);
                    }
                } catch (InterruptedException e) {
                    model.addAttribute("error", "Interrupted while setting up '" + methodClassName + "': " + e);
                }
            } else {
                model.addAttribute("error", "Unsupported method class '" + methodClassName + "'");
            }
        }
        if (method != null) {
            return callback;
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        for (String currMethod : Method.methods.keySet()) {
            options.put(currMethod, "/method?methodClassName=" + currMethod + "&callback=" + callback);
        }
        options.put("Cancel", "/collection");
        model.addAttribute("menuName", "Method Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/method/login")
    public String loginForm(Model model,
            @RequestParam(value = "methodClass", required = true) String methodClassName,
            @RequestParam(value = "callback", required = true) String callback,
            @ModelAttribute("method") Method method) {
        Class<? extends Method> methodClass = Method.methods.get(methodClassName);
        if (methodClass == null) {
            model.addAttribute("error", "Unsupported method provided");
            return methodMenu(model, methodClassName, callback, method);
        }
        if (!Method.isCredentialsEmpty(methodClass)) {
            model.addAttribute("info", "Found existing credentials");
            return methodMenu(model, methodClassName, callback, method);
        }

        LinkedHashMap<String, String> classCredentials = Method.credentialGroups.get(methodClass);
        if (classCredentials == null || classCredentials.isEmpty()) {
            model.addAttribute("info", "No credentials required");
            return methodMenu(model, methodClassName, callback, method);
        }
        model.addAttribute("loginName", methodClass.getSimpleName());
        model.addAttribute("loginFields", classCredentials.keySet());
        model.addAttribute("methodClass", methodClassName);
        model.addAttribute("callback", callback);
        return "login";
    }

    @PostMapping("/method/login")
    public String login(Model model, HttpServletRequest request,
            @RequestParam(value = "methodClass", required = true) String methodClassName,
            @RequestParam(value = "callback", required = true) String callback,
            @ModelAttribute("method") Method method) {
        Class<? extends Method> methodClass = Method.methods.get(methodClassName);
        if (methodClass == null) {
            model.addAttribute("error", "Unsupported method '" + methodClassName + "' provided in login");
            return methodMenu(model, null, callback, method);
        }
        LinkedHashMap<String, String> classCredentials = Method.credentialGroups.get(methodClass);

        for (Map.Entry<String, String> entry : classCredentials.entrySet()) {
            String value = request.getParameter(entry.getKey());
            if (value == null || value.trim().isEmpty()) {
                model.addAttribute("error", "Missing value for: " + entry.getKey());
                return loginForm(model, methodClassName, callback, method);
            }
            if (!credentials.change(entry.getValue(), value)) {
                model.addAttribute("error", "Failed to set credential: " + entry.getKey());
                return loginForm(model, methodClassName, callback, method);
            }
        }

        if (Method.isCredentialsEmpty(methodClass)) {
            model.addAttribute("error", "Failed to set credentials");
            return loginForm(model, methodClassName, callback, method);
        }
        model.addAttribute("info", "Successfully signed in");
        return methodMenu(model, methodClassName, callback, method);
    }

    @GetMapping("/collection/import")
    public String importMenu(Model model, @ModelAttribute("method") Method method) {
        if (method == null) {
            return methodMenu(model, null, "/collection/import", method);
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
    public String exportMenu(Model model,
            @ModelAttribute("method") Method method) {
        if (method == null) {
            return methodMenu(model, null, "/collection/export", method);
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Return", "/method/return");
        model.addAttribute("menuName", "Export Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/collection/sync")
    public String sync(Model model,
            @ModelAttribute("method") Method method) {
        if (method == null) {
            return methodMenu(model, null, "/collection/sync", method);
        }
        // TODO: sync
        return "redirect:/collection";
    }

    @GetMapping("/method/return")
    public String optionReturn(Model model) {
        return "redirect:/collection";
    }
}
