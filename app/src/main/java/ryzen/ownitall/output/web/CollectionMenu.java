package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CollectionMenu {

    @GetMapping("/collection")
    public static String collectionMenu(Model model,
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

    @GetMapping("/collection/modify")
    public String optionModify() {
        // TODO: modify menu
        return "redirect:/collection";
    }

    @GetMapping("/collection/browse")
    public String optionBrowse() {
        // TODO: browse (print inventory)
        return "redirect:/collection";
    }

    @GetMapping("/collection/return")
    public String optionReturn(Model model) {
        return MainMenu.mainMenu(model, null);
    }
}
