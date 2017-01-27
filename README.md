# Example JVM project for the Pact workshop

This project has 3 components, a consumer project and two service providers, one Dropwizard and one 
Springboot service that the consumer will interaction with.
 
## Step 1 - Simple Consumer calling Provider

Given we have a client that needs to make a HTTP GET request to a provider service, and requires a response in JSON format. 
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

Running the client with either provider works nicely.

```console
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

Let's run this spec and see it all pass:

```console
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

```console
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

## Step 3 - Pact to the rescue

Let us add Pact to the project and write a consumer pact test.

*consumer/src/test/groovy/au/com/dius/pactworkshop/consumer/ClientPactSpec.groovy*

```groovy
class ClientPactSpec extends Specification {

  private Client client
  private LocalDateTime date
  private PactBuilder provider

  def setup() {
    client = new Client('http://localhost:1234')
    date = LocalDateTime.now()
    provider = new PactBuilder()
    provider {
      serviceConsumer 'Our Little Consumer'
      hasPactWith 'Our Provider'
      port 1234
    }
  }

  def 'Pact with our provider'() {
    given:
    def json = [
      test: 'NO',
      date: '2013-08-16T15:31:20+10:00',
      count: 100
    ]
    provider {
      serviceConsumer 'Our Little Consumer'
      hasPactWith 'Our Provider'
      port 1234

      given('data count > 0')
      uponReceiving('a request for json data')
      withAttributes(path: '/provider.json', query: [validDate: date.toString()])
      willRespondWith(status: 200, body: JsonOutput.toJson(json), headers: ['Content-Type': 'application/json'])
    }

    when:
    def result
    VerificationResult pactResult = provider.run {
      result = client.fetchAndProcessData(date)
    }

    then:
    pactResult == PactVerified$.MODULE$
    result == [1, ZonedDateTime.parse(json.date)]
  }

}
```

This test starts a mock server on port 1234 that pretends to be our provider. To get this to work we needed to update
our consumer to pass in the URL of the provider. We also updated the `fetchAndProcessData` method to pass in the
query parameter.

Running this spec still passes, but it creates a pact file which we can use to validate our assumptions on the provider side.

```console
$ ./gradlew :consumer:check
:consumer:compileJava UP-TO-DATE
:consumer:compileGroovy UP-TO-DATE
:consumer:processResources UP-TO-DATE
:consumer:classes UP-TO-DATE
:consumer:compileTestJava UP-TO-DATE
:consumer:compileTestGroovy UP-TO-DATE
:consumer:processTestResources UP-TO-DATE
:consumer:testClasses UP-TO-DATE
:consumer:test
:consumer:check

BUILD SUCCESSFUL
```

Generated pact file (*consumer/build/pacts/Our Little Consumer-Our Provider.json*):

```json
{
    "provider": {
        "name": "Our Provider"
    },
    "consumer": {
        "name": "Our Little Consumer"
    },
    "interactions": [
        {
            "description": "a request for json data",
            "request": {
                "method": "GET",
                "path": "/provider.json",
                "query": "validDate=2017-01-27T15%3A01%3A38.027"
            },
            "response": {
                "status": 200,
                "headers": {
                    "Content-Type": "application/json"
                },
                "body": {
                    "test": "NO",
                    "date": "2013-08-16T15:31:20+10:00",
                    "count": 100
                }
            },
            "providerState": "data count > 0"
        }
    ],
    "metadata": {
        "pact-specification": {
            "version": "2.0.0"
        },
        "pact-jvm": {
            "version": "3.3.6"
        }
    }
}
```

## Step 4 - Verify pact against provider

There are two ways of validating a pact file against a provider. The first is using a build tool (like Gradle) to
execute the pact against the running service. The second is to write a pact verification test. We will be doing both
in this step.

First, we need to 'publish' the pact file from the consumer project. For this workshop, we will just copy it over to the
provider project.

### Verifying the springboot provider

For the springboot provider, we are going to use Gradle to verify the pact file for us. We need to add the pact gradle 
plugin and the spawn plugin to the project and configure them.

*providers/springboot-provider/build.gradle:*

```groovy
plugins {
  id "au.com.dius.pact" version "3.3.6"
  id "com.wiredforcode.spawn" version "0.8.2"
}
```

```groovy
task startProvider(type: SpawnProcessTask, dependsOn: 'assemble') {
  command "java -jar ${jar.archivePath}"
  ready 'Started MainApplication'
}

task stopProvider(type: KillProcessTask) {

}

pact {
  serviceProviders {
    'Our Provider' {
      port = 8080

      startProviderTask = startProvider
      terminateProviderTask = stopProvider

      hasPactWith('Our Little Consumer') {
        pactFile = file("$buildDir/pacts/Our Little Consumer-Our Provider.json")
      }
    }
  }
}
```

Now if we copy the pact file from the consumer project and run our pact verification task, it should fail.

```console
$ ./gradlew :providers:springboot-provider:pactVerify
Starting a Gradle Daemon (subsequent builds will be faster)
:providers:springboot-provider:compileJava UP-TO-DATE
:providers:springboot-provider:compileGroovy UP-TO-DATE
:providers:springboot-provider:processResources UP-TO-DATE
:providers:springboot-provider:classes UP-TO-DATE
:providers:springboot-provider:findMainClass
:providers:springboot-provider:jar
:providers:springboot-provider:bootRepackage
:providers:springboot-provider:assemble
:providers:springboot-provider:startProvider

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v1.4.4.RELEASE)
```

... omitting lots of logs ...

```console
2017-01-27 16:04:36.817  INFO 17300 --- [           main] a.c.d.p.s.MainApplication                : Started MainApplication in 3.422 seconds (JVM running for 3.952)
java -jar /home/ronald/Development/Projects/Pact/pact-workshop-jvm/providers/springboot-provider/build/libs/springboot-provider.jar is ready.
:providers:springboot-provider:compileTestJava UP-TO-DATE
:providers:springboot-provider:compileTestGroovy UP-TO-DATE
:providers:springboot-provider:processTestResources UP-TO-DATE
:providers:springboot-provider:testClasses UP-TO-DATE
:providers:springboot-provider:pactVerify_Our Provider

Verifying a pact between Our Little Consumer and Our Provider
  [Using file /home/ronald/Development/Projects/Pact/pact-workshop-jvm/providers/springboot-provider/build/pacts/Our Little Consumer-Our Provider.json]
  Given data count > 0
         WARNING: State Change ignored as there is no stateChange URL
  a request for json data
    returns a response which
      has status code 200 (OK)
      includes headers
        "Content-Type" with value "application/json" (OK)
      has a matching body (FAILED)

Failures:

0) Verifying a pact between Our Little Consumer and Our Provider - a request for json data Given data count > 0 returns a response which has a matching body
      $.body.count -> Expected 100 but received 1000

      $.body -> Expected date='2013-08-16T15:31:20+10:00' but was missing

        Diff:

        {
        -    "count": 100,
        -    "date": "2013-08-16T15:31:20+10:00",
        -    "test": "NO"
        +    "count": 1000,
        +    "test": "NO",
        +    "validDate": "2017-01-27T16:04:37.686"
        }


:providers:springboot-provider:pactVerify_Our Provider FAILED
:providers:springboot-provider:stopProvider

FAILURE: Build failed with an exception.

* What went wrong:
There were 1 pact failures for provider Our Provider

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

BUILD FAILED
```

The test has failed for 2 reasons. Firstly, the count field has a different value to what was expected by the consumer. 
Secondly, and more importantly, the consumer was expecting a `date` field while the provider generates a `validDate`
field. Also, the date formats are different.

## Step 5 - Verify the provider with a test

In this step we will verify the same pact file against the Dropwizard provider using a JUnit test. First we copy it over,
or 'publish' it to our provider project.

We add the pact provider junit jar and the dropwizard testing jar to our project dependencies, and then we can create a
simple test to verify our provider.

```groovy
@RunWith(PactRunner)
@Provider('Our Provider')
@PactFolder('build/pacts')
class PactVerificationTest {

  @ClassRule
  public static final DropwizardAppRule<ServiceConfig> RULE = new DropwizardAppRule<ServiceConfig>(MainApplication,
    ResourceHelpers.resourceFilePath("main-app-config.yaml"))

  @TestTarget
  public final Target target = new HttpTarget(8080)

  @State("data count > 0")
  void dataCountGreaterThanZero() { }
}
```

This test will start the dropwizard app (using the class rule), and then execute the pact requests (defined by the
`@PactFolder` annotation) against the test target.

Running this test will fail for the same reasons as in step 4.

```console
$ ./gradlew :providers:dropwizard-provider:test
:providers:dropwizard-provider:compileJava UP-TO-DATE
:providers:dropwizard-provider:compileGroovy UP-TO-DATE
:providers:dropwizard-provider:processResources UP-TO-DATE
:providers:dropwizard-provider:classes UP-TO-DATE
:providers:dropwizard-provider:compileTestJava UP-TO-DATE
:providers:dropwizard-provider:compileTestGroovy
:providers:dropwizard-provider:processTestResources UP-TO-DATE
:providers:dropwizard-provider:testClasses
:providers:dropwizard-provider:test

au.com.dius.pactworkshop.dropwizardprovider.PactVerificationTest > Our Little Consumer - a request for json data FAILED
    java.lang.AssertionError

1 test completed, 1 failed
:providers:dropwizard-provider:test FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':providers:dropwizard-provider:test'.
> There were failing tests. See the report at: file:///home/ronald/Development/Projects/Pact/pact-workshop-jvm/providers/dropwizard-provider/build/reports/tests/test/index.html

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

BUILD FAILED
```

The JUnit build report has the expected failures (standard output shown here).

```
Verifying a pact between Our Little Consumer and Our Provider
  Given data count > 0
  a request for json data
    returns a response which
      has status code 200 (OK)
      includes headers
        "Content-Type" with value "application/json" (OK)
      has a matching body (FAILED)

Failures:

0) a request for json data returns a response which has a matching body
      $.body -> Expected date='2013-08-16T15:31:20+10:00' but was missing

        Diff:

            "test": "NO",
        -    "date": "2013-08-16T15:31:20+10:00",
        -    "count": 100
        +    "validDate": "2017-01-27T16:53:32.291",
        +    "count": 1000
        }

      $.body.count -> Expected 100 but received 1000
```
