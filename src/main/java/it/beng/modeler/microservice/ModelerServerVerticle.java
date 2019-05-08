package it.beng.modeler.microservice;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.subroute.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.util.locale.LanguageTag;

import java.util.concurrent.TimeUnit;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class ModelerServerVerticle extends AbstractVerticle {
    private static final Logger logger = LogManager.getLogger(ModelerServerVerticle.class);

    @Override
    public void start(Future<Void> startFuture) {

        // Create a router object
        Router router = Router.router(vertx);

        router.route().handler(context -> {
            if (context.user() == null) {
                String userAgent = context.request().getHeader("User-Agent");
                if (userAgent.contains("Windows NT") && userAgent.contains("Trident")
                    && (userAgent.contains("MSIE") || userAgent.contains("rv:11"))) {
                    context.response()
                           .putHeader("Content-Type", "text/html; charset=UTF-8")
                           .setChunked(true)
                           .write("<html><body>" + "<p>Internet Explorer is not supported!</p>"
                                   + "<p>Please consider upgrading to <a href=\"https://www.microsoft.com/Windows\">Windows 10</a></p>"
                                   + "<p>Or use one of the supported browsers:</p>" + "<ul>"
                                   + "<li><a href=\"https://chrome.google.com\">Google Chrome</a></li>"
                                   + "<li><a href=\"https://www.mozilla.org/firefox\">Mozilla Firefox</a></li>"
                                   + "</ul>" + "</body></html>",
                               "UTF-8")
                           .end();
                    return;
                }
            }
            context.next();
        });

        // configure CORS origins and allowed methods
        CorsHandler corsHandler = CorsHandler.create(cpd.server.allowedOriginPattern);
        logger.info("CORS pattern is: " + cpd.server.allowedOriginPattern);
        corsHandler.allowedMethod(HttpMethod.GET) // select # /<collection>/:id
                   .allowedMethod(HttpMethod.POST)       // insert # /<collection>
                   .allowedMethod(HttpMethod.PUT)        // update # /<collection>/:id
                   .allowedMethod(HttpMethod.DELETE)     // delete # /<collection>/:id
                   .allowedHeader("X-PINGARUNER")
                   .allowedHeader("Content-Type");
        router.route().handler(corsHandler);

        // set secure headers in each response
        router.route().handler(context -> {
            context.response()
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
//                   .putHeader("X-FRAME-OPTIONS", "DENY")
                   .putHeader("X-FRAME-OPTIONS", "SAMEORIGIN")
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
            logger
                .debug(context.request().remoteAddress() + " [" + context.request().method() + "] " + context.request()
                                                                                                             .uri());
            context.next();
        });

        // create cookie and session handler
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler
            .create(/* ClusteredSessionStore */LocalSessionStore.create(vertx))
            .setSessionCookieName(cpd.sessionCookieName())
            .setCookieHttpOnlyFlag(true)
            .setCookieSecureFlag(!cpd.develop())
            .setNagHttps(!cpd.develop())
            .setSessionTimeout(TimeUnit.HOURS.toMillis(12)));
        logger.info("Session cookie created: " + new JsonObject()
            .put("name", cpd.sessionCookieName())
            .put("HTTPOnly", true)
            .put("SECURE", !cpd.develop())
            .put("TimeoutMS", TimeUnit.HOURS.toMillis(12))
            .encodePrettily()
        );

        // this must be declared here, after the session handler and before the body handler
        new EventBusSubRoute(vertx, router);

        // enable body handler for [POST] and [PUT] methods
        final BodyHandler bodyHandler = BodyHandler.create();
        //        router.route().method(HttpMethod.GET).handler(bodyHandler);
        router.route().method(HttpMethod.POST).handler(bodyHandler);
        router.route().method(HttpMethod.PUT).handler(bodyHandler);
        router.route().method(HttpMethod.DELETE).handler(bodyHandler);

        // CSRF
        router.route().handler(CSRFHandler.create(cpd.server.secret.csrf));

        // JWT: {"typ":"JWT","alg":"HS256"}
        // TODO: JWT

        // redirect base-href to app
        router.routeWithRegex(HttpMethod.GET, "^" + cpd.server.baseHref.replace("/", "\\/") + "?$").handler(context -> {
            SubRoute.redirect(context, cpd.server.appPath(context));
        });

        // vertx.getOrCreateContext().put("schemaTools", schemaTools);
        // ModelTools modelTools = new ModelTools(vertx, config.mongoDB(), schemaTools, config.develop);
        // vertx.getOrCreateContext().put("modelTools", modelTools);

        // NOTE: subroute instantiation order IS IMPORTANT!!!
        new AssetsSubRoute(vertx, router);          // assets accessible to the world
        new SchemaSubRoute(vertx, router);          // schemas accessible to the world
        new AppSubRoute(vertx, router);             // app accessible to the world
        new AuthSubRoute(vertx, router);            // UserSessionHandler in order to retrieve the user associated to the session
        new ApiSubRoute(vertx, router);
        new CollaborationsSubRoute(vertx, router);
        new DataSubRoute(vertx, router);

        // if we arrived here it means no resource has been found, 
        // lets use the failure handler instead of default "Resource not found" page
        router.route().handler(context -> context.fail(HttpResponseStatus.NOT_FOUND.code()));

        // handle failures
        router.route().failureHandler(context -> {
            logger.debug("route failure (" + context.statusCode() + "): " + Json.encodePrettily(context.failure()));
            switch (context.statusCode()) {
                case 404: // NOT_FOUND
                    String path = context.request().path();
                    if (path.startsWith(cpd.server.baseHref)) {
                        path = path.substring(cpd.server.baseHref.length());
                        final String lang = path.substring(path.indexOf('/'));
                        if (LanguageTag.isLanguage(lang))
                            path = path.substring(lang.length());
                    }
                    while (path.startsWith("/")) {
                        // remove any beginning slash
                        path = path.substring(1);
                    }
                    if (cpd.server.isSubRoute(path))
                        new JsonResponse(context).fail(null, null);
                    else
                        SubRoute.redirect(context, cpd.server.appPath(context) + path);
                    break;
                default:
                    new JsonResponse(context).fail(null, null);
            }
        });

        HttpServerOptions serverOptions = new HttpServerOptions().setSsl(cpd.ssl.enabled);
        if (serverOptions.isSsl()) {
            serverOptions.setKeyStoreOptions(
                new JksOptions()
                    .setPath(cpd.ssl.keyStoreFilename)
                    .setPassword(cpd.ssl.keyStorePassword))
            // .setTrustStoreOptions(
            //     new JksOptions()
            //         .setPath(config.ssl.keyStoreFilename)
            //         .setPassword(config.ssl.keyStorePassword))
            // .setClientAuth(ClientAuth.REQUIRED)
            // .setUseAlpn(true)
            ;
        }
        vertx.createHttpServer(serverOptions)
             .requestHandler(router::accept).listen(cpd.server.port, ar -> {
            if (ar.succeeded()) {
                logger.info("HTTP Server started: " + cpd.server.origin());
                startFuture.complete();
            } else {
                logger.fatal(
                    "Cannot start HTTP Server: " + cpd.server.origin() + ". Cause: " + ar.cause().getMessage());
                startFuture.fail(ar.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> future) throws Exception {
        logger.info("Shutting down server: " + cpd.server.origin() + "...");
        super.stop(future);
    }
}
