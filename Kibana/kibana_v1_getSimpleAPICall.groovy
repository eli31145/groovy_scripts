@Grab(group='org.codehaus.groovy', module='groovy-all', version='2.4.21')
@Grab(group='io.github.http-builder-ng', module='http-builder-ng-core', version='1.0.3')
@Grab(group='xml-resolver', module='xml-resolver', version='1.2')
import groovyx.net.http.HttpBuilder

def KIBANA_HOST = "https://<Hostaddress>:port"

def USERNAME = "myuser"
def PASSWORD = "mypassword"

def url = "${KIBANA_HOST}/_search"

HttpBuilder.configure {
    request.uri = url
    request.auth.basic(USERNAME, PASSWORD)
    request.contentType = 'application/json'
}.get {

    response.success { fromServer, body ->
        println "Status Reason: ${fromServer}"
        println "Response: ${body}"
    }

    response.failure { fromServer, body ->
            println "Request failed!"
            println "Status Code: ${fromServer.statusCode}"
            println "Status Reason: ${fromServer.message}"
            println ""
        }
}