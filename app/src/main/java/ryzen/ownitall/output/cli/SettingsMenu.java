package ryzen.ownitall.output.cli;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Settings;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;

public class SettingsMenu {
    private static final Logger logger = LogManager.getLogger(SettingsMenu.class);

    public SettingsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Save Settings", this::optionSave);
        options.put("Change Setting", this::optionChange);
        options.put("Reset Settings", this::optionReset);
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

    private void optionSave() {
        Settings.load().save();
        logger.info("Successfully saved settings");
    }

    private void optionChange() {
        Settings settings = Settings.load();
        try {
            while (true) {
                String choice = Menu.optionMenuWithValue(settings.getAllSettings(), "SETTINGS");
                if (choice.equals("Exit")) {
                    break;
                }
                if (settings.changeSetting(choice)) {
                    logger.info("Successfully changed setting '" + choice + "'");
                } else {
                    logger.error("Unsuccessfully changed setting");
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting change setting option");
        }
    }

    /**
     * clear memory settings and local settings
     */
    private void optionReset() {
        System.out.print("Are you sure y/N: ");
        try {
            if (Input.request().getAgreement()) {
                Settings.load().clear();
                logger.info("Successfully reset settings");
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting reset settings agreement");
        }
    }
}
