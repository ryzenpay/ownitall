package ryzen.ownitall.ui.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Settings;

/**
 * <p>
 * SettingsMenu class.
 * </p>
 *
 * @author ryzen
 */
@Controller
public class SettingsMenu {

    /**
     * <p>
     * settingsMenu.
     * </p>
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
     * <p>
     * optionSave.
     * </p>
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
     * <p>
     * optionChange.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    // TODO: implement options
    // also in library and method /login
    @GetMapping("/settings/change")
    public String changeSettingForm(Model model) {
        model.addAttribute("formName", "Change Settings");
        model.addAttribute("loginFields", Settings.load().getAll(true));
        model.addAttribute("postAction", "/settings/change");
        model.addAttribute("callback", "/settings");
        return "form";
    }

    // TODO: hardencode predefined options as a dropdown
    /**
     * <p>
     * login.
     * </p>
     *
     * @param model    a {@link org.springframework.ui.Model} object
     * @param callback a {@link java.lang.String} object
     * @param params   a {@link java.util.LinkedHashMap} object
     * @return a {@link java.lang.String} object
     */
    @PostMapping("/settings/change")
    public String login(Model model,
            @RequestParam(value = "callback", required = true) String callback,
            @RequestParam(required = false) LinkedHashMap<String, String> params) {

        if (params != null) {
            Settings settings = Settings.load();
            for (String name : params.keySet()) {
                if (!settings.set(name, params.get(name))) {
                    model.addAttribute("warn", "Failed to set setting '" + name + "'");
                }
            }
            model.addAttribute("info", "Successfully updated Settings");
        }
        return settingsMenu(model);
    }

    /**
     * <p>
     * optionReset.
     * </p>
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
     * <p>
     * optionReturn.
     * </p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/settings/return")
    public String optionReturn(Model model) {
        return "redirect:/";
    }
}
