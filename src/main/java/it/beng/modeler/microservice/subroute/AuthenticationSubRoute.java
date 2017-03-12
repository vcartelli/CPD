package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import it.beng.modeler.config;
import it.beng.modeler.microservice.auth.local.LocalAuthHandler;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.microservice.auth.local.impl.LocalUser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class AuthenticationSubRoute extends SubRoute {

    private static String AUTHENTICATION_ROLE_CITIZEN = "semantic:organization:roles:AuthenticationRole:citizen";
    private static JsonObject AUTHORIZATION_ROLES_OBSERVER = new JsonObject("{ \"*\": [ \"semantic:organization:roles:AuthorizationRole:observer\" ] }");

    public AuthenticationSubRoute(Vertx vertx, Router router, MongoClient mongodb) {
        super(config.server.auth.path, vertx, router, mongodb);
    }

    @Override
    protected void init() {

        // default callback handler
        router.route(HttpMethod.GET, path + "callback").handler(this::callbackHandler);

        /** LOCAL AUTHENTICATION **/

        // configure local auth provider
        final LocalAuthProvider localAuthProvider = LocalAuthProvider.create(vertx);
        LocalAuthHandler localAuthHandler = LocalAuthHandler.create(localAuthProvider);

        // create local auth user session handler
        router.route().handler(UserSessionHandler.create(localAuthProvider));

        /** OAUTH2 AUTHENTICATION **/

        // configure oauth2 auth provider
        OAuth2ClientOptions credentials = new OAuth2ClientOptions()
            .setClientID(config.oauth2.clientId)
            .setClientSecret(config.oauth2.clientSecret)
            .setSite(config.oauth2.site)
            .setTokenPath(config.oauth2.tokenPath)
            .setAuthorizationPath(config.oauth2.authPath);

        final OAuth2Auth oauth2Provider = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, credentials);
        // create OAuth2 user session handler
        router.route().handler(UserSessionHandler.create(oauth2Provider));

        // create google OAuth2 handler
        final OAuth2AuthHandler googleOAuth2Handler = OAuth2AuthHandler.create(oauth2Provider, config.oauth2.host);
        googleOAuth2Handler.addAuthority(config.oauth2.scope);
        googleOAuth2Handler.setupCallback(router.route(HttpMethod.GET, path + "callback"));

        // in order to retrieve localAuthProvider in local login handler
        router.route(HttpMethod.GET, path + "local/login/handler").handler(rc -> {
            rc.put("localAuthProvider", localAuthProvider);
            rc.next();
        });

        // NOTE: "router.route("/auth/google/*").handler(oauth2Handler)" is not used
        //       since I didn't find a way to handle the state object with the default handler
        //       defined in that way!
//         router.route("/auth/google/*").handler(oauth2Handler);

        // in order to retrieve the OAuth2 handler
        router.route(HttpMethod.GET, path + "google/login").handler(rc -> {
            // googleOAuth2Handler is *only* used for generating the auth uri
            rc.put("oAuth2Handler", googleOAuth2Handler);
            rc.next();
        });
//        router.route(HttpMethod.GET, path + "/github/login").handler(rc -> {
//            rc.put("oAuth2Handler", githubOAuth2Handler);
//            rc.next();
//        });
        // 2) handle login by provider (in case of oauth2, the previuos route passes the oAuth2Handler)
        router.route(HttpMethod.GET, path + ":provider/login").handler(this::providerLoginHandler);

        // 3) the profile setup handlers
        router.route(HttpMethod.GET, path + "local/login/handler").handler(this::localLoginHandler);
        router.route(HttpMethod.GET, path + "google/login/handler").handler(this::googleLoginHandler);

        // logout
        router.route(HttpMethod.GET, path + "logout").handler(this::logoutHandler);

        // profile
        router.route(HttpMethod.GET, path + "profile").handler(this::getProfile);
    }

    private void callbackHandler(RoutingContext rc) {
        String error = getQueryParameter(rc.request().query(), "error");
        if (error != null) {
            System.err.println("ERROR: " + error);
            redirect(rc, baseHref + "login");
        } else rc.next();
    }

    private void providerLoginHandler(RoutingContext rc) {
        if (config.develop) System.out.println("query: " + rc.request().query());
        String state = getQueryParameter(rc.request().query(), "state");
        if (state != null) {
            String provider = rc.request().getParam("provider");
            switch (provider) {
                case "local":
                    rc.reroute(path + "local/login/handler");
                    break;
                case "google":
                    OAuth2AuthHandler oAuth2Handler = rc.get("oAuth2Handler");
                    redirect(rc, oAuth2Handler.authURI(path + "google/login/handler", state));
                    break;
                default:
                    throw new IllegalStateException("Unknown authentication provider: " + provider);
            }
        } else {
            redirect(rc, baseHref + "login");
        }
    }

    private void localLoginHandler(RoutingContext rc) {
        if (config.develop) System.out.println("query: " + rc.request().query());
        JsonObject state = new JsonObject(new String(Base64.getDecoder().decode(
            getQueryParameter(rc.request().query(), "state"))));
        LocalAuthProvider localAuthProvider = rc.get("localAuthProvider");
        localAuthProvider.authenticate(state.getJsonObject("authInfo"), rh -> {
            if (rh.succeeded()) {
                LocalUser user = (LocalUser) rh.result();
                if (config.develop) System.out.println(Json.encodePrettily(user.principal()));
                rc.setUser(user);
                redirect(rc, state.getString("redirect"));
            } else {
                String redirect = Base64.getEncoder().encodeToString(state.getString("redirect").getBytes());
                redirect(rc, baseHref + "login/" + redirect);
            }
        });
    }

    private void googleLoginHandler(RoutingContext rc) {

        User user = rc.user();

        if (user != null) {

            String userId = null;
            {
                Base64.Decoder decoder = Base64.getDecoder();
                for (String s : rc.user().principal().getString("id_token").split("\\.")) {
                    try {
                        String decoded = new String(decoder.decode(s), StandardCharsets.UTF_8);
                        JsonObject o = new JsonObject(decoded);
                        userId = o.getString("sub");
                        if (userId != null) {
                            if (config.develop) System.out.println("userId found: " + userId);
                            break;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            if (userId == null) throw new IllegalStateException("user-id not found in user principal!");
            String token = user.principal().getString("access_token");
            vertx.createHttpClient(new HttpClientOptions().setSsl(true))
                 .request(HttpMethod.GET, 443, "www.googleapis.com", "/plus/v1/people/" + userId)
                 .putHeader("Authorization", "Bearer " + token)
                 .handler(response -> {
                     response.bodyHandler(buffer -> {
                         JsonObject profile = new JsonObject(buffer.getString(0, buffer.length()));
                         profile.mergeIn(new JsonObject()
                             .put("provider", "google")
                             .put("accessToken", token)
                             .put("authenticationRole", AUTHENTICATION_ROLE_CITIZEN)
                             .put("authorizationRoles", AUTHORIZATION_ROLES_OBSERVER));
                         rc.user().principal()
                           .put("profile", profile);
                         if (config.develop) System.out.println(Json.encodePrettily(rc.user().principal()));
                     });
                 })
                 .endHandler(v -> {
                     JsonObject state;
                     try {
                         state = new JsonObject(new String(
                             Base64.getDecoder()
                                   .decode(getQueryParameter(rc.request().query(), "state"))));
                     } catch (Exception e) {
                         e.printStackTrace();
                         state = new JsonObject().put("redirect", baseHref);
                     }
                     redirect(rc, state.getString("redirect"));
                 })
                 .end();
        } else {
            redirect(rc, baseHref + "login");
        }

    }

    private void logoutHandler(RoutingContext rc) {
        rc.setUser(null);
        rc.session().destroy();
        redirect(rc, baseHref + "login");
    }

    private void getProfile(RoutingContext rc) {
        String profile = "null";
        if (isAuthenticated(rc))
            profile = rc.user().principal().getJsonObject("profile").encode();
        rc.response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(profile);
//          .end(isAuthenticated(rc) ?
//              rc.user().principal().getJsonObject("profile").encode() :
//              Json.encode(null));
    }

}
