package au.com.dius.pactworkshop.dropwizardprovider;

public class InvalidQueryParameterException extends RuntimeException {
  public InvalidQueryParameterException(String message, Exception cause) {
    super(message, cause);
  }
}
