package it.beng.modeler;

import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class config {

    private static JsonObject _config;

    public static final String ASSETS_PATH = "assets/";
    public static Boolean develop;

    public static final class ssl {
        public static boolean enabled;
        public static String keyStoreFilename;
        public static String keyStorePassword;
    }

    public static final class server {
        public static String name;
        public static String scheme;
        public static String hostname;
        public static Integer port;
        public static String baseHref;
        public static String allowedOriginPattern;
        public static Long simLagTime;

        public static class cacheBuilder {
            public static int concurrencyLevel;
            public static int initialCapacity;
            public static int maximumSize;
            public static String expireAfterAccess;
        }

        public static class auth {
            public static String path;
        }

        public static class api {
            public static String path;
        }

        public static class assets {
            public static Boolean allowListing;
        }

    }

    public static class webapp {
        public static List<String> routes;
        public static String diagramPath;
    }

    public static class oauth2 {
        public static String host;
        public static String clientId;
        public static String clientSecret;
        public static String site;
        public static String tokenPath;
        public static String authPath;
        public static String scope;
    }

    private static String checkBaseHref(String href) {
        if (href == null)
            throw new IllegalStateException("href cannot be null!");
        if (!href.startsWith("/"))
            throw new IllegalStateException("href MUST start with '/' character!");
        if (!href.endsWith("/"))
            throw new IllegalStateException("href MUST end with '/' character!");
        return href;
    }

    private static String checkPath(String path) {
        if (path == null)
            throw new IllegalStateException("path cannot be null!");
        if (path.startsWith("/"))
            throw new IllegalStateException("path CANNOT start with '/' character!");
        if (!path.endsWith("/"))
            throw new IllegalStateException("path MUST end with '/' character!");
        return path;
    }

    public static void set(JsonObject main) {

        develop = main.getBoolean("develop", false);

        JsonObject node;

        /** ssl **/
        node = main.getJsonObject("ssl");
        ssl.enabled = node.getBoolean("enabled");
        ssl.keyStoreFilename = node.getString("keyStoreFilename");
        ssl.keyStorePassword = node.getString("keyStorePassword");

        /** server **/
        node = main.getJsonObject("server");
        server.name = node.getString("name", "BEng CPD Server");
        server.scheme = node.getString("scheme", "https");
        server.hostname = node.getString("hostname", "localhost");
        server.port = node.getInteger("port", 8901);
        server.baseHref = checkBaseHref(node.getString("baseHref", "/"));
        server.allowedOriginPattern = node.getString("allowedOriginPattern");
        server.simLagTime = node.getLong("simLagTime", -1L);
        /** server.cacheBuilder **/
        node = main.getJsonObject("server").getJsonObject("cacheBuilder");
        server.cacheBuilder.concurrencyLevel = node.getInteger("concurrencyLevel", 1);
        server.cacheBuilder.initialCapacity = node.getInteger("initialCapacity", 100);
        server.cacheBuilder.maximumSize = node.getInteger("maximumSize", 1000);
        server.cacheBuilder.expireAfterAccess = node.getString("expireAfterAccess", "60m");
        /** server.auth **/
        node = main.getJsonObject("server").getJsonObject("auth");
        server.auth.path = checkPath(node.getString("path", "auth/"));
        /** server.api **/
        node = main.getJsonObject("server").getJsonObject("api");
        server.api.path = checkPath(node.getString("path", "api/"));
        /** server.assets **/
        node = main.getJsonObject("server").getJsonObject("assets");
        server.assets.allowListing = node.getBoolean("allowListing", false);

        /** ROOT app **/
        node = main.getJsonObject("webapp");
        webapp.routes = node.getJsonArray("routes").getList();
        webapp.diagramPath = node.getString("diagramPath", "diagram/");

        /** mongodb **/
        node = main.getJsonObject("mongodb");
        if ("".equals(node.getString("username"))) node.put("username", (String) null);
        if ("".equals(node.getString("password"))) node.put("password", (String) null);

        /** oauth2 **/
        node = main.getJsonObject("oauth2");
        oauth2.host = node.getString("host");
        oauth2.clientId = node.getString("client.id");
        oauth2.clientSecret = node.getString("client.secret");
        oauth2.site = node.getString("site");
        oauth2.tokenPath = node.getString("token.path");
        oauth2.authPath = node.getString("auth.path");
        oauth2.scope = node.getString("scope");

        _config = main;

    }

    public static JsonObject get() {
        return _config;
    }

    public static String host() {
        return new StringBuilder()
            .append(server.scheme)
            .append("://")
            .append(server.hostname)
            .append(":")
            .append(server.port)
            .toString();
    }

    public static String rootHref() {
        return new StringBuilder(host())
            .append(server.baseHref)
            .toString();
    }

    public static String apiHref() {
        return new StringBuilder(rootHref())
            .append(server.api.path)
            .toString();
    }

    public static String assetsHref() {
        return new StringBuilder(rootHref())
            .append(ASSETS_PATH)
            .toString();
    }

}
