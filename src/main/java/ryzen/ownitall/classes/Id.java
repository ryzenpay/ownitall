package ryzen.ownitall.classes;

import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ryzen.ownitall.util.Logger;

public class Id {
    private static final Logger logger = new Logger(Id.class);

    private String key;
    private String value;

    @JsonCreator
    public Id(@JsonProperty("key") String key, @JsonProperty("value") String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

    public void setKey(String key) {
        if (key == null) {
            logger.debug(this.toString() + ": null key provided in setKey");
            return;
        }
        this.key = key;
    }

    public void setValue(String value) {
        if (value == null) {
            logger.debug(this.toString() + ": null value provided in setValue");
            return;
        }
        this.value = value;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return value.isEmpty();
    }

    @JsonIgnore
    public static boolean hasMatching(LinkedHashSet<Id> ids1, LinkedHashSet<Id> ids2) {
        for (Id id1 : ids1) {
            if (ids2.contains(id1)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    @Override
    public int hashCode() {
        return this.key.hashCode();
    }
}
