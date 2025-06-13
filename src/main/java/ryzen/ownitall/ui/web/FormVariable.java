package ryzen.ownitall.ui.web;

/**
 * <p>
 * FormVariable class.
 * </p>
 *
 * @author ryzen
 */
public class FormVariable {

    private String key;
    private String name;
    private String description;
    private Object value;
    private boolean secret = false;
    private boolean required = false;
    private boolean multipleChoice = false;
    private String[] options;

    /**
     * <p>
     * Constructor for FormVariable.
     * </p>
     *
     * @param key a {@link java.lang.String} object
     */
    public FormVariable(String key) {
        this.key = key;
    }

    /**
     * <p>
     * Getter for the field <code>key</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getKey() {
        return this.key;
    }

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     *
     * @param name a {@link java.lang.String} object
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * Setter for the field <code>description</code>.
     * </p>
     *
     * @param description a {@link java.lang.String} object
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * <p>
     * Setter for the field <code>value</code>.
     * </p>
     *
     * @param value a {@link java.lang.Object} object
     */
    public void setValue(Object value) {
        this.value = value;

    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     *
     * @return a {@link java.lang.Object} object
     */
    // TODO: support array as value
    // needed for multiple choice
    public Object getValue() {
        return value;
    }

    /**
     * <p>
     * Setter for the field <code>secret</code>.
     * </p>
     *
     * @param secret a boolean
     */
    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    /**
     * <p>
     * Getter for the field <code>secret</code>.
     * </p>
     *
     * @return a boolean
     */
    public boolean getSecret() {
        return secret;
    }

    /**
     * <p>
     * Setter for the field <code>required</code>.
     * </p>
     *
     * @param required a boolean
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * <p>
     * Getter for the field <code>required</code>.
     * </p>
     *
     * @return a boolean
     */
    public boolean getRequired() {
        return this.required;
    }

    public void setMultipleChoice(boolean multipleChoice) {
        this.multipleChoice = multipleChoice;
    }

    public boolean getMultipleChoice() {
        return this.multipleChoice;
    }

    /**
     * <p>
     * Setter for the field <code>options</code>.
     * </p>
     *
     * @param options an array of {@link java.lang.String} objects
     */
    public void setOptions(String[] options) {
        this.options = options;
    }

    /**
     * <p>
     * Getter for the field <code>options</code>.
     * </p>
     *
     * @return an array of {@link java.lang.String} objects
     */
    public String[] getOptions() {
        return this.options;
    }
}
