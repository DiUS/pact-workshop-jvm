package au.com.dius.pactworkshop.consumer

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.groovy.PactBuilder
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ClientPactSpec extends Specification {

  private Client client
  private LocalDateTime date
  private PactBuilder provider

  def setup() {
    client = new Client('http://localhost:1234')
    date = LocalDateTime.now()
    provider = new PactBuilder()
    provider {
      serviceConsumer 'Our Little Consumer'
      hasPactWith 'Our Provider'
      port 1234
    }
  }

  def 'Pact with our provider'() {
    given:
    def json = [
      test: 'NO',
      date: '2013-08-16T15:31:20+10:00',
      count: 100
    ]
    provider {
      serviceConsumer 'Our Little Consumer'
      hasPactWith 'Our Provider'
      port 1234

      given('data count > 0')
      uponReceiving('a request for json data')
      withAttributes(path: '/provider.json', query: [validDate: date.toString()])
      willRespondWith(status: 200)
      withBody {
        test 'NO'
        validDate timestamp("yyyy-MM-dd'T'HH:mm:ssXXX", json.date)
        count integer(json.count)
      }
    }

    when:
    def result
    PactVerificationResult pactResult = provider.runTest {
      result = client.fetchAndProcessData(date)
    }

    then:
    pactResult == PactVerificationResult.Ok.INSTANCE
    result == [1, OffsetDateTime.parse(json.date, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx"))]
  }

}
