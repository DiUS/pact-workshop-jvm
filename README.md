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


Running the client with either provider works nicely. For example, start the dropwizard-provider in one terminal:

```console
$ ./gradlew :providers:dropwizard-provider:run
```

Once the provider has successfully initialized, open another terminal session and run the consumer:

```console

$ ./gradlew :consumer:run
Starting a Gradle Daemon, 1 busy and 1 incompatible Daemons could not be reused, use --status for details

> Task :consumer:run
[test:NO, validDate:2018-04-05T16:27:43.243, count:1000]


BUILD SUCCESSFUL in 7s
2 actionable tasks: 1 executed, 1 up-to-date
```

Don't forget to stop the dropwizard-provider that is running in the first terminal when you have finished this step.
