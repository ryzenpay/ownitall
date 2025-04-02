package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class MethodMenu {

    @GetMapping("/collection/method/choose")
    public static String menu() {
        // TODO: option menu
        // enforce it before any of the other options are set
        return "menu";
    }

    // TODO: import menu
    @GetMapping("/collection/method/import")
    public static String importMenu(Model model,
            @RequestParam(value = "notification", required = false) String notification) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Return", "/collection/method/return");
        model.addAttribute("menuName", "Import Menu");
        model.addAttribute("menuOptions", options);
        if (notification != null) {
            model.addAttribute("notification", notification);
        }
        return "menu";
    }

    // TODO: export menu
    @GetMapping("/collection/method/export")
    public static String exportMenu(Model model,
            @RequestParam(value = "notification", required = false) String notification) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Return", "/collection/method/return");
        model.addAttribute("menuName", "Export Menu");
        model.addAttribute("menuOptions", options);
        if (notification != null) {
            model.addAttribute("notification", notification);
        }
        return "menu";
    }

    @GetMapping("/collection/method/sync")
    public static String sync() {
        // TODO: sync
        return "redirect:/collection";
    }

    @GetMapping("/collection/method/modify")
    public static String modify() {
        // TODO: modify menu
        return "redirect:/collection";
    }

    @GetMapping("/collection/method/browse")
    public static String browse() {
        // TODO: browse menu
        return "redirect:/collection";
    }

    @PostMapping("/collection/method/return")
    public static String optionReturn(Model model) {
        return CollectionMenu.menu(model, null);
    }
}
