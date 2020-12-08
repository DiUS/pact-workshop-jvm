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
