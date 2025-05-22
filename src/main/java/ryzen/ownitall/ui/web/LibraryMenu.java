package ryzen.ownitall.ui.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;
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
    private static final Logger logger = new Logger(LibraryMenu.class);

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
            try {
                Library.initLibrary(libraryClass);
                return libraryMenu(model);
            } catch (MissingSettingException e) {
                logger.info(model, "Missing settings to set up library '" + libraryClass.getSimpleName() + "'");
                return loginForm(model, library, "/library/change?library=" + library);
            } catch (AuthenticationException e) {
                logger.info(model,
                        "Authentication exception setting up library '" + libraryClass.getSimpleName()
                                + "', retrying...");
                Library.clearCredentials(libraryClass);
                return loginForm(model, library, "/library/change?library=" + library);
            } catch (NoSuchMethodException e) {
                logger.error(model, "Unsupported library type '" + library + "'", e);
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
        Class<? extends Library> libraryClass = Library.libraries.get(library);
        if (libraryClass == null) {
            logger.warn(model, "Unsupported library '" + library + "'' provided");
            return optionChange(model, null);
        }
        Settings settings = Settings.load();
        LinkedHashMap<String, String> classCredentials = settings.getGroup(libraryClass);
        if (classCredentials == null || classCredentials.isEmpty()) {
            logger.info(model, "No credentials required");
            return optionChange(model, libraryClass.getSimpleName());
        }
        LinkedHashMap<String, String> currentCredentials = new LinkedHashMap<>();
        for (String name : classCredentials.keySet()) {
            String settingName = classCredentials.get(name);
            String value = "";
            if (!settings.isEmpty(settingName)) {
                value = settings.get(settingName).toString();
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
            logger.warn(model, "invalid library '" + library + "' provided");
            return loginForm(model, library, callback);
        }
        Settings settings = Settings.load();
        LinkedHashMap<String, String> classCredentials = settings.getGroup(libraryClass);

        if (params != null) {
            for (String name : classCredentials.keySet()) {
                String value = params.get(name);
                if (value == null || value.trim().isEmpty()) {
                    logger.warn(model, "Missing value for: '" + name + "' for '" + libraryClass.getSimpleName() + "'");
                    return loginForm(model, library, callback);
                }
                try {
                    settings.set(classCredentials.get(name), value);
                    logger.info(model, "Successfully changed setting '" + name + "'");
                } catch (NoSuchFieldException e) {
                    logger.error(model,
                            "Failed to set credential: '" + name + "' for '" + libraryClass.getSimpleName() + "'", e);
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
        logger.info(model, "Successfully cleared library cache");
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
        logger.info(model, "There currently are '" + size + "' cache entries");
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
