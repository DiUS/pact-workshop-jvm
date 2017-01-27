package au.com.dius.pactworkshop.consumer

import groovyx.net.http.RESTClient

import java.time.LocalDateTime
import java.time.ZonedDateTime

class Client {
  private http

  Client() {

  }

  Client(String url) {
    http = new RESTClient(url)
  }

  def loadProviderJson(LocalDateTime dateTime) {
    def response = http.get(path: '/provider.json', query: [validDate: dateTime.toString()])
    if (response.success) {
      response.data
    }
  }

  def fetchAndProcessData(LocalDateTime dateTime) {
    def data = loadProviderJson(dateTime)
    println "data=$data"
    def value = 100 / data.count
    def date = ZonedDateTime.parse(data.date)
    println "value=$value"
    println "date=$date"
    [value, date]
  }
}
