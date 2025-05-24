package ryzen.ownitall.ui.web;

import ryzen.ownitall.Settings;

public class Setting {
    private static final Settings settings = Settings.load();

    private String key;
    private String name;
    private Object value;
    private String description;
    private boolean secret = false;
    private String[] options;

    public Setting(String key) {
        this.key = key;
        setName();
        setValue();
        setDescription();
        setSecret();
        setOptions();
    }

    public String getKey() {
        return this.key;
    }

    private void setName() {
        this.name = settings.getName(key);
    }

    public String getName() {
        return this.name;
    }

    private void setValue() {
        if (!settings.isEmpty(key)) {
            value = settings.get(key);
        }
    }

    public Object getValue() {
        return value;
    }

    private void setDescription() {

    }

    public String getDescription() {
        return this.description;
    }

    private void setSecret() {
        if (settings.isSecret(key)) {
            secret = true;
        }
    }

    public boolean getSecret() {
        return secret;
    }

    private void setOptions() {
        this.options = settings.getOptions(key);
    }

    public String[] getOptions() {
        return this.options;
    }
}
