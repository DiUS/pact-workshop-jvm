package au.com.dius.pactworkshop.dropwizardprovider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/provider.json")
@Produces(MediaType.APPLICATION_JSON)
public class RootResource {

  @GET
  public Map<String, Serializable> providerJson(@QueryParam("validDate") Optional<String> validDate) {
    if (validDate.isPresent()) {
      if (DataStore.INSTANCE.getDataCount() > 0) {
        try {
          LocalDateTime validTime = LocalDateTime.parse(validDate.get());
          Map<String, Serializable> result = new HashMap<>(3);
          result.put("test", "NO");
          result.put("validDate", OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")));
          result.put("count", DataStore.INSTANCE.getDataCount());
          return result;
        } catch (DateTimeParseException e) {
          throw new InvalidQueryParameterException("'" + validDate.get() + "' is not a date", e);
        }
      } else {
        throw new NoDataException();
      }
    } else {
      throw new QueryParameterRequiredException("validDate is required");
    }
  }
}
