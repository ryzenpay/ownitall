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
    public String showMenu() {
        return "toolsmenu";
    }

    @PostMapping("/tools/archive")
    public String optionArchive() {
        Storage.load().archive();
        return "redirect:/tools";
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
        return "redirect:/tools";
    }

    @PostMapping("/tools/clearcache")
    public String optionClearCache() {
        Library.load().clear();
        return "redirect:/tools";
    }

    @PostMapping("/tools/clearcredentials")
    public String optionClearCredentials() {
        Credentials.load().clear();
        return "redirect:/tools";
    }

    @PostMapping("/tools/return")
    public String optionReturn() {
        return "redirect:/";
    }
}
