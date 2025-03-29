package ryzen.ownitall.output.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import ryzen.ownitall.Collection;
import ryzen.ownitall.Credentials;
import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;
import java.awt.Desktop;

@Controller
@SpringBootApplication
public class MainMenu {
    private static final Logger logger = LogManager.getLogger(MainMenu.class);

    public static void load(String[] args) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:8080"));
            } catch (IOException | URISyntaxException e) {
                logger.error("Exception opening web browser: " + e);
            }
        } else {
            logger.info("Unable to open web browser automatically, navigate to 'http://localhost:8080'");
        }
        SpringApplication.run(MainMenu.class, args);
    }

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
        return "saving";
    }

    @PostMapping("/saving")
    public String saving() {
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

    private void save() {
        Settings settings = Settings.load();
        Collection.load().save();
        settings.save();
        if (settings.isSaveCredentials()) {
            Credentials.load().save();
        }
        if (Library.checkInstance()) {
            Library.load(); // caches in the load
        }
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
        return "exiting";
    }

    @PostMapping("/exiting")
    public String exit() {
        save();
        return "redirect:/";
    }
}
