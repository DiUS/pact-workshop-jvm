package au.com.dius.pactworkshop.dropwizardprovider;

public class NoDataException extends RuntimeException {
  public NoDataException() {
    super("No Data");
  }
}
