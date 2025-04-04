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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Main;

@Controller
@SpringBootApplication
public class MainMenu {
    private static final Logger logger = LogManager.getLogger();
    public static final String url = "http://localhost:8080";

    public static void main(String[] args) {
        logger.info("Starting up, browser will open soon");
        SpringApplication.run(MainMenu.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    private static void openBrowser() {
        System.setProperty("java.awt.headless", "false");
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                logger.error("Exception opening web browser", e);
            }
        } else {
            logger.info("Unable to open web browser automatically. Please navigate to " + url);
        }
    }

    @GetMapping("/")
    public static String mainMenu(Model model,
            @RequestParam(value = "notification", required = false) String notification) {
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

    @GetMapping("/save")
    public static String optionSave() {
        Main.save();
        return "redirect:/?notification=Successfully saved";
    }

    @GetMapping("/exit")
    public static String optionExit() {
        logger.info("Exiting program...");
        Main.save();
        return "exit";
    }
}