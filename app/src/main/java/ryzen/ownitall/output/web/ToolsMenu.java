package ryzen.ownitall.output.web;

import java.io.File;
import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.Storage;

@Controller
public class ToolsMenu {

    @GetMapping("/tools")
    public static String toolsMenu(Model model,
            @RequestParam(value = "notification", required = false) String notification) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Archive", "/tools/archive");
        options.put("Unarchive", "/tools/unarchive");
        options.put("Library", "/library");
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
        Storage.archive();
        return toolsMenu(model, "Successfully archived");
    }

    @GetMapping("/tools/unarchive")
    public static String optionUnArchive(Model model,
            @RequestParam(value = "folderPath", required = false) String folderPath) {
        if (folderPath == null) {
            LinkedHashMap<String, String> options = new LinkedHashMap<>();
            for (File file : Storage.getArchiveFolders()) {
                options.put(file.getName(), file.getAbsolutePath());
            }
            options.put("Exit", "Exit");
            model.addAttribute("options", options);
            return "unarchive";
        } else if (folderPath.equals("Exit")) {
            return toolsMenu(model, null);
        } else {
            Storage.unArchive(new File(folderPath));
            return toolsMenu(model, "Successfully unarchived");
        }
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
