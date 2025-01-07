@Grab(group='org.codehaus.groovy', module='groovy-all', version='2.4.21')
@Grab(group='io.github.http-builder-ng', module='http-builder-ng-core', version='1.0.3')
@Grab(group='xml-resolver', module='xml-resolver', version='1.2')
import groovy.json.JsonSlurper
import groovyx.net.http.HttpBuilder

def jenkinsConfigs = [
    [   
        master: "ci-ent1",
        url: "https://<JENKINS_URL>/computer/api/json",
        username: "jenkinsusername",
        apiToken: "token1",
        targetDisplayNames: ["displayName1", "displayName2", "displayName3", "displayName4", "displayName5"]
    ],
    [
        master: "ci4",
        url: "https://<JENKINS_URL>/computer/api/json",
        username: "jenkinsusername",
        apiToken: "token2",
        targetDisplayNames: ["displayName1", "displayName2", "displayName3", "displayName4", "displayName5"]
    ],
    [
        master: "ci5",
        url: "https://<JENKINS_URL>/computer/api/json",
        username: "jenkinsusername",
        apiToken: "token3",
        targetDisplayNames: ["displayName1", "displayName2", "displayName3", "displayName4", "displayName5", "displayName6", "displayName7"]
    ],
    [
        master: "ci6",
        url: "https://<JENKINS_URL>/computer/api/json",
        username: "jenkinsusername",
        apiToken: "token4",
        targetDisplayNames: ["displayName1", "displayName2", "displayName3"]
    ]
]

def fetchJenkinsData(String master, String url, String username, String apiToken, List<String> targetDisplayNames) {
    def token = Base64.encoder.encodeToString("${username}:${apiToken}".getBytes("UTF-8"))

    HttpBuilder.configure {
    request.uri = url
    request.auth.basic(username, apiToken)
    // Add the required headers
    request.headers['Content-Type'] = 'application/json;charset=utf-8'
    request.headers['Authorization'] = "Basic $token"
    }.get {
        response.success { fromServer, body ->
            //println "Response: ${body}"
            // Getting Body Response in JSON
            def jsonResponse = (body instanceof String) ? new JsonSlurper().parseText(body) : body

            println "Slave Status in ${master}:"
            // Extract relevant info
            jsonResponse.computer.each { computer -> 
                if (computer.displayName in targetDisplayNames) {
                    println "Display Name: ${computer.displayName}, Offline: ${computer.offline}"
                }
            }
            println ""
        }
        response.failure { fromServer, body ->
            println "Request failed!"
            println "Status Code: ${fromServer.statusCode}"
            println "Status Reason: ${fromServer.message}"
            println ""
        }
    }
}

//Iterate through configs
jenkinsConfigs.each { config ->
    fetchJenkinsData(config.master, config.url, config.username, config.apiToken, config.targetDisplayNames)
}