package au.com.dius.pactworkshop.dropwizardprovider

import io.dropwizard.Application
import io.dropwizard.setup.Environment

class MainApplication extends Application<ServiceConfig> {
  @Override
  void run(ServiceConfig configuration, Environment environment) {
    environment.jersey().register(new RootResource())
  }

  static void main(String[] args) {
    new MainApplication().run(args)
  }
}
