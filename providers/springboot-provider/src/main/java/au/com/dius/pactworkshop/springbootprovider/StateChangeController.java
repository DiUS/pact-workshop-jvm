package au.com.dius.pactworkshop.springbootprovider;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@Profile("test")
public class StateChangeController {
  @RequestMapping(value = "/pactStateChange", method = RequestMethod.POST)
  public Map providerState(@RequestBody Map body) {
    if (body.get("state").equals("data count > 0")) {
      DataStore.INSTANCE.setDataCount(1000);
    } else if (body.get("state").equals("data count == 0")) {
      DataStore.INSTANCE.setDataCount(0);
    }
    return Collections.emptyMap();
  }
}
