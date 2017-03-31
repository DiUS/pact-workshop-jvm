package au.com.dius.pactworkshop.dropwizardprovider

import groovy.json.JsonOutput

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper

class InvalidQueryParameterExceptionMapper implements ExceptionMapper<InvalidQueryParameterException> {
  @Override
  Response toResponse(InvalidQueryParameterException exception) {
    Response.status(Response.Status.BAD_REQUEST)
      .type(MediaType.APPLICATION_JSON_TYPE)
      .entity(JsonOutput.toJson(exception.message))
      .build()
  }
}
