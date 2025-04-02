package ryzen.ownitall.output.web;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Main;

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
    public String menu(Model model, @RequestParam(value = "notification", required = false) String notification) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("collection", "/collection");
        options.put("save", "/save");
        options.put("tools", "/tools");
        options.put("settings", "/settings");
        options.put("exit", "/exit");
        model.addAttribute("menuName", "Main Menu");
        model.addAttribute("menuOptions", options);
        if (notification != null) {
            model.addAttribute("notification", notification);
        }
        return "menu";
    }

    @PostMapping("/collection")
    public String optionCollection(Model model) {
        return new CollectionMenu().menu(model, null);
    }

    @PostMapping("/save")
    public String optionSave() {
        Main.save();
        return "redirect:/?notification=Successfully saved";
    }

    @PostMapping("/tools")
    public String optionTools(Model model) {
        return new ToolsMenu().menu(model, null);
    }

    @PostMapping("/settings")
    public String optionSettings(Model model) {
        return new SettingsMenu().menu(model, null);
    }

    @PostMapping("/exit")
    public String optionExit() {
        logger.info("Exiting program...");
        Main.save();
        return "exit";
    }
}