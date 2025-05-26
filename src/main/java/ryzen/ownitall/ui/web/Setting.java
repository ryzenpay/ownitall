package ryzen.ownitall.ui.web;

import ryzen.ownitall.Settings;

public class Setting {

    private String key;
    private String name;
    private Object value;
    private boolean secret = false;
    private String[] options;

    public Setting(String key) {
        Settings settings = Settings.load();
        this.key = key;
        setName(settings.getName(key));
        if (!settings.isEmpty(key)) {
            setValue(settings.get(key));
        }
        if (settings.isSecret(key)) {
            setSecret();
        }
        if (settings.getOptions(key) != null) {
            setOptions(settings.getOptions(key));
        }
    }

    public String getKey() {
        return this.key;
    }

    private void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    private void setValue(Object value) {
        this.value = value;

    }

    public Object getValue() {
        return value;
    }

    private void setSecret() {
        secret = true;
    }

    public boolean getSecret() {
        return secret;
    }

    private void setOptions(String[] options) {
        this.options = options;
    }

    public String[] getOptions() {
        return this.options;
    }
}
