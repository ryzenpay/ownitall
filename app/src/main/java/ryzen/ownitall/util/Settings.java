package ryzen.ownitall.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * logger.warn("If this persists, delete the file: " +
 * instance.getSettingsFilePath());
 * }
 * }
 * return instance;
 * }
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Settings {
    private static final Logger logger = LogManager.getLogger(Settings.class);
    private final ObjectMapper objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD,
            JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC);
    private final String settingsFolderPath = ".appdata";

    /**
     * check if settings folder exists, if not make it (to prevent errors)
     */
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
     * @throws IOException - incase of running into error while saving
     */
    protected <T extends Settings> void importSettings(Class<T> settingsClass, String filePath) throws IOException {
        setSettingsFolder();
        File settingsFile = new File(this.getSettingsFolderPath(), filePath);

        if (!settingsFile.exists() || settingsFile.length() == 0) {
            this.save(filePath);
            return;
        }
        T importedSettings = this.objectMapper.readValue(settingsFile, settingsClass);
        if (importedSettings == null || importedSettings.isEmpty()) {
            logger.error("Failed to import settings from file '" + settingsFile.getAbsolutePath() + "'");
        } else {
            this.setSettings(importedSettings);
        }

    }

    /**
     * save settings to predefined file
     * 
     * @param filePath - filepath of settings file
     */
    protected void save(String filePath) {
        this.setSettingsFolder();
        File settingsFile = new File(settingsFolderPath, filePath);
        try {
            this.objectMapper.writeValue(settingsFile, this);
        } catch (IOException e) {
            logger.error("Exception saving settings: " + e);
        }
    }

    /**
     * delete settings of predefined file
     * 
     * @param filePath - filepath of settings file
     */
    protected void clearSettings(String filePath) {
        this.setSettingsFolder();
        File settingsFile = new File(settingsFolderPath, filePath);
        settingsFile.delete();
    }

    /**
     * flexibly get all settings
     * 
     * @return - ArrayList of all setting varialbes as Object
     */
    @JsonIgnore
    public LinkedHashMap<String, String> getAllSettings() {
        LinkedHashMap<String, String> settings = new LinkedHashMap<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            // only allow option to change non final and protected fields
            if (!Modifier.isFinal(field.getModifiers()) && Modifier.isProtected(field.getModifiers())) {
                field.setAccessible(true);
                try {
                    settings.put(field.getName(), field.get(this).toString());
                } catch (IllegalAccessException e) {
                    logger.error("Exception fetching settings value: " + e);
                }
            }
        }
        return settings;
    }

    /**
     * 
     * @param setting - desired setting to modify
     * @return - true if modified, false if not
     * @throws IllegalAccessException - if unaccessible setting is being modified
     */
    @JsonIgnore
    public boolean changeSetting(String settingName) throws InterruptedException {
        try {
            Field setting = this.getClass().getDeclaredField(settingName);
            setting.setAccessible(true);
            System.out.print(
                    "Enter new value " + setting.getType().getSimpleName() + " for '" + setting.getName() + "': ");
            if (setting.getType() == boolean.class) {
                boolean input = Input.request().getBool();
                setting.set(this, input);
                return true;
            } else if (setting.getType() == String.class) {
                String input = Input.request().getString();
                setting.set(this, input);
                return true;
            } else if (setting.getType() == int.class) {
                int input = Input.request().getInt();
                setting.set(this, input);
                return true;
            } else if (setting.getType() == long.class) {
                long input = Input.request().getLong();
                setting.set(this, input);
                return true;
            } else {
                logger.warn("Modifying settings of the type '" + setting.getType() + "' is currently not supported");
            }
            setting.setAccessible(false);
        } catch (NoSuchFieldException e) {
            logger.error("Exception modifying setting (No Such Field Exception): " + e);
        } catch (IllegalAccessException e) {
            logger.error("Exception modifying setting (IllegalAccessException): " + e);
        }
        return false;
    }

    /**
     * copy over settings from constructed Settings to this
     * 
     * @param setting - constructed Settings
     */
    @JsonIgnore
    private <T extends Settings> void setSettings(T settings) {
        for (Field field : settings.getClass().getDeclaredFields()) {
            if (!Modifier.isFinal(field.getModifiers()) && Modifier.isProtected(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(settings);
                    field.set(this, value);
                    field.setAccessible(false);
                } catch (IllegalAccessException e) {
                    logger.error("Exception setting setting (Illegal Access Exception): " + e);
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
                    logger.error("Exception checking field: " + e);

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
