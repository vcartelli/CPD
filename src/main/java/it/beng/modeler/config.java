package it.beng.modeler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.*;


/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class config {

    public static final String DATA_PATH = "data/";
    public static final String ASSETS_PATH = "assets/";

    private static JsonObject _config;

    private static List<String> HOSTS;

    public static String host(String scheme, String hostname, int port) {
        StringBuilder s = new StringBuilder(hostname);
        switch (scheme) {
            case "http":
                if (port != 80) s.append(":").append(port);
            case "https":
                if (port != 443) s.append(":").append(port);
        }
        return s.toString();
    }

    public static class OAuth2Config {
        public static class Flow {
            public String scope;
            public String getUserProfile;
        }

        public String provider;
        public String logoUrl;
        public String site;
        public String authPath;
        public String tokenPath;
        public String introspectionPath;
        public String clientId;
        public String clientSecret;
        public Map<String, Flow> flows;
    }

    public static Boolean develop;
    public static String version;

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

        public static class pub {
            public static String scheme;
            public static String hostname;
            public static Integer port;

        }

        public static class cacheBuilder {
            public static int concurrencyLevel;
            public static int initialCapacity;
            public static int maximumSize;
            public static String expireAfterAccess;
        }

        public static class schema {
            public static String path;

            public static String uriBase() {
                return server.href() + path;
            }

            public static String fixUriScheme(String uri) {
                return uri.replaceFirst("^" + config.server.scheme, config.server.pub.scheme);
            }

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

        public static String origin() {
            return pub.scheme + "://" + host(pub.scheme, pub.hostname, pub.port);
        }

        public static String href() {
            return origin() + server.baseHref;
        }

        public static String path() {
            return server.baseHref;
        }

        public static String apiHref() {
            return href() + api.path;
        }

        public static String apiPath() {
            return path() + api.path;
        }

        public static String assetsHref() {
            return href() + ASSETS_PATH;
        }

        public static String assetsPath() {
            return path() + ASSETS_PATH;
        }

        public static String appHref(RoutingContext rc) {
            return href() + app.path + locale(rc) + "/";
        }

        public static String appPath(RoutingContext rc) {
            return path() + app.path + locale(rc) + "/";
        }

    }

    public static class app {
        public static String path;
        public static List<String> locales;
        public static List<String> routes;
        public static String diagramPath;
    }

    public static class oauth2 {
        public static String origin;
        public static List<OAuth2Config> configs;
    }

    public static class role {
        public static class cpd {
            public static class access {
                public static String prefix;
                public static String admin;
                public static String civilServant;
                public static String citizen;
            }

            public static class context {
                public static String prefix;

                public static class diagram {
                    public static String editor;
                    public static String owner;
                    public static String reviewer;
                    public static String collaborator;
                    public static String observer;
                }
            }
        }
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
        if (path.length() > 0) {
            if (path.startsWith("/"))
                throw new IllegalStateException("path CANNOT start with '/' character!");
            if (!path.endsWith("/"))
                throw new IllegalStateException("path MUST end with '/' character!");
        }
        return path;
    }

    public static void set(JsonObject config) {

        develop = config.getBoolean("develop", false);
        version = config.getString("version");

        JsonObject node;

        /* ssl */
        node = config.getJsonObject("ssl");
        ssl.enabled = node.getBoolean("enabled");
        ssl.keyStoreFilename = node.getString("keyStoreFilename");
        ssl.keyStorePassword = node.getString("keyStorePassword");

        /* server */
        node = config.getJsonObject("server");
        server.name = node.getString("name", "BEng CPD Server");
        server.scheme = node.getString("scheme", "https");
        server.hostname = node.getString("hostname", "localhost");
        server.port = node.getInteger("port", 8901);
        server.baseHref = checkBaseHref(node.getString("baseHref", "/"));
        server.allowedOriginPattern = node.getString("allowedOriginPattern");
        server.simLagTime = node.getLong("simLagTime", -1L);
        /* server.pub */
        node = config.getJsonObject("server").getJsonObject("pub");
        server.pub.scheme = node.getString("scheme", server.scheme);
        server.pub.hostname = node.getString("hostname", server.hostname);
        server.pub.port = node.getInteger("port", server.port);
        /* server.cacheBuilder */
        node = config.getJsonObject("server").getJsonObject("cacheBuilder");
        server.cacheBuilder.concurrencyLevel = node.getInteger("concurrencyLevel", 1);
        server.cacheBuilder.initialCapacity = node.getInteger("initialCapacity", 100);
        server.cacheBuilder.maximumSize = node.getInteger("maximumSize", 1000);
        server.cacheBuilder.expireAfterAccess = node.getString("expireAfterAccess", "60m");
        /* server.schema */
        node = config.getJsonObject("server").getJsonObject("schema");
        server.schema.path = checkPath(node.getString("path", "schema/"));
        /* server.auth */
        node = config.getJsonObject("server").getJsonObject("auth");
        server.auth.path = checkPath(node.getString("path", "auth/"));
        /* server.api */
        node = config.getJsonObject("server").getJsonObject("api");
        server.api.path = checkPath(node.getString("path", "api/"));
        /* server.assets */
        node = config.getJsonObject("server").getJsonObject("assets");
        server.assets.allowListing = node.getBoolean("allowListing", false);

        /* ROOT app */
        node = config.getJsonObject("app");
        app.path = checkPath(node.getString("path", ""));
        app.locales = node.getJsonArray("locales").getList();
        app.routes = node.getJsonArray("routes").getList();
        app.diagramPath = node.getString("diagramPath", "diagram/");

        /* mongodb */
        node = config.getJsonObject("mongodb");
        if ("".equals(node.getString("username"))) node.put("username", (String) null);
        if ("".equals(node.getString("password"))) node.put("password", (String) null);

        /* oauth2 */
        node = config.getJsonObject("oauth2");
        oauth2.origin = node.getString("origin");
        oauth2.configs = new LinkedList<>();
        for (Object provider : node.getJsonArray("providers")) {
            JsonObject p = JsonObject.class.cast(provider);
            OAuth2Config oAuth2Config = new OAuth2Config();
            oAuth2Config.provider = p.getString("provider");
            oAuth2Config.logoUrl = server.baseHref + app.path + p.getString("logoUrl");
            oAuth2Config.site = p.getString("site");
            oAuth2Config.tokenPath = p.getString("tokenPath");
            oAuth2Config.authPath = p.getString("authPath");
            oAuth2Config.introspectionPath = p.getString("introspectionPath");
            oAuth2Config.clientId = p.getString("clientId");
            oAuth2Config.clientSecret = p.getString("clientSecret");
            oAuth2Config.flows = new LinkedHashMap<>();
            for (Object flow : p.getJsonArray("flows")) {
                JsonObject f = JsonObject.class.cast(flow);
                OAuth2Config.Flow oAuth2ConfigFlow = new OAuth2Config.Flow();
                oAuth2ConfigFlow.scope = f.getString("scope");
                oAuth2ConfigFlow.getUserProfile = f.getString("getUserProfile");
                oAuth2Config.flows.put(f.getString("flowType"), oAuth2ConfigFlow);
            }
            oauth2.configs.add(oAuth2Config);
        }

        /* model */
        // roles.position
        node = config.getJsonObject("role").getJsonObject("cpd").getJsonObject("access");
        role.cpd.access.prefix = node.getString("prefix");
        role.cpd.access.admin = node.getString("admin");
        role.cpd.access.civilServant = node.getString("civilServant");
        role.cpd.access.citizen = node.getString("citizen");
        // roles.context
        node = config.getJsonObject("role").getJsonObject("cpd").getJsonObject("context");
        role.cpd.context.prefix = node.getString("prefix");
        // roles.context.diagram
        node = config.getJsonObject("role").getJsonObject("cpd").getJsonObject("context").getJsonObject("diagram");
        role.cpd.context.diagram.editor = node.getString("editor");
        role.cpd.context.diagram.owner = node.getString("owner");
        role.cpd.context.diagram.reviewer = node.getString("reviewer");
        role.cpd.context.diagram.collaborator = node.getString("collaborator");
        role.cpd.context.diagram.observer = node.getString("observer");

        _config = config;

    }

    public static JsonObject get() {
        return _config;
    }

    private static final List<String> SPANISH_ALTERNATIVES = Arrays.asList("ca", "gl");

    public static String locale(RoutingContext rc) {
        if (config.develop) return "en";
        String locale = null;
        // TODO: re-enable user language
//        if (rc.user() != null) locale = rc.user().principal().getJsonObject("profile").getString("language");
//        if (locale == null) {
        locale = rc.request().getHeader("Accept-Language");
        if (locale != null) locale = locale.substring(0, 2);
//        }
        if (locale != null && SPANISH_ALTERNATIVES.contains(locale)) locale = "es";
        if (locale == null || !config.app.locales.contains(locale)) locale = "en";
        return locale;
    }

}
