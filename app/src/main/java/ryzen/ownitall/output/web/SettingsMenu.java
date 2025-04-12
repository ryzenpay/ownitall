package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ryzen.ownitall.Settings;

@Controller
public class SettingsMenu {

    @GetMapping("/settings")
    public static String settingsMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Save Settings", "/settings/save");
        options.put("Change Setting", "/settings/change");
        options.put("Clear Cache", "/settings/reset");
        options.put("Return", "/settings/return");
        model.addAttribute("menuName", "Settings Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/settings/save")
    public static String optionSave(Model model) {
        Settings.load().save();
        model.addAttribute("info", "Successfully saved");
        return settingsMenu(model);
    }

    @GetMapping("/settings/change")
    public static String optionChange(Model model) {
        // TODO: change setting menu
        // look at method setCredentials
        return settingsMenu(model);
    }

    @GetMapping("/settings/reset")
    public static String optionReset(Model model) {
        Settings.load().clear();
        model.addAttribute("info", "Successfully reset");
        return settingsMenu(model);
    }

    @GetMapping("/settings/return")
    public static String optionReturn(Model model) {
        return "redirect:/";
    }
}
