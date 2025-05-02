package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ryzen.ownitall.Settings;

/**
 * <p>SettingsMenu class.</p>
 *
 * @author ryzen
 */
@Controller
public class SettingsMenu {

    /**
     * <p>settingsMenu.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/settings")
    public String settingsMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Save Settings", "/settings/save");
        options.put("Change Setting", "/settings/change");
        options.put("Clear Cache", "/settings/reset");
        model.addAttribute("menuName", "Settings Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/settings/return");
        return "menu";
    }

    /**
     * <p>optionSave.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/settings/save")
    public String optionSave(Model model) {
        Settings.load().save();
        model.addAttribute("info", "Successfully saved");
        return settingsMenu(model);
    }

    /**
     * <p>optionChange.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/settings/change")
    public String optionChange(Model model) {
        // TODO: change setting menu
        // look at method setCredentials
        return settingsMenu(model);
    }

    /**
     * <p>optionReset.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/settings/reset")
    public String optionReset(Model model) {
        Settings.load().clear();
        model.addAttribute("info", "Successfully reset");
        return settingsMenu(model);
    }

    /**
     * <p>optionReturn.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/settings/return")
    public String optionReturn(Model model) {
        return "redirect:/";
    }
}
