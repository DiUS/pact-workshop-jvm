package au.com.dius.pactworkshop.dropwizardprovider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class InvalidQueryParameterExceptionMapper implements ExceptionMapper<InvalidQueryParameterException> {
  @Override
  public Response toResponse(InvalidQueryParameterException exception) {
    return Response.status(Response.Status.BAD_REQUEST)
      .type(MediaType.APPLICATION_JSON_TYPE)
      .entity("{\"error\": \"" + exception.getMessage() + "\"}")
      .build();
  }

}
