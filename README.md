## Introduction

This workshop is aimed at demonstrating core features and benefits of contract testing with Pact.

Whilst contract testing can be applied retrospectively to systems, we will follow the [consumer driven contracts](https://martinfowler.com/articles/consumerDrivenContracts.html) approach in this workshop - where a new consumer and provider are created in parallel to evolve a service over time, especially where there is some uncertainty with what is to be built.

This workshop should take from 1 to 2 hours, depending on how deep you want to go into each topic.

**Example project overview**

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

[Java](https://java.com/en/download/) (version 1.8+)
[Docker Compose](https://docs.docker.com/compose/install/)

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
