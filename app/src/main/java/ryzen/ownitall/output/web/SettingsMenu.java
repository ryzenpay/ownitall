package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Settings;

@Controller
public class SettingsMenu {

    @GetMapping("/settings")
    public static String settingsMenu(Model model,
            @RequestParam(value = "notification", required = false) String notification) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Save Settings", "/settings/save");
        options.put("Change Setting", "/settings/change");
        options.put("Clear Cache", "/settings/reset");
        options.put("Return", "/settings/return");
        model.addAttribute("menuName", "Settings Menu");
        model.addAttribute("menuOptions", options);
        if (notification != null) {
            model.addAttribute("notification", notification);
        }
        return "menu";
    }

    @GetMapping("/settings/save")
    public static String optionSave(Model model) {
        Settings.load().save();
        return settingsMenu(model, "Successfully saved");
    }

    @GetMapping("/settings/change")
    public static String optionChange() {
        // TODO: change setting menu
        // look at unarchive
        return "redirect:/settings";
    }

    @GetMapping("/settings/reset")
    public static String optionReset(Model model) {
        Settings.load().clear();
        return settingsMenu(model, "Successfully reset");
    }

    @GetMapping("/settings/return")
    public static String optionReturn(Model model) {
        return MainMenu.mainMenu(model, null);
    }
}
