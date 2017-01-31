package au.com.dius.pactworkshop.consumer

import groovyx.net.http.RESTClient
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ClientSpec extends Specification {

  private Client client
  private RESTClient mockHttp

  def setup() {
    mockHttp = Mock(RESTClient)
    client = new Client(http: mockHttp)
  }

  def 'can process the json payload from the provider'() {
    given:
    def json = [
      test: 'NO',
      validDate: '2013-08-16T15:31:20+10:00',
      count: 100
    ]

    when:
    def result = client.fetchAndProcessData(LocalDateTime.now())

    then:
    1 * mockHttp.get(_) >> [data: json, success: true]
    result == [1, OffsetDateTime.parse(json.validDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))]
  }

}
