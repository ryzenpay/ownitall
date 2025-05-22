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
    private static final Logger logger = new Logger(SettingsMenu.class);

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
        logger.info(model, "Successfully saved");
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
    // TODO: hardencode predefined options as a dropdown
    // also in library and method /login
    // ^ point them to this
    @GetMapping("/settings/change")
    public String changeSettingForm(Model model, @RequestParam(value = "choice", required = false) String choice) {
        Settings settings = Settings.load();
        if (choice != null) {
            LinkedHashMap<String, String> fields = new LinkedHashMap<>();
            // allows semi colon seperated list
            for (String currChoice : choice.split(";")) {
                if (settings.isEmpty(currChoice)) {
                    fields.put(currChoice, "");
                } else {
                    fields.put(currChoice, settings.get(currChoice).toString());
                }
            }
            model.addAttribute("formName", "Change Setting(s)");
            model.addAttribute("loginFields", fields);
            model.addAttribute("postAction", "/settings/change?choice=" + choice);
            model.addAttribute("callback", "/settings/change");
            return "form";
        }
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        for (String name : settings.getAll().keySet()) {
            String value;
            if (settings.isEmpty(name)) {
                value = "";
            } else {
                if (settings.isSecret(name)) {
                    value = settings.getHashedValue(name);
                } else {
                    value = settings.get(name).toString();
                }
            }
            options.put(name + ": " + value, "/settings/change?choice=" + name);
        }
        model.addAttribute("menuName", "Choose Setting Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/settings");
        return "menu";
    }

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
            @RequestParam(required = true) LinkedHashMap<String, String> params) {
        Settings settings = Settings.load();
        for (String name : params.keySet()) {
            if (settings.exists(name)) {
                try {
                    settings.set(name, params.get(name));
                } catch (NoSuchFieldException e) {
                    logger.warn(model, "Failed to set setting '" + name + "'");
                    break;
                }
            }
        }
        logger.info(model, "Successfully updated Settings");
        return "redirect:" + callback;
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
        logger.info(model, "Successfully reset");
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
