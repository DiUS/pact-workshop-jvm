package au.com.dius.pactworkshop.consumer

import groovyx.net.http.RESTClient

import java.time.LocalDateTime

class Client {

  def loadProviderJson() {
    def http = new RESTClient('http://localhost:8080')
    def response = http.get(path: '/provider.json', query: [validDate: LocalDateTime.now().toString()])
    if (response.success) {
      response.data
    }
  }
}
