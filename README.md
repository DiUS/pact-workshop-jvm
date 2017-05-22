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
    PactVerificationResult pactResult = provider.runTest {
      result = client.fetchAndProcessData(date)
    }

    then:
    pactResult == PactVerificationResult.Ok.INSTANCE
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
                "query": "validDate=2017-05-22T10%3A16%3A29.732"
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
            "version": "3.4.0"
        }
    }
}
```

## Step 4 - Verify pact against provider

There are two ways of validating a pact file against a provider. The first is using a build tool (like Gradle) to
execute the pact against the running service. The second is to write a pact verification test. We will be doing both
in this step.

First, we need to 'publish' the pact file from the consumer project. For this workshop, we have a `publishWorkshopPact` task in the
main project to do this.

### Verifying the springboot provider

For the springboot provider, we are going to use Gradle to verify the pact file for us. We need to add the pact gradle 
plugin and the spawn plugin to the project and configure them.

*providers/springboot-provider/build.gradle:*

```groovy
plugins {
  id "au.com.dius.pact" version "3.4.0"
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
      $.body -> Expected date='2013-08-16T15:31:20+10:00' but was missing

        Diff:

            "test": "NO",
        -    "date": "2013-08-16T15:31:20+10:00",
        -    "count": 100
        +    "validDate": "2017-05-22T10:39:28.137",
        +    "count": 1000
        }

      $.body.count -> Expected 100 but received 1000


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

## Step 6 - Back to the client we go

Let's correct the consumer test to handle any integer for `count` and use the correct field for the `date`. Then we need 
to add a type matcher for `count` and change the field for the date to be `validDate`. We can also add a date expression 
to make sure the `validDate` field is a valid date. This is important because we are parsing it.

The updated consumer test is now:

```groovy
    provider {
      serviceConsumer 'Our Little Consumer'
      hasPactWith 'Our Provider'
      port 1234

      given('data count > 0')
      uponReceiving('a request for json data')
      withAttributes(path: '/provider.json', query: [validDate: date.toString()])
      willRespondWith(status: 200)
      withBody {
        test 'NO'
        validDate timestamp("yyyy-MM-dd'T'HH:mm:ssXXX", json.date)
        count integer(json.count)
      }
    }
```

Running this test will fail until we fix the client. Here is the correct client function:

```groovy
  def fetchAndProcessData(LocalDateTime dateTime) {
    def data = loadProviderJson(dateTime)
    println "data=$data"
    def value = 100 / data.count
    def date = OffsetDateTime.parse(data.validDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))
    println "value=$value"
    println "date=$date"
    [value, date]
  }
```

Now the test passes. But we still have a problem with the date format, which we must fix in the provider. Running the
client now fails because of that.

```console
$ ./gradlew :consumer:run
Starting a Gradle Daemon, 1 busy Daemon could not be reused, use --status for details
:consumer:compileJava UP-TO-DATE
:consumer:compileGroovy UP-TO-DATE
:consumer:processResources UP-TO-DATE
:consumer:classes UP-TO-DATE
:consumer:run
data=[test:NO, validDate:2017-01-27T17:26:13.911, count:1000]
Exception in thread "main" java.time.format.DateTimeParseException: Text '2017-01-27T17:26:13.911' could not be parsed at index 23
        at java.time.format.DateTimeFormatter.parseResolved0(DateTimeFormatter.java:1949)
        at java.time.format.DateTimeFormatter.parse(DateTimeFormatter.java:1851)
        at java.time.ZonedDateTime.parse(ZonedDateTime.java:597)
        at java.time.ZonedDateTime.parse(ZonedDateTime.java:582)
        at org.codehaus.groovy.vmplugin.v7.IndyInterface.selectMethod(IndyInterface.java:232)
        at au.com.dius.pactworkshop.consumer.Client.fetchAndProcessData(Client.groovy:30)
        at org.codehaus.groovy.vmplugin.v7.IndyInterface.selectMethod(IndyInterface.java:232)
        at au.com.dius.pactworkshop.consumer.Consumer.run(Consumer.groovy:5)
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
        at org.codehaus.groovy.vmplugin.v7.IndyInterface.selectMethod(IndyInterface.java:232)
        at au.com.dius.pactworkshop.consumer.Consumer.main(Consumer.groovy)
:consumer:run FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':consumer:run'.
> Process 'command '/usr/lib/jvm/java-8-openjdk-amd64/bin/java'' finished with non-zero exit value 1

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

BUILD FAILED
```

## Step 7 - Verify the providers again

We need to 'publish' the consumer pact file to the provider projects again. Then, running the provider verification
tests we get the expected failure about the date format.

```
Failures:

0) Verifying a pact between Our Little Consumer and Our Provider - a request for json data Given data count > 0 returns a response which has a matching body
      $.body.validDate -> Expected '2017-01-27T17:33:52.293' to match a timestamp of 'yyyy-MM-dd'T'HH:mm:ssXXX': Unable to parse the date: 2017-01-27T17:33:52.293
```

Lets fix the providers and then re-run the verification tests. Here is the corrected Dropwizard resource:

```groovy
@Path("/provider.json")
@Produces(MediaType.APPLICATION_JSON)
class RootResource {

  @GET
  Map providerJson(@QueryParam("validDate") Optional<String> validDate) {
    def valid_time = LocalDateTime.parse(validDate.get())
    [
      test: 'NO',
      validDate: OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")),
      count: 1000
    ]
  }

}
```

Running the verification against the providers now pass. Yay!

## Step 8 - Test for the missing query parameter

In this step we are going to add a test for the case where the query parameter is missing or invalid. We do this by 
adding additional tests and expectations to the consumer pact test. Our client code needs to be modified slightly to
be able to pass invalid dates in, and if the date parameter is null, don't include it in the request.

Here are the two additional tests:

*consumer/src/test/groovy/au/com/dius/pactworkshop/consumer/ClientPactSpec.groovy:*

```groovy
  def 'handles a missing date parameter'() {
    given:
    provider {
      given('data count > 0')
      uponReceiving('a request with a missing date parameter')
      withAttributes(path: '/provider.json')
      willRespondWith(status: 400, body: '"validDate is required"', headers: ['Content-Type': 'application/json'])
    }

    when:
    PactVerificationResult pactResult = provider.runTest {
      client.fetchAndProcessData(null)
    }

    then:
    pactResult == PactVerificationResult.Ok.INSTANCE
  }

  def 'handles an invalid date parameter'() {
    given:
    provider {
      given('data count > 0')
      uponReceiving('a request with an invalid date parameter')
      withAttributes(path: '/provider.json', query: [validDate: 'This is not a date'])
      willRespondWith(status: 400, body: $/"'This is not a date' is not a date"/$, headers: ['Content-Type': 'application/json'])
    }

    when:
    def result
    PactVerificationResult pactResult = provider.runTest {
      result = client.fetchAndProcessData('This is not a date')
    }

    then:
    pactResult == PactVerificationResult.Ok.INSTANCE
  }
```

After running our specs, the pact file will have 2 new interactions.

*consumer/build/pacts/Our Little Consumer-Our Provider.json:*

```json
[
  {
      "description": "a request with a missing date parameter",
      "request": {
          "method": "GET",
          "path": "/provider.json"
      },
      "response": {
          "status": 400,
          "headers": {
              "Content-Type": "application/json"
          },
          "body": "\"validDate is required\""
      },
      "providerState": "data count > 0"
  },
  {
      "description": "a request with an invalid date parameter",
      "request": {
          "method": "GET",
          "path": "/provider.json",
          "query": "validDate=This+is+not+a+date"
      },
      "response": {
          "status": 400,
          "headers": {
              "Content-Type": "application/json"
          },
          "body": "\"'This is not a date' is not a date\""
      },
      "providerState": "data count > 0"
  }
]
```

## Step 9 - Verify the provider with the missing/invalid date query parameter
   
Let us run this updated pact file with our providers. We get a 500 response as the provider can't handle the missing 
or incorrect date.

Here is the dropwizard test output:

```console
Verifying a pact between Our Little Consumer and Our Provider
  Given data count > 0
  a request with a missing date parameter
      returns a response which
        has status code 400 (FAILED)
        includes headers
          "Content-Type" with value "application/json" (OK)
        has a matching body (FAILED)
  
  Failures:
  
  0) a request with a missing date parameter returns a response which has a matching body
        $.body -> Expected 'validDate is required' but received Map(code -> 500, message -> There was an error processing your request. It has been logged (ID 57cd4a2eae1d5293).)
  
  
  1) a request with a missing date parameter returns a response which has status code 400
        assert expectedStatus == actualStatus
               |              |  |
               400            |  500
                              false
  
```

and the springboot build output:

```console
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
      has a matching body (OK)
  Given data count > 0
         WARNING: State Change ignored as there is no stateChange URL
  a request with a missing date parameter
    returns a response which
      has status code 400 (FAILED)
      includes headers
        "Content-Type" with value "application/json" (OK)
      has a matching body (FAILED)
  Given data count > 0
         WARNING: State Change ignored as there is no stateChange URL
  a request with an invalid date parameter
    returns a response which
      has status code 400 (FAILED)
      includes headers
        "Content-Type" with value "application/json" (OK)
      has a matching body (FAILED)

Failures:

0) Verifying a pact between Our Little Consumer and Our Provider - a request with a missing date parameter Given data count > 0 returns a response which has status code 400
      assert expectedStatus == actualStatus
             |              |  |
             400            |  500
                            false

1) Verifying a pact between Our Little Consumer and Our Provider - a request with a missing date parameter Given data count > 0 returns a response which has a matching body
      $.body -> Expected 'validDate is required' but received Map(path -> /provider.json, timestamp -> 1490922210835, exception -> java.lang.NullPointerException, error -> Internal Server Error, status -> 500, message -> org.springframework.web.util.NestedServletException: Request processing failed; nested exception is java.lang.NullPointerException: text)


2) Verifying a pact between Our Little Consumer and Our Provider - a request with an invalid date parameter Given data count > 0 returns a response which has status code 400
      assert expectedStatus == actualStatus
             |              |  |
             400            |  500
                            false

3) Verifying a pact between Our Little Consumer and Our Provider - a request with an invalid date parameter Given data count > 0 returns a response which has a matching body
      $.body -> Expected ''This is not a date' is not a date' but received Map(path -> /provider.json, timestamp -> 1490922210891, exception -> java.time.format.DateTimeParseException, error -> Internal Server Error, status -> 500, message -> org.springframework.web.util.NestedServletException: Request processing failed; nested exception is java.time.format.DateTimeParseException: Text 'This is not a date' could not be parsed at index 0)


:providers:springboot-provider:pactVerify_Our Provider FAILED
:providers:springboot-provider:stopProvider

FAILURE: Build failed with an exception.

* What went wrong:
There were 4 pact failures for provider Our Provider

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

BUILD FAILED
```

Time to update the providers to handle these cases.

## Step 10 - Update the providers to handle the missing/invalid query parameters

Let's fix our providers so they generate the correct responses for the query parameters.

### Dropwizard provider

The Dropwizard root resource gets updated to check if the parameter has been passed, and handle the date parse exception
if it is invalid. Two new exceptions get thrown for these cases.

```groovy
  @GET
  Map providerJson(@QueryParam("validDate") Optional<String> validDate) {
    if (validDate.present) {
      try {
        def valid_time = LocalDateTime.parse(validDate.get())
        [
          test: 'NO',
          validDate: OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")),
          count: 1000
        ]
      } catch (e) {
        throw new InvalidQueryParameterException("'${validDate.get()}' is not a date", e)
      }
    } else {
      throw new QueryParameterRequiredException('validDate is required')
    }
  }
```

Next step is to create exception mappers for the new exceptions, and register them with the Dropwizard environment.

```groovy
class InvalidQueryParameterExceptionMapper implements ExceptionMapper<InvalidQueryParameterException> {
  @Override
  Response toResponse(InvalidQueryParameterException exception) {
    Response.status(Response.Status.BAD_REQUEST)
      .type(MediaType.APPLICATION_JSON_TYPE)
      .entity(JsonOutput.toJson(exception.message))
      .build()
  }
}
```

The main provider run method becomes:

```groovy
  void run(ServiceConfig configuration, Environment environment) {
    environment.jersey().register(new InvalidQueryParameterExceptionMapper())
    environment.jersey().register(new QueryParameterRequiredExceptionMapper())
    environment.jersey().register(new RootResource())
  }
```

Now running the `PactVerificationTest` will pass.

### Springboot provider

The Springboot root controller gets updated in a similar way to the Dropwizard resource.

```groovy
  @RequestMapping("/provider.json")
  Map providerJson(@RequestParam(required = false) String validDate) {
    if (validDate) {
      try {
        def valid_time = LocalDateTime.parse(validDate)
        [
          test: 'NO',
          validDate: OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")),
          count: 1000
        ]
      } catch (e) {
        throw new InvalidQueryParameterException("'$validDate' is not a date", e)
      }
    } else {
      throw new QueryParameterRequiredException('validDate is required')
    }
  }
```

Then, to get the exceptions mapped to the correct response, we need to create a controller advice.

```groovy
@ControllerAdvice(basePackageClasses = RootController)
class RootControllerAdvice extends ResponseEntityExceptionHandler {

  @ExceptionHandler([InvalidQueryParameterException, QueryParameterRequiredException])
  @ResponseBody
  ResponseEntity handleControllerException(HttpServletRequest request, Throwable ex) {
    new ResponseEntity(JsonOutput.toJson(ex.message), HttpStatus.BAD_REQUEST)
  }

}
```

Now running the `pactVerify` is all successful.

## Step 11 - Provider states

We have one final thing to test for. If the provider ever returns a count of zero, we will get a division by
zero error in our client. This is an important bit of information to add to our contract. Let us start with a
consumer test for this.

```groovy
  def 'when there is no data'() {
    given:
    provider {
      given('data count == 0')
      uponReceiving('a request for json data')
      withAttributes(path: '/provider.json', query: [validDate: date.toString()])
      willRespondWith(status: 404)
    }

    when:
    def result
    PactVerificationResult pactResult = provider.runTest {
      result = client.fetchAndProcessData(date.toString())
    }

    then:
    pactResult == PactVerificationResult.Ok.INSTANCE
  }
```

This adds a new interaction to the pact file:

```json

  {
      "description": "a request for json data",
      "request": {
          "method": "GET",
          "path": "/provider.json",
          "query": "validDate=2017-05-22T13%3A34%3A41.515"
      },
      "response": {
          "status": 404
      },
      "providerState": "data count == 0"
  }

```

## Step 12 - provider states for the providers

To be able to verify our providers, we need to be able to change the data that the provider returns. There are different
ways of doing this depending on how the provider is being verified.


### Dropwizard provider

The dropwizard provider is being verified by a test, so we can setup methods annotated with the state and then modify the
controller appropriately. First, we need some data store that we could manipulate. For out case, we are just going to
use a singleton class, but in a real project you would probably use a database.

```groovy
@Singleton
class DataStore {
  int dataCount = 1000
}
```

Next, we update out root resource to use the value from the data store, and throw an exception if there is no data.

```groovy
  @GET
  Map providerJson(@QueryParam("validDate") Optional<String> validDate) {
    if (validDate.present) {
      if (DataStore.instance.dataCount > 0) {
        try {
          def valid_time = LocalDateTime.parse(validDate.get())
          [
            test     : 'NO',
            validDate: OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")),
            count    : DataStore.instance.dataCount
          ]
        } catch (e) {
          throw new InvalidQueryParameterException("'${validDate.get()}' is not a date", e)
        }
      } else {
        throw new NoDataException()
      }
    } else {
      throw new QueryParameterRequiredException('validDate is required')
    }
  }
```

We do the same exception mapping for the new exception as we did before.

```groovy
class NoDataExceptionMapper implements ExceptionMapper<NoDataException> {
  @Override
  Response toResponse(NoDataException exception) {
    Response.status(Response.Status.NOT_FOUND)
      .build()
  }
}
```

Now we can change the data store value in our test based on the provider state.

```groovy
  @State("data count > 0")
  void dataCountGreaterThanZero() {
    DataStore.instance.dataCount = 1000
  }

  @State("data count == 0")
  void dataCountZero() {
    DataStore.instance.dataCount = 0
  }
```

Running the test now passes.

### Springboot provider

Our Springboot provider is being verified by the Pact Gradle verification task, which requires the provider to be
running in the background. We can not directly manipulate it. The Gradle task has a state change URL feature that can
help us here. This is basically a special URL that will receive the state that the provider needs to be in.

First, lets enable the state change URL handling in the build gradle file.

```groovy
pact {
  serviceProviders {
    'Our Provider' {
      port = 8080

      startProviderTask = startProvider
      terminateProviderTask = stopProvider
      stateChangeUrl = url('http://localhost:8080/pactStateChange')

      hasPactWith('Our Little Consumer') {
        pactFile = file("$buildDir/pacts/Our Little Consumer-Our Provider.json")
      }
    }
  }
}
```

Now we create a new controller to handle this. As this controller is only for our test, we make sure it is only available
in the test profile. We also need to make sure the app runs in the test profile by adding a parameter to the start task.

```groovy
task startProvider(type: SpawnProcessTask, dependsOn: 'assemble') {
  command "java -Dspring.profiles.active=test -jar ${jar.archivePath}"
  ready 'Started MainApplication'
}
```

Here is the state change controller:

```groovy
@RestController
@Profile("test")
class StateChangeController {

  @RequestMapping(value = "/pactStateChange", method = RequestMethod.POST)
  void providerState(@RequestBody Map body) {
    switch (body.state) {
      case 'data count > 0':
        DataStore.instance.dataCount = 1000
        break
      case 'data count == 0':
        DataStore.instance.dataCount = 0
        break
    }
  }

}
```

This controller will change the value of the datastore. We then use the datastore in our normal controller.

```groovy
@Singleton
class DataStore {
  int dataCount = 1000
}
```

```groovy
  @RequestMapping("/provider.json")
  Map providerJson(@RequestParam(required = false) String validDate) {
    if (validDate) {
      if (DataStore.instance.dataCount > 0) {
        try {
          def valid_time = LocalDateTime.parse(validDate)
          [
            test     : 'NO',
            validDate: OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")),
            count    : DataStore.instance.dataCount
          ]
        } catch (e) {
          throw new InvalidQueryParameterException("'$validDate' is not a date", e)
        }
      } else {
        throw new NoDataException()
      }
    } else {
      throw new QueryParameterRequiredException('validDate is required')
    }
  }
```

and update our controller advice to return the 404 response when a `NoDataException` is raised.

```groovy
@ControllerAdvice(basePackageClasses = RootController)
class RootControllerAdvice extends ResponseEntityExceptionHandler {

  @ExceptionHandler([InvalidQueryParameterException, QueryParameterRequiredException])
  @ResponseBody
  ResponseEntity handleControllerException(HttpServletRequest request, Throwable ex) {
    new ResponseEntity(JsonOutput.toJson(ex.message), HttpStatus.BAD_REQUEST)
  }

  @ExceptionHandler(NoDataException)
  @ResponseBody
  ResponseEntity handleNoDataException(HttpServletRequest request, Throwable ex) {
    new ResponseEntity(HttpStatus.NOT_FOUND)
  }

}
```

Running the Gradle pact verification now passes.

# Step 13 - Using a Pact Broker

We've been publishing our pacts from the consumer project by coping the files over to the provider project, but we can
use a Pact Broker to do this instead.

### Consumer

First, in the consumer project we need to tell the Gradle Pact plugin about our broker.

```groovy
plugins {
  id "au.com.dius.pact" version "3.3.7"
}

apply plugin: 'application'

mainClassName = 'au.com.dius.pactworkshop.consumer.Consumer'
version = 0

dependencies {
  compile 'org.codehaus.groovy.modules.http-builder:http-builder:0.7.1'
}

pact {
  publish {
    pactBrokerUrl = "https://$pactBrokerUser:$pactBrokerPassword@test.pact.dius.com.au"
  }
}
```

Now, we can run `./gradlew consumer:pactPublish` after running the consumer tests to have the generated pact file 
published to the broker. Afterwards, you can navigate to the Pact Broker URL and see the published pact there.

### Dropwizard provider

In the `PactVerificationTest` we can change the source we fetch pacts from by using a `@PactBroker` annotation instead
of the `@PactFolder` one. We also need to pass the username and property through to the test.

Updated gradle build file:

```groovy
test {
  systemProperty 'pactBrokerUser', pactBrokerUser
  systemProperty 'pactBrokerPassword', pactBrokerPassword
}
```

Updated test:

```groovy
@RunWith(PactRunner)
@Provider('Our Provider')
@PactBroker(host = 'test.pact.dius.com.au', protocol = 'https', port = "443",
  authentication = @PactBrokerAuth(username = '${pactBrokerUser}', password = '${pactBrokerPassword}'))
class PactVerificationTest {

  @ClassRule
  public static final DropwizardAppRule<ServiceConfig> RULE = new DropwizardAppRule<ServiceConfig>(MainApplication,
    ResourceHelpers.resourceFilePath("main-app-config.yaml"))

  @TestTarget
  public final Target target = new HttpTarget(8080)

  @State("data count > 0")
  void dataCountGreaterThanZero() {
    DataStore.instance.dataCount = 1000
  }

  @State("data count == 0")
  void dataCountZero() {
    DataStore.instance.dataCount = 0
  }
}
```

### Springboot provider

The springboot provider is using the Gradle plugin, so we can just configure its build to fetch the pacts from the 
broker.

Updated build file:

```groovy
pact {
  serviceProviders {
    'Our Provider' {
      port = 8080

      startProviderTask = startProvider
      terminateProviderTask = stopProvider
      stateChangeUrl = url('http://localhost:8080/pactStateChange')

      hasPactsFromPactBroker("https://test.pact.dius.com.au", authentication: ['Basic', pactBrokerUser, pactBrokerPassword])
    }
  }
}
```
