package it.beng.modeler;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.Countdown;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.microservice.subroute.CollaborationsSubRoute;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class config {
    private static final Log logger = LogFactory.getLog(config.class);

    public static final String PROCESS_DEFINITION_KEY = "procedure-modeling-process";

    public static final String DATA_PATH = "data/";
    public static final String ASSETS_PATH = "assets/";

    private static JsonObject _config;
    private static MongoDB _mongoDB;
    private static SchemaTools _schemaTools;
    private static ProcessEngine _processEngine;
/*
    private static IdmEngine _idm;
*/

    public static String host(String scheme, String hostname, int port) {
        StringBuilder s = new StringBuilder(hostname);
        switch (scheme) {
            case "http":
                if (port != 80)
                    s.append(":").append(port);
                break;
            case "https":
                if (port != 443)
                    s.append(":").append(port);
                break;
        }
        return s.toString();
    }

    public static class OAuth2Config {
        public static class Flow {
            public List<String> scope;
            public String getUserProfile;

            public String scopeString(String delimiter) {
                return String.join(delimiter, scope);
            }
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
        public static String adminId;
        public static String name;
        public static String scheme;
        public static String hostname;
        public static Integer port;
        public static String baseHref;
        public static String allowedOriginPattern;
        public static Long simLagTime;
        public static List<String> subroutePaths = new LinkedList<>();

        public static void checkAndSetIfMainAdmin(JsonObject account) {
            if (account == null || adminId == null) return;
            if (adminId.equals(account.getString("id"))) {
                JsonObject roles = account.getJsonObject("roles");
                if (roles == null) return;
                roles.put("system", "admin");
            }
        }

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

        public static class eventBus {
            public static String path;
            public static String diagramAddress;
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
            return href() + /* app.path + */ languageCode(rc) + "/";
        }

        public static String appPath(RoutingContext rc) {
            return path() + /* app.path + */ languageCode(rc) + "/";
        }

        public static boolean isSubRoute(final String path) {
            return !subroutePaths.stream()
                                 .filter(p -> path.startsWith(p))
                                 .collect(Collectors.toList())
                                 .isEmpty();
        }

    }

    public static class app {
        // public static String path;
        // TODO: loginPage and notFoundPage must go in the configuration file
        public static String loginPage = "login";
        public static String notFoundPage = "404";
        public static boolean useLocalAuth;
        public static List<String> locales;
        public static String designerPath;

        public static List<String> localePaths() {
            return locales.stream().map(locale -> (locale + "/")).collect(Collectors.toList());
        }
    }

    public static class oauth2 {
        public static String origin;
        public static List<OAuth2Config> configs;
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

    private static String checkPath(String path, boolean isSubRoute) {
        if (path == null)
            throw new IllegalStateException("path cannot be null!");
        if (path.length() > 0) {
            if (path.startsWith("/"))
                throw new IllegalStateException("path CANNOT start with '/' character!");
            if (!path.endsWith("/"))
                throw new IllegalStateException("path MUST end with '/' character!");
        }
        if (isSubRoute)
            server.subroutePaths.add(path);
        return path;
    }

    public static void set(final Vertx vertx, final JsonObject config, Handler<Future<Void>> handler) {
        vertx.<JsonObject>executeBlocking(
            future -> future.complete(buildConfig(config)),
            done -> {
                if (done.succeeded())
                    _config = done.result();
            }
        );

        vertx.<MongoDB>executeBlocking(
            future -> future.complete(buildMongoDB(vertx)),
            done -> {
                if (done.succeeded())
                    _mongoDB = done.result();
            }
        );

        final Countdown countdown = new Countdown(2).onZero(zero -> {
            if (zero.succeeded())
                handler.handle(Future.succeededFuture());
            else
                handler.handle(Future.failedFuture(zero.cause()));
        });

        vertx.<SchemaTools>executeBlocking(
            blocking -> blocking.complete(new SchemaTools(
                vertx,
                _config.getJsonObject("mongodb"),
                "schemas",
                server.schema.uriBase(),
                server.scheme,
                new HashMap<String, String>() {{
                    put("$date", "\uFF04date");
                    put("$domain", "\uFF04domain");
                    put("$hidden", "\uFF04hidden");
                }},
                complete -> {
                    if (complete.succeeded())
                        countdown.next();
                    else
                        countdown.fail(complete.cause());
                })),
            result -> {
                if (result.succeeded())
                    _schemaTools = result.result();
            }
        );
        vertx.<ProcessEngine>executeBlocking(blocking -> blocking.complete(buildProcessEngine()), done -> {
            if (done.succeeded()) {
                _processEngine = done.result();
                countdown.next();
            } else countdown.fail(done.cause());
        });
    }

    private static JsonObject buildConfig(JsonObject config) {
        JsonObject node;

        develop = config.getBoolean("develop", false);
        version = config.getString("version");

        /* ssl */
        node = config.getJsonObject("ssl");
        ssl.enabled = node.getBoolean("enabled");
        ssl.keyStoreFilename = node.getString("keyStoreFilename");
        ssl.keyStorePassword = node.getString("keyStorePassword");

        /* server */
        node = config.getJsonObject("server");
        server.adminId = node.getString("adminId", null);
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
        server.schema.path = checkPath(node.getString("path", "schema/"), true);
        /* server.auth */
        node = config.getJsonObject("server").getJsonObject("auth");
        server.auth.path = checkPath(node.getString("path", "auth/"), true);
        /* server.api */
        node = config.getJsonObject("server").getJsonObject("api");
        server.api.path = checkPath(node.getString("path", "api/"), true);
        /* server.eventBus */
        node = config.getJsonObject("server").getJsonObject("eventBus");
        server.eventBus.path = checkPath(node.getString("path", "eventbus/"), true);
        server.eventBus.diagramAddress = node.getString("diagramAddress", "cpd::diagram");
        /* server.assets */
        node = config.getJsonObject("server").getJsonObject("assets");
        server.assets.allowListing = node.getBoolean("allowListing", false);

        checkPath(CollaborationsSubRoute.PATH, true);

        /* ROOT app */
        node = config.getJsonObject("app");
        // app.path = checkPath(node.getString("path", ""), false);
        app.useLocalAuth = node.getBoolean("useLocalAuth", false);
        app.locales = node.getJsonArray("locales").getList();
        app.designerPath = checkPath(node.getString("designerPath", "designer/"), false);

        /* oauth2 */
        node = config.getJsonObject("oauth2");
        oauth2.origin = node.getString("origin");
        oauth2.configs = new LinkedList<>();
        for (Object provider : node.getJsonArray("providers")) {
            JsonObject p = JsonObject.class.cast(provider);
            OAuth2Config oAuth2Config = new OAuth2Config();
            oAuth2Config.provider = p.getString("provider");
            oAuth2Config.logoUrl = server.baseHref + /* app.path + */ p.getString("logoUrl");
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
                JsonArray scope = f.getJsonArray("scope");
                if (scope != null)
                    oAuth2ConfigFlow.scope = scope.getList();
                oAuth2ConfigFlow.getUserProfile = f.getString("getUserProfile");
                oAuth2Config.flows.put(f.getString("flowType"), oAuth2ConfigFlow);
            }
            oauth2.configs.add(oAuth2Config);
        }

        _config = config;

        /* mongodb */

/*
            "host": "localhost",
            "port": 27017,
            "username": "",
            "password": "",
            "db_name": "cpd",
*/

        node = config.getJsonObject("mongodb");
        if ("".equals(node.getString("username")))
            node.put("username", (String) null);
        if ("".equals(node.getString("password")))
            node.put("password", (String) null);

        return config;
    }

    private static MongoDB buildMongoDB(Vertx vertx) {
        final JsonObject mongoDbConfig = _config.getJsonObject("mongodb");

        return MongoDB.createShared(
            vertx,
            mongoDbConfig,
            DATA_PATH + "db/commands/",
            new HashMap<String, String>() {
                {
                    put("id", "_id");
                    put("$domain", "\uFF04domain");
                }
            });
    }

    private static ProcessEngine buildProcessEngine() {
        if (develop) try {
            org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort", "9081")
                               .start();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ProcessEngine pe = new StandaloneProcessEngineConfiguration()
            .setJdbcUrl("jdbc:h2:file:./process-engine/database")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJdbcDriver("org.h2.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
            .buildProcessEngine();

        RepositoryService repositoryService = pe.getRepositoryService();

        if (repositoryService.createDeploymentQuery()
                             .processDefinitionKey(PROCESS_DEFINITION_KEY)
                             .singleResult() != null) {
            logger.info("'" + PROCESS_DEFINITION_KEY + "' found!");
        } else {
            logger.info("No '" + PROCESS_DEFINITION_KEY + "' found => deploying process definition...");
            repositoryService.createDeployment()
                             .key("procedure-modeling")
                             .name("Procedure Modeling")
                             .addClasspathResource("it/beng/modeler/processengine/Procedure_Modeling.bpmn20.xml")
                             .deploy();
        }

/*
        _idm = ((IdmEngineConfiguration) new StandaloneIdmEngineConfiguration()
            .setJdbcUrl("jdbc:h2:file:./process-engine/process-engine")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJdbcDriver("org.h2.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE))
            .buildIdmEngine();
*/
        return pe;
    }

    public static void tearDown() {
        _schemaTools.close();
        _processEngine.close();
        _mongoDB.close();
    }

    public static JsonObject get() {
        return _config;
    }

    public static MongoDB mongoDB() {
        return _mongoDB;
    }

    public static SchemaTools schemaTools() {
        return _schemaTools;
    }

    public static ProcessEngine processEngine() {
        return _processEngine;
    }

/*
    public static IdmEngine idmEngine() {
        return _idm;
    }
*/

    private static final Map<String, String> LANG_ALTERNATIVES = new HashMap<String, String>() {{
        put("ca", "es");
//        put("gl", "es");
    }};

    public static String languageCode(RoutingContext context) {
        if (config.develop)
            return "en";
        String code = context.preferredLanguage().tag();
        if (code == null || !config.app.locales.contains(code))
            code = "en";
        if (LANG_ALTERNATIVES.containsKey(code))
            return LANG_ALTERNATIVES.get(code);
        return code;
    }

    public static String language(RoutingContext context) {
        return language(languageCode(context));
    }

    public static String language(String code) {
        switch (code) {
            case "da":
                return "danish";
            case "nl":
                return "dutch";
            case "en":
                return "english";
            case "fi":
                return "finnish";
            case "fr":
                return "french";
            case "de":
                return "german";
            case "hu":
                return "hungarian";
            case "it":
                return "italian";
            case "nb":
                return "norwegian";
            case "pt":
                return "portuguese";
            case "ro":
                return "romanian";
            case "ru":
                return "russian";
            case "es":
                return "spanish";
            case "sv":
                return "swedish";
            case "tr":
                return "turkish";
            case "ara":
                return "arabic";
            case "prs":
                return "dari";
            case "pes":
                return "iranian persian";
            case "urd":
                return "urdu";
            case "zhs":
                return "simplified chinese";
            case "zht":
                return "traditional chinese";
            default:
                return "english";
        }
    }

    private static Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            } else {
                return getField(superClass, fieldName);
            }
        }
    }

}
