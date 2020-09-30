package au.com.dius.pactworkshop.consumer;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ClientPactTest {

  // This sets up a mock server that pretends to be our provider
  @Rule
  public PactProviderRule provider = new PactProviderRule("Our Provider", this);

  private LocalDateTime dateTime;
  private OffsetDateTime dateResult;

  // This defines the expected interaction for out test
  @Pact(provider = "Our Provider", consumer = "Our Little Consumer")
  public RequestResponsePact pact(PactDslWithProvider builder) {
    dateTime = LocalDateTime.now();
    dateResult = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
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
              .datetime("validDate", "yyyy-MM-dd'T'HH:mm:ssXX", dateResult.toInstant())
              .integerType("count", 100)
      )
      .toPact();
  }

  @Test
  @PactVerification(value = "Our Provider", fragment = "pact")
  public void pactWithOurProvider() throws UnirestException {
    // Set up our HTTP client class
    Client client = new Client(provider.getUrl());

    // Invoke out client
    List<Object> result = client.fetchAndProcessData(dateTime.toString());

    assertThat(result, hasSize(2));
    assertThat(result.get(0), is(1));
    assertThat(result.get(1), is(dateResult));
  }

  @Pact(provider = "Our Provider", consumer = "Our Little Consumer")
  public RequestResponsePact pactForMissingDateParameter(PactDslWithProvider builder) {
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
}
