package au.com.dius.pactworkshop.consumer;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

public class Client {

  private final String url;

  public Client(String url) {
    this.url = url;
  }

  private JsonNode loadProviderJson(LocalDateTime dateTime) throws UnirestException {
    return Unirest.get(url + "/provider.json")
      .queryString("validDate", dateTime.toString())
      .asJson().getBody();
  }

  public List<Object> fetchAndProcessData(LocalDateTime dateTime) throws UnirestException {
    JsonNode data = loadProviderJson(dateTime);
    System.out.println("data=" + data);

    JSONObject jsonObject = data.getObject();
    int value = 100 / jsonObject.getInt("count");
    ZonedDateTime date = ZonedDateTime.parse(jsonObject.getString("date"));

    System.out.println("value=" + value);
    System.out.println("date=" + date);
    return Arrays.asList(value, date);
  }
}
