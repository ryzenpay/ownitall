package ryzen.ownitall.ui.cli;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import ryzen.ownitall.Settings;
import ryzen.ownitall.util.Input;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.Menu;
import ryzen.ownitall.util.exceptions.MenuClosed;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>
 * SettingsMenu class.
 * </p>
 *
 * @author ryzen
 */
public class SettingsMenu {
    private static final Logger logger = new Logger(SettingsMenu.class);

    /**
     * <p>
     * Constructor for SettingsMenu.
     * </p>
     */
    public SettingsMenu() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Save Settings", this::optionSave);
        options.put("Change Setting", this::optionChange);
        options.put("Reset Settings", this::optionReset);
        try {
            while (true) {
                String choice = Menu.optionMenu(options.keySet(), "TOOL MENU");
                options.get(choice).run();
            }
        } catch (MenuClosed e) {
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
                LinkedHashMap<String, String> options = new LinkedHashMap<>();
                for (String name : settings.getAll().keySet()) {
                    if (settings.isEmpty(name)) {
                        options.put(name, "");
                    } else {
                        if (settings.isSecret(name)) {
                            options.put(name, settings.getHashedValue(name));
                        } else {
                            options.put(name, settings.get(name).toString());
                        }
                    }
                }
                String choice = Menu.optionMenuWithValue(options, "SETTINGS");
                try {
                    changeSetting(choice);
                    logger.info("Successfully changed setting '" + choice + "'");
                } catch (NoSuchFieldException e) {
                    logger.error("Unsuccessfully changed setting '" + choice + "'", e);
                } catch (MissingSettingException e) {
                    logger.warn("Unable to find setting '" + choice + "'");
                }
            }
        } catch (InterruptedException | MenuClosed e) {
            logger.debug("Interrupted while getting change setting choice");
        }
    }

    public static void changeSetting(String settingName)
            throws InterruptedException, NoSuchFieldException, MissingSettingException {
        if (settingName == null) {
            logger.debug("null settingName provided in changeSetting");
            return;
        }
        Settings settings = Settings.load();
        Class<?> settingType = settings.getType(settingName);
        if (settingType == null) {
            throw new MissingSettingException("Unable to find setting type  of '" + settingName + "'");
        }
        String[] options = settings.getOptions(settingName);
        Object input;
        if (options != null) {
            try {
                String choice = Menu.optionMenu(new LinkedHashSet<String>(Arrays.asList(options)),
                        "'" + settingName.toUpperCase() + "' OPTIONS");
                input = choice;
            } catch (MenuClosed e) {
                logger.info("Interruption caught changing setting");
                return;
            }
        } else {
            System.out.print(
                    "Enter new '" + settingType.getSimpleName() + "' value for '" + settingName + "': ");
            input = Input.request().getValue(settingType);
        }
        settings.set(settingName, input);
    }

    public static void changeSettings(LinkedHashSet<String> settingNames)
            throws InterruptedException, NoSuchFieldException, MissingSettingException {
        if (settingNames == null) {
            logger.debug("null settingNames provided in changeSetting");
            return;
        }
        Settings settings = Settings.load();
        for (String setting : settingNames) {
            changeSetting(setting);
        }
        for (String setting : settingNames) {
            if (settings.isEmpty(setting)) {
                throw new MissingSettingException("Unable to set setting for '" + setting + "'");
            }
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
