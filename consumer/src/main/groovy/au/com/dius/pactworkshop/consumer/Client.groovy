package au.com.dius.pactworkshop.consumer

import groovyx.net.http.RESTClient

import java.time.LocalDateTime
import java.time.ZonedDateTime

class Client {
  private http

  Client() {
    http = new RESTClient('http://localhost:8080')
  }

  def loadProviderJson() {
    def response = http.get(path: '/provider.json', query: [validDate: LocalDateTime.now().toString()])
    if (response.success) {
      response.data
    }
  }

  def fetchAndProcessData() {
    def data = loadProviderJson()
    println "data=$data"
    def value = 100 / data.count
    def date = ZonedDateTime.parse(data.date)
    println "value=$value"
    println "date=$date"
    [value, date]
  }
}
