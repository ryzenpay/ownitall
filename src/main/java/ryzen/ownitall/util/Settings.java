package ryzen.ownitall.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>Settings class.</p>
 *
 * @author ryzen
 */
public class Settings {
    private static final Logger logger = LogManager.getLogger(Settings.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            // needed to include null values
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);
    protected String folderPath = ".appdata";
    private File file;

    /**
     * <p>Constructor for Settings.</p>
     *
     * @param saveFile a {@link java.lang.String} object
     * @throws java.io.IOException if any.
     */
    public Settings(String saveFile) throws IOException {
        this.file = new File(this.folderPath, saveFile);
        this.setFolder();
        this.read();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    protected @interface SettingsGroup {
        Class<?>[] group();

        String desc();
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

    /**
     * <p>read.</p>
     *
     * @throws java.io.IOException if any.
     */
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
        if (settings == null) {
            logger.debug("null settings provided in setAll");
            return;
        }
        if (settings.isEmpty()) {
            logger.warn("no settings provided in setAll");
            return;
        }
        for (String name : settings.keySet()) {
            this.set(name, settings.get(name));
        }
    }

    /**
     * <p>set.</p>
     *
     * @return - true if modified, false if not
     * @param name a {@link java.lang.String} object
     * @param value a {@link java.lang.Object} object
     */
    protected boolean set(String name, Object value) {
        if (name == null) {
            logger.debug("null name provided in change");
            return false;
        }
        if (value == null) {
            logger.debug("null value provided in change for '" + name + "'");
            return false;
        }
        try {
            Field setting = this.getClass().getDeclaredField(name);
            Object converted = objectMapper.convertValue(value, setting.getType());
            setting.setAccessible(true);
            setting.set(this, converted);
            setting.setAccessible(false);
            return true;
        } catch (NoSuchFieldException e) {
            logger.error("Unable to find field '" + name + "'", e);
        } catch (IllegalAccessException e) {
            logger.error("Exception modifying '" + name + "'", e);
        }
        return false;
    }

    /**
     * <p>getType.</p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link java.lang.Class} object
     */
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
     * <p>isEmpty.</p>
     *
     * @param name a {@link java.lang.String} object
     * @return a boolean
     */
    protected boolean isEmpty(String name) {
        if (name == null) {
            logger.debug("null name provided in isEmpty");
            return true;
        }
        Object value = this.get(name);
        if (value == null || value.toString().isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * <p>get.</p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link java.lang.Object} object
     */
    protected Object get(String name) {
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

    /**
     * <p>getGroup.</p>
     *
     * @param groupClass a {@link java.lang.Class} object
     * @return a {@link java.util.LinkedHashMap} object
     */
    public LinkedHashMap<String, String> getGroup(Class<?> groupClass) {
        if (groupClass == null) {
            logger.debug("null groupClass provided in getGroup");
            return null;
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SettingsGroup.class)) {
                SettingsGroup annotation = (SettingsGroup) field.getAnnotation(SettingsGroup.class);
                for (Class<?> currClass : annotation.group()) {
                    if (currClass.equals(groupClass)) {
                        values.put(annotation.desc(), field.getName());
                        break;
                    }
                }
            }
        }
        return values;
    }
}
