package ryzen.ownitall.classes;

import java.util.LinkedHashSet;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import ryzen.ownitall.util.Logger;

@Entity
@Table(name = "Id")
public class Id {
    private static final Logger logger = new Logger(Id.class);

    @jakarta.persistence.Id
    private String key;
    private String value;

    public Id(String key, String value) {
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

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public static boolean hasMatching(LinkedHashSet<Id> ids1, LinkedHashSet<Id> ids2) {
        for (Id id1 : ids1) {
            if (ids2.contains(id1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }
}
