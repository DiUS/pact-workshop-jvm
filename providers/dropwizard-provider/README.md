# Example Dropwizard Service Provider

## To Build It

To compile the service, run `./gradlew build`

This will build a JAR file and place it in your build directory (build/libs).

To run tests: `./gradlew test`

To run code quality checks: `./gradlew check` (this will also run tests).

To build an application bundle to run: `./gradlew installDist`

This will create an application bundle in `build/install/dropwizard-provider` and you can run the script in the bin directory.

## To Run It

You can run the service with `./gradlew :providers:dropwizard-provider:run`

To run it with other parameters (like `check`): `./gradlew :providers:dropwizard-provider:run -PappArgs=check`
