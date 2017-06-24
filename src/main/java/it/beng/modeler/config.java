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

    private static JsonObject _config;

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

    public static final String ASSETS_PATH = "assets/";
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
        public static String host;
        public static Integer port;
        public static String baseHref;
        public static String allowedOriginPattern;
        public static Long simLagTime;

        public static class pub {
            public static String scheme;
            public static String host;
            public static Integer port;

            public static String origin() {
                StringBuilder s = new StringBuilder().append(server.pub.scheme).append("://").append(server.pub.host);
                if (("http".equals(pub.scheme) && pub.port != 80) ||
                    ("https".equals(pub.scheme) && pub.port != 443))
                    s.append(":").append(server.pub.port);
                return s.toString();
            }

            public static String href() {
                return new StringBuilder(server.pub.origin()).append(server.baseHref).toString();
            }

            public static String apiHref() {
                return new StringBuilder(server.pub.href()).append(server.api.path).toString();
            }

            public static String assetsHref() {
                return new StringBuilder(server.pub.href()).append(ASSETS_PATH).toString();
            }

            public static String appHref() {
                return new StringBuilder(server.pub.href()).append(app.path).toString();
            }

            public static String appHref(RoutingContext rc) {
                return new StringBuilder(server.pub.appHref()).append(locale(rc) + "/").toString();
            }

        }

        public static class cacheBuilder {
            public static int concurrencyLevel;
            public static int initialCapacity;
            public static int maximumSize;
            public static String expireAfterAccess;
        }

        public static class schema {
            public static String path;

            public static String uriBase() {return server.href() + schema.path;}
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
            StringBuilder s = new StringBuilder().append(server.scheme).append("://").append(server.host);
            if (("http".equals(server.scheme) && server.port != 80) ||
                ("https".equals(server.scheme) && server.port != 443))
                s.append(":").append(server.port);
            return s.toString();
        }

        public static String href() {
            return server.origin() + server.baseHref;
        }

        public static String apiHref() {
            return server.href() + api.path;
        }

        public static String assetsHref() {
            return server.href() + ASSETS_PATH;
        }

        public static String appHref() {
            return server.href() + app.path;
        }

        public static String appPath(RoutingContext rc) {
            return server.baseHref + app.path + locale(rc);
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

    public static void set(JsonObject main) {

        develop = main.getBoolean("develop", false);
        version = main.getString("version");

        JsonObject node;

        /* ssl */
        node = main.getJsonObject("ssl");
        ssl.enabled = node.getBoolean("enabled");
        ssl.keyStoreFilename = node.getString("keyStoreFilename");
        ssl.keyStorePassword = node.getString("keyStorePassword");

        /* server */
        node = main.getJsonObject("server");
        server.name = node.getString("name", "BEng CPD Server");
        server.scheme = node.getString("scheme", "https");
        server.host = node.getString("host", "localhost");
        server.port = node.getInteger("port", 8901);
        server.baseHref = checkBaseHref(node.getString("baseHref", "/"));
        server.allowedOriginPattern = node.getString("allowedOriginPattern");
        server.simLagTime = node.getLong("simLagTime", -1L);
        /* server.pub */
        node = main.getJsonObject("server").getJsonObject("pub");
        server.pub.scheme = node.getString("scheme", server.scheme);
        server.pub.host = node.getString("host", server.host);
        server.pub.port = node.getInteger("port", server.port);
        /* server.cacheBuilder */
        node = main.getJsonObject("server").getJsonObject("cacheBuilder");
        server.cacheBuilder.concurrencyLevel = node.getInteger("concurrencyLevel", 1);
        server.cacheBuilder.initialCapacity = node.getInteger("initialCapacity", 100);
        server.cacheBuilder.maximumSize = node.getInteger("maximumSize", 1000);
        server.cacheBuilder.expireAfterAccess = node.getString("expireAfterAccess", "60m");
        /* server.schema */
        node = main.getJsonObject("server").getJsonObject("schema");
        server.schema.path = checkPath(node.getString("path", "schema/"));
        /* server.auth */
        node = main.getJsonObject("server").getJsonObject("auth");
        server.auth.path = checkPath(node.getString("path", "auth/"));
        /* server.api */
        node = main.getJsonObject("server").getJsonObject("api");
        server.api.path = checkPath(node.getString("path", "api/"));
        /* server.assets */
        node = main.getJsonObject("server").getJsonObject("assets");
        server.assets.allowListing = node.getBoolean("allowListing", false);

        /* ROOT app */
        node = main.getJsonObject("app");
        app.path = checkPath(node.getString("path", ""));
        app.locales = node.getJsonArray("locales").getList();
        app.routes = node.getJsonArray("routes").getList();
        app.diagramPath = node.getString("diagramPath", "diagram/");

        /* mongodb */
        node = main.getJsonObject("mongodb");
        if ("".equals(node.getString("username"))) node.put("username", (String) null);
        if ("".equals(node.getString("password"))) node.put("password", (String) null);

        /* oauth2 */
        node = main.getJsonObject("oauth2");
        oauth2.origin = node.getString("origin");
        oauth2.configs = new LinkedList<>();
        for (Object provider : node.getJsonArray("providers")) {
            JsonObject p = JsonObject.class.cast(provider);
            OAuth2Config oAuth2Config = new OAuth2Config();
            oAuth2Config.provider = p.getString("provider");
            oAuth2Config.logoUrl = server.baseHref + p.getString("logoUrl");
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
        node = main.getJsonObject("role").getJsonObject("cpd").getJsonObject("access");
        role.cpd.access.prefix = node.getString("prefix");
        role.cpd.access.admin = node.getString("admin");
        role.cpd.access.civilServant = node.getString("civilServant");
        role.cpd.access.citizen = node.getString("citizen");
        // roles.context
        node = main.getJsonObject("role").getJsonObject("cpd").getJsonObject("context");
        role.cpd.context.prefix = node.getString("prefix");
        // roles.context.diagram
        node = main.getJsonObject("role").getJsonObject("cpd").getJsonObject("context").getJsonObject("diagram");
        role.cpd.context.diagram.editor = node.getString("editor");
        role.cpd.context.diagram.owner = node.getString("owner");
        role.cpd.context.diagram.reviewer = node.getString("reviewer");
        role.cpd.context.diagram.collaborator = node.getString("collaborator");
        role.cpd.context.diagram.observer = node.getString("observer");

        _config = main;

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
        if (locale == null) {
            locale = rc.request().getHeader("Accept-Language");
            if (locale != null) locale = locale.substring(0, 2);
        }
        if (locale != null && SPANISH_ALTERNATIVES.contains(locale)) locale = "es";
        if (locale == null || !config.app.locales.contains(locale)) locale = "en";
        return locale;
    }

}
