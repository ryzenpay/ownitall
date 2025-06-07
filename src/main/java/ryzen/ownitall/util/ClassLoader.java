package ryzen.ownitall.util;

import java.util.LinkedHashSet;

import org.reflections.Reflections;

public class ClassLoader {
    private static final Logger logger = new Logger(ClassLoader.class);
    private Reflections reflections;

    private static ClassLoader instance;

    private ClassLoader(String packageName) {
        reflections = new Reflections(packageName);
    }

    public static void init(String packageName) {
        if (packageName == null) {
            logger.debug("null packageName provided in load");
            throw new RuntimeException();
        }
        instance = new ClassLoader(packageName);
    }

    public static ClassLoader load() {
        if (instance == null) {
            logger.error("ClassLoader was never initialized, you should do it at the start of your code",
                    new Exception());
            throw new RuntimeException();
        }
        return instance;
    }

    public <T> LinkedHashSet<Class<? extends T>> getSubClasses(Class<T> clazz) {
        if (clazz == null) {
            logger.debug("null class provided in getSubClasses");
            return null;
        }
        return new LinkedHashSet<Class<? extends T>>(reflections.getSubTypesOf(clazz));
    }

    public <T> Class<? extends T> getSubClass(Class<T> clazz, String className) {
        if (clazz == null) {
            logger.debug("null class provided in getSubClass");
            return null;
        }
        for (Class<? extends T> subClass : reflections.getSubTypesOf(clazz)) {
            if (subClass.getSimpleName().equals(className)) {
                return subClass;
            }
        }
        return null;
    }
}
