package it.beng.modeler;

import io.vertx.core.json.JsonObject;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class config {

    private static JsonObject _config;

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
        public static String allowedOriginPattern;
        public static Long simLagTime;

        public static class cacheBuilder {
            public static int concurrencyLevel;
            public static int initialCapacity;
            public static int maximumSize;
            public static String expireAfterAccess;
        }

        public static class auth {
            public static String base;
        }

        public static class api {
            public static String base;
        }

        public static class assets {
            public static String base;
            public static Boolean allowListing;
        }

    }

    public static class webapp {
        public static String loginRoute;
        public static String diagramRoute;
    }

    public static class oauth2 {
        public static String clientId;
        public static String clientSecret;
        public static String site;
        public static String tokenPath;
        public static String authPath;
        public static String scope;
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
        server.auth.base = node.getString("base", "/auth");
        /** server.api **/
        node = main.getJsonObject("server").getJsonObject("api");
        server.api.base = node.getString("base", "/api");
        /** server.assets **/
        node = main.getJsonObject("server").getJsonObject("assets");
        server.assets.base = node.getString("base", "/assets");
        server.assets.allowListing = node.getBoolean("allowListing", false);

        /** ROOT app **/
        node = main.getJsonObject("webapp");
        webapp.loginRoute = node.getString("loginRoute", "/login");
        webapp.diagramRoute = node.getString("diagramRoute", "/diagram");

        /** mongodb **/
        node = main.getJsonObject("mongodb");
        if ("".equals(node.getString("username"))) node.put("username", (String) null);
        if ("".equals(node.getString("keyStorePassword"))) node.put("keyStorePassword", (String) null);

        /** oauth2 **/
        node = main.getJsonObject("oauth2");
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

    public static String rootOrigin() {
        return new StringBuilder()
            .append(server.scheme)
            .append("://")
            .append(server.hostname)
            .append(":")
            .append(server.port)
            .toString();
    }

    public static String apiOrigin() {
        return new StringBuilder(rootOrigin())
            .append(server.api.base)
            .toString();
    }

    public static String assetOrigin() {
        return new StringBuilder(rootOrigin())
            .append(server.assets.base)
            .toString();
    }

}
