package ryzen.ownitall.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Settings {
    private static final Logger logger = LogManager.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper()
            // needed to include null values
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);
    protected String folderPath = ".appdata";
    private File file;

    public Settings(String saveFile) throws IOException {
        this.file = new File(this.folderPath, saveFile);
        this.setFolder();
        this.read();
    }

    /**
     * check if settings folder exists, if not make it (to prevent errors)
     */
    private void setFolder() {
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                logger.warn("Unable to create folder '" + file.getParentFile().getAbsolutePath() + "'");
            }
        }
    }

    protected void read() throws IOException {
        setFolder();
        if (!file.exists()) {
            this.save();
            return;
        }
        LinkedHashMap<String, Object> imported = objectMapper.readValue(
                file,
                new TypeReference<LinkedHashMap<String, Object>>() {
                });
        if (imported == null) {
            logger.error("Failed to import from file '" + file.getAbsolutePath() + "'");
        } else {
            this.setAll(imported);
        }
    }

    /**
     * save settings to predefined file
     * 
     * @param filePath - filepath of settings file
     */
    protected void save() {
        this.setFolder();
        try {
            objectMapper.writeValue(file, this.getAll());
        } catch (IOException e) {
            logger.error("Exception saving", e);
        }
    }

    /**
     * delete settings of predefined file
     * 
     * @param filePath - filepath of settings file
     */
    protected void clear() {
        file.delete();
    }

    /**
     * flexibly get all settings
     * 
     * @return - LinkedHashSet of all settings with mapping field name : field
     *         value, only gets protected and non final entries
     */

    protected LinkedHashMap<String, Object> getAll() {
        LinkedHashMap<String, Object> settings = new LinkedHashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            try {
                int modifiers = field.getModifiers();
                if (Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers)) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                settings.put(field.getName(), value);
            } catch (IllegalAccessException e) {
                logger.error("Field access error '" + field.getName() + "'", e);
            }
        }
        return settings;
    }

    private void setAll(LinkedHashMap<String, Object> settings) {
        for (String name : settings.keySet()) {
            try {
                Field field = this.getClass().getDeclaredField(name);
                int modifiers = field.getModifiers();
                if (Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers)) {
                    continue;
                }
                Object converted = objectMapper.convertValue(settings.get(name), field.getType());
                field.setAccessible(true);
                field.set(null, converted);
                field.setAccessible(false);
            } catch (IllegalAccessException e) {
                logger.error("Failed to overwrite value for '" + name + "'", e);
            } catch (NoSuchFieldException e) {
                logger.debug("Failed to find '" + name + "'");
            }
        }
    }

    public Class<?> getType(String name) {
        if (name == null) {
            logger.debug("null name provided in getType");
            return null;
        }
        try {
            return this.getClass().getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            logger.debug("no variable with name '" + name + "' found");
            return null;
        }
    }

    /**
     * 
     * @param setting - desired setting to modify
     * @return - true if modified, false if not
     * @throws IllegalAccessException - if unaccessible setting is being modified
     */

    protected boolean change(String name, Object value) {
        if (name == null) {
            logger.debug("null name provided in change");
            return false;
        }
        if (value == null) {
            logger.debug("null value provided in change");
            return false;
        }
        try {
            Field setting = this.getClass().getDeclaredField(name);
            setting.setAccessible(true);
            setting.set(this, value);
            setting.setAccessible(false);
            return true;
        } catch (NoSuchFieldException e) {
            logger.error("Unable to find field '" + name + "'", e);
        } catch (IllegalAccessException e) {
            logger.error("Exception modifying '" + name + "'", e);
        }
        return false;
    }

    protected boolean isEmpty(String name) {
        if (name == null) {
            logger.debug("null name provided in isEmpty");
            return true;
        }
        Object value = this.getFieldValue(name);
        if (value == null || value.toString().isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    protected Object getFieldValue(String name) {
        if (name == null) {
            logger.debug("null name provided in getFieldValue");
            return null;
        }
        try {
            Field field = this.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(this);
            field.setAccessible(false);
            return value;
        } catch (NoSuchFieldException e) {
            logger.warn("No field named '" + name + "' found"); // Use warn or debug
        } catch (IllegalAccessException e) {
            logger.error("Unable to access field named '" + name + "'", e);
        }
        return null;
    }
}
