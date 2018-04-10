package au.com.dius.pactworkshop.springbootprovider;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RootController {

  @RequestMapping("/provider.json")
  public Map<String, Serializable> providerJson(@RequestParam(required = false) String validDate) {
    if (StringUtils.isNotEmpty(validDate)) {
      try {
        LocalDateTime validTime = LocalDateTime.parse(validDate);
        Map<String, Serializable> map = new HashMap<>(3);
        map.put("test", "NO");
        map.put("validDate", OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")));
        map.put("count", 1000);
        return map;
      } catch (DateTimeParseException e) {
        throw new InvalidQueryParameterException("'" + validDate + "' is not a date", e);
      }
    } else {
      throw new QueryParameterRequiredException("validDate is required");
    }
  }

}
