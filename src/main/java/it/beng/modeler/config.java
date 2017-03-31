package it.beng.modeler;

import io.vertx.core.json.JsonObject;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class config {

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

            public static String appHref(String locale) {
                return new StringBuilder(server.pub.appHref()).append(locale + "/").toString();
            }

        }

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

        public static String origin() {
            StringBuilder s = new StringBuilder().append(server.scheme).append("://").append(server.host);
            if (("http".equals(server.scheme) && server.port != 80) ||
                ("https".equals(server.scheme) && server.port != 443))
                s.append(":").append(server.port);
            return s.toString();
        }

        public static String href() {
            return new StringBuilder(server.origin())
                .append(server.baseHref)
                .toString();
        }

        public static String apiHref() {
            return new StringBuilder(server.href())
                .append(server.api.path)
                .toString();
        }

        public static String assetsHref() {
            return new StringBuilder(server.href())
                .append(ASSETS_PATH)
                .toString();
        }

        public static String appHref() {
            return new StringBuilder(server.href())
                .append(app.path)
                .toString();
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

    public static class model {
        public static class roles {
            public static class position {
                public static String admin;
                public static String civilServant;
                public static String citizen;
            }

            public static class diagramRole {
                public static String editor;
                public static String owner;
                public static String reviewer;
                public static String collaborator;
                public static String observer;
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
        server.host = node.getString("host", "localhost");
        server.port = node.getInteger("port", 8901);
        server.baseHref = checkBaseHref(node.getString("baseHref", "/"));
        server.allowedOriginPattern = node.getString("allowedOriginPattern");
        server.simLagTime = node.getLong("simLagTime", -1L);
        /** server.pub **/
        node = main.getJsonObject("server").getJsonObject("pub");
        server.pub.scheme = node.getString("scheme", server.scheme);
        server.pub.host = node.getString("host", server.host);
        server.pub.port = node.getInteger("port", server.port);
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
        node = main.getJsonObject("app");
        app.path = checkPath(node.getString("path", ""));
        app.locales = node.getJsonArray("locales").getList();
        app.routes = node.getJsonArray("routes").getList();
        app.diagramPath = node.getString("diagramPath", "diagram/");

        /** mongodb **/
        node = main.getJsonObject("mongodb");
        if ("".equals(node.getString("username"))) node.put("username", (String) null);
        if ("".equals(node.getString("password"))) node.put("password", (String) null);

        /** oauth2 **/
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

        /** model **/
        // roles.position
        node = main.getJsonObject("model").getJsonObject("roles").getJsonObject("position");
        model.roles.position.admin = node.getString("admin");
        model.roles.position.civilServant = node.getString("civilServant");
        model.roles.position.citizen = node.getString("citizen");
        // roles.diagramRole
        node = main.getJsonObject("model").getJsonObject("roles").getJsonObject("diagramRole");
        model.roles.diagramRole.editor = node.getString("editor");
        model.roles.diagramRole.owner = node.getString("owner");
        model.roles.diagramRole.reviewer = node.getString("reviewer");
        model.roles.diagramRole.collaborator = node.getString("collaborator");
        model.roles.diagramRole.observer = node.getString("observer");

        _config = main;

    }

    public static JsonObject get() {
        return _config;
    }

}
