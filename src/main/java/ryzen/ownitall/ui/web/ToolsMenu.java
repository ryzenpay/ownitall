package ryzen.ownitall.ui.web;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Storage;
import ryzen.ownitall.method.Method;

/**
 * <p>
 * ToolsMenu class.
 * </p>
 *
 * @author ryzen
 */
@Controller
public class ToolsMenu {
    private static final Logger logger = new Logger(ToolsMenu.class);

    /**
     * <p>
     * toolsMenu.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/tools")
    public String toolsMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Archive", "/tools/archive");
        options.put("Unarchive", "/tools/unarchive");
        options.put("Library", "/library");
        options.put("Reset Credentials", "/tools/clearcredentials");
        model.addAttribute("menuName", "Tools Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/tools/return");
        return "menu";
    }

    /**
     * <p>
     * optionArchive.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/tools/archive")
    public String optionArchive(Model model) {
        Storage.archive();
        logger.info(model, "Successfully archived");
        return toolsMenu(model);
    }

    /**
     * <p>
     * optionUnArchive.
     * </p>
     *
     * @param model      a {@link org.springframework.ui.Model} object
     * @param folderPath a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
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
                    logger.error(model, "Exception converting file path", e);
                }
            }
            model.addAttribute("menuName", "Choose Folder to Unarchive");
            model.addAttribute("menuOptions", options);
            model.addAttribute("callback", "/tools");
            return "menu";
        } else if (folderPath.equals("Exit")) {
            return toolsMenu(model);
        } else {
            Storage.unArchive(new File(folderPath));
            logger.info(model, "Successfully unarchived '" + folderPath + "'");
            return toolsMenu(model);
        }
    }

    /**
     * <p>
     * optionClearCredentials.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/tools/clearcredentials")
    public String optionClearCredentials(Model model) {
        for (Class<? extends Method> methodClass : Method.getMethods().values()) {
            Method.clearCredentials(methodClass);
        }
        logger.info(model, "Successfully cleared credentials");
        return toolsMenu(model);
    }

    /**
     * <p>
     * optionReturn.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/tools/return")
    public String optionReturn(Model model) {
        return "redirect:/";
    }
}
