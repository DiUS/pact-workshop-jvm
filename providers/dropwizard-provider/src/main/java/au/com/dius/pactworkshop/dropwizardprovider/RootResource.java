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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/provider.json")
@Produces(MediaType.APPLICATION_JSON)
public class RootResource {

  @GET
  public Map<String, Serializable> providerJson(@QueryParam("validDate") Optional<String> validDate) {
    LocalDateTime validTime = LocalDateTime.parse(validDate.get());
    Map<String, Serializable> result = new HashMap<>(3);
    result.put("test", "NO");
    result.put("validDate", OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")));
    result.put("count", 1000);
    return result;
  }

}
