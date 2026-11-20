package ryzen.ownitall.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

public class WebTools {
    private static final Logger logger = new Logger(WebTools.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private static Client client;
    private static int retries = 5;
    private static int timeout = 20; // seconds to sleep on a 429
    private static final Map<String, Long> paces = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastQueryByHost = new ConcurrentHashMap<>();

    /**
     * <p>
     * client. every line of client.properties lands on the client
     * configuration as a jersey client property, so any key jersey or its
     * apache connector understands (proxy, timeouts, cookies) configures
     * itself without code; pace.<host> keys instead feed the per host pacer.
     * </p>
     *
     * @return a {@link jakarta.ws.rs.client.Client} object
     */
    public static synchronized Client client() {
        if (client == null) {
            ClientConfig config = new ClientConfig().connectorProvider(new ApacheConnectorProvider());
            Properties properties = new Properties();
            try (InputStream in = WebTools.class.getClassLoader().getResourceAsStream("client.properties")) {
                if (in == null) {
                    logger.debug("no client.properties found, using jersey defaults");
                } else {
                    properties.load(in);
                    for (String key : properties.stringPropertyNames()) {
                        if (key.startsWith("pace.")) {
                            paces.put(key.substring("pace.".length()), Long.parseLong(properties.getProperty(key)));
                        } else {
                            config.property(key, properties.getProperty(key));
                        }
                    }
                    logger.debug("Configured the http client with " + properties.size()
                            + " properties from client.properties");
                }
            } catch (IOException e) {
                logger.error("Unable to read client.properties, using jersey defaults", e);
            }
            retries = Integer.parseInt(properties.getProperty("retries", String.valueOf(retries)));
            timeout = Integer.parseInt(properties.getProperty("timeout", String.valueOf(timeout)));
            config.register(new JacksonJsonProvider(objectMapper));
            client = ClientBuilder.newClient(config);
        }
        return client;
    }

    /**
     * <p>
     * timeout. seconds loaded from client.properties, used both to sleep on a
     * 429 and to wait on the oauth code redirect; asks for the client first
     * so the properties are read no matter who reads the timeout first.
     * </p>
     *
     * @return a int
     */
    public static int timeout() {
        client();
        return timeout;
    }

    public static WebTarget target(URI url) {
        if (url == null) {
            logger.debug("null url provided in target");
            return null;
        }
        return client().target(url);
    }

    public static WebTarget target(String url) {
        if (url == null) {
            logger.debug("null url provided in target");
            return null;
        }
        return client().target(url);
    }

    /**
     * <p>
     * redirect. Follows nothing: returns the Location of a 3xx instead, or null
     * when the url answers without one.
     * </p>
     *
     * @param url a {@link java.net.URI} object
     * @return a {@link java.net.URI} object
     */
    public static URI redirect(URI url) {
        if (url == null) {
            logger.debug("null url provided in redirect");
            return null;
        }
        try (Response response = client().target(url).request()
                .property(ClientProperties.FOLLOW_REDIRECTS, false)
                .buildGet().invoke()) {
            return response.getLocation();
        } catch (ProcessingException e) {
            logger.error("Exception while getting redirect of: " + url.toString(), e);
            return null;
        }
    }

    /**
     * <p>
     * query. the request stated as data: a target, a method, headers, an
     * optional body, and the type wanted back. every caller uses this: the
     * response binds to the named type on the way in, and every non-2xx
     * answer comes back as a webapplicationexception, handled here once
     * instead of as a status code each caller switches over.
     * </p>
     *
     * @param target       a {@link jakarta.ws.rs.client.WebTarget} object
     * @param method       the http method, ex: get, post
     * @param headers      headers to send, may be null
     * @param entity       the request body, null when the method goes without one
     * @param responseType the bean the response binds to
     * @param <T>          the bean the response binds to
     * @return a T object
     * @throws ryzen.ownitall.util.WebTools.QueryException if any.
     */
    public static <T> T query(WebTarget target, String method, Map<String, ?> headers,
            Entity<?> entity, Class<T> responseType) throws QueryException {
        Invocation.Builder builder = target.request();
        if (headers != null) {
            for (Map.Entry<String, ?> header : headers.entrySet()) {
                builder = builder.header(header.getKey(), header.getValue());
            }
        }
        Invocation invocation = entity == null ? builder.build(method.toUpperCase())
                : builder.build(method.toUpperCase(), entity);
        return query(target, invocation, responseType, 0);
    }

    private static <T> T query(WebTarget target, Invocation invocation, Class<T> responseType, int attempt)
            throws QueryException {
        URI url = target.getUri();
        try {
            WebTools.pace(url.getHost());
            T entity = invocation.invoke(responseType);
            return entity;
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            int status = response == null ? 0 : response.getStatus();
            switch (status) {
                case 429: { // too many requests
                    if (attempt >= retries) {
                        throw new QueryException("Reached " + retries + " retries");
                    }
                    logger.debug("Too many requests, trying again in " + timeout + " seconds...");
                    try {
                        TimeUnit.SECONDS.sleep(timeout);
                    } catch (InterruptedException e2) {
                        logger.error("Interrupted while retrying: " + url.toString(), e2);
                        throw new QueryException(e2);
                    }
                    return query(target, invocation, responseType, attempt + 1);
                }
                case 404: {
                    logger.debug("Requested page '" + url.toString() + "'does not exist");
                    return null;
                }
                default: {
                    String body = response == null || !response.hasEntity() ? null : response.readEntity(String.class);
                    logger.error("Unknown status processing: " + url.toString() + " (" + status + ")");
                    throw new QueryException(String.valueOf(status), body);
                }
            }
        } catch (ProcessingException e) {
            logger.error("Exception while processing: " + url.toString(), e);
            throw new QueryException(e);
        }
    }

    /**
     * <p>
     * pace. holds a request until the host's configured minimum gap since the
     * last one has passed, so a host like musicbrainz that counts requests
     * per second sees what it asked for; a host with no pace line is not
     * held at all.
     * </p>
     *
     * @param host the host about to receive the request
     * @throws ryzen.ownitall.util.WebTools.QueryException if the wait is
     *                                                     interrupted.
     */
    private static void pace(String host) throws QueryException {
        Long gap = paces.get(host);
        if (gap == null) {
            return;
        }
        synchronized (lastQueryByHost) {
            try {
                long wait = gap - (System.currentTimeMillis() - lastQueryByHost.getOrDefault(host, 0L));
                if (wait > 0) {
                    TimeUnit.MILLISECONDS.sleep(wait);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new QueryException("Interrupted while pacing requests to " + host, e);
            }
            lastQueryByHost.put(host, System.currentTimeMillis());
        }
    }

    /**
     * <p>
     * json. Serialises a bean into a request body, leaving out every field that
     * was never set.
     * </p>
     *
     * @param payload a {@link java.lang.Object} object
     * @return a {@link java.lang.String} object
     * @throws com.fasterxml.jackson.core.JsonProcessingException if any.
     */
    public static String json(Object payload) throws JsonProcessingException {
        return objectMapper.writeValueAsString(payload);
    }

    /**
     * <p>
     * convert. Rebinds an already parsed value with the project's mapper. Only
     * union members need it: tidal's items documents answer with an included
     * list the spec types as more than one resource, the provider parses each
     * member as a map, and the caller picks the bean each member is.
     * </p>
     *
     * @param from         an already parsed value, usually one member of a union
     *                     list
     * @param responseType the bean the value describes
     * @param <T>          the bean the value describes
     * @return a T object
     */
    public static <T> T convert(Object from, Class<T> responseType) {
        if (from == null || responseType.isInstance(from)) {
            return responseType.cast(from);
        }
        return objectMapper.convertValue(from, responseType);
    }

    /**
     * download an image from the web
     *
     * @param url  - URI to fetch image from
     * @param file - file to download the image to (will make a new one)
     * @throws java.io.IOException - java.io.IOException while downloading
     */
    public static void downloadImage(URI url, File file) throws IOException {
        if (url == null || file == null) {
            logger.debug("null url or file passed in downloadImage");
            return;
        }
        if (file.exists()) {
            logger.debug("coverimage already found: '" + file.getAbsolutePath() + "'");
            return;
        }
        try (InputStream in = url.toURL().openStream()) {
            Files.copy(in, file.toPath());
        } catch (FileNotFoundException e) {
            logger.debug("Image at url '" + url + "' not found");
        }
    }

    // /**
    // * <p>
    // * getExtension.
    // * </p>
    // *
    // * @param uri a {@link java.net.URI} object
    // * @return a {@link java.lang.String} object
    // */
    public static String getExtension(URI uri) {
        if (uri == null) {
            logger.debug("null uri provided in getExtension");
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            logger.debug("empty path provided for url: '" + uri + "'");
            return null;
        }
        int lastSlashIndex = path.lastIndexOf('/');
        String lastSegment = path.substring(lastSlashIndex + 1);
        int extensionIndex = lastSegment.lastIndexOf('.');
        if (extensionIndex == -1 || extensionIndex == lastSegment.length() - 1) {
            // logger.debug("url has no extension: '" + uri + "'");
            return null;
        }
        return lastSegment.substring(extensionIndex + 1).toLowerCase();
    }

    // exceptions
    public static class AuthenticationException extends Exception {
        /**
         * <p>
         * Constructor for AuthenticationException.
         * </p>
         *
         * @param message a {@link java.lang.String} object
         */
        public AuthenticationException(String message) {
            super(message);
        }

        /**
         * <p>
         * Constructor for AuthenticationException.
         * </p>
         *
         * @param e a {@link java.lang.Throwable} object
         */
        public AuthenticationException(Throwable e) {
            super(e);
        }

        /**
         * <p>
         * Constructor for AuthenticationException.
         * </p>
         *
         * @param message a {@link java.lang.String} object
         * @param e       a {@link java.lang.Throwable} object
         */
        public AuthenticationException(String message, Throwable e) {
            super(message, e);
        }
    }

    public static class QueryException extends Exception {

        public QueryException(String message) {
            super(message);
        }

        public QueryException(Throwable e) {
            super(e);
        }

        public QueryException(String message, Throwable e) {
            super(message, e);
        }

        public QueryException(String code, String message) {
            super("(" + code + ") " + message);
        }
    }
}
