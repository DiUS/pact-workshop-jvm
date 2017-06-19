# Example JVM project for the Pact workshop

This project has 3 components, a consumer project and two service providers, one Dropwizard and one
Springboot service that the consumer will interaction with.

## Step 1 - Simple Consumer calling Provider

Given we have a client that needs to make a HTTP GET request to a provider service, and requires a response in JSON format.


![Simple Consumer](diagrams/workshop_step1.png)


The client is quite simple and looks like this

*consumer/src/main/groovy/au/com/dius/pactworkshop/consumer/Client.groovy:*

```groovy
class Client {

  def loadProviderJson() {
    def http = new RESTClient('http://localhost:8080')
    def response = http.get(path: '/provider.json', query: [validDate: LocalDateTime.now().toString()])
    if (response.success) {
      response.data
    }
  }
}
```

and the dropwizard provider resource

*providers/dropwizard-provider/src/main/groovy/au/com/dius/pactworkshop/dropwizardprovider/RootResource.groovy:*

```groovy
@Path("/provider.json")
@Produces(MediaType.APPLICATION_JSON)
class RootResource {

  @GET
  Map providerJson(@QueryParam("validDate") Optional<String> validDate) {
    def valid_time = LocalDateTime.parse(validDate.get())
    [
      test: 'NO',
      validDate: LocalDateTime.now().toString(),
      count: 1000
    ]
  }

}
```

The springboot provider controller is similar

*providers/springboot-provider/src/main/groovy/au/com/dius/pactworkshop/springbootprovider/RootController.groovy:*

```groovy
@RestController
class RootController {

  @RequestMapping("/provider.json")
  Map providerJson(@RequestParam(required = false) String validDate) {
    def validTime = LocalDateTime.parse(validDate)
    [
      test: 'NO',
      validDate: LocalDateTime.now().toString(),
      count: 1000
    ]
  }

}
```

This providers expects a `validDate` parameter in HTTP date format, and then return some simple json back.


![Sequence Diagram](diagrams/sequence_diagram.png)


Running the client with either provider works nicely.

```
$ ./gradlew :consumer:run
Starting a Gradle Daemon, 4 stopped Daemons could not be reused, use --status for details
:consumer:compileJava UP-TO-DATE
:consumer:compileGroovy UP-TO-DATE
:consumer:processResources UP-TO-DATE
:consumer:classes UP-TO-DATE
:consumer:run
[test:NO, validDate:2017-01-27T11:49:04.131, count:1000]

BUILD SUCCESSFUL
```

## Step 2 - Client Tested but integration fails

Now lets get the client to use the data it gets back from the provider. Here is the updated client method that uses the returned data:

*consumer/src/main/groovy/au/com/dius/pactworkshop/consumer/Client.groovy:*

```groovy
  def fetchAndProcessData() {
    def data = loadProviderJson()
    println "data=$data"
    def value = 100 / data.count
    def date = LocalDateTime.parse(data.date)
    println "value=$value"
    println "date=$date"
    [value, date]
  }
```

![Sequence 2](diagrams/step2_sequence_diagram.png)

Let's now test our updated client.

*consumer/src/test/groovy/au/com/dius/pactworkshop/consumer/ClientSpec.groovy:*

```groovy
class ClientSpec extends Specification {

  private Client client
  private RESTClient mockHttp

  def setup() {
    mockHttp = Mock(RESTClient)
    client = new Client(http: mockHttp)
  }

  def 'can process the json payload from the provider'() {
    given:
    def json = [
      test: 'NO',
      date: '2013-08-16T15:31:20+10:00',
      count: 100
    ]

    when:
    def result = client.fetchAndProcessData()

    then:
    1 * mockHttp.get(_) >> [data: json, success: true]
    result == [1, ZonedDateTime.parse(json.date)]
  }

}
```

![Unit Test With Mocked Response](diagrams/step2_unit_test.png)

Let's run this spec and see it all pass:

```
$ ./gradlew :consumer:check
:consumer:compileJava UP-TO-DATE
:consumer:compileGroovy
:consumer:processResources UP-TO-DATE
:consumer:classes
:consumer:compileTestJava UP-TO-DATE
:consumer:compileTestGroovy
:consumer:processTestResources UP-TO-DATE
:consumer:testClasses
:consumer:test
:consumer:check

BUILD SUCCESSFUL
```

However, there is a problem with this integration point. Running the actual client against any of the providers results in
 a runtime exception!

```
$ ./gradlew :consumer:run
:consumer:compileJava UP-TO-DATE
:consumer:compileGroovy UP-TO-DATE
:consumer:processResources UP-TO-DATE
:consumer:classes UP-TO-DATE
:consumer:run
data=[test:NO, validDate:2017-01-27T14:21:23.174, count:1000]
Exception in thread "main" java.lang.NullPointerException: text
        at java.util.Objects.requireNonNull(Objects.java:228)
        at java.time.format.DateTimeFormatter.parse(DateTimeFormatter.java:1848)
        at java.time.ZonedDateTime.parse(ZonedDateTime.java:597)
        at java.time.ZonedDateTime.parse(ZonedDateTime.java:582)
        at java_time_ZonedDateTime$parse.call(Unknown Source)
        at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCall(CallSiteArray.java:48)
        at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:113)
        at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:125)
        at au.com.dius.pactworkshop.consumer.Client.fetchAndProcessData(Client.groovy:26)
        at au.com.dius.pactworkshop.consumer.Client$fetchAndProcessData.call(Unknown Source)
        at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCall(CallSiteArray.java:48)
        at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:113)
        at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:117)
        at au.com.dius.pactworkshop.consumer.Consumer.run(Consumer.groovy:3)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at org.codehaus.groovy.reflection.CachedMethod.invoke(CachedMethod.java:93)
        at groovy.lang.MetaMethod.doMethodInvoke(MetaMethod.java:325)
        at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1218)
        at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1027)
        at org.codehaus.groovy.runtime.InvokerHelper.invokePogoMethod(InvokerHelper.java:925)
        at org.codehaus.groovy.runtime.InvokerHelper.invokeMethod(InvokerHelper.java:908)
        at org.codehaus.groovy.runtime.InvokerHelper.runScript(InvokerHelper.java:412)
        at org.codehaus.groovy.runtime.InvokerHelper$runScript.call(Unknown Source)
        at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCall(CallSiteArray.java:48)
        at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:113)
        at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:133)
        at au.com.dius.pactworkshop.consumer.Consumer.main(Consumer.groovy)
:consumer:run FAILED

FAILURE: Build failed with an exception.
```

The provider returns a `validDate` while the consumer is
trying to use `date`, which will blow up when run for real even with the tests all passing. Here is where Pact comes in.
