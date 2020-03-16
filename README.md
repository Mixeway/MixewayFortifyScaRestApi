[![Build Status](https://travis-ci.org/Mixeway/MixewayFortifyScaRestApi.svg?branch=master)](https://travis-ci.org/Mixeway/MixewayFortifyScaRestApi)

<img src="https://mixeway.github.io/img/mixewaybadge.png">

# Mixeway Fortify SCA Rest API <img src="https://mixeway.github.io/img/logo_dashboard.png" height="60px">

Mixeway Fortify SCA Rest API is Spring Boot application which allows to remotley use sourceanalyzer command of Fortify SCA.

### Why
Why use limited and predefined sourceanalyzer instead of taking advantage of fullpackage? We have found it to be hard to 
integrate Fortify Sourceanalyzer with each single jenkins slave, gitlab-runner or any other CICD solution.

Integration using REST API is easy, straight forward and doesn't need anything but network connection. So it is first choice for using for integratiomn with Mixeway.

### Integraitons
Mixeway Fortify SCA Rest API by default is integrated with <a href="https://mixeway.io"> Mixeway Project</a> but can 
be also pretty easily used in stand alone mode.

During scan phase executed by REST API there is GIT actions made on a code base which is goint to be scanned. It is great place
to introduce OpenSource vulnerability scanning, so it is possible to use OWASP DependencyTrack integration at this stage.
Integration with DependencyTrack is being done via prepared shell scripts which can be found in `DependencyTrackScripts` directory.

### Download
Get newest release from release page - https://github.com/Mixeway/MixewayFortifyScaRestApi/releases

### Startup
```
java -Dhttps.proxyPort=<proxyPort> \
    -DhttpsProxyHost=<proxyHost> \
    -Dhttp.nonProxyHosts=<nonProxyHosts> \
    --server.port=<serverPort> \
    --server.ssl.key-store=<keyStore> \
    --server.ssl.key-store-password=<keyStorePassword> \
    --server.ssl.trust-store=<trustStore> \
    --server.ssl.trust-store-password=<trustStorePassword> \
    --allowed.user=<allowedUsers> \
    --dependencytrack.script.mvn=<dTrackMvn> \
    --dependencytrack.script.python=<dTrackPython> \
    --dependencytrack.script.js=<dTrackJS> \
    --dependencytrack.script.php=<dTrackPHP> \
    -jar fortifyscaapi-1.0.0-SNAPSHOT.jar
```
where:
```$xslt
proxyPort,proxyHost,nonProxyHosts - proxy informations used by git and sourceanalyzer (mvn, npm)
serverPort - listening port for REST API
keyStore, keyStorePassword - keystore for TLS 
trustStore, trustStorePassword - key store contaning certificates which will be accepted by API
allowedUsers - CN of x509 Certificate which is authorized to use REST API
dTrackMvn, dTrackJS, dTrackPHP, dTrackPython - path to scripts which are executing dependency track extraction and upload
```

### Requirements
- Java 1.8
- Sourceanalyzer version 18.2 + 

### Authentication
Mixeway Fortify REST API use x509 certificates to authenticate requests. It is crucial that client certificate will be trusted by REST API 
(proper setting of trust store).

### USAGE
- Initialization (creates /opt/mixerscan directory)
```$xslt
curl --cacert ./ca.pem \
    --key ./private-key.pem \
    --cert ./client-certificate.pem \
    https://localhost/initialize

Response 200 OK - already initialized
Response 201 CREATED - successfully initialized
Response 409 CONFLICT - something went wrong, make sure there are proper permissions set
```
- Running scan
```$xslt
curl --cacert ./ca.pem \
    --key ./private-key.pem \
    --cert ./client-certificate.pem \
    -X PUT \
    -H "Content-Type: application/json" \
    -d "{'repoUrl':'https://github.com/mixeway/mixewayhub', 'username':'gitusername','password':'gitpassword','versionId':1,
'single':true,'dTrackUuid':'dTrackProjectId','cloudCtrlToken':'CloudControllerToken','sscUrl':'https://fortifyssc/ssc','groupName':'buildName',projects:
[{'projectName':'projectName',projectRepoUrl':'https://github.com/mixeway/mixewayhub','versionId':1, 'technique':'MVN',
'branch':'master','dTrackUuid':'dtrackUuid'}]}"
    https://localhost/createscan

Response 200 OK
{"commitId":"cds8719dad8dsacaj809asd","error":false,"requestId":"fffffff-ffff-ffff-ffff-ffffffffff", "running":true}
```
- Check status
```$xslt
curl --cacert ./ca.pem \
    --key ./private-key.pem \
    --cert ./client-certificate.pem \
    https://localhost/check/fffffff-ffff-ffff-ffff-ffffffffff

Response 200 OK 
{"commitId":"cds8719dad8dsacaj809asd","error":false,"requestId":"fffffff-ffff-ffff-ffff-ffffffffff", "running":false}
 ```
