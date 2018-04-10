package au.com.dius.pactworkshop.consumer;

import com.mashape.unirest.http.exceptions.UnirestException;

import java.time.LocalDateTime;

public class Consumer {
  public static void main(String[] args) throws UnirestException {
    System.out.println(new Client("http://localhost:8080")
      .fetchAndProcessData(args.length> 0 ? args[0] : LocalDateTime.now().toString()));
  }
}
