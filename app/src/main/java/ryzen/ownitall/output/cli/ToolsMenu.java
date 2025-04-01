package ryzen.ownitall.output.cli;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Tools;
import ryzen.ownitall.util.Menu;

public class ToolsMenu {
    private static final Logger logger = LogManager.getLogger(ToolsMenu.class);

    public ToolsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Archive", Tools::archive);
        options.put("UnArchive", Tools::unArchive);
        options.put("Clear Cache", Tools::clearCache);
        options.put("Clear Saved Logins", Tools::clearCredentials);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "TOOL MENU");
                if (choice.equals("Exit")) {
                    break;
                }
                options.get(choice).run();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting tools menu response");
        }
    }
}
