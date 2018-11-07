package it.beng.modeler.config;

import io.vertx.core.AsyncResult;
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
import it.beng.modeler.microservice.utils.DBUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class cpd {
    private static final Logger logger = LogManager.getLogger(cpd.class);

    public static final Pattern VERSION_PATTERN = Pattern.compile("(^\\d+)(?:\\.(\\d+))?");
    private static boolean VERSION_UPGRADE = false;

    public static class Process {
        public static final String CATEGORY = "Collaborative Procedure Designer";
        public static final String PROCEDURE_MODELING_KEY = "procedure-modeling-process";
        public static final String PROCEDURE_MODELING_NAME = "Procedure Modeling Process";
    }

    public static final String DATA_PATH = "data/";
    public static final String ASSETS_PATH = "assets/";

    public static boolean develop;

    private static JsonObject _config;
    private static MongoDB _mongoDB;
    private static ProcessEngine _processEngine;
    //    private static IdmEngine _idm;
    private static SchemaTools _schemaTools;
    private static Map<String, Object> _properties;

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
                return uri.replaceFirst("^" + cpd.server.scheme, cpd.server.pub.scheme);
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

    String xxx = "undefined";

    public static void setup(final Vertx vertx, final JsonObject config, Handler<AsyncResult<Void>> complete) {
        final Countdown setupStep = new Countdown(8).setCompleteHandler(zero -> {
            if (zero.succeeded())
                complete.handle(Future.succeededFuture());
            else
                complete.handle(Future.failedFuture(zero.cause()));
        });

        final Handler<AsyncResult<Void>> onBlockingCodeCompleted = c -> {
            if (c.succeeded()) setupStep.next();
            else setupStep.fail(c.cause());
        };

        // (1) configuration => setupStep x2
        vertx.executeBlocking(
            future -> future.complete(buildConfig(vertx, config, onBlockingCodeCompleted)),
            result -> {
                if (result.succeeded()) setupStep.next();
                else complete.handle(Future.failedFuture(result.cause()));
            }
        );

        // (2) database => setupStep x2
        vertx.executeBlocking(
            future -> buildDatabase(vertx, databaseUpgraded -> {
                if (databaseUpgraded.succeeded()) {
                    VERSION_UPGRADE = databaseUpgraded.result();
                    future.complete();
                    onBlockingCodeCompleted.handle(Future.succeededFuture());
                } else onBlockingCodeCompleted.handle(Future.failedFuture(databaseUpgraded.cause()));
            }),
            result -> {
                if (result.succeeded()) setupStep.next();
                else complete.handle(Future.failedFuture(result.cause()));
            }
        );

        // (3) process engine => setupStep x2
        vertx.executeBlocking(
            future -> future.complete(buildProcessEngine(onBlockingCodeCompleted)),
            result -> {
                if (result.succeeded()) setupStep.next();
                else complete.handle(Future.failedFuture(result.cause()));
            }
        );

        // (4) schema tools => setupStep x2
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
                }}, onBlockingCodeCompleted)),
            result -> {
                if (result.succeeded()) {
                    _schemaTools = result.result();
                    setupStep.next();
                } else setupStep.fail(result.cause());
            }
        );
    }

    public static Void buildConfig(Vertx vertx, JsonObject config, Handler<AsyncResult<Void>> complete) {
        _config = config;
        develop = config.getBoolean("develop", false);

        JsonObject node;
        /* ssl */
        node = config.getJsonObject("ssl");
        cpd.ssl.enabled = node.getBoolean("enabled");
        cpd.ssl.keyStoreFilename = node.getString("keyStoreFilename");
        cpd.ssl.keyStorePassword = node.getString("keyStorePassword");

        /* server */
        node = config.getJsonObject("server");
        cpd.server.adminId = node.getString("adminId", null);
        cpd.server.name = node.getString("name", "BEng CPD Server");
        cpd.server.scheme = node.getString("scheme", "https");
        cpd.server.hostname = node.getString("hostname", "localhost");
        cpd.server.port = node.getInteger("port", 8901);
        cpd.server.baseHref = checkBaseHref(node.getString("baseHref", "/"));
        cpd.server.allowedOriginPattern = node.getString("allowedOriginPattern");
        cpd.server.simLagTime = node.getLong("simLagTime", -1L);

        /* server.pub */
        node = config.getJsonObject("server").getJsonObject("pub");
        cpd.server.pub.scheme = node.getString("scheme", cpd.server.scheme);
        cpd.server.pub.hostname = node.getString("hostname", cpd.server.hostname);
        cpd.server.pub.port = node.getInteger("port", cpd.server.port);

        /* server.cacheBuilder */
        node = config.getJsonObject("server").getJsonObject("cacheBuilder");
        cpd.server.cacheBuilder.concurrencyLevel = node.getInteger("concurrencyLevel", 1);
        cpd.server.cacheBuilder.initialCapacity = node.getInteger("initialCapacity", 100);
        cpd.server.cacheBuilder.maximumSize = node.getInteger("maximumSize", 1000);
        cpd.server.cacheBuilder.expireAfterAccess = node.getString("expireAfterAccess", "60m");

        /* server.schema */
        node = config.getJsonObject("server").getJsonObject("schema");
        cpd.server.schema.path = checkPath(node.getString("path", "schema/"), true);

        /* server.auth */
        node = config.getJsonObject("server").getJsonObject("auth");
        cpd.server.auth.path = checkPath(node.getString("path", "auth/"), true);

        /* server.api */
        node = config.getJsonObject("server").getJsonObject("api");
        cpd.server.api.path = checkPath(node.getString("path", "api/"), true);

        /* server.eventBus */
        node = config.getJsonObject("server").getJsonObject("eventBus");
        cpd.server.eventBus.path = checkPath(node.getString("path", "eventbus/"), true);
        cpd.server.eventBus.diagramAddress = node.getString("diagramAddress", "cpd::diagram");

        /* server.assets */
        node = config.getJsonObject("server").getJsonObject("assets");
        cpd.server.assets.allowListing = node.getBoolean("allowListing", false);

        checkPath(CollaborationsSubRoute.PATH, true);

        /* ROOT app */
        node = config.getJsonObject("app");
        // app.path = checkPath(node.getString("path", ""), false);
        cpd.app.useLocalAuth = node.getBoolean("useLocalAuth", false);
        cpd.app.locales = node.getJsonArray("locales").getList();
        cpd.app.designerPath = checkPath(node.getString("designerPath", "designer/"), false);

        /* oauth2 */
        node = config.getJsonObject("oauth2");
        cpd.oauth2.origin = node.getString("origin");
        cpd.oauth2.configs = new LinkedList<>();
        for (Object provider : node.getJsonArray("providers")) {
            JsonObject p = JsonObject.class.cast(provider);
            OAuth2Config oAuth2Config = new OAuth2Config();
            oAuth2Config.provider = p.getString("provider");
            oAuth2Config.logoUrl = cpd.server.baseHref + /* app.path + */ p.getString("logoUrl");
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
            cpd.oauth2.configs.add(oAuth2Config);
        }

//        set mongodb username and password to null if empty
        node = config.getJsonObject("mongodb");
        if ("".equals(node.getString("username")))
            node.put("username", (String) null);
        if ("".equals(node.getString("password")))
            node.put("password", (String) null);

        complete.handle(Future.succeededFuture());
        return null;
    }

    private static Void buildDatabase(Vertx vertx, Handler<AsyncResult<Boolean>> complete) {
        _mongoDB = MongoDB.createShared(
            vertx,
            _config.getJsonObject("mongodb"),
            DATA_PATH + "db/commands/",
            new HashMap<String, String>() {
                {
                    put("id", "_id");
                    put("$domain", "\uFF04domain");
                }
            });

        final String version = _config.getString("version");
        DBUtils.loadCollection("properties", loadProperties -> {
            if (loadProperties.succeeded()) {
                _properties = loadProperties.result().stream().collect(Collectors.toMap(
                    o -> o.getString("id"),
                    o -> o.getValue("value")));
                final String dbVersion = (String) _properties.get("version");
                if (dbVersion != null && compareVersions(version, dbVersion) > 0) {
                    // version > DB version
                    vertx.fileSystem().readFile(DATA_PATH + "db/upgrade.json", readFile -> {
                        if (readFile.succeeded()) {
                            JsonObject upgrade = readFile.result().toJsonObject();
                            if (!version.equals(upgrade.getString("version"))) {
                                complete.handle(Future.failedFuture(
                                    "upgrade version mismatch: expected version is '" + version
                                        + "' but upgrade version is '" + upgrade.getString("version") + "'"));
                                return;
                            }
                            if (!upgrade.containsKey(dbVersion)) {
                                logger.warn("no database upgrade found for '" + dbVersion + "' => '" + version + "'");
                                // NO DB upgrade, but it is an upgrade (version > DB version)
                                complete.handle(Future.succeededFuture(true)); // upgrade
                            } else {
                                logger.warn("upgrading database from version '" + dbVersion
                                    + "' to version '" + version + "'");
                                JsonObject changes = upgrade.getJsonObject(dbVersion);
                                logger.debug("database changes: " + changes.encodePrettily());
                                // TODO: upgrade the database based on changes model
                                // ... and finally update version:
                                _mongoDB.findOneAndUpdate(
                                    "properties",
                                    DBUtils.ID("version"),
                                    new JsonObject()
                                        .put("$set", new JsonObject()
                                            .put("value", version)),
                                    findOneAndUpdate -> {
                                        if (findOneAndUpdate.succeeded()) {
                                            _properties.put("version", version);
                                            complete.handle(Future.succeededFuture(true)); // upgrade
                                        } else complete.handle(Future.failedFuture(findOneAndUpdate.cause()));
                                    });
                            }
                        } else complete.handle(Future.failedFuture(readFile.cause()));
                    });
                } else if (version.equals(dbVersion)) {
                    // DB version = version => NO upgrade
                    complete.handle(Future.succeededFuture(false)); // NO upgrade
                } else {
                    // DB version = null => there is no database, so it needs to be created from scripts
                    // TODO: create new DB from some script files and insert latestDbVersion in "versions" collection
                    complete.handle(Future.succeededFuture(true)); // upgrade
                }

            } else complete.handle(Future.failedFuture(loadProperties.cause()));
        });
        return null;
    }

    private static int compareVersions(String version, String other) {
        Matcher v = VERSION_PATTERN.matcher(version);
        Matcher o = VERSION_PATTERN.matcher(other);
        if (v.find() && o.find()) {
            int major = Integer.parseInt(v.group(1)) - Integer.parseInt(o.group(1));
            return major != 0 ? major : Integer.parseInt(v.group(2)) - Integer.parseInt(o.group(2));
        } else return 0;
    }

    private static Void buildProcessEngine(Handler<AsyncResult<Void>> complete) {
        if (develop) try {
            org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort", "9081")
                               .start();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        _processEngine = new StandaloneProcessEngineConfiguration()
            .setJdbcUrl("jdbc:h2:file:./process-engine/database")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJdbcDriver("org.h2.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
            .buildProcessEngine();

        RepositoryService repositoryService = _processEngine.getRepositoryService();

        if (VERSION_UPGRADE || repositoryService.createDeploymentQuery()
                                                .deploymentCategory(Process.CATEGORY)
                                                .list()
                                                .size() == 0) {
            // deploy the new processes in case of a version upgrade or no deploy present
            logger.info("deploying process definitions because of " +
                (VERSION_UPGRADE ? "VERSION UPGRADE (" + _properties.get("version") + ")" : "NO DEPLOYMENTS FOUND"));
            repositoryService.createDeployment()
                             .category(Process.CATEGORY)
                             .key(Process.PROCEDURE_MODELING_KEY)
                             .name(Process.PROCEDURE_MODELING_NAME)
                             .addClasspathResource("it/beng/modeler/processengine/Procedure_Modeling.bpmn20.xml")
                             .deploy();
            // other process definitions here ...
        }
        logger.info("'" + Process.CATEGORY + "' is deployed");

/*
        _idm = ((IdmEngineConfiguration) new StandaloneIdmEngineConfiguration()
            .setJdbcUrl("jdbc:h2:file:./process-engine/process-engine")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJdbcDriver("org.h2.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE))
            .buildIdmEngine();
*/
        complete.handle(Future.succeededFuture());
        return null;
    }

    public static void tearDown() {
        logger.info("Disposing SchemaTools...");
        _schemaTools.close();
        logger.info("Disposing ProcessEngine...");
        _processEngine.close();
        logger.info("Disposing MongoDB...");
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
        if (cpd.develop)
            return "en";
        String code = context.preferredLanguage().tag();
        if (code == null || !cpd.app.locales.contains(code))
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
