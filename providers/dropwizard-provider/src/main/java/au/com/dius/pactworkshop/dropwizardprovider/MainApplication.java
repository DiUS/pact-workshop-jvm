package au.com.dius.pactworkshop.dropwizardprovider;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class MainApplication extends Application<ServiceConfig> {
  @Override
  public void run(ServiceConfig configuration, Environment environment) {
    environment.jersey().register(new InvalidQueryParameterExceptionMapper());
    environment.jersey().register(new QueryParameterRequiredExceptionMapper());
    environment.jersey().register(new NoDataExceptionMapper());
    environment.jersey().register(new RootResource());
  }

  public static void main(String[] args) throws Exception {
    new MainApplication().run(args);
  }
}
