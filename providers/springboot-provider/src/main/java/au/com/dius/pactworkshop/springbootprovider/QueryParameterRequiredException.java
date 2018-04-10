package au.com.dius.pactworkshop.springbootprovider;

public class QueryParameterRequiredException extends RuntimeException {
  public QueryParameterRequiredException(String message) {
    super(message);
  }
}
