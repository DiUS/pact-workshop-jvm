package au.com.dius.pactworkshop.consumer;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Client {

  private final String url;

  public Client(String url) {
    this.url = url;
  }

  private Optional<JsonNode> loadProviderJson(String dateTime) throws UnirestException {
    HttpRequest getRequest = Unirest.get(url + "/provider.json");

    if (StringUtils.isNotEmpty(dateTime)) {
      getRequest = getRequest.queryString("validDate", dateTime);
    }

    HttpResponse<JsonNode> jsonNodeHttpResponse = getRequest.asJson();
    if (jsonNodeHttpResponse.getStatus() == 200) {
      return Optional.of(jsonNodeHttpResponse.getBody());
    } else {
      return Optional.empty();
    }
  }

  public List<Object> fetchAndProcessData(String dateTime) throws UnirestException {
    Optional<JsonNode> data = loadProviderJson(dateTime);
    System.out.println("data=" + data);

    if (data != null && data.isPresent()) {
      JSONObject jsonObject = data.get().getObject();
      int value = 100 / jsonObject.getInt("count");
      TemporalAccessor date =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")
            .parse(jsonObject.getString("validDate"));

      System.out.println("value=" + value);
      System.out.println("date=" + date);
      return Arrays.asList(value, OffsetDateTime.from(date));
    } else {
      return Arrays.asList(0, null);
    }
  }
}
