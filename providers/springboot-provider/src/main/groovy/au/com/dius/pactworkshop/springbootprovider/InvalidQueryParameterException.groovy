package au.com.dius.pactworkshop.springbootprovider

class InvalidQueryParameterException extends RuntimeException {
  InvalidQueryParameterException(String message, Exception cause) {
    super(message, cause)
  }
}
