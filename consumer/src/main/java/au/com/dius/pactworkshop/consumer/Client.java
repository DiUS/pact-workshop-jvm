package au.com.dius.pactworkshop.consumer;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.time.LocalDateTime;

public class Client {
  public JsonNode loadProviderJson() throws UnirestException {
    return Unirest.get("http://localhost:8080/provider.json")
      .queryString("validDate", LocalDateTime.now().toString())
      .asJson().getBody();
  }
}
