package ryzen.ownitall.method;

import java.util.LinkedHashSet;

import ryzen.ownitall.Settings;
import ryzen.ownitall.method.download.Download;
import ryzen.ownitall.method.interfaces.Export;
import ryzen.ownitall.method.interfaces.Import;
import ryzen.ownitall.method.interfaces.Sync;
import ryzen.ownitall.util.Logger;
import ryzen.ownitall.util.exceptions.AuthenticationException;
import ryzen.ownitall.util.exceptions.MissingSettingException;

/**
 * <p>
 * Abstract Method class.
 * </p>
 *
 * @author ryzen
 */
abstract public class Method implements Import, Export, Sync {
    private static final Logger logger = new Logger(Method.class);
    /** Constant <code>methods</code> */
    private static final LinkedHashSet<Class<?>> methods;
    static {
        methods = new LinkedHashSet<>();
        methods.add(Jellyfin.class);
        methods.add(Spotify.class);
        methods.add(Youtube.class);
        methods.add(Upload.class);
        methods.add(Download.class);
    }

    public static LinkedHashSet<Class<?>> getMethods() {
        return methods;
    }

    public static <type> LinkedHashSet<Class<?>> getMethods(type filter) {
        LinkedHashSet<Class<?>> filteredMethods = new LinkedHashSet<>();
        for (Class<?> method : methods) {
            for (Class<?> currInterface : method.getInterfaces()) {
                if (currInterface.equals(filter)) {
                    filteredMethods.add(method);
                }
            }
        }
        return filteredMethods;
    }

    public static Class<?> getMethod(String name) {
        if (name == null) {
            logger.debug("null name provided in getMethod");
            return null;
        }
        for (Class<?> method : methods) {
            if (method.getSimpleName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    /**
     * <p>
     * initMethod.
     * </p>
     *
     * @param methodClass a {@link java.lang.Class} object
     * @return a {@link ryzen.ownitall.method.Method} object
     * @throws ryzen.ownitall.util.exceptions.MissingSettingException if any.
     * @throws ryzen.ownitall.util.exceptions.AuthenticationException if any.
     * @throws java.lang.NoSuchMethodException                        if any.
     */
    // https://stackoverflow.com/questions/262367/type-safety-unchecked-cast
    @SuppressWarnings("unchecked")
    public static <T> T initMethod(Class<?> methodClass, Class<T> type)
            throws MissingSettingException, AuthenticationException,
            NoSuchMethodException {
        if (methodClass == null) {
            logger.debug("null method class provided in initMethod");
            throw new NoSuchMethodException();
        }
        try {
            logger.debug("Initializing '" + methodClass.getSimpleName() + "' method");
            return (T) methodClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof MissingSettingException) {
                throw new MissingSettingException(e);
            }
            if (cause instanceof AuthenticationException) {
                throw new AuthenticationException(e);
            }
            logger.error("Exception while setting up method '" + methodClass.getSimpleName() + "'", e);
            throw new NoSuchMethodException(methodClass.getName());
        }
    }

    /**
     * <p>
     * clearCredentials.
     * </p>
     *
     * @param type a {@link java.lang.Class} object
     * @return a boolean
     */
    public static void clearCredentials(Class<?> type) {
        if (type == null) {
            logger.debug("null type provided in clearCredentials");
            return;
        }
        Settings settings = Settings.load();
        LinkedHashSet<String> credentials = settings.getGroup(type);
        if (credentials == null) {
            logger.debug("Unable to find credentials for '" + type.getSimpleName() + "'");
            return;
        }
        for (String credential : credentials) {
            try {
                settings.set(credential, "");
            } catch (NoSuchFieldException e) {
                logger.warn("Unable to find method setting '" + credential + "'");
            }
        }
        logger.debug("Cleared credentials for '" + type.getSimpleName() + "'");
    }
}
