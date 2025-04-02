package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ryzen.ownitall.Settings;

public class SettingsMenu {

    @GetMapping("/settings")
    public static String menu(Model model,
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

    @PostMapping("/settings/save")
    public static String optionSave() {
        Settings.load().save();
        return "redirect:/settings?notification=Successfully saved";
    }

    @PostMapping("/settings/change")
    public static String optionChange() {
        // TODO: change setting menu
        // look at unarchive
        return "redirect:/settings";
    }

    @PostMapping("/settings/reset")
    public static String optionReset() {
        Settings.load().clear();
        return "redirect:/settings?notification=Successfully reset";
    }

    @PostMapping("/settings/return")
    public static String optionReturn(Model model) {
        return MainMenu.menu(model, null);
    }
}
