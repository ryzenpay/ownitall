package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.Logs;

@Controller
public class LibraryMenu {

    @GetMapping("/library")
    public String libraryMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Change Provider", "/library/change");
        options.put("Clear Cache", "/library/cache/clear");
        options.put("Cache Size", "/library/cache");
        model.addAttribute("menuName", "Library Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/library/return");
        return "menu";
    }

    @GetMapping("/library/change")
    public String optionChange(Model model, @RequestParam(value = "library", required = false) String library) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        if (library != null) {
            Class<? extends Library> libraryClass = Library.libraries.get(library);
            if (libraryClass != null) {
                Settings.load().set("libraryType", libraryClass);
                if (Library.isCredentialsEmpty(libraryClass)) {
                    return loginForm(model, library, "/library/change");
                }
                model.addAttribute("info", "Successfully changed library type to '" + library + "'");
            } else {
                model.addAttribute("error", "Error: Unsupported library type '" + library + "'");
            }
            return libraryMenu(model);
        }
        for (String currLibrary : Library.libraries.keySet()) {
            options.put(currLibrary, "/library/change?library=" + currLibrary);
        }
        model.addAttribute("menuName", "Library Options");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/library");
        return "menu";
    }

    @GetMapping("/library/login")
    public String loginForm(Model model,
            @RequestParam(value = "methodClass", required = true) String libraryClassName,
            @RequestParam(value = "callback", required = true) String callback) {

        if (Logs.isDebug()) {
            model.addAttribute("debug",
                    "libraryclass=" + libraryClassName + ", callback=" + callback);
        }

        Class<? extends Library> libraryClass = Library.libraries.get(libraryClassName);
        if (libraryClass == null) {
            model.addAttribute("error", "Unsupported library provided");
            return optionChange(model, null);
        }

        if (!Library.isCredentialsEmpty(libraryClass)) {
            model.addAttribute("info", "Found existing credentials");
            return optionChange(model, libraryClassName);
        }

        LinkedHashMap<String, String> classCredentials = Library.credentialGroups.get(libraryClass);
        if (classCredentials == null || classCredentials.isEmpty()) {
            model.addAttribute("info", "No credentials required");
            return optionChange(model, libraryClassName);
        }

        model.addAttribute("loginName", libraryClass.getSimpleName());
        model.addAttribute("loginFields", classCredentials.keySet());
        model.addAttribute("methodClass", libraryClassName);
        model.addAttribute("callback", callback);

        return "login";
    }

    @PostMapping("/library/login")
    public String login(Model model,
            @RequestParam(value = "methodClass", required = true) String libraryClassName,
            @RequestParam(value = "callback", required = true) String callback,
            @RequestParam(required = false) LinkedHashMap<String, String> params) {

        Class<? extends Library> libraryClass = Library.libraries.get(libraryClassName);
        LinkedHashMap<String, String> classCredentials = Library.credentialGroups.get(libraryClass);

        if (params != null) {
            Credentials credentials = Credentials.load();
            for (String name : classCredentials.keySet()) {
                String value = params.get(name);
                if (value == null || value.trim().isEmpty()) {
                    model.addAttribute("error",
                            "Missing value for: '" + name + "' for '" + libraryClassName + "'");
                    break;
                }
                if (!credentials.set(classCredentials.get(name), value)) {
                    model.addAttribute("error",
                            "Failed to set credential: '" + name + "' for '" + libraryClassName + "'");
                    break;
                }
            }
            if (Library.isCredentialsEmpty(libraryClass)) {
                model.addAttribute("error", "Missing credentials for '" + libraryClassName + "'");
            } else {
                model.addAttribute("info", "Successfully signed into '" + libraryClassName + "'");
                return optionChange(model, libraryClassName);
            }
        }

        model.addAttribute("loginName", libraryClass.getSimpleName());
        model.addAttribute("loginFields", classCredentials.keySet());
        model.addAttribute("methodClass", libraryClassName);
        model.addAttribute("callback", callback);

        return "login";
    }

    @GetMapping("/library/cache/clear")
    public String optionClearCache(Model model) {
        Library.clear();
        model.addAttribute("info", "Successfully cleared library cache");
        return libraryMenu(model);
    }

    @GetMapping("/library/cache")
    public String optionCache(Model model) {
        int size = Library.getCacheSize();
        model.addAttribute("info", "There currently are '" + size + "' cache entries");
        return libraryMenu(model);
    }

    @GetMapping("/library/return")
    public String optionReturn(Model model) {
        return "redirect:/tools";
    }
}
