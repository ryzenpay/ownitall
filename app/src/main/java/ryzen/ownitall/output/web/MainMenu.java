package ryzen.ownitall.output.web;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;

@Controller
@SpringBootApplication
public class MainMenu {

    @GetMapping("/")
    private String showMenu() {
        return "mainmenu";
    }

    @PostMapping("/collection")
    public String optionCollection() {
        // TODO: collection menu
        return "redirect:/"; // Redirect back to main menu
    }

    @PostMapping("/save")
    public String optionSave() {
        Settings settings = Settings.load();
        Collection.load().save();
        settings.save();
        if (settings.isSaveCredentials()) {
            Credentials.load().save();
        }
        if (Library.checkInstance()) {
            Library.load(); // caches in the load
        }
        return "redirect:/";
    }

    @PostMapping("/tools")
    public String optionTools() {
        // TODO: tools menu
        return "redirect:/";
    }

    @PostMapping("/settings")
    public String optionSettings() {
        // TODO: change settings menu
        return "redirect:/";
    }

    @PostMapping("/exit")
    public String optionExit() {
        optionSave();
        return "exit"; // Redirect to an exit page or handle exit logic
    }
}
