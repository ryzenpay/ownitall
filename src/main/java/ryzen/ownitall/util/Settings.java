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
import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>
 * Settings class.
 * </p>
 *
 * @author ryzen
 */
public class Settings {
    private static final Logger logger = new Logger(Settings.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            // needed to include null values
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);
    protected String folderPath = ".appdata";
    private File file;

    /**
     * <p>
     * Constructor for Settings.
     * </p>
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
    protected @interface Name {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    protected @interface Description {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    protected @interface Group {
        Class<?>[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    protected @interface Options {
        String[] options();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    protected @interface Secret {
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
     * <p>
     * read.
     * </p>
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
            logger.warn("Failed to import from file '" + file.getAbsolutePath() + "'");
        } else {
            this.setAll(imported);
        }
    }

    /**
     * save settings to predefined file
     */
    public void save() {
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
    public LinkedHashMap<String, Object> getAll() {
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
            try {
                this.set(name, settings.get(name));
            } catch (NoSuchFieldException e) {
                logger.warn("Unable to find setting '" + name + "' declared");
            }
        }
    }

    /**
     * <p>
     * set.
     * </p>
     *
     * @param name  a {@link java.lang.String} object
     * @param value a {@link java.lang.Object} object
     * @throws java.lang.NoSuchFieldException if any.
     */
    public void set(String name, Object value) throws NoSuchFieldException {
        if (name == null) {
            logger.debug("null name provided in change");
            return;
        }
        if (value == null) {
            logger.debug("null value provided in change for '" + name + "'");
            return;
        }
        try {
            Field setting = this.getClass().getDeclaredField(name);
            Object converted = objectMapper.convertValue(value, setting.getType());
            setting.setAccessible(true);
            setting.set(this, converted);
            setting.setAccessible(false);
        } catch (IllegalAccessException e) {
            logger.error("Exception modifying '" + name + "'", e);
        }
    }

    /**
     * <p>
     * exists.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a boolean
     */
    public boolean exists(String name) {
        if (name == null) {
            logger.debug("null name provided in exists");
            return false;
        }
        try {
            this.getClass().getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /**
     * <p>
     * getType.
     * </p>
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
     * <p>
     * isEmpty.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a boolean
     */
    public boolean isEmpty(String name) {
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
     * <p>
     * isGroupEmpty.
     * </p>
     *
     * @param group a {@link java.lang.Class} object
     * @return a boolean
     */
    public boolean isGroupEmpty(Class<?> group) {
        if (group == null) {
            logger.debug("null group provided in isGroupEmpty");
            return true;
        }
        LinkedHashSet<String> vars = getGroup(group);
        if (vars == null) {
            logger.debug("Unable to find credentials for '" + group.getSimpleName() + "'");
            return false;
        }
        for (String varName : vars) {
            if (isEmpty(varName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * get.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link java.lang.Object} object
     */
    public Object get(String name) {
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
        } catch (IllegalAccessException e) {
            logger.error("Unable to access field named '" + name + "'", e);
        } catch (NoSuchFieldException e) {
            logger.warn("Unable to find value '" + name + "'");
        }
        return null;
    }

    /**
     * <p>
     * getName.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    public String getName(String name) {
        if (name == null) {
            logger.debug("null name provided in getName");
            return null;
        }
        try {
            Field field = this.getClass().getDeclaredField(name);
            if (field.isAnnotationPresent(Name.class)) {
                Name annotation = (Name) field.getAnnotation(Name.class);
                return annotation.value();
            } else {
                return field.getName();
            }
        } catch (NoSuchFieldException e) {
            logger.warn("Unable to find variable '" + name + "'");
        }
        return null;
    }

    /**
     * <p>
     * getDescription.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    public String getDescription(String name) {
        if (name == null) {
            logger.debug("null name provided in getDescription");
            return null;
        }
        try {
            Field field = this.getClass().getDeclaredField(name);
            if (field.isAnnotationPresent(Description.class)) {
                Description annotation = (Description) field.getAnnotation(Description.class);
                return annotation.value();
            }
        } catch (NoSuchFieldException e) {
            logger.warn("Unable to find variable '" + name + "'");
        }
        return null;
    }

    /**
     * <p>
     * getGroup.
     * </p>
     *
     * @param groupClass a {@link java.lang.Class} object
     * @return a {@link java.util.LinkedHashMap} object
     */
    public LinkedHashSet<String> getGroup(Class<?> groupClass) {
        if (groupClass == null) {
            logger.debug("null groupClass provided in getGroup");
            return null;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Group.class)) {
                Group annotation = (Group) field.getAnnotation(Group.class);
                for (Class<?> currClass : annotation.value()) {
                    if (currClass.equals(groupClass)) {
                        values.add(field.getName());
                        break;
                    }
                }
            }
        }
        return values;
    }

    /**
     * <p>
     * getOptions.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return an array of {@link java.lang.String} objects
     */
    public String[] getOptions(String name) {
        if (name == null) {
            logger.debug("null field provided in getOptions");
            return null;
        }
        try {
            Field field = this.getClass().getField(name);
            if (field.isAnnotationPresent(Options.class)) {
                Options annotation = (Options) field.getAnnotation(Options.class);
                return annotation.options();
            }
            if (field.getType() == boolean.class) {
                String[] options = { "true", "false" };
                return options;
            }
        } catch (NoSuchFieldException e) {
            logger.debug("Unable to find field '" + name + "'");
        }
        return null;
    }

    /**
     * <p>
     * isSecret.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a boolean
     */
    public boolean isSecret(String name) {
        if (name == null) {
            logger.debug("null name provided in isSecret");
            return false;
        }
        try {
            Field field = this.getClass().getField(name);
            if (field.isAnnotationPresent(Secret.class)) {
                return true;
                // Secret annotation = (Secret) field.getAnnotation(Secret.class);
                // return annotation.mask();
            }
        } catch (NoSuchFieldException e) {
            logger.debug("Unable to find field '" + name + "'");
        }
        return false;
    }

    /**
     * <p>
     * getHashedValue.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    public String getHashedValue(String name) {
        if (name == null) {
            logger.debug("null name provided in getHashedValue");
            return null;
        }
        if (isEmpty(name)) {
            return "";
        }
        String value = get(name).toString();
        return "*".repeat(value.length());
    }
}
