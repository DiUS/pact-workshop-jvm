package au.com.dius.pactworkshop.dropwizardprovider

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Path("/provider.json")
@Produces(MediaType.APPLICATION_JSON)
class RootResource {

  @GET
  Map providerJson(@QueryParam("validDate") Optional<String> validDate) {
    if (validDate.present) {
      try {
        def valid_time = LocalDateTime.parse(validDate.get())
        [
          test: 'NO',
          validDate: OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")),
          count: 1000
        ]
      } catch (e) {
        throw new InvalidQueryParameterException("'${validDate.get()}' is not a date", e)
      }
    } else {
      throw new QueryParameterRequiredException('validDate is required')
    }
  }

}
