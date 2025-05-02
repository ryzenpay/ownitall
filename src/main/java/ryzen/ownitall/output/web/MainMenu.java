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
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ryzen.ownitall.Main;

/**
 * <p>MainMenu class.</p>
 *
 * @author ryzen
 */
@Controller
@SpringBootApplication
public class MainMenu {
    private static final Logger logger = LogManager.getLogger(MainMenu.class);
    /** Constant <code>url="http://localhost:8080"</code> */
    public static final String url = "http://localhost:8080";

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects
     */
    public static void main(String[] args) {
        logger.info("Starting up, browser will open soon");
        SpringApplication application = new SpringApplication(MainMenu.class);
        application.run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    private void openBrowser() {
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

    @EventListener(ContextClosedEvent.class)
    private void exit() {
        Main.save();
    }

    /**
     * <p>mainMenu.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/")
    public String mainMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("collection", "/collection");
        options.put("save", "/save");
        options.put("tools", "/tools");
        options.put("settings", "/settings");
        model.addAttribute("menuName", "Main Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/exit");
        return "menu";
    }

    /**
     * <p>optionSave.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/save")
    public String optionSave(Model model) {
        Main.save();
        model.addAttribute("info", "Successfully saved");
        return mainMenu(model);
    }

    /**
     * <p>optionExit.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/exit")
    public String optionExit() {
        logger.info("Exiting program...");
        // saving is handled by exit()
        return "exit";
    }
}
