package ryzen.ownitall.output.web;

import java.io.File;
import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Credentials;
import ryzen.ownitall.Storage;
import ryzen.ownitall.library.Library;

@Controller
public class ToolsMenu {

    @GetMapping("/tools")
    public String menu(Model model, @RequestParam(value = "notification", required = false) String notification) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Archive", "/tools/archive");
        options.put("Unarchive", "/tools/unarchive/choose");
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

    @PostMapping("/tools/archive")
    public String optionArchive() {
        Storage.load().archive();
        return "redirect:/tools?notification=Successfully archived";
    }

    @PostMapping("/tools/unarchive/choose")
    public String optionUnArchive(Model model) {
        Storage storage = Storage.load();
        LinkedHashMap<String, String> archiveFoldersMap = new LinkedHashMap<>();
        for (File file : storage.getArchiveFolders()) {
            archiveFoldersMap.put(file.getName(), file.getAbsolutePath());
        }
        model.addAttribute("archiveFolders", archiveFoldersMap);
        return "unarchiving";
    }

    @PostMapping("/tools/unarchive")
    public String unarchive(@RequestParam("folderPath") String folderPath) {
        Storage.load().unArchive(new File(folderPath));
        return "redirect:/tools?notification=Successfully unarchived";
    }

    @PostMapping("/tools/clearcache")
    public String optionClearCache() {
        Library.load().clear();
        return "redirect:/tools?notification=Successfully cleared cache";
    }

    @PostMapping("/tools/clearcredentials")
    public String optionClearCredentials() {
        Credentials.load().clear();
        return "redirect:/tools?notification=Successfully cleared credentials";
    }

    @PostMapping("/tools/return")
    public String optionReturn() {
        return "redirect:/";
    }
}
