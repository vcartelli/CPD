package it.beng.modeler;

import io.vertx.core.json.JsonObject;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class config {

    private static JsonObject _config;

    public static Boolean developMode;

    public static final class keystore {
        public static String filename;
        public static String password;
    }

    public static final class server {
        public static boolean developMode;
        public static String name;
        public static String scheme;
        public static String hostname;
        public static Integer port;
        public static String allowedOriginPattern;
        public static String cacheBuilderSpec;
        public static Long simLagTime;

        public static class api {
            public static String base;
        }

        public static class assets {
            public static String base;
            public static Boolean allowListing;
        }

        public static class root {
            public static String diagramBase;
            public static String elementBase;
        }
    }

    public static class mongodb {

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

        developMode = main.getBoolean("develop.mode", false);

        JsonObject node;

        /** keystore **/
        node = main.getJsonObject("keystore");
        keystore.filename = node.getString("filename");
        keystore.password = node.getString("password");

        /** server **/
        node = main.getJsonObject("server");
        server.name = node.getString("name", "BEng Modeler Server");
        server.scheme = node.getString("scheme", "https");
        server.hostname = node.getString("hostname");
        server.port = node.getInteger("port", 8901);
        server.allowedOriginPattern = node.getString("allowed.origin.pattern");
        server.cacheBuilderSpec = node.getString("cache.builder.spec");
        server.simLagTime = node.getLong("sim.lagtime", -1L);
        /** server.api **/
        node = main.getJsonObject("server").getJsonObject("api");
        server.api.base = node.getString("base", "/api");
        /** server.assets **/
        node = main.getJsonObject("server").getJsonObject("assets");
        server.assets.base = node.getString("base", "/assets");
        server.assets.allowListing = node.getBoolean("allow.listing", false);
        /** server.root **/
        node = main.getJsonObject("server").getJsonObject("root");
        server.root.diagramBase = node.getString("diagram.base", "/diagram/:diagramId");
        server.root.elementBase = node.getString("diagram.base", "/diagram/:diagramId/:elementId");

        /** mongodb **/
        node = main.getJsonObject("mongodb");

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

    public static String diagramHref(String diagramId) {
        return new StringBuilder(rootOrigin())
            .append(server.root.diagramBase.replace("{diagramId}", diagramId))
            .toString();
    }

    public static String diagramElementHref(String diagramId, String elementId) {
        return new StringBuilder(rootOrigin())
            .append(server.root.elementBase.replace("{diagramId}", diagramId).replace("{diagramElementId}", elementId))
            .toString();
    }

}
