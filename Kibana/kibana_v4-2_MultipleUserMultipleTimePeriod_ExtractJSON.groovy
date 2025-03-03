@Grab(group='org.codehaus.groovy', module='groovy-all', version='2.4.21')
@Grab(group='io.github.http-builder-ng', module='http-builder-ng-core', version='1.0.3')
@Grab(group='xml-resolver', module='xml-resolver', version='1.2')
import groovyx.net.http.HttpBuilder
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def KIBANA_HOST = "https://<Hostaddress>:port"
def INDEX_NAME = "myIndex*"
def REMOTE_USER_LIST = ["usertoread1", "usertoread2", "usertoread3", "usertoread4"]

def USERNAME = "myuser"
def PASSWORD = "mypassword"

def url = "${KIBANA_HOST}/${INDEX_NAME}/_search"

def usersJson = new JsonOutput().toJson(REMOTE_USER_LIST)

def queryJson = """{
                    "size": 0,
                    "query": {
                        "terms": { "remote_user.keyword": $usersJson }
                    },
                    "aggs": {
                        "users": {
                        "terms": { "field": "remote_user.keyword", "size": 25 },
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
        //println "Response: ${body}"
        def extractedData = jsonResponse.aggregations.users.buckets.collect { bucket ->
            [
                user: bucket.key,
                today: bucket.time_ranges.buckets.today.doc_count,
                weekly: bucket.time_ranges.buckets.weekly.doc_count,
                monthly:bucket.time_ranges.buckets.monthly.doc_count
            ]
        }.sort { it.user }
        extractedData.each { data ->
            println "User: ${data.user}"
            println "Today: ${data.today}"
            println "Weekly: ${data.weekly}"
            println "Monthly: ${data.monthly}"
            println "\n----------------------------\n"
        }

        def timestamp = new Date().format("yyyyMMdd_HH-mm-ss")
        
        //Export to .txt File in current directory
        // def txtFile = new File("Kibana_Output-${timestamp}.txt")
        // txtFile.withWriter('UTF-8') { writer ->
        //     extractedData.each { data ->
        //         writer.println "User: ${data.user}"
        //         writer.println "Today: ${data.today}"
        //         writer.println "Weekly: ${data.weekly}"
        //         writer.println "Monthly: ${data.monthly}"
        //         writer.println "\n----------------------------\n"
        //     }
        // }

        //Export to .csv File in current directory
        def csvFile = new File("Kibana_Output-${timestamp}.csv")
        csvFile.withWriter('UTF-8') { writer ->
            writer.println "${timestamp}, , , "
            writer.println "User, Today, Weekly, Monthly"

            extractedData.each { data -> 
                writer.println "${data.user}, ${data.today}, ${data.weekly}, ${data.monthly}"
            }
        }

        println "Data successfully retrieved and written at ${timestamp}"
    }

    response.failure { fromServer, body ->
            println "Request failed!"
            println "Status Code: ${fromServer.statusCode}"
            println "Status Reason: ${fromServer.message}"
            println ""
        }
}