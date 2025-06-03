package ryzen.ownitall.ui.web;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/library/change")
    public String optionChange(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        for (Class<? extends Library> libraryClass : Library.getLibraries()) {
            options.put(libraryClass.getSimpleName(), "/library/change/" + libraryClass.getName());
        }
        model.addAttribute("menuName", "Library Options");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/library");
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
    @GetMapping("/library/change/{library}")
    public String optionChange(Model model, @PathVariable(value = "library") String library) {
        Class<? extends Library> libraryClass = Library.getLibrary(library);
        try {
            Library.initLibrary(libraryClass);
            Settings.libraryType = library;
            logger.info(model, "Successfully changed library to '" + libraryClass.getSimpleName() + "'");
            return libraryMenu(model);
        } catch (MissingSettingException e) {
            logger.warn(model, "Missing settings to set up library '" + libraryClass.getSimpleName() + "': "
                    + e.getMessage());
            return loginForm(model, library, "/library/change/" + library);
        } catch (AuthenticationException e) {
            logger.warn(model,
                    "Authentication exception setting up library '" + libraryClass.getSimpleName()
                            + "': " + e.getMessage());
            Library.clearCredentials(libraryClass);
            return loginForm(model, library, "/library/change/" + library);
        } catch (NoSuchMethodException e) {
            logger.error(model, "Unsupported library type '" + library + "'", e);
        }
        return optionChange(model);
    }

    /**
     * <p>
     * loginForm.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     * @param library a {@link java.lang.String} object
     */
    @GetMapping("/library/login/{library}")
    public String loginForm(Model model,
            @PathVariable(value = "library") String library,
            @RequestParam(value = "callback", required = true) String callback) {
        Class<? extends Library> libraryClass = Library.getLibrary(library);
        if (libraryClass == null) {
            logger.warn(model, "Unsupported library '" + library + "'' provided");
            return optionChange(model, null);
        }
        Settings settings = Settings.load();
        LinkedHashSet<String> credentials = settings.getGroup(libraryClass);
        if (credentials == null || credentials.isEmpty()) {
            logger.info(model, "Library '" + libraryClass.getSimpleName() + "' does not have credentials");
            return "redirect:" + callback;
        }
        return SettingsMenu.changeSettingForm(model, credentials, callback);
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
        int size = Library.getAlbumCacheSize() + Library.getArtistCacheSize() + Library.getIdCacheSize()
                + Library.getSongCacheSize();
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
