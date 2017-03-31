package au.com.dius.pactworkshop.springbootprovider

class NoDataException extends RuntimeException {
  NoDataException() {
    super('No Data')
  }
}
