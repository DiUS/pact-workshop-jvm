package au.com.dius.pactworkshop.consumer

import groovyx.net.http.RESTClient

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class Client {
  private http

  Client() {

  }

  Client(String url) {
    http = new RESTClient(url)
    http.handler.failure = { resp, data ->
      resp
    }
  }

  def loadProviderJson(String dateTime) {
    def query = [:]
    if (dateTime) {
      query.validDate = dateTime
    }
    def response = http.get(path: '/provider.json', query: query)
    if (response.success) {
      response.data
    }
  }

  def fetchAndProcessData(String dateTime) {
    def data = loadProviderJson(dateTime)
    println "data=$data"
    if (data) {
      def value = 100 / data.count
      def date = OffsetDateTime.parse(data.validDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))
      println "value=$value"
      println "date=$date"
      [value, date]
    } else [
      [0, null]
    ]
  }
}
