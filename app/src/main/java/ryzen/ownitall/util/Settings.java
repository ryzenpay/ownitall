package ryzen.ownitall.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This class serves as a default settings configuration that can be easily
 * extended
 * to include custom settings. To create your own settings class, simply define
 * the private variables along with their corresponding getters and setters.
 * 
 * To implement a singleton pattern, ensuring that all settings are
 * synchronized,
 * include the following method in your extended class:
 * 
 * public static Settings load() {
 * if (instance == null) {
 * instance = new Settings();
 * try {
 * instance.loadSettings(Settings.class); // Rename to your class if
 * not
 * "Settings"
 * } catch (Exception e) {
 * logger.error(e);
 * logger.info("If this persists, delete the file: " +
 * instance.getSettingsFilePath());
 * }
 * }
 * return instance;
 * }
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Settings {

    @JsonIgnore
    private ObjectMapper objectMapper = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC);
    @JsonIgnore
    private final String settingsFolderPath = ".appdata";

    /**
     * check if settings folder exists, if not make it (to prevent errors)
     */
    @JsonIgnore
    private void setSettingsFolder() {
        File settingsFolder = new File(this.settingsFolderPath);
        if (!settingsFolder.exists()) {
            settingsFolder.mkdirs(); // Create folder if it does not exist
        }
    }

    /**
     * load settings from saved file
     * 
     * @param <T>           - settings class
     * @param settingsClass - settings class
     * @param filePath      - filepath to save to
     * @throws Exception - incase of running into error while saving
     */
    @JsonIgnore
    protected <T extends Settings> void importSettings(Class<T> settingsClass, String filePath) throws Exception {
        setSettingsFolder();
        File settingsFile = new File(this.getSettingsFolderPath(), filePath);

        if (!settingsFile.exists() || settingsFile.length() == 0) {
            this.saveSettings(filePath);
            return;
        }

        try {
            T importedSettings = this.objectMapper.readValue(settingsFile, settingsClass);
            if (importedSettings == null) {
                throw new Exception("Failed to import settings from file");
            }
            if (importedSettings.isEmpty()) {
                throw new Exception("Loaded settings were null");
            } else {
                this.setSettings(importedSettings);
            }
        } catch (IOException e) {
            throw new Exception("Error loading settings: " + e);
        }
    }

    /**
     * save settings to predefined file
     * 
     * @param filePath - filepath of settings file
     * @throws Exception - if unable to save to file
     */
    @JsonIgnore
    protected void saveSettings(String filePath) throws Exception {
        this.setSettingsFolder();
        File settingsFile = new File(settingsFolderPath, filePath);
        try {
            this.objectMapper.writeValue(settingsFile, this);
        } catch (IOException e) {
            throw new Exception("Error saving settings: " + e);
        }
    }

    /**
     * delete settings of predefined file
     * 
     * @param filePath - filepath of settings file
     */
    @JsonIgnore
    protected void clearSettings(String filePath) {
        this.setSettingsFolder();
        File settingsFile = new File(settingsFolderPath, filePath);
        settingsFile.delete();
    }

    /**
     * flexibly get all settings
     * 
     * @return - ArrayList of all setting varialbes as Object
     * @throws Exception - if get into error obtaining and setting settings
     *                   accessibility
     */
    @JsonIgnore
    public ArrayList<Field> getAllSettings() throws Exception {
        ArrayList<Field> allSettings = new ArrayList<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            // only allow option to change non final and protected fields
            if (!Modifier.isFinal(field.getModifiers()) && Modifier.isProtected(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    allSettings.add(field); // Add field value to the list
                } catch (Exception e) {
                    throw new Exception("Error getting all settings: " + e);
                }
            }
        }
        return allSettings;
    }

    /**
     * print menu of settings and values, prompt user for which to change
     * 
     * @throws Exception - exception if unable to modify setting
     */
    @JsonIgnore
    public void changeSettings() throws Exception {
        System.out.println("Choose a setting to change: ");
        Map<String, String> options = new HashMap<>();
        try {
            while (true) {
                for (Field setting : this.getAllSettings()) {
                    options.put(setting.getName(), setting.get(this).toString());
                }
                String choice = Menu.optionMenuWithValue(options, "SETTINGS");
                if (choice == "Exit") {
                    break;
                } else {
                    try {
                        if (!this.changeSetting(choice)) {
                            throw new Exception("Unsuccessfully changed setting, read the log for more information");
                        }
                    } catch (IllegalAccessException e) {
                        throw new Exception("Error updating setting: " + e);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new Exception("Error listing settings: " + e);
        }
    }

    /**
     * 
     * @param setting - desired setting to modify
     * @return - true if modified, false if not
     * @throws IllegalAccessException - if unaccessible setting is being modified
     */
    @JsonIgnore
    private boolean changeSetting(String settingName) throws IllegalAccessException, Exception {
        try {
            Field setting = this.getClass().getDeclaredField(settingName);
            System.out.print("Enter new value for " + setting.getName() + ": ");
            setting.setAccessible(true);
            if (setting.getType() == boolean.class) {
                boolean input = Input.request().getBool();
                setting.set(this, input);
                return true;
            } else if (setting.getType() == String.class) {
                String input = Input.request().getString();
                setting.set(this, input);
                return true;
            } else if (setting.getType() == Integer.class) {
                int input = Input.request().getInt();
                setting.set(this, input);
                return true;
            } else if (setting.getType() == long.class) {
                long input = Input.request().getLong();
                setting.set(this, input);
                return true;
            } else {
                System.out
                        .println("Modifying settings of the type " + setting.getType() + " is currently not supported");
            }
            setting.setAccessible(false);
        } catch (NoSuchFieldException e) {
            throw new Exception("Error modifying setting: " + e);
        }
        return false;
    }

    /**
     * copy over settings from constructed Settings to this
     * 
     * @param setting - constructed Settings
     */
    @JsonIgnore
    private <T extends Settings> void setSettings(T settings) throws Exception {
        for (Field field : settings.getClass().getDeclaredFields()) {
            if (!Modifier.isFinal(field.getModifiers()) && Modifier.isProtected(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(settings);
                    field.set(this, value);
                    field.setAccessible(false);
                } catch (IllegalAccessException e) {
                    throw new Exception("Error copying over settings: " + e);
                }
            }
        }
    }

    /**
     * check if settings correctly imported
     * 
     * @return - true if errors, false if none
     */
    @JsonIgnore
    public boolean isEmpty() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    if (field.get(this) == null) {
                        return true;
                    }
                    field.setAccessible(false);
                } catch (IllegalAccessException e) {
                    // Log the exception or handle it as needed
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @JsonIgnore
    public String getSettingsFolderPath() {
        return settingsFolderPath;
    }
}
