package ryzen.ownitall.output.web;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    public String toolsMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Archive", "/tools/archive");
        options.put("Unarchive", "/tools/unarchive");
        options.put("Library", "/library");
        options.put("Reset Credentials", "/tools/clearcredentials");
        options.put("Return", "/tools/return");
        model.addAttribute("menuName", "Tools Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/tools/archive")
    public String optionArchive(Model model) {
        Storage.archive();
        model.addAttribute("info", "Successfully archived");
        return toolsMenu(model);
    }

    @GetMapping("/tools/unarchive")
    public String optionUnArchive(Model model,
            @RequestParam(value = "folderPath", required = false) String folderPath) {
        if (folderPath == null) {
            LinkedHashMap<String, String> options = new LinkedHashMap<>();
            for (File file : Storage.getArchiveFolders()) {
                try {
                    String path = URLEncoder.encode(file.getAbsolutePath(), StandardCharsets.UTF_8.toString());
                    options.put(file.getName(), "/tools/unarchive?folderPath=" + path);
                } catch (UnsupportedEncodingException e) {
                    model.addAttribute("error", "Exception converting file path: " + e);
                }
            }
            options.put("Cancel", "/tools");
            model.addAttribute("menuName", "Choose Folder to Unarchive");
            model.addAttribute("menuOptions", options);
            return "menu";
        } else if (folderPath.equals("Exit")) {
            return toolsMenu(model);
        } else {
            Storage.unArchive(new File(folderPath));
            model.addAttribute("info", "Successfully unarchived '" + folderPath + "'");
            return toolsMenu(model);
        }
    }

    @GetMapping("/tools/clearcredentials")
    public String optionClearCredentials(Model model) {
        Credentials.load().clear();
        model.addAttribute("info", "Successfully cleared credentials");
        return toolsMenu(model);
    }

    @GetMapping("/tools/return")
    public String optionReturn(Model model) {
        return "redirect:/";
    }
}
