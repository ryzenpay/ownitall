package ryzen.ownitall.ui.web;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
        new Storage().archive();
        logger.info(model, "Successfully archived");
        return toolsMenu(model);
    }

    /**
     * <p>unarchiveMenu.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/tools/unarchive")
    public String unarchiveMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        for (File file : new Storage().getArchiveFolders()) {
            try {
                String path = URLEncoder.encode(file.getAbsolutePath(), StandardCharsets.UTF_8.toString());
                options.put(file.getName(), "/tools/unarchive/" + URLEncoder.encode(path, StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException e) {
                logger.error(model, "Exception converting file path", e);
            }
        }
        model.addAttribute("menuName", "Choose Folder to Unarchive");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/tools");
        return "menu";
    }

    /**
     * <p>
     * optionUnArchive.
     * </p>
     *
     * @param model      a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     * @param path a {@link java.lang.String} object
     */
    @GetMapping("/tools/unarchive/{path}")
    public String unarchive(Model model,
            @PathVariable(value = "path") String path) {
        path = URLDecoder.decode(path, StandardCharsets.UTF_8);
        new Storage().unArchive(new File(path));
        logger.info(model, "Successfully unarchived '" + path + "'");
        return toolsMenu(model);
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
        for (Class<?> methodClass : Method.getMethods()) {
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
