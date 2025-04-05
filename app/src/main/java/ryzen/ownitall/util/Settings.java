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
        this.setFile();
        this.read();
    }

    /**
     * check if settings folder exists, if not make it (to prevent errors)
     */
    private void setFile() {
        if (!file.exists()) {
            if (!file.getParentFile().mkdirs()) {
                logger.warn("Unable to create folder '" + file.getParentFile().getAbsolutePath() + "'");
            }
        }
    }

    protected void read() throws IOException {
        setFile();
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
        this.setFile();
        try {
            objectMapper.writeValue(file, this.getAll());
        } catch (IOException e) {
            logger.error("Exception saving settings", e);
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
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                // only allow option to change non final and protected fields
                if (!Modifier.isFinal(field.getModifiers()) && Modifier.isProtected(field.getModifiers())) {
                    settings.put(field.getName(), field.get(this));
                }
                field.setAccessible(false);
            } catch (IllegalAccessException e) {
                logger.error("Unable to access '" + field.getName() + "'", e);
            }
        }
        return settings;
    }

    private void setAll(LinkedHashMap<String, Object> settings) {
        for (String name : settings.keySet()) {
            try {
                Field field = this.getClass().getDeclaredField(name);
                field.setAccessible(true);
                field.set(this, this.transform(field.getType(), name));
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
            logger.debug("null setting name provided in changeSetting");
            return false;
        }
        if (value == null) {
            logger.debug("null setting value provided in changeSetting");
            return false;
        }
        try {
            Field setting = this.getClass().getDeclaredField(name);
            setting.setAccessible(true);
            setting.set(this, value);
            setting.setAccessible(false);
            return true;
        } catch (NoSuchFieldException e) {
            logger.error("Exception modifying setting (No Such Field Exception)", e);
        } catch (IllegalAccessException e) {
            logger.error("Exception modifying setting (IllegalAccessException)", e);
        }
        return false;
    }

    public boolean isEmpty(String name) {
        if (name == null) {
            logger.debug("null name provided in isEmpty");
            return true;
        }
        String string = this.getString(name);
        if (string == null || string.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    private Object transform(Class<?> type, String name) {
        if (type == null) {
            logger.debug("null type provided in transform");
            return null;
        }
        if (name == null) {
            logger.debug("null name provided in transform");
            return null;
        }
        if (type.equals(String.class)) {
            return this.getString(name);
        } else if (type.equals(boolean.class)) {
            return this.getBool(name);
        } else if (type.equals(int.class)) {
            return this.getInt(name);
        } else if (type.equals(Long.class)) {
            return this.getLong(name);
        } else if (type.equals(char.class)) {
            return this.getChar(name);
        } else if (type.equals(File.class)) {
            return this.getFile(name);
        } else {
            logger.error("Unsupported type '" + type.getName() + "' provided");
            return null;
        }
    }

    // TODO: restrict?
    private Object getFieldValue(String name) {
        if (name == null) {
            logger.debug("null setting name provided in getFieldValue");
            return null;
        }
        try {
            Field field = this.getClass().getDeclaredField(name.toLowerCase());
            field.setAccessible(true);
            Object value = field.get(this);
            field.setAccessible(false);
            return value;
        } catch (NoSuchFieldException e) {
            logger.warn("No setting field named '" + name + "' found"); // Use warn or debug
        } catch (IllegalAccessException e) {
            logger.error("Unable to access setting field named '" + name + "'", e);
        }
        return null;
    }

    public String getString(String name) {
        Object value = this.getFieldValue(name);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    public boolean getBool(String name) {
        Object value = this.getFieldValue(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return false;
    }

    public Integer getInt(String name) {
        Object value = this.getFieldValue(name);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return null;
    }

    public Long getLong(String name) {
        Object value = this.getFieldValue(name);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value != null) {
            return Long.parseLong(value.toString());
        }
        return null;
    }

    public char getChar(String name) {
        Object value = this.getFieldValue(name);
        if (value instanceof Character) {
            return (char) value;
        }
        if (value != null) {
            return value.toString().charAt(0);
        }
        return '\0';
    }

    public File getFile(String name) {
        Object value = this.getFieldValue(name);
        if (value instanceof File) {
            return (File) value;
        }
        if (value != null) {
            return new File(value.toString());
        }
        return null;
    }
}
