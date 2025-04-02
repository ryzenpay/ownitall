package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class CollectionMenu {

    @GetMapping("/collection")
    public static String menu(Model model,
            @RequestParam(value = "notification", required = false) String notification) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Import", "/collection/import");
        options.put("Export", "/collection/export");
        options.put("Sync", "/collection/sync");
        options.put("Modify", "/collection/modify");
        options.put("Browse", "/collection/browse");
        options.put("Return", "/collection/return");
        model.addAttribute("menuName", "Collection Menu");
        model.addAttribute("menuOptions", options);
        if (notification != null) {
            model.addAttribute("notification", notification);
        }
        return "menu";
    }

    @PostMapping("/collection/import")
    public static String optionImport(Model model) {
        return MethodMenu.importMenu(model, null);
    }

    @PostMapping("/collection/export")
    public static String optionExport(Model model) {
        return MethodMenu.exportMenu(model, null);
    }

    @PostMapping("/collection/sync")
    public static String optionSync() {
        return MethodMenu.sync();
    }

    @PostMapping("/collection/modify")
    public static String optionModify() {
        return MethodMenu.modify();
    }

    @PostMapping("/collection/browse")
    public static String optionBrowse() {
        return MethodMenu.browse();
    }

    @PostMapping("/collection/return")
    public static String optionReturn(Model model) {
        return MainMenu.menu(model, null);
    }
}
