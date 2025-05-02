package ryzen.ownitall.output.web;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ryzen.ownitall.Collection;

/**
 * <p>CollectionMenu class.</p>
 *
 * @author ryzen
 */
@Controller
public class CollectionMenu {

    /**
     * <p>collectionMenu.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection")
    public String collectionMenu(Model model) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Import", "/collection/import");
        options.put("Export", "/collection/export");
        options.put("Sync", "/collection/sync");
        options.put("Modify", "/collection/modify");
        options.put("Browse", "/collection/browse");
        model.addAttribute("menuName", "Collection Menu");
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", "/collection/return");
        return "menu";
    }

    /**
     * <p>optionImport.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/import")
    public String optionImport() {
        return "redirect:/method/import";
    }

    /**
     * <p>optionExport.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/export")
    public String optionExport() {
        return "redirect:/method/export";
    }

    /**
     * <p>optionSync.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/sync")
    public String optionSync() {
        return "redirect:/method/sync";
    }

    /**
     * <p>optionModify.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/modify")
    public String optionModify() {
        // TODO: modify menu
        // update browse to have delete / update buttons
        return "redirect:/collection";
    }

    /**
     * <p>optionBrowse.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/browse")
    public String optionBrowse(Model model) {
        model.addAttribute("playlists", Collection.getPlaylists());
        model.addAttribute("albums", Collection.getAlbums());
        model.addAttribute("likedsongs", Collection.getLikedSongs());
        model.addAttribute("callback", "/collection");
        return "browse";
    }

    /**
     * <p>optionReturn.</p>
     *
     * @param model a {@link org.springframework.ui.Model} object
     * @return a {@link java.lang.String} object
     */
    @GetMapping("/collection/return")
    public String optionReturn(Model model) {
        return "redirect:/";
    }
}
