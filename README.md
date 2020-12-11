## Introduction

This workshop is aimed at demonstrating core features and benefits of contract testing with Pact.

Whilst contract testing can be applied retrospectively to systems, we will follow the [consumer driven contracts](https://martinfowler.com/articles/consumerDrivenContracts.html) approach in this workshop - where a new consumer and provider are created in parallel to evolve a service over time, especially where there is some uncertainty with what is to be built.

This workshop should take from 1 to 2 hours, depending on how deep you want to go into each topic.

**Example project overview**

This workshop is setup with a number of steps that can be run through. Each step is in a branch, so to run through a
step of the workshop just check out the branch for that step (i.e. `git checkout step1`).

This project has 3 components, a consumer project and two service providers, one Dropwizard and one
Springboot service that the consumer will interaction with.

**Workshop outline**:

- [step 1: **Simple Consumer calling Provider**](https://github.com/DiUS/pact-workshop-jvm#step-1---simple-consumer-calling-provider): Create our consumer before the Provider API even exists
- [step 2: **Client Tested but integration fails**](https://github.com/DiUS/pact-workshop-jvm#step-2---client-tested-but-integration-fails): Write a unit test for our consumer
- [step 3: **Pact to the rescue**](https://github.com/DiUS/pact-workshop-jvm#step-3---pact-to-the-rescue): Write a Pact test for our consumer
- [step 4: **Verify pact against provider**](https://github.com/DiUS/pact-workshop-jvm#step-4---verify-pact-against-provider): Verify the consumer pact with the Provider API (Gradle)
- [step 5: **Verify the provider with a test**](https://github.com/DiUS/pact-workshop-jvm#step-5---verify-the-provider-with-a-test): Verify the consumer pact with the Provider API (JUnit)
- [step 6: **Back to the client we go**](https://github.com/DiUS/pact-workshop-jvm#step-6---back-to-the-client-we-go): Fix the consumer's bad assumptions about the Provider
- [step 7: **Verify the providers again**](https://github.com/DiUS/pact-workshop-jvm#step-7---verify-the-providers-again): Update the provider  build
- [step 8: **Test for the missing query parameter**](https://github.com/DiUS/pact-workshop-jvm#step-8---test-for-the-missing-query-parameter): Test unhappy path of missing query string
- [step 9: **Verify the provider with the missing/invalid date query parameter**](https://github.com/DiUS/pact-workshop-jvm#step-9---verify-the-provider-with-the-missinginvalid-date-query-parameter): Verify provider's ability to handle the missing query string
- [step 10: **Update the providers to handle the missing/invalid query parameters**](https://github.com/DiUS/pact-workshop-jvm#step-10---update-the-providers-to-handle-the-missinginvalid-query-parameters): Update provider to handlre mising query string
- [step 11: **Provider states**](https://github.com/DiUS/pact-workshop-jvm#step-11---provider-states): Write a pact test for the `404` case
- [step 12: **provider states for the providers**](https://github.com/DiUS/pact-workshop-jvm#step-12---provider-states-for-the-providers): Update API to handle `404` case
- [step 13: **Using a Pact Broker**](https://github.com/DiUS/pact-workshop-jvm#step-13---using-a-pact-broker): Implement a broker workflow for integration with CI/CD

_NOTE: Each step is tied to, and must be run within, a git branch, allowing you to progress through each stage incrementally. For example, to move to step 2 run the following: `git checkout step2`_

## Learning objectives

If running this as a team workshop format, you may want to take a look through the [learning objectives](./LEARNING.md).

## Requirements

- [Java](https://java.com/en/download/) (version 1.8+)
- [Docker Compose](https://docs.docker.com/compose/install/)

## Step 1 - Simple Consumer calling Provider

Given we have a client that needs to make a HTTP GET request to a provider service, and requires a response in JSON format.


![Simple Consumer](diagrams/workshop_step1.png)


The client is quite simple and looks like this

*consumer/src/main/java/au/com/dius/pactworkshop/consumer/Client.java:*

```java
public class Client {
  public Object loadProviderJson() throws UnirestException {
    return Unirest.get("http://localhost:8080/provider.json")
      .queryString("validDate", LocalDateTime.now().toString())
      .asJson().getBody();
  }
}
```

and the dropwizard provider resource

*providers/dropwizard-provider/src/main/java/au/com/dius/pactworkshop/dropwizardprovider/RootResource.java:*

```java
@Path("/provider.json")
@Produces(MediaType.APPLICATION_JSON)
public class RootResource {

  @GET
  public Map<String, Object> providerJson(@QueryParam("validDate") Optional<String> validDate) {
    LocalDateTime valid_time = LocalDateTime.parse(validDate.get());
    Map<String, Object> result = new HashMap<>();
    result.put("test", "NO");
    result.put("validDate", LocalDateTime.now().toString());
    result.put("count", 1000);
    return result;
  }

}
```

The springboot provider controller is similar

*providers/springboot-provider/src/main/java/au/com/dius/pactworkshop/springbootprovider/RootController.java:*

```java
@RestController
public class RootController {

  @RequestMapping("/provider.json")
  public Map<String, Serializable> providerJson(@RequestParam(required = false) String validDate) {
    LocalDateTime validTime = LocalDateTime.parse(validDate);
    Map<String, Serializable> map = new HashMap<>(3);
    map.put("test", "NO");
    map.put("validDate", LocalDateTime.now().toString());
    map.put("count", 1000);
    return map;
  }

}
```

This providers expects a `validDate` parameter in HTTP date format, and then return some simple json back.


![Sequence Diagram](diagrams/sequence_diagram.png)


Running the client with either provider works nicely. For example, start the dropwizard-provider in one terminal:

```console
$ ./gradlew :providers:dropwizard-provider:run
```

_NOTE_: this task won't complete, it will get to 75% and remain that way until you shutdown the process: `<=========----> 75% EXECUTING [59s]`)

(to start the Spring boot provider instead, you would run `./gradlew :providers:springboot-provider:bootRun`).

Once the provider has successfully initialized, open another terminal session and run the consumer:

```console
$ ./gradlew :consumer:run

> Task :consumer:run
{"test":"NO","validDate":"2018-04-10T10:59:41.122","count":1000}


BUILD SUCCESSFUL in 1s
2 actionable tasks: 2 executed

```

Don't forget to stop the dropwizard-provider that is running in the first terminal when you have finished this step.

## Step 2 - Client Tested but integration fails

Now lets get the client to use the data it gets back from the provider. Here is the updated client method that uses the returned data:

*consumer/src/main/java/au/com/dius/pactworkshop/consumer/Client.java:*

```java
  public List<Object> fetchAndProcessData() throws UnirestException {
      JsonNode data = loadProviderJson();
      System.out.println("data=" + data);

      JSONObject jsonObject = data.getObject();
      int value = 100 / jsonObject.getInt("count");
      ZonedDateTime date = ZonedDateTime.parse(jsonObject.getString("date"));

      System.out.println("value=" + value);
      System.out.println("date=" + date);
      return Arrays.asList(value, date);
  }
```

![Sequence 2](diagrams/step2_sequence_diagram.png)

Let's now test our updated client. We're using [Wiremock](http://wiremock.org/) here to mock out the provider.

*consumer/src/test/java/au/com/dius/pactworkshop/consumer/ClientTest.java:*

```java
public class ClientTest {

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8080);

  @Test
  public void canProcessTheJsonPayloadFromTheProvider() throws UnirestException {

    String date = "2013-08-16T15:31:20+10:00";

    stubFor(get(urlPathEqualTo("/provider.json"))
      .withQueryParam("validDate", matching(".+"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"test\": \"NO\", \"date\": \"" + date + "\", \"count\": 100}")));

    List<Object> data = new Client().fetchAndProcessData();

    assertThat(data, hasSize(2));
    assertThat(data.get(0), is(1));
    assertThat(data.get(1), is(ZonedDateTime.parse(date)));
  }

}
```

![Unit Test With Mocked Response](diagrams/step2_unit_test.png)

Let's run this spec and see it all pass:

```console
$ ./gradlew :consumer:check

BUILD SUCCESSFUL in 0s
3 actionable tasks: 3 up-to-date
```

However, there is a problem with this integration point. Running the actual client against any of the providers results in
 a runtime exception!

```console
$ ./gradlew :consumer:run

> Task :consumer:run FAILED
data={"test":"NO","validDate":"2018-04-10T11:48:36.838","count":1000}
Exception in thread "main" org.json.JSONException: JSONObject["date"] not found.
        at org.json.JSONObject.get(JSONObject.java:471)
        at org.json.JSONObject.getString(JSONObject.java:717)
        at au.com.dius.pactworkshop.consumer.Client.fetchAndProcessData(Client.java:26)
        at au.com.dius.pactworkshop.consumer.Consumer.main(Consumer.java:7)


FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':consumer:run'.
> Process 'command '/usr/lib/jvm/java-8-oracle/bin/java'' finished with non-zero exit value 1

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output. Run with --scan to get full insights.

* Get more help at https://help.gradle.org

BUILD FAILED in 1s
2 actionable tasks: 1 executed, 1 up-to-date
```

The provider returns a `validDate` while the consumer is trying to use `date`, which will blow up when run for
real even with the tests all passing. Here is where Pact comes in.

## Step 3 - Pact to the rescue

Let us add Pact to the project and write a consumer pact test.

*consumer/src/test/java/au/com/dius/pactworkshop/consumer/ClientPactTest.java*

```java
public class ClientPactTest {

  // This sets up a mock server that pretends to be our provider
  @Rule
  public PactProviderRule provider = new PactProviderRule("Our Provider", "localhost", 1234, this);

  private LocalDateTime dateTime;
  private String dateResult;

  // This defines the expected interaction for out test
  @Pact(provider = "Our Provider", consumer = "Our Little Consumer")
  public RequestResponsePact pact(PactDslWithProvider builder) {
    dateTime = LocalDateTime.now();
    dateResult = "2013-08-16T15:31:20+10:00";
    return builder
      .given("data count > 0")
      .uponReceiving("a request for json data")
      .path("/provider.json")
      .method("GET")
      .query("validDate=" + dateTime.toString())
      .willRespondWith()
      .status(200)
      .body(
          new PactDslJsonBody()
              .stringValue("test", "NO")
              .stringValue("date", dateResult)
              .numberValue("count", 100)
      )
      .toPact();
  }

  @Test
  @PactVerification("Our Provider")
  public void pactWithOurProvider() throws UnirestException {
    // Set up our HTTP client class
    Client client = new Client(provider.getUrl());

    // Invoke out client
    List<Object> result = client.fetchAndProcessData(dateTime);

    assertThat(result, hasSize(2));
    assertThat(result.get(0), is(1));
    assertThat(result.get(1), is(ZonedDateTime.parse(dateResult)));
  }
}
```


![Test using Pact](diagrams/step3_pact.png)


This test starts a mock server on a random port that pretends to be our provider. To get this to work we needed to update
our consumer to pass in the URL of the provider. We also updated the `fetchAndProcessData` method to pass in the
query parameter.

Running this spec still passes, but it creates a pact file which we can use to validate our assumptions on the provider side.

```console
$ ./gradlew :consumer:check
Starting a Gradle Daemon, 1 incompatible and 3 stopped Daemons could not be reused, use --status for details

BUILD SUCCESSFUL in 8s
4 actionable tasks: 1 executed, 3 up-to-date
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
        "query": {
          "validDate": [
            "2020-06-16T11:49:49.485"
          ]
        }
      },
      "response": {
        "status": 200,
        "headers": {
          "Content-Type": "application/json; charset=UTF-8"
        },
        "body": {
          "date": "2013-08-16T15:31:20+10:00",
          "test": "NO",
          "count": 100
        },
        "matchingRules": {
          "header": {
            "Content-Type": {
              "matchers": [
                {
                  "match": "regex",
                  "regex": "application/json(;\\s?charset=[\\w\\-]+)?"
                }
              ],
              "combine": "AND"
            }
          }
        }
      },
      "providerStates": [
        {
          "name": "data count > 0"
        }
      ]
    }
  ],
  "metadata": {
    "pactSpecification": {
      "version": "3.0.0"
    },
    "pact-jvm": {
      "version": "4.1.2"
    }
  }
}
```

## Step 4 - Verify pact against provider

There are two ways of validating a pact file against a provider. The first is using a build tool (like Gradle) to
execute the pact against the running service. The second is to write a pact verification test. We will be doing both
in this step.

First, we need to **publish** the pact file from the consumer project. For this workshop, we have a `publishWorkshopPact` task in the
main project to do this.

```console
$ ./gradlew publishWorkshopPact

BUILD SUCCESSFUL in 0s
2 actionable tasks: 2 up-to-dat
```

The Pact file from the consumer project will now exist in the build directory of the two provider projects.

![Pact Verification](diagrams/step4_pact.png)


### Verifying the springboot provider

For the springboot provider, we are going to use Gradle to verify the pact file for us. We need to add the pact gradle
plugin and the spawn plugin to the project and configure them.

**NOTE: This will not work on Windows, as the Gradle spawn plugin will not work with Windows.**

*providers/springboot-provider/build.gradle:*

```groovy
plugins {
  id "au.com.dius.pact" version "4.1.7"
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
    'Our_Provider' {
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

> Task :providers:springboot-provider:startProvider

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.0.0.RELEASE)
```

... omitting lots of logs ...

```console
2018-04-10 13:55:19.709  INFO 7912 --- [           main] a.c.d.p.s.MainApplication                : Started MainApplication in 3.311 seconds (JVM running for 3.774)
java -jar /home/ronald/Development/Projects/Pact/pact-workshop-jvm/providers/springboot-provider/build/libs/springboot-provider.jar is ready.

> Task :providers:springboot-provider:pactVerify_Our_Provider FAILED

Verifying a pact between Our Little Consumer and Our_Provider
  [Using File /home/ronald/Development/Projects/Pact/pact-workshop-jvm/providers/springboot-provider/build/pacts/Our Little Consumer-Our Provider.json]
  Given data count > 0
         WARNING: State Change ignored as there is no stateChange URL
  a request for json data
    returns a response which
      has status code 200 (OK)
      has a matching body (FAILED)

NOTE: Skipping publishing of verification results as it has been disabled (pact.verifier.publishResults is not 'true')


Failures:

1) Verifying a pact between Our Little Consumer and Our_Provider - a request for json data Given data count > 0

    1.1) BodyMismatch: $ BodyMismatch: Expected date='2013-08-16T15:31:20+10:00' but was missing

        {
        -  "date": "2013-08-16T15:31:20+10:00",
          "test": "NO",
        -  "count": 100
        +  "count": 1000,
        +  "validDate": "2020-06-16T12:08:04.314696"
        }


    1.2) BodyMismatch: $.count BodyMismatch: Expected 100 (Integer) but received 1000 (Integer)




FAILURE: Build failed with an exception.

* What went wrong:
There were 2 non-pending pact failures for provider Our_Provider

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output. Run with --scan to get full insights.

* Get more help at https://help.gradle.org

BUILD FAILED in 6s
5 actionable tasks: 5 executed
```

The test has failed for 2 reasons. Firstly, the count field has a different value to what was expected by the consumer.
Secondly, and more importantly, the consumer was expecting a `date` field while the provider generates a `validDate`
field. Also, the date formats are different.

## Step 5 - Verify the provider with a test

In this step we will verify the same pact file against the Dropwizard provider using a JUnit test. If you need to,
re-run the `publishWorkshopPact` to get the pact file in the provider project.

We add the pact provider junit jar and the dropwizard testing jar to our project dependencies, and then we can create a
simple test to verify our provider.

```java
@RunWith(PactRunner.class)
@Provider("Our Provider")
@PactFolder("build/pacts")
public class PactVerificationTest {
  @ClassRule
  public static final DropwizardAppRule<ServiceConfig> RULE = new DropwizardAppRule<ServiceConfig>(MainApplication.class,
    ResourceHelpers.resourceFilePath("main-app-config.yaml"));

  @TestTarget
  public final Target target = new HttpTarget(8080);

  @State("data count > 0")
  public void dataCountGreaterThanZero() {

  }
}
```

This test will start the dropwizard app (using the class rule), and then execute the pact requests (defined by the
`@PactFolder` annotation) against the test target.

Running this test will fail for the same reasons as in step 4.

```console
$ ./gradlew :providers:dropwizard-provider:test
Starting a Gradle Daemon, 1 incompatible and 2 stopped Daemons could not be reused, use --status for details

> Task :providers:dropwizard-provider:test

au.com.dius.pactworkshop.dropwizardprovider.PactVerificationTest > Our Little Consumer - a request for json data FAILED
    java.lang.AssertionError

1 test completed, 1 failed


FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':providers:dropwizard-provider:test'.
> There were failing tests. See the report at: file:///home/ronald/Development/Projects/Pact/pact-workshop-jvm/providers/dropwizard-provider/build/reports/tests/test/index.html

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output. Run with --scan to get full insights.

* Get more help at https://help.gradle.org

BUILD FAILED in 12s
4 actionable tasks: 4 executed
```

The JUnit build report has the expected failures.

```
java.lang.AssertionError: 
Failures:

1) a request for json data

    1.1) BodyMismatch: $ BodyMismatch: Expected date='2013-08-16T15:31:20+10:00' but was missing

        {
        -  "date": "2013-08-16T15:31:20+10:00",
          "test": "NO",
        -  "count": 100
        +  "count": 1000,
        +  "validDate": "2020-06-16T12:29:52.836"
        }


    1.2) BodyMismatch: $.count BodyMismatch: Expected 100 (Integer) but received 1000 (Integer)
```

## Step 6 - Back to the client we go

Let's correct the consumer test to handle any integer for `count` and use the correct field for the `date`. Then we need
to add a type matcher for `count` and change the field for the date to be `validDate`. We can also add a date expression
to make sure the `validDate` field is a valid date. This is important because we are parsing it.

The consumer test is now updated to:

```java
          .body(
              new PactDslJsonBody()
                  .stringValue("test", "NO")
                  .datetime("validDate", "yyyy-MM-dd'T'HH:mm:ssXX", dateResult.toInstant())
                  .integerType("count", 100)
          )
```

Running this test will fail until we fix the client. Here is the correct client function:

```java
  public List<Object> fetchAndProcessData(LocalDateTime dateTime) throws UnirestException {
      JsonNode data = loadProviderJson(dateTime);
      System.out.println("data=" + data);

      JSONObject jsonObject = data.getObject();
      int value = 100 / jsonObject.getInt("count");
      TemporalAccessor date = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")
              .parse(jsonObject.getString("validDate"));
      System.out.println("value=" + value);
      System.out.println("date=" + date);
      return Arrays.asList(value, OffsetDateTime.from(date));
  }
```

Now the test passes. But we still have a problem with the date format, which we must fix in the provider. Running the
client now fails because of that.

```console
$ ./gradlew consumer:run
Starting a Gradle Daemon, 1 busy and 1 incompatible and 2 stopped Daemons could not be reused, use --status for details

> Task :consumer:run FAILED
data={"test":"NO","validDate":"2018-04-10T14:39:50.419","count":1000}
Exception in thread "main" java.time.format.DateTimeParseException: Text '2018-04-10T14:39:50.419' could not be parsed at index 19
        at java.time.format.DateTimeFormatter.parseResolved0(DateTimeFormatter.java:1949)
        at java.time.format.DateTimeFormatter.parse(DateTimeFormatter.java:1851)
        at java.time.OffsetDateTime.parse(OffsetDateTime.java:402)
        at au.com.dius.pactworkshop.consumer.Client.fetchAndProcessData(Client.java:34)
        at au.com.dius.pactworkshop.consumer.Consumer.main(Consumer.java:9)


FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':consumer:run'.
> Process 'command '/usr/lib/jvm/java-8-oracle/bin/java'' finished with non-zero exit value 1

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output. Run with --scan to get full insights.

* Get more help at https://help.gradle.org

BUILD FAILED in 5s
2 actionable tasks: 1 executed, 1 up-to-date
```

We need to **publish** the consumer pact file to the provider projects again. Then, running the provider verification
tests we get the expected failure about the date format.

```
Failures:

1) Verifying a pact between Our Little Consumer and Our_Provider - a request for json data Given data count > 0

    1.1) BodyMismatch: $.validDate BodyMismatch: Expected "2020-06-16T13:01:21.675150" to match a datetime of 'yyyy-MM-dd'T'HH:mm:ssXX': Text '2020-06-16T13:01:21.675150' could not be parsed at index 19

```

## Step 7 - Verify the providers again

Lets fix the providers and then re-run the verification tests. Here is the corrected Dropwizard resource:

```java
@RestController
public class RootController {

  @RequestMapping("/provider.json")
  public Map<String, Serializable> providerJson(@RequestParam(required = false) String validDate) {
    LocalDateTime validTime = LocalDateTime.parse(validDate);
    Map<String, Serializable> map = new HashMap<>(3);
    map.put("test", "NO");
    map.put("validDate", OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")));
    map.put("count", 1000);
    return map;
  }

}
```


![Verification Passes](diagrams/step7_pact.png)


Running the verification against the providers now pass. Yay!

## Step 8 - Test for the missing query parameter

In this step we are going to add a test for the case where the query parameter is missing or invalid. We do this by 
adding additional tests and expectations to the consumer pact test. Our client code needs to be modified slightly to
be able to pass invalid dates in, and if the date parameter is null, don't include it in the request.

Here are the two additional tests:

*consumer/src/test/java/au/com/dius/pactworkshop/consumer/ClientPactTest.java:*

```java
  @Pact(provider = "Our Provider", consumer = "Our Little Consumer")
  public RequestResponsePact pactForMissingDateParameter(PactDslWithProvider builder) {
    dateTime = LocalDateTime.now();
    dateResult = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    return builder
            .given("data count > 0")
            .uponReceiving("a request with a missing date parameter")
            .path("/provider.json")
            .method("GET")
            .willRespondWith()
            .status(400)
            .body(
                new PactDslJsonBody().stringValue("error", "validDate is required")
            )
            .toPact();
  }

  @Test
  @PactVerification(value = "Our Provider", fragment = "pactForMissingDateParameter")
  public void handlesAMissingDateParameter() throws UnirestException {
    // Set up our HTTP client class
    Client client = new Client(provider.getUrl());

    // Invoke out client
    List<Object> result = client.fetchAndProcessData(null);

    assertThat(result, hasSize(2));
    assertThat(result.get(0), is(0));
    assertThat(result.get(1), nullValue());
  }

  @Pact(provider = "Our Provider", consumer = "Our Little Consumer")
  public RequestResponsePact pactForInvalidDateParameter(PactDslWithProvider builder) {
    dateTime = LocalDateTime.now();
    dateResult = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    return builder
            .given("data count > 0")
            .uponReceiving("a request with an invalid date parameter")
            .path("/provider.json")
            .method("GET")
            .query("validDate=This is not a date")
            .willRespondWith()
            .status(400)
            .body(
                 new PactDslJsonBody().stringValue("error", "'This is not a date' is not a date")
            )
            .toPact();
  }

  @Test
  @PactVerification(value = "Our Provider", fragment = "pactForInvalidDateParameter")
  public void handlesAInvalidDateParameter() throws UnirestException {
    // Set up our HTTP client class
    Client client = new Client(provider.getUrl());

    // Invoke out client
    List<Object> result = client.fetchAndProcessData("This is not a date");

    assertThat(result, hasSize(2));
    assertThat(result.get(0), is(0));
    assertThat(result.get(1), nullValue());
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
        "Content-Type": "application/json; charset=UTF-8"
      },
      "body": {
        "error": "validDate is required"
      },
      "matchingRules": {
        "header": {
          "Content-Type": {
            "matchers": [
              {
                "match": "regex",
                "regex": "application/json(;\\s?charset=[\\w\\-]+)?"
              }
            ],
            "combine": "AND"
          }
        }
      }
    },
    "providerStates": [
      {
        "name": "data count > 0"
      }
    ]
  },
  {
    "description": "a request with an invalid date parameter",
    "request": {
      "method": "GET",
      "path": "/provider.json",
      "query": {
        "validDate": [
          "This is not a date"
        ]
      }
    },
    "response": {
      "status": 400,
      "headers": {
        "Content-Type": "application/json; charset=UTF-8"
      },
      "body": {
        "error": "'This is not a date' is not a date"
      },
      "matchingRules": {
        "header": {
          "Content-Type": {
            "matchers": [
              {
                "match": "regex",
                "regex": "application/json(;\\s?charset=[\\w\\-]+)?"
              }
            ],
            "combine": "AND"
          }
        }
      }
    },
    "providerStates": [
      {
        "name": "data count > 0"
      }
    ]
  }
]
```

## Step 9 - Verify the provider with the missing/invalid date query parameter
   
Let us run this updated pact file with our providers (first run the `publishWorkshopPact` task). We get a 500 response as the provider can't handle the missing
or incorrect date.

Here is the springboot test output:

```console
Verifying a pact between Our Little Consumer and Our_Provider
  [Using File /home/ronald/Development/Projects/Pact/pact-workshop-jvm/providers/springboot-provider/build/pacts/Our Little Consumer-Our Provider.json]
  Given data count > 0
         WARNING: State Change ignored as there is no stateChange URL
  a request for json data
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)
  Given data count > 0
         WARNING: State Change ignored as there is no stateChange URL
  a request with a missing date parameter
    returns a response which
      has status code 400 (FAILED)
      has a matching body (FAILED)
  Given data count > 0
         WARNING: State Change ignored as there is no stateChange URL
  a request with an invalid date parameter
    returns a response which
      has status code 400 (FAILED)
      has a matching body (FAILED)

NOTE: Skipping publishing of verification results as it has been disabled (pact.verifier.publishResults is not 'true')


Failures:

1) Verifying a pact between Our Little Consumer and Our_Provider - a request with a missing date parameter Given data count > 0

    1.1) StatusMismatch: expected status of 400 but was 500

    1.2) BodyMismatch: $.error BodyMismatch: Expected 'validDate is required' (String) but received 'Internal Server Error' (String)

    1.3) StatusMismatch: expected status of 400 but was 500

    1.4) BodyMismatch: $.error BodyMismatch: Expected ''This is not a date' is not a date' (String) but received 'Internal Server Error' (String)

```

Time to update the providers to handle these cases.

## Step 10 - Update the providers to handle the missing/invalid query parameters

Let's fix our providers so they generate the correct responses for the query parameters.

### Dropwizard provider

The Dropwizard root resource gets updated to check if the parameter has been passed, and handle the date parse exception
if it is invalid. Two new exceptions get thrown for these cases.

```java
  @GET
  public Map<String, Serializable> providerJson(@QueryParam("validDate") Optional<String> validDate) {
    if (validDate.isPresent()) {
      try {
        LocalDateTime validTime = LocalDateTime.parse(validDate.get());
        Map<String, Serializable> result = new HashMap<>(3);
        result.put("test", "NO");
        result.put("validDate", OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")));
        result.put("count", 1000);
        return result;
      } catch (DateTimeParseException e) {
        throw new InvalidQueryParameterException("'" + validDate.get() + "' is not a date", e);
      }
    } else {
      throw new QueryParameterRequiredException("validDate is required");
    }
  }
```

Next step is to create exception mappers for the new exceptions, and register them with the Dropwizard environment.

```java
public class InvalidQueryParameterExceptionMapper implements ExceptionMapper<InvalidQueryParameterException> {
  @Override
  public Response toResponse(InvalidQueryParameterException exception) {
    return Response.status(Response.Status.BAD_REQUEST)
      .type(MediaType.APPLICATION_JSON_TYPE)
      .entity("{\"error\": \"" + exception.getMessage() + "\"}")
      .build();
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

```java
  @RequestMapping("/provider.json")
  public Map<String, Serializable> providerJson(@RequestParam(required = false) String validDate) {
    if (StringUtils.isNotEmpty(validDate)) {
      try {
        LocalDateTime validTime = LocalDateTime.parse(validDate);
        Map<String, Serializable> map = new HashMap<>(3);
        map.put("test", "NO");
        map.put("validDate", OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")));
        map.put("count", 1000);
        return map;
      } catch (DateTimeParseException e) {
        throw new InvalidQueryParameterException("'" + validDate + "' is not a date", e);
      }
    } else {
      throw new QueryParameterRequiredException("validDate is required");
    }
  }
```

Then, to get the exceptions mapped to the correct response, we need to create a controller advice.

```java
@ControllerAdvice(basePackageClasses = RootController.class)
public class RootControllerAdvice extends ResponseEntityExceptionHandler {
  @ExceptionHandler({InvalidQueryParameterException.class, QueryParameterRequiredException.class})
  @ResponseBody
  public ResponseEntity<String> handleControllerException(HttpServletRequest request, Throwable ex) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new ResponseEntity<>("{\"error\": \"" + ex.getMessage() + "\"}", headers, HttpStatus.BAD_REQUEST);
  }
}
```

Now running the `pactVerify` is all successful.

## Step 11 - Provider states

We have one final thing to test for. If the provider ever returns a count of zero, we will get a division by
zero error in our client. This is an important bit of information to add to our contract. Let us start with a
consumer test for this.

```java
    @Pact(provider = "Our Provider", consumer = "Our Little Consumer")
    public RequestResponsePact pactForWhenThereIsNoData(PactDslWithProvider builder) {
      dateTime = LocalDateTime.now();
      return builder
              .given("data count == 0")
              .uponReceiving("a request for json data")
              .path("/provider.json")
              .method("GET")
              .query("validDate=" + dateTime.toString())
              .willRespondWith()
              .status(404)
              .toPact();
    }
  
    @Test
    @PactVerification(value = "Our Provider", fragment = "pactForWhenThereIsNoData")
    public void whenThereIsNoData() throws UnirestException {
      // Set up our HTTP client class
      Client client = new Client(provider.getUrl());
  
      // Invoke out client
      List<Object> result = client.fetchAndProcessData(dateTime.toString());
  
      assertThat(result, hasSize(2));
      assertThat(result.get(0), is(0));
      assertThat(result.get(1), nullValue());
    }
```

This adds a new interaction to the pact file:

```json

  {
        "description": "a request for json data",
        "request": {
          "method": "GET",
          "path": "/provider.json",
          "query": {
            "validDate": [
              "2020-06-16T13:56:47.303"
            ]
          }
        },
        "response": {
          "status": 404
        },
        "providerStates": [
          {
            "name": "data count == 0"
          }
        ]
  }

```

## Step 12 - provider states for the providers

To be able to verify our providers, we need to be able to change the data that the provider returns. There are different
ways of doing this depending on how the provider is being verified.


### Dropwizard provider

The dropwizard provider is being verified by a test, so we can setup methods annotated with the state and then modify the
controller appropriately. First, we need some data store that we could manipulate. For out case, we are just going to
use a singleton class, but in a real project you would probably use a database.

```java
public class DataStore {
  public static final DataStore INSTANCE = new DataStore();
  private int dataCount = 1000;

  private DataStore() { }

  public int getDataCount() {
    return dataCount;
  }

  public void setDataCount(int dataCount) {
    this.dataCount = dataCount;
  }
}
```

Next, we update our root resource to use the value from the data store, and throw an exception if there is no data.

```java
  @GET
  public Map<String, Serializable> providerJson(@QueryParam("validDate") Optional<String> validDate) {
    if (validDate.isPresent()) {
      if (DataStore.INSTANCE.getDataCount() > 0) {
        try {
          LocalDateTime validTime = LocalDateTime.parse(validDate.get());
          Map<String, Serializable> result = new HashMap<>(3);
          result.put("test", "NO");
          result.put("validDate", OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")));
          result.put("count", DataStore.INSTANCE.getDataCount());
          return result;
        } catch (DateTimeParseException e) {
          throw new InvalidQueryParameterException("'" + validDate.get() + "' is not a date", e);
        }
      } else {
        throw new NoDataException();
      }
    } else {
      throw new QueryParameterRequiredException("validDate is required");
    }
  }
```

We do the same exception mapping for the new exception as we did before.

```java
public class NoDataExceptionMapper implements ExceptionMapper<NoDataException> {
  @Override
  public Response toResponse(NoDataException exception) {
    return Response.status(Response.Status.NOT_FOUND).build();
  }
}
```

Now we can change the data store value in our test based on the provider state.

```java
  @State("data count > 0")
  public void dataCountGreaterThanZero() {
    DataStore.INSTANCE.setDataCount(1000);
  }

  @State("data count == 0")
  public void dataCountZero() {
    DataStore.INSTANCE.setDataCount(0);
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
    'Our_Provider' {
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

```java
@RestController
@Profile("test")
public class StateChangeController {
  @RequestMapping(value = "/pactStateChange", method = RequestMethod.POST)
  public void providerState(@RequestBody Map body) {
    if (body.get("state").equals("data count > 0")) {
      DataStore.INSTANCE.setDataCount(1000);
    } else if (body.get("state").equals("data count == 0")) {
      DataStore.INSTANCE.setDataCount(0);
    }
  }
}
```

This controller will change the value of the datastore. We then use the datastore in our normal controller.

```java
  @RequestMapping("/provider.json")
  public Map<String, Serializable> providerJson(@RequestParam(required = false) String validDate) {
    if (StringUtils.isNotEmpty(validDate)) {
      if (DataStore.INSTANCE.getDataCount() > 0) {
        try {
          LocalDateTime validTime = LocalDateTime.parse(validDate);
          Map<String, Serializable> map = new HashMap<>(3);
          map.put("test", "NO");
          map.put("validDate", OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")));
          map.put("count", DataStore.INSTANCE.getDataCount());
          return map;
        } catch (DateTimeParseException e) {
          throw new InvalidQueryParameterException("'" + validDate + "' is not a date", e);
        }
      } else {
        throw new NoDataException();
      }
    } else {
      throw new QueryParameterRequiredException("validDate is required");
    }
  }
```

and update our controller advice to return the 404 response when a `NoDataException` is raised.

```java
@ControllerAdvice(basePackageClasses = RootController.class)
public class RootControllerAdvice extends ResponseEntityExceptionHandler {
  @ExceptionHandler({InvalidQueryParameterException.class, QueryParameterRequiredException.class})
  @ResponseBody
  public ResponseEntity<String> handleControllerException(HttpServletRequest request, Throwable ex) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new ResponseEntity<>("{\"error\": \"" + ex.getMessage() + "\"}", headers, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NoDataException.class)
  @ResponseBody
  ResponseEntity handleNoDataException(HttpServletRequest request, Throwable ex) {
    return new ResponseEntity(HttpStatus.NOT_FOUND);
  }
}
```

Running the Gradle pact verification now passes.

# Step 13 - Using a Pact Broker

We've been publishing our pacts from the consumer project by coping the files over to the provider project, but we can
use a Pact Broker to do this instead.

### Running the Pact Broker locally or against a hosted broker (e.g. Pactflow)

If you'd like to play along locally we have a docker-compose example you can use. Start it by running:

```
docker-compose up
```

Afterwards, it should be running on port `9292`. Head to `http://localhost:9292` and you should see the OSS pact broker running.

The credentials are `pact_workshop` / `pact_workshop`.

The project properties (`gradle.properties`) defaults to using this with the relevant credentials. Update as required to your own hosted platform such as
or your pactflow.io account.

### Consumer

First, in the consumer project we need to add the Gradle Pact plugin and tell it about our broker.

```groovy
plugins {
  id "au.com.dius.pact" version "4.1.7"
}

... omitted ...

pact {
  publish {
    pactBrokerUrl = 'https://test.pact.dius.com.au'
    pactBrokerUsername = project.pactBrokerUser
    pactBrokerPassword = project.pactBrokerPassword
  }
}

```

Now, we can run `./gradlew consumer:pactPublish` after running the consumer tests to have the generated pact file 
published to the broker. Afterwards, you can navigate to the Pact Broker URL and see the published pact against
the consumer and provider names setup in our consumer test.

![Our Pact Is Published](diagrams/published.png)

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

```java
@RunWith(PactRunner.class)
@Provider("Our Provider")
@PactBroker(host = "test.pact.dius.com.au", protocol = "https", port = "443",
  authentication = @PactBrokerAuth(username = "${pactBrokerUser}", password = "${pactBrokerPassword}"))
public class PactVerificationTest {
  @ClassRule
  public static final DropwizardAppRule<ServiceConfig> RULE = new DropwizardAppRule<ServiceConfig>(MainApplication.class,
    ResourceHelpers.resourceFilePath("main-app-config.yaml"));

  @TestTarget
  public final Target target = new HttpTarget(8080);

  @State("data count > 0")
  public void dataCountGreaterThanZero() {
    DataStore.INSTANCE.setDataCount(1000);
  }

  @State("data count == 0")
  public void dataCountZero() {
    DataStore.INSTANCE.setDataCount(0);
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

Running either of the verification tests will now publish the result back to the broker. If you refresh the index page in the broker,
you will see the pacts marked as verified.

![Our Pact Is Verified](diagrams/verified.png)
