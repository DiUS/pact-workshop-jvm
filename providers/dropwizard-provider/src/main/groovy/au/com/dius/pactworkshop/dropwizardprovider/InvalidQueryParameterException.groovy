package au.com.dius.pactworkshop.dropwizardprovider

class InvalidQueryParameterException extends RuntimeException {
  InvalidQueryParameterException(String message, Exception cause) {
    super(message, cause)
  }
}
