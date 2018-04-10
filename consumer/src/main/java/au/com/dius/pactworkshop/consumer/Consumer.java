package au.com.dius.pactworkshop.consumer;

import com.mashape.unirest.http.exceptions.UnirestException;

public class Consumer {
  public static void main(String[] args) throws UnirestException {
    System.out.println(new Client().loadProviderJson());
  }
}
