package au.com.dius.pactworkshop.dropwizardprovider;

public class QueryParameterRequiredException extends RuntimeException {
  public QueryParameterRequiredException(String message) {
    super(message);
  }
}
