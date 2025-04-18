package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;

public class LibraryMenu {
    private static final Logger logger = LogManager.getLogger();

    @GetMapping("/library")
    public static String libraryMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Change Provider", "/library/change");
        options.put("Clear Cache", "/library/cache/clear");
        options.put("Cache Size", "/library/cache");
        options.put("Return", "/library/return");
        model.addAttribute("menuName", "Library Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/library/change")
    public static String optionChange(Model model, @RequestParam(value = "library", required = false) String library) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        if (library != null) {
            Class<? extends Library> libraryClass = Library.libraries.get(library);
            if (libraryClass != null) {
                Settings.load().change("libraryType", libraryClass);
                if (Library.isCredentialsEmpty(libraryClass)) {
                    // TODO: prompt
                    // setCredentials(libraryClass);
                }
                logger.info("Successfully changed library type to '" + library + "'");
            } else {
                model.addAttribute("error", "Error: Unsupported library type '" + library + "'");
                return libraryMenu(model);
            }
        }
        for (String currLibrary : Library.libraries.keySet()) {
            options.put(currLibrary, "/library/change?library=" + currLibrary);
        }
        return "menu";
    }

    @GetMapping("/library/cache/clear")
    public static String optionClearCache(Model model) {
        Library.clear();
        model.addAttribute("info", "Successfully cleared library cache");
        return libraryMenu(model);
    }

    @GetMapping("/library/cache")
    public static String optionCache(Model model) {
        int size = Library.getCacheSize();
        model.addAttribute("info", "There currently are '" + size + "' cache entries");
        return libraryMenu(model);
    }

    @GetMapping("/library/return")
    public static String optionReturn(Model model) {
        return "redirect:/tools";
    }
}
