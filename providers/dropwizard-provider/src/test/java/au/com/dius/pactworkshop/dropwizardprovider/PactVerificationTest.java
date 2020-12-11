package au.com.dius.pactworkshop.dropwizardprovider;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

@RunWith(PactRunner.class)
@Provider("Our Provider")
@PactBroker(host = "${pactBrokerHost}", scheme = "${pactBrokerScheme}", port = "${pactBrokerPort}",
  authentication = @PactBrokerAuth(username = "${pactBrokerUser}", password = "${pactBrokerPassword}"))
public class PactVerificationTest {
  @ClassRule
  public static final DropwizardAppRule<ServiceConfig> RULE = new DropwizardAppRule<ServiceConfig>(MainApplication.class,
    ResourceHelpers.resourceFilePath("main-app-config.yaml"));

  @TestTarget
  public final Target target = new HttpTarget(8080);

  @State("data count > 0")
  public void dataCountGreaterThanZero() {
    DataStore.INSTANCE.setDataCount(1000);
  }

  @State("data count == 0")
  public void dataCountZero() {
    DataStore.INSTANCE.setDataCount(0);
  }
}
