package au.com.dius.pactworkshop.dropwizardprovider

import au.com.dius.pact.provider.junit.PactRunner
import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.State
import au.com.dius.pact.provider.junit.loader.PactBroker
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth
import au.com.dius.pact.provider.junit.loader.PactFolder
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.junit.target.TestTarget
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.junit.ClassRule
import org.junit.runner.RunWith

@RunWith(PactRunner)
@Provider('Our Provider')
@PactBroker(host = 'test.pact.dius.com.au', protocol = 'https', port = "443",
  authentication = @PactBrokerAuth(username = '${pactBrokerUser}', password = '${pactBrokerPassword}'))
class PactVerificationTest {

  @ClassRule
  public static final DropwizardAppRule<ServiceConfig> RULE = new DropwizardAppRule<ServiceConfig>(MainApplication,
    ResourceHelpers.resourceFilePath("main-app-config.yaml"))

  @TestTarget
  public final Target target = new HttpTarget(8080)

  @State("data count > 0")
  void dataCountGreaterThanZero() {
    DataStore.instance.dataCount = 1000
  }

  @State("data count == 0")
  void dataCountZero() {
    DataStore.instance.dataCount = 0
  }
}
