package au.com.dius.pactworkshop.dropwizardprovider

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper

class NoDataExceptionMapper implements ExceptionMapper<NoDataException> {
  @Override
  Response toResponse(NoDataException exception) {
    Response.status(Response.Status.NOT_FOUND)
      .build()
  }
}
