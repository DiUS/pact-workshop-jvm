package au.com.dius.pactworkshop.springbootprovider;

public class NoDataException extends RuntimeException {
  public NoDataException() {
    super("No Data");
  }
}
