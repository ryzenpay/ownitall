package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CollectionMenu {

    @GetMapping("/collection")
    public String collectionMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Import", "/collection/import");
        options.put("Export", "/collection/export");
        options.put("Sync", "/collection/sync");
        options.put("Modify", "/collection/modify");
        options.put("Browse", "/collection/browse");
        options.put("Return", "/collection/return");
        model.addAttribute("menuName", "Collection Menu");
        model.addAttribute("menuOptions", options);
        return "menu";
    }

    @GetMapping("/collection/import")
    public String optionImport() {
        return "redirect:/method/import?callback=/method/import";
    }

    @GetMapping("/collection/export")
    public String optionExport() {
        return "redirect:/method/import?callback=/method/export";
    }

    @GetMapping("/collection/sync")
    public String optionSync() {
        return "redirect:/method/import?callback=/method/sync";
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
        return "redirect:/";
    }
}
