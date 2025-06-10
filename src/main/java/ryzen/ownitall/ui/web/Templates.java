package ryzen.ownitall.ui.web;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.springframework.ui.Model;

public class Templates {
    public static String menu(Model model, String menuName, LinkedHashMap<String, String> options, String callback) {
        model.addAttribute("menuName", menuName);
        model.addAttribute("menuOptions", options);
        model.addAttribute("callback", callback);
        return "menu";
    }

    public static String form(Model model, String formName, LinkedHashSet<FormVariable> fields, String postUrl,
            String callback) {
        model.addAttribute("formName", formName);
        model.addAttribute("values", fields);
        model.addAttribute("postUrl", postUrl);
        model.addAttribute("callback", callback);
        return "form";
    }

    public static String process(Model model, String processName, String processUrl, String processUpdateUrl,
            String processLogs, String callback) {
        model.addAttribute("processName", processName);
        model.addAttribute("processFunction", processUrl);
        model.addAttribute("processProgress", processUpdateUrl);
        model.addAttribute("processLogs", processLogs);
        model.addAttribute("callback", callback);
        return "process";
    }
}
