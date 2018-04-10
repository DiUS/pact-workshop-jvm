# Example JVM project for the Pact workshop

This project has 3 components, a consumer project and two service providers, one Dropwizard and one
Springboot service that the consumer will interaction with.

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
