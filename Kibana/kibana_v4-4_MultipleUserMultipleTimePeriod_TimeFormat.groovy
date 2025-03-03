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

        def timestamp = new Date().format("yyyyMMdd_HH-mm-ss")

        //Export to .csv File in current directory
        def csvFile = new File("Kibana_Hits.csv")

        if (csvFile.exists()) {
            //read current content 
            def existingData = csvFile.readLines()

            def existingTimestampRow = existingData[0]
            def existingHeaders = existingData[1].split(",")

            def updatedTimestampRow = "${existingTimestampRow},\"${timestamp}\",\"\","
    
            def updatedHeaders = existingHeaders + ["Today", "Weekly", "Monthly"]

            //converts extracted data to format where it contains just values
                // bef: def extractedData = [
                //                             [user: "user1", today: 1, weekly: 2, monthly: 3],
                //                             [user: "user2", today: 11, weekly: 12, monthly: 13]
                //                         ]

                // aft: def newData = [
                //                         ["user1", 1, 2, 3],   // user1's new values
                //                         ["user2", 11, 12, 13] // user2's new values
                //                     ]
            def newData = extractedData.collect { data ->
                return [data.user, data.today, data.weekly, data.monthly]
            }

            //merge old with new data
            def updatedData = existingData[2..-1].collect { row ->
                def userData = row.split(",")

                def userNewData = newData.find { it[0] == userData[0] }

                if (userNewData) {
                    userData += [userNewData[1], userNewData[2], userNewData[3]] //add values for Today, Weekly, Monthly into existing values (leave out "User" value at userData[0])
                }
               return userData.join(",")
            }

            //Write updated data back to csv file
            csvFile.withWriter('UTF-8') { writer ->
                
                writer.println updatedTimestampRow
                writer.println updatedHeaders.join(",")

                updatedData.each { row ->
                    writer.println(row)
                }
            }
        } else {
                csvFile.withWriter('UTF-8') { writer ->
                writer.println "\"\",\"${timestamp}\",\"\","
                writer.println "User, Today, Weekly, Monthly"

                extractedData.each { data -> 
                    writer.println "${data.user}, ${data.today}, ${data.weekly}, ${data.monthly}"
                }
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