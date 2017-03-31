package au.com.dius.pactworkshop.dropwizardprovider

class NoDataException extends RuntimeException {
  NoDataException() {
    super('No Data')
  }
}
