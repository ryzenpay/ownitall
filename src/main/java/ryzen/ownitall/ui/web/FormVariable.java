package ryzen.ownitall.ui.web;

public class FormVariable {

    private String key;
    private String name;
    private String description;
    private Object value;
    private boolean secret = false;
    private boolean required = false;
    private String[] options;

    public FormVariable(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setValue(Object value) {
        this.value = value;

    }

    public Object getValue() {
        return value;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public boolean getSecret() {
        return secret;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean getRequired() {
        return this.required;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public String[] getOptions() {
        return this.options;
    }
}
