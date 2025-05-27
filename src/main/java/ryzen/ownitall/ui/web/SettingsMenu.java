package ryzen.ownitall.ui.web;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
        options.put("Change Setting", "/settings/change?callback=/settings");
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
    @GetMapping("/settings/change")
    public static String changeSettingForm(Model model,
            @RequestParam(value = "choices", required = false) LinkedHashSet<String> choices,
            @RequestParam(value = "callback", required = true) String callback) {
        boolean required = true;
        LinkedHashSet<FormVariable> fields = new LinkedHashSet<>();
        // allows semi colon seperated list
        Settings settings = Settings.load();
        if (choices == null) {
            choices = new LinkedHashSet<>(settings.getAll().keySet());
            required = false;
        }
        for (String setting : choices) {
            FormVariable field = new FormVariable(setting);
            field.setName(settings.getName(setting));
            if (!settings.isEmpty(setting)) {
                field.setValue(settings.get(setting));
            }
            if (settings.getOptions(setting) != null) {
                field.setOptions(settings.getOptions(setting));
            }
            field.setSecret(settings.isSecret(setting));
            field.setRequired(required);
            fields.add(field);
        }
        model.addAttribute("formName", "Change Setting(s)");
        model.addAttribute("values", fields);
        model.addAttribute("postUrl", "/settings/change");
        model.addAttribute("callback", callback);
        return "form";
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
    @ResponseBody
    public ResponseEntity<String> login(Model model,
            @RequestBody LinkedHashMap<String, String> variables) {
        Settings settings = Settings.load();
        for (String setting : variables.keySet()) {
            if (setting == null || setting.isEmpty()) {
                continue;
            }
            String value = variables.get(setting);
            if (value != null) {
                try {
                    settings.set(setting, value);
                    logger.debug(model, "Updated '" + setting + "' setting");
                } catch (NoSuchFieldException e) {
                    logger.error(model, "Failed to set setting '" + setting + "'", e);
                    return ResponseEntity.badRequest().body("Failed to set setting '" + setting + "'");
                }
            } else {
                logger.warn(model, "Missing parameter for setting '" + setting + "'");
                return ResponseEntity.badRequest().body("Missing parameter for setting '" + setting + "'");
            }
        }
        logger.info(model, "Successfully updated Settings");
        return ResponseEntity.ok().body("Successfully updated Settings");
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
