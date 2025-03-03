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
def url = "${KIBANA_HOST}/${INDEX_NAME}/_search"

//Same syntax as Elastic DevTools -> Console
def queryJson = """{
                    "size": 0,
                    "query": {
                        "term": { "remote_user.keyword": "$REMOTE_USER" }
                    },
                    "aggs": {
                        "time_ranges": {
                        "filters": {
                            "filters": {
                            "today": { "range": { "@timestamp": { "gte": "now/d", "lte": "now" } } },
                            "weekly": { "range": { "@timestamp": { "gte": "now-7d/d", "lte": "now" } } },
                            "monthly": { "range": { "@timestamp": { "gte": "now-1M/d", "lte": "now" } } }
                            }
                        }
                        }
                    }
                    }"""

HttpBuilder.configure {
    request.uri = url
    request.auth.basic(USERNAME, PASSWORD)
    request.contentType = 'application/json'
}.get {
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