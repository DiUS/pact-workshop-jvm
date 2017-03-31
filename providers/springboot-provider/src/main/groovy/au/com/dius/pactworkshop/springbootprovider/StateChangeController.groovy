package au.com.dius.pactworkshop.springbootprovider

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("test")
class StateChangeController {

  @RequestMapping(value = "/pactStateChange", method = RequestMethod.POST)
  void providerState(@RequestBody Map body) {
    switch (body.state) {
      case 'data count > 0':
        DataStore.instance.dataCount = 1000
        break
      case 'data count == 0':
        DataStore.instance.dataCount = 0
        break
    }
  }

}
