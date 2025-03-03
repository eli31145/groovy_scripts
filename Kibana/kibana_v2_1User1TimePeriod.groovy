@Grab(group='org.codehaus.groovy', module='groovy-all', version='2.4.21')
@Grab(group='io.github.http-builder-ng', module='http-builder-ng-core', version='1.0.3')
@Grab(group='xml-resolver', module='xml-resolver', version='1.2')
import groovyx.net.http.HttpBuilder
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

// Kibana/Elasticsearch details
def KIBANA_HOST = "https://<Hostaddress>:port"
def INDEX_NAME = "myIndex*"
def REMOTE_USER = "usertoread"

// Authentication
def USERNAME = "myuser"
def PASSWORD = "mypassword"

// Send request to Elasticsearch
def url = "${KIBANA_HOST}/${INDEX_NAME}/_count"

def queryJson = """{
                "query": {
                    "bool": {
                        "must": [
                            { "term": { "remote_user.keyword": "$REMOTE_USER" } },
                            { "range": { "@timestamp": { "gte": "now-1M/d", "lte": "now" } } }
                        ]
                    }
                }
            }"""

//Either way to define query JSON will work
// Define query JSON
// def queryJson = JsonOutput.toJson([
//     query: [
//         bool: [
//             must: [
//                 [term: ["remote_user.keyword": REMOTE_USER]],
//                 [range: ["@timestamp": [gte: "now-1M/d", lte: "now"]]]
//             ]
//         ]
//     ]
// ])

HttpBuilder.configure {
    request.uri = url
    request.auth.basic(USERNAME, PASSWORD)
    //Below way incorrect, will throw IllegalStateException: Found request body, but content type is undefined
    //request.headers['Content-Type'] = 'application/json'
    request.contentType = 'application/json'
}.get {
    //Below way incorrect, will throw Status 400 Bad Request
    //request.uri.query = [q: queryJson]
    request.body = queryJson

    response.success { fromServer, body ->
        def jsonResponse = (body instanceof String) ? new JsonSlurper().parseText(body) : body
        println "Response: ${body}"
    }

    response.failure { fromServer, body ->
            println "Request failed!"
            println "Status Code: ${fromServer.statusCode}"
            println "Status Reason: ${fromServer.message}"
            println ""
        }
}