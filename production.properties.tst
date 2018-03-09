# ssl
cpd.ssl.enabled=false
cpd.ssl.keystore.filename=keystore.jks
cpd.ssl.keystore.password=simpatico
# server
cpd.server.host=localhost
cpd.server.port=8901
cpd.server.baseHref=/cpd/
cpd.server.allowedOriginPattern=^https?:\\\\/\\\\/(localhost:8901|origin1|origin2|...)$
# server.public
cpd.server.pub.scheme=https
cpd.server.pub.host=example.host.com
cpd.server.pub.port=443
# QAE webapp
cpd.qae.href=https://simpatico.morelab.deusto.es/qae/
cpd.qae.api.path=api/
cpd.qae.api.getQuestionCount=stats/diagrams/{elementId}
cpd.qae.link.newQuestion=questions/create?tags={eServiceId},{diagramId},{elementId},Diagram
cpd.qae.link.relatedQuestions=diagrams/list/{elementId}
# mongodb
cpd.mongodb.host=localhost
cpd.mongodb.port=27017
cpd.mongodb.name=cpd
cpd.mongodb.username=
cpd.mongodb.password=
# oauth2
cpd.oauth2.origin=http://my.server:8901
! NOTE: the oauth2 redirect callback endpoint will be:
!! "${cpd.oauth2.origin}${cpd.server.baseHref}oauth2/server/callback" for AUTH_CODE and CLIENT (1,2) flows
!! "${cpd.oauth2.origin}${cpd.server.baseHref}${cpd.app.path}oauth2/client/callback for IMPLICIT (3) flows
!! leave empty if no oauth2 is required (NOTE: if no oauth2 providers make sure to set cpd.app.useLocalAuth=true)
cpd.oauth2.providers= #fare qui un read a parte, chiedendo all'utente il percorso del file json (suggerendo ~/oauth2providers.json). quindi copiare su array[1] il contenuto del file, invece array[0] sara' cpd.oauth2.providers
! cpd.oauth2.providers must be a list of comma separated json objects (see example):
! example for 2 providers
#  cpd.oauth2.providers=\
#  {\
#    "provider":"Google",\
#    "logoUrl":"assets/img/oauth2_google_logo.png",\
#    "site":"https://accounts.google.com",\
#    "authPath":"/o/oauth2/auth",\
#    "tokenPath":"https://www.googleapis.com/oauth2/v3/token",\
#    "introspectionPath":"https://www.googleapis.com/oauth2/v3/tokeninfo",\
#    "clientId":"my google app client id",\
#    "clientSecret":"my google app client secret",\
#    "flows":[\
#      {\
#        "flowType":"AUTH_CODE",\
#        "scope":"email",\
#        "getUserProfile": "https://www.googleapis.com/plus/v1/people/{userId}"\
#      }\
#    ]\
#  },\
#  {\
#    "provider":"AAC",\
#    "logoUrl":"assets/img/oauth2_aac_logo.png",\
#    "site":"http://my.aac:8080",\
#    "authPath":"/aac/eauth/authorize",\
#    "tokenPath":"/aac/oauth/token",\
#    "clientId":"my aac app client id",\
#    "clientSecret":"my aac app client secret",\
#    "flows":[\
#      {\
#        "flowType":"IMPLICIT",\
#        "scope":"profile.basicprofile.me",\
#        "getUserProfile": "http://my.aac:8080/aac/basicprofile/me"\
#      },\
#      {\
#        "flowType":"CLIENT"\
#      }\
#    ]\
#  }
