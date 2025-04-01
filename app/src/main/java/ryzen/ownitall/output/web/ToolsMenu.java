package ryzen.ownitall.output.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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
    public String optionArhive() {
        // TODO: archive menu
        Storage.load().archive(true);
        return "redirect:/tools";
    }

    @PostMapping("/tools/unarchive")
    public String optionUnArchive() {
        // TODO: unarchive menu
        Storage.load().unArchive();
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
