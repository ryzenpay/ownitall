package ryzen.ownitall.ui.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;
import ryzen.ownitall.util.LogConfig;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>
 * LibraryMenu class.
 * </p>
 *
 * @author ryzen
 */
@Controller
public class LibraryMenu {

    /**
     * <p>
     * libraryMenu.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
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

    /**
     * <p>
     * optionChange.
     * </p>
     *
     * @param model   a {@link org.springframework.ui.Model} object
     * @param library a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/library/change")
    public String optionChange(Model model, @RequestParam(value = "library", required = false) String library) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        if (library != null) {
            Class<? extends Library> libraryClass = Library.libraries.get(library);
            while (true) {
                try {
                    Library.initLibrary(libraryClass);
                    return libraryMenu(model);
                } catch (MissingSettingException e) {
                    model.addAttribute("info",
                            "Missing settings to set up library '" + libraryClass.getSimpleName() + "'");
                    return loginForm(model, library, "/library/change?library=" + library);
                } catch (AuthenticationException e) {
                    model.addAttribute("info",
                            "Authentication exception setting up library '" + libraryClass.getSimpleName()
                                    + "', retrying...");
                    Library.clearCredentials(libraryClass);
                    return loginForm(model, library, "/library/change?library=" + library);
                } catch (NoSuchMethodException e) {
                    model.addAttribute("error", "Error: Unsupported library type '" + library + "'");
                }
            }
        }
        for (String currLibrary : Library.libraries.keySet()) {
            options.put(currLibrary, "/library/change?library=" + currLibrary);
        }
        model.addAttribute("menuName", "Library Options");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/library");
        return "menu";
    }

    /**
     * <p>
     * loginForm.
     * </p>
     *
     * @param model            a {@link org.springframework.ui.Model} object
     * @param libraryClassName a {@link java.lang.String} object
     * @param callback         a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/library/login")
    public String loginForm(Model model,
            @RequestParam(value = "library", required = true) String library,
            @RequestParam(value = "callback", required = true) String callback) {

        if (LogConfig.isDebug()) {
            model.addAttribute("debug",
                    "library=" + library + ", callback=" + callback);
        }

        Class<? extends Library> libraryClass = Library.libraries.get(library);
        if (libraryClass == null) {
            model.addAttribute("error", "Unsupported library provided");
            return optionChange(model, null);
        }
        LinkedHashMap<String, String> classCredentials = Settings.load().getGroup(libraryClass);
        if (classCredentials == null || classCredentials.isEmpty()) {
            model.addAttribute("info", "No credentials required");
            return optionChange(model, libraryClass.getSimpleName());
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
        model.addAttribute("formName", libraryClass.getSimpleName() + " Credentials");
        model.addAttribute("loginFields", currentCredentials);
        model.addAttribute("postAction", "/library/login?library=" + library);
        model.addAttribute("callback", callback);
        return "login";
    }

    /**
     * [POST] library login
     *
     * @param model            - model
     * @param libraryClassName - library class
     * @param callback         - callback
     * @param params           - all parameters where credentials will be pulled
     *                         from
     * @return - successful or retry login
     */
    @PostMapping("/library/login")
    public String login(Model model,
            @RequestParam(value = "library", required = true) String library,
            @RequestParam(value = "callback", required = true) String callback,
            @RequestParam(required = false) LinkedHashMap<String, String> params) {

        Class<? extends Library> libraryClass = Library.libraries.get(library);
        if (libraryClass == null) {
            model.addAttribute("error", "invalid library '" + library + "' provided");
            return loginForm(model, library, callback);
        }
        LinkedHashMap<String, String> classCredentials = Settings.load().getGroup(libraryClass);

        if (params != null) {
            Settings settings = Settings.load();
            for (String name : classCredentials.keySet()) {
                String value = params.get(name);
                if (value == null || value.trim().isEmpty()) {
                    model.addAttribute("error",
                            "Missing value for: '" + name + "' for '" + libraryClass.getSimpleName() + "'");
                    return loginForm(model, library, callback);
                }
                if (!settings.set(classCredentials.get(name), value)) {
                    model.addAttribute("error",
                            "Failed to set credential: '" + name + "' for '" + libraryClass.getSimpleName() + "'");
                    return loginForm(model, library, callback);
                }
            }
        }
        return "redirect:" + callback;
    }

    /**
     * <p>
     * optionClearCache.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/library/cache/clear")
    public String optionClearCache(Model model) {
        Library.clear();
        model.addAttribute("info", "Successfully cleared library cache");
        return libraryMenu(model);
    }

    /**
     * <p>
     * optionCache.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/library/cache")
    public String optionCache(Model model) {
        int size = Library.getCacheSize();
        model.addAttribute("info", "There currently are '" + size + "' cache entries");
        return libraryMenu(model);
    }

    /**
     * <p>
     * optionReturn.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/library/return")
    public String optionReturn(Model model) {
        return "redirect:/tools";
    }
}
