package au.com.dius.pactworkshop.springbootprovider

import groovy.json.JsonOutput
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

import javax.servlet.http.HttpServletRequest

@ControllerAdvice(basePackageClasses = RootController)
class RootControllerAdvice extends ResponseEntityExceptionHandler {

  @ExceptionHandler([InvalidQueryParameterException, QueryParameterRequiredException])
  @ResponseBody
  ResponseEntity handleControllerException(HttpServletRequest request, Throwable ex) {
    new ResponseEntity(JsonOutput.toJson(ex.message), HttpStatus.BAD_REQUEST)
  }

  @ExceptionHandler(NoDataException)
  @ResponseBody
  ResponseEntity handleNoDataException(HttpServletRequest request, Throwable ex) {
    new ResponseEntity(HttpStatus.NOT_FOUND)
  }

}
