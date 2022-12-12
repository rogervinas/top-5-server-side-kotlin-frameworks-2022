# Quarkus

To begin with you can follow the [Quarkus quick start](https://quarkus.io/get-started/). You will see that there is a `quarkus` command line (easily installable via [sdkman](https://sdkman.io/)) to create an application choosing **Gradle** or **Maven** as the build tool. Once the application is created we can use both `quarkus` command line or `gradlew`/`mvn`.

You can also check [Creating your first application](https://quarkus.io/guides/getting-started) as well as [all the other guides](https://quarkus.io/guides/).

To create a simple application with a reactive REST endpoint:
```shell
sdk install quarkus
quarkus create app org.rogervinas:quarkus-app --gradle-kotlin-dsl --extension='kotlin,resteasy-reactive-jackson'
```

A **Gradle** project will be created with the following:
* **src/main/docker**: a few Dockerfiles with different options to create the application docker image.
* **src/main**: main sources with a template `GreetingResource` implementing a REST endpoint on `/hello`.
* **src/test**: test sources with an integration test of the endpoint.
* **src/native-test**: test sources with the same integration test of the endpoint but starting the application using the docker native image.

Just run it once to check everything is ok:
```shell
quarkus dev
```

And make a request to the endpoint:
```shell
curl http://localhost:8080/hello
Hello from RESTEasy Reactive
```

We can remove the `src/main/resources/META-INF` directory containing some HTML files that we will not need.

## Implementation

### YAML configuration

To use yaml configuration files we need to add this extension:
```shell
quarkus extension add quarkus-config-yaml
```

And then rename `application.properties` to `application.yaml` and add our first configuration property:
```yaml
greeting:
  name: "Bitelchus"
```

Note that in **Quarkus** we have these [Default profiles](https://quarkus.io/guides/config-reference#default-profiles):
* **dev** - Activated when in development mode (i.e. quarkus:dev).
* **test** - Activated when running tests.
* **prod** - The default profile when not running in development or test mode.

So we will create a `application-prod.yaml` file to put there all the production configuration properties.

More documentation about configuration sources at [Configuration Reference](https://quarkus.io/guides/config-reference) guide.

### GreetingRepository

We will create a `GreetingRepository`:
```kotlin
@ApplicationScoped
class GreetingRepository(private val client: PgPool) {
  fun getGreeting() = client
    .query("SELECT greeting FROM greetings ORDER BY random() limit 1")
    .executeAndAwait()
    .map { r -> r.get(String::class.java, "greeting") }
    .first()
}
```

* The `@ApplicationScoped` annotation will make **Quarkus** to create an instance at startup.
* We inject the [Reactive SQL Client](https://quarkus.io/guides/reactive-sql-clients).
* We use `query` and that SQL to retrieve one random `greeting` from the `greetings` table.

For this to work, we need some extra steps ...

Add the [Reactive SQL Client](https://quarkus.io/guides/reactive-sql-clients) extension:
```shell
quarkus extension add quarkus-reactive-pg-client
```

Configure it for `dev` and `test` profiles in `application.yaml`:
```yaml
quarkus:
  datasource:
    devservices:
      image-name: "postgres:14.5"
```

Configure it for `prod` profile in `application-prod.yaml`:
```yaml
quarkus:
  datasource:
    db-kind: "postgresql"
    username: "myuser"
    password: "mypassword"
    reactive:
      url: "postgresql://${DB_HOST:localhost}:5432/mydb"
      max-size: 20
```

Note that for `dev` and `test` profiles we just use something called "Dev Services", meaning it will automatically start containers and configure the application to use them. You can check the [Dev Services Overview](https://quarkus.io/guides/dev-services) and [Dev Services for Databases](https://quarkus.io/guides/databases-dev-services) documentation. 

To enable [Flyway](https://quarkus.io/guides/flyway) we need to add these dependencies manually (apparently there is no extension):
```kotlin
implementation("io.quarkus:quarkus-flyway")
implementation("io.quarkus:quarkus-jdbc-postgresql")
```

And, as it seems that we cannot use the same reactive datasource, we will have to configure the standard one:
* `application.yaml`
```yaml
quarkus:
  flyway:
    migrate-at-start: true
```
* `application-prod.yaml`
```yaml
quarkus:
  datasource:
    jdbc:
      url: "jdbc:postgresql://${DB_HOST:localhost}:5432/mydb"
      max-size: 20
```

So `quarkus.datasource.jdbc` will be used by **Flyway** and `quarkus.datasource.reactive` by the application.

Finally, [Flyway](https://flywaydb.org/) migrations under [src/main/resources/db/migration](src/main/resources/db/migration) to create and populate `greetings` table.

### GreetingController

We will rename the generated `GreetingResource` class to `GreetingController`, so it looks like the **Spring Boot** one:
```kotlin
@Path("/hello")
class GreetingController(
  private val repository: GreetingRepository,
  @ConfigProperty(name = "greeting.name") private val name: String,
  @ConfigProperty(name = "greeting.secret", defaultValue = "unknown") private val secret: String
) {
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  fun hello() = "${repository.getGreeting()} my name is $name and my secret is $secret"
}
```

* We can inject dependencies via constructor and configuration properties using `@ConfigProperty` annotation.
* We expect to get `greeting.secret` from **Vault**, that is why we configure `unknown` as its default value, so it does not fail until we configure **Vault** properly.
* Everything is pretty similar to **Spring Boot**. Note that it uses standard JAX-RS annotations which is also possible in **Spring Boot** (but not by default).

### Vault configuration

Following the [Using HashiCorp Vault](https://quarkiverse.github.io/quarkiverse-docs/quarkus-vault/dev/index.html) guide we add the extension:
```shell
quarkus extension add vault
```

For `dev` and `test` profiles we configure **Vault** "Dev Service" in `application.yaml`:
```yaml
quarkus:
  vault:
    secret-config-kv-path: "myapp"
    devservices:
      image-name: "vault:1.12.1"
      init-commands:
        - "kv put secret/myapp greeting.secret=watermelon"
```

For `prod` profile we configure **Vault** in `application-prod.yaml`:
```yaml
quarkus:
  vault:
    url: "http://${VAULT_HOST:localhost}:8200"
    authentication:
      client-token: "mytoken"
```

## Testing the endpoint

We can test the endpoint this way:
```kotlin
@QuarkusTest
@TestHTTPEndpoint(GreetingController::class)
class GreetingControllerTest {
    
  @InjectMock
  private lateinit var repository: GreetingRepository

  @Test
  fun `should say hello`() {
    doReturn("Hello").`when`(repository).getGreeting()

    `when`()
      .get()
      .then()
      .statusCode(200)
      .body(`is`("Hello my name is Bitelchus and my secret is watermelon"))
    }
}
```

* `@QuarkusTest` will start all "Dev Services", despite the database not being used 🤷
* We mock the repository with `@InjectMock`.
* We use [RestAssured](https://rest-assured.io/) to test the endpoint.
* As this test uses **Vault**, the secret should be `watermelon`. 

## Testing the application

We can test the whole application this way:
```kotlin
@QuarkusTest
class GreetingApplicationTest {
    
  @Test
  fun `should say hello`() {
    given()
      .`when`().get("/hello")
      .then()
      .statusCode(200)
      .body(matchesPattern(".+ my name is Bitelchus and my secret is watermelon"))
    }
}
```

* `@QuarkusTest` will start all "Dev Services", now all of them are being used.
* We use [RestAssured](https://rest-assured.io/) to test the endpoint.
* We use pattern matching to check the greeting, as it is random.
* As this test uses **Vault**, the secret should be `watermelon`.

## Test

```shell
./gradlew test
```

## Run

```shell
# Start Application with "Dev Services"
quarkus dev

# Make requests
curl http://localhost:8080/hello
```

## Build a fatjar and run it

```shell
# Build fatjar
quarkus build

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
java -jar build/quarkus-app/quarkus-run.jar

# Make requests
curl http://localhost:8080/hello

# Stop Application with control-c

# Stop all containers
docker compose down
```

### Build a docker image and run it

```shell
# Build docker image
quarkus build
docker build -f src/main/docker/Dockerfile.jvm -t quarkus-app . 

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
docker compose --profile quarkus up -d

# Make requests
curl http://localhost:8080/hello

# Stop all containers
docker compose --profile quarkus down
docker compose down
```

### Build a native executable and run it

You can follow [Build a Native Executable](https://quarkus.io/guides/building-native-image) for more details, but what worked for me:
```shell
# Install GraalVM via sdkman
sdk install java 22.3.r19-grl
sdk default java 22.3.r19-grl
export GRAALVM_HOME=$JAVA_HOME

# Install the native-image
gu install native-image

# Build native executable
quarkus build --native

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application using native executable
./build/quarkus-app-1.0.0-SNAPSHOT-runner

# Make requests
curl http://localhost:8080/hello

# Stop Application with control-c

# Stop all containers
docker compose down
```
