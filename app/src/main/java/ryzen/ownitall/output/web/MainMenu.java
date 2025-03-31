package ryzen.ownitall.output.web;

import java.awt.Desktop;
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
import ryzen.ownitall.Main;
import ryzen.ownitall.Settings;
import ryzen.ownitall.library.Library;

@Controller
@SpringBootApplication
public class MainMenu {
    private static final Logger logger = LogManager.getLogger(MainMenu.class);

    public static void main(String[] args) {
        // for some reason desktop bricks after springapplication is started
        openBrowser("http://localhost:8080");
        SpringApplication.run(MainMenu.class, args);
    }

    private static void openBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                logger.error("Exception opening web browser: " + e);
            }
        } else {
            logger.info("Unable to open web browser automatically. Please navigate to " + url);
        }
    }

    @GetMapping("/")
    public String showMenu() {
        return "mainmenu/index";
    }

    @PostMapping("/collection")
    public String optionCollection() {
        // TODO: collection menu
        return "collectionmenu/index";
    }

    @PostMapping("/save")
    public String optionSave() {
        save();
        return "redirect:/";
    }

    private void save() {
        Main.save();
    }

    @PostMapping("/tools")
    public String optionTools() {
        return "toolsmenu/index";
    }

    @PostMapping("/settings")
    public String optionSettings() {
        // TODO: settings menu
        return "settingsmenu/index";
    }

    @PostMapping("/exit")
    public String optionExit() {
        save();
        return "exit";
    }
}