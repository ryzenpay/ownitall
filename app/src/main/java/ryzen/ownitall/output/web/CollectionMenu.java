package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class CollectionMenu {

    @GetMapping("/collection")
    public String menu(Model model, @RequestParam(value = "notification", required = false) String notification) {
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
    public String optionImport(Model model) {
        return new MethodMenu().importMenu(model, null);
    }

    @PostMapping("/collection/export")
    public String optionExport(Model model) {
        return new MethodMenu().exportMenu(model, null);
    }

    @PostMapping("/collection/sync")
    public String optionSync() {
        return new MethodMenu().sync();
    }

    @PostMapping("/collection/modify")
    public String optionModify() {
        return new MethodMenu().modify();
    }

    @PostMapping("/collection/browse")
    public String optionBrowse() {
        return new MethodMenu().browse();
    }

    @PostMapping("/collection/return")
    public String optionReturn() {
        return "redirect:/";
    }
}
