package ryzen.ownitall.output.web;

import java.io.File;
import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.Storage;
import ryzen.ownitall.library.Library;

@Controller
public class ToolsMenu {

    @GetMapping("/tools")
    public static String toolsMenu(Model model,
            @RequestParam(value = "notification", required = false) String notification) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Archive", "/tools/archive");
        options.put("Unarchive", "/tools/unarchive");
        options.put("Clear Cache", "/tools/clearcache");
        options.put("Reset Credentials", "/tools/clearcredentials");
        options.put("Return", "/tools/return");
        model.addAttribute("menuName", "Tools Menu");
        model.addAttribute("menuOptions", options);
        if (notification != null) {
            model.addAttribute("notification", notification);
        }
        return "menu";
    }

    @GetMapping("/tools/archive")
    public static String optionArchive(Model model) {
        Storage.load().archive();
        return toolsMenu(model, "Successfully archived");
    }

    @GetMapping("/tools/unarchive")
    public static String optionUnArchive(Model model,
            @RequestParam(value = "folderPath", required = false) String folderPath) {
        if (folderPath == null) {
            Storage storage = Storage.load();
            LinkedHashMap<String, String> options = new LinkedHashMap<>();
            for (File file : storage.getArchiveFolders()) {
                options.put(file.getName(), file.getAbsolutePath());
            }
            options.put("Exit", "Exit");
            model.addAttribute("options", options);
            return "unarchive";
        } else if (folderPath.equals("Exit")) {
            return toolsMenu(model, null);
        } else {
            Storage.load().unArchive(new File(folderPath));
            return toolsMenu(model, "Successfully unarchived");
        }
    }

    @GetMapping("/tools/clearcache")
    public static String optionClearCache(Model model) {
        Library.clear();
        return toolsMenu(model, "Successfully cleared cache");
    }

    @GetMapping("/tools/clearcredentials")
    public static String optionClearCredentials(Model model) {
        Credentials.load().clear();
        return toolsMenu(model, "Successfully cleared credentials");
    }

    @GetMapping("/tools/return")
    public static String optionReturn(Model model) {
        return MainMenu.mainMenu(model, null);
    }
}
