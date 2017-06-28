package it.beng.modeler.microservice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config;
import it.beng.modeler.microservice.subroute.*;
import it.beng.modeler.model.ModelTools;

import java.util.concurrent.TimeUnit;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class ModelerServerVerticle extends AbstractVerticle {

    static {
//        Typed.init();
//        Diagram.init();
//        SemanticElement.init();
    }

    @Override
    public void start(Future<Void> startFuture) {

        final String baseHref = config.server.baseHref;

        // Create a router object
        Router router = Router.router(vertx);

        router.route().handler(rc -> {
            if (rc.user() == null) {
                String userAgent = rc.request().getHeader("User-Agent");
                System.out.println("User-Agent: " + userAgent);
                if (userAgent.contains("Windows NT")
                    && userAgent.contains("Trident")
                    && (userAgent.contains("MSIE") || userAgent.contains("rv:11"))) {
                    rc.response()
                      .putHeader("Content-Type", "text/html; charset=UTF-8")
                      .setChunked(true)
                      .write("<html><body>" +
                          "<p>Internet Explorer is not supported!</p>" +
                          "<p>Please consider upgrading to <a href=\"https://www.microsoft.com/Windows\">Windows 10</a></p>" +
                          "<p>Or use one of the supported browsers:</p>" +
                          "<ul>" +
                          "<li><a href=\"https://chrome.google.com\">Google Chrome</a></li>" +
                          "<li><a href=\"https://www.mozilla.org/firefox\">Mozilla Firefox</a></li>" +
                          "</ul>" +
                          "</body></html>", "UTF-8").end();
                    return;
                }
            }
            rc.next();
        });

        // configure CORS origins and allowed methods
        router.route().handler(
            CorsHandler.create(config.server.allowedOriginPattern)
                       .allowedMethod(HttpMethod.GET)      // select # /<collection>/:id
                       .allowedMethod(HttpMethod.POST)     // insert # /<collection>
                       .allowedMethod(HttpMethod.PUT)      // update # /<collection>/:id
                       .allowedMethod(HttpMethod.DELETE)   // delete # <collection>/:id
                       .allowedHeader("X-PINGARUNER")
                       .allowedHeader("Content-Type"));
        System.out.println("CORS pattern is: " + config.server.allowedOriginPattern);

        // create cookie and session handler
        router.route().handler(CookieHandler.create());
        router.route().handler(
            SessionHandler.create(/*ClusteredSessionStore*/LocalSessionStore.create(vertx))
                          .setSessionCookieName("cpd.web.session")
                          .setCookieHttpOnlyFlag(true)
                          .setCookieSecureFlag(config.ssl.enabled)
                          .setNagHttps(config.ssl.enabled)
                          .setSessionTimeout(TimeUnit.HOURS.toMillis(12))
        );

        // enable body handler for [POST] and [PUT] methods
        final BodyHandler bodyHandler = BodyHandler.create();
//        router.route().method(HttpMethod.GET).handler(bodyHandler);
        router.route().method(HttpMethod.POST).handler(bodyHandler);
        router.route().method(HttpMethod.PUT).handler(bodyHandler);
//        router.route().method(HttpMethod.DELETE).handler(bodyHandler);

        // set secure headers in each response
        router.route().handler(rc -> {
            rc.response()
/*
                **X-Content-Type-Options**

                The 'X-Content-Type-Options' HTTP header if set to 'nosniff' stops the browser from guessing the MIME
                type of a file via content sniffing. Without this option set there is a potential increased risk of
                cross-site scripting.

                Secure configuration: Server returns the 'X-Content-Type-Options' HTTP header set to 'nosniff'.
*/
              .putHeader("X-Content-Type-Options", "nosniff")
/*
                **X-XSS-Protection**

                The 'X-XSS-Protection' HTTP header is used by Internet Explorer version 8 and higher. Setting this HTTP
                header will instruct Internet Explorer to enable its inbuilt anti-cross-site scripting filter. If
                enabled, but without 'mode=block' then there is an increased risk that otherwise non exploitable
                cross-site scripting vulnerabilities may potentially become exploitable.

                Secure configuration: Server returns the 'X-XSS-Protection' HTTP header set to '1; mode=block'.
*/
              .putHeader("X-XSS-Protection", "1; mode=block")
/*
                **X-Frame-Options**

                The 'X-Frame-Options' HTTP header can be used to indicate whether or not a browser should be allowed to
                render a page within a <frame> or <iframe>. The valid options are DENY, to deny allowing the page to
                exist in a frame or SAMEORIGIN to allow framing but only from the originating host. Without this option
                set the site is at a higher risk of click-jacking unless application level mitigations exist.

                Secure configuration: Server returns the 'X-Frame-Options' HTTP header set to 'DENY' or 'SAMEORIGIN'.
*/
              .putHeader("X-FRAME-OPTIONS", "DENY")
/*
                **Cache-Control**

                The 'Cache-Control' response header controls how pages can be cached either by proxies or the user's
                browser. Using this response header can provide enhanced privacy by not caching sensitive pages in the
                users local cache at the potential cost of performance. To stop pages from being cached the server sets
                a cache control by returning the 'Cache-Control' HTTP header set to 'no-store'.

                Secure configuration: Either the server sets a cache control by returning the 'Cache-Control' HTTP
                header set to 'no-store, no-cache' or each page sets their own via the 'meta' tag for secure
                connections.

                Updated: The above was updated after our friend Mark got in-touch. Originally we had said no-store was
                sufficient. But as with all things web related it appears Internet Explorer and Firefox work slightly
                differently (so everyone ensure you thank Mark!).
*/
              .putHeader("Cache-Control", "no-store, no-cache")
/*
                **Strict-Transport-Security**

                The 'HTTP Strict Transport Security' (Strict-Transport-Security) HTTP header is used to control if the
                browser is allowed to only access a site over a secure connection and how long to remember the server
                response for thus forcing continued usage.

                Note: This is a draft standard which only Firefox and Chrome support. But it is supported by sites such
                as PayPal. This header can only be set and honoured by web browsers over a trusted secure connection.

                Secure configuration: Return the 'Strict-Transport-Security' header with an appropriate timeout over an
                secure connection.
*/
              .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
/*
                **Access-Control-Allow-Origin**

                The 'Access Control Allow Origin' HTTP header is used to control which sites are allowed to bypass same
                origin policies and send cross-origin requests. This allows cross origin access without web application
                developers having to write mini proxies into their apps.

                Note: This is a draft standard which only Firefox and Chrome support, it is also advocarted by sites
                such as http://enable-cors.org/.

                Secure configuration: Either do not set or return the 'Access-Control-Allow-Origin' header restricting
                it to only a trusted set of sites.
*/
//                .putHeader("Access-Control-Allow-Origin", "a b c")
/*
                 IE8+ do not allow opening of attachments in the context of this resource
*/
              .putHeader("X-Download-Options", "noopen");
            if (config.develop) System.out.println("[" + rc.request().method() + "] " + rc.request().uri());
            rc.next();
        });

        // redirect base-href to app
        router.route(HttpMethod.GET, baseHref).handler(rc -> {
            SubRoute.redirect(rc, config.server.appPath(rc));
        });

        // create the mongodb client
        // use \uFF04 instead of $
        MongoDB mongodb = MongoDB.createShared(vertx, config().getJsonObject("mongodb"),
            config.DATA_PATH + "db/commands/", false/*config.develop*/);
        vertx.getOrCreateContext().put("mongodb", mongodb);
        SchemaTools schemaTools = new SchemaTools(vertx, config().getJsonObject("mongodb"), "schema",
            config.server.schema.uriBase(), config.develop);
        vertx.getOrCreateContext().put("schemaTools", schemaTools);
        ModelTools modelTools = new ModelTools(vertx, mongodb, schemaTools, config.develop);
        vertx.getOrCreateContext().put("modelTools", modelTools);

/*
        router.route(HttpMethod.GET, baseHref + "create-demo-data").handler(this::crateDemoData);
*/

        // in this order: assets, schema, auth, api, root
        new AssetsSubRoute(vertx, router, mongodb, schemaTools, modelTools);
        new SchemaSubRoute(vertx, router, mongodb, schemaTools, modelTools);
        new AuthSubRoute(vertx, router, mongodb, schemaTools, modelTools);
        new ApiSubRoute(vertx, router, mongodb, schemaTools, modelTools);
        new AppSubRoute(vertx, router, mongodb, schemaTools, modelTools);

        // redirect all non-handled [GET] to app
        router.route(HttpMethod.GET, "/*").handler(rc -> {
            String path = rc.request().path();
            if (path.startsWith(baseHref))
                path = path.replace(baseHref, "/");
            if (path.length() > 3 && config.app.locales.contains(path.substring(1, 3)))
                path = path.substring(3);
            System.out.println("redirecting to " + path);
            SubRoute.redirect(rc, config.server.appPath(rc) + path);
        });

        // handle failures
        router.route().failureHandler(rc -> {
            JsonObject error = rc.get("error");
            if (error == null) {
                if (config.develop) System.err.println("!!! no error info found in Routing Context !!!");
                error = ResponseError.json(rc, null);
            }
            System.err.println("ERROR (" + error.getInteger("statusCode") + "): " + error.encodePrettily());
            switch (rc.statusCode()) {
                case 404: {
                    // let root application find the resource or show the 404 not found page
                    rc.reroute(config.server.appPath(rc));
                    break;
                }
                default: {
                    rc.response()
                      .putHeader("content-type", "application/json; charset=utf-8")
                      .setStatusCode(error.getInteger("statusCode"))
                      .end(error.encode());
                }
            }
        });

        HttpServerOptions serverOptions = new HttpServerOptions().setSsl(config.ssl.enabled);
        if (serverOptions.isSsl())
            serverOptions.setKeyStoreOptions(
                new JksOptions()
                    .setPath(config.ssl.keyStoreFilename)
                    .setPassword(config.ssl.keyStorePassword))
//            .setTrustStoreOptions(
//                new JksOptions()
//                    .setPath(config.ssl.keyStoreFilename)
//                    .setPassword(config.ssl.keyStorePassword)
//            )
//            .setClientAuth(ClientAuth.REQUIRED)
                ;
        vertx.createHttpServer(serverOptions)
             .requestHandler(router::accept)
             .listen(config.server.port, ar -> {
                     if (ar.succeeded()) {
                         System.out.println("HTTP Server started: " + config.server.origin());
                         startFuture.complete();
                     } else {
                         System.err.println("Cannot start HTTP Server: " + config.server.origin() +
                             ". Cause: " + ar.cause().getMessage());
                         startFuture.fail(ar.cause());
                     }
                 }
             );
    }

}
