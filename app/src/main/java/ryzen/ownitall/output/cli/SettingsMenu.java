package ryzen.ownitall.output.cli;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ryzen.ownitall.Settings;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Menu;

public class SettingsMenu {
    private static final Logger logger = LogManager.getLogger();

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
            logger.debug("Interrupted while getting tools menu choice");
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
                String choice = Menu.optionMenuWithValue(settings.getAll(), "SETTINGS");
                if (choice.equals("Exit")) {
                    break;
                }
                if (this.changeSetting(choice)) {
                    logger.info("Successfully changed setting '" + choice + "'");
                } else {
                    logger.error("Unsuccessfully changed setting '" + choice + "'");
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while getting change setting choice");
        }
    }

    private boolean changeSetting(String settingName) throws InterruptedException {
        if (settingName == null) {
            logger.debug("null settingName provided in changeSetting");
            return false;
        }
        Settings settings = Settings.load();
        Class<?> settingType = settings.getType(settingName);
        if (settingType == null) {
            logger.error("Unable to find setting type  of '" + settingName + "'");
            return false;
        }
        System.out.print(
                "Enter new '" + settingType.getSimpleName() + "' value for '" + settingName + "': ");
        Object input = Input.request().getValue(settingType);
        return settings.change(settingName, input);
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
