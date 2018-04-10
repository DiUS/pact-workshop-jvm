package au.com.dius.pactworkshop.springbootprovider;

public class InvalidQueryParameterException extends RuntimeException {
  public InvalidQueryParameterException(String message, Exception cause) {
    super(message, cause);
  }
}
