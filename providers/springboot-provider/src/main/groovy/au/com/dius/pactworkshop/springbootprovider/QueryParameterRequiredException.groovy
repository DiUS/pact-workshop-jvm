package au.com.dius.pactworkshop.springbootprovider

class QueryParameterRequiredException extends RuntimeException {
  QueryParameterRequiredException(String message) {
    super(message)
  }
}
