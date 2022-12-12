# Quarkus

To begin with you can follow the [Quarkus quick start](https://quarkus.io/get-started/), you will see that there is a `quarkus` command line (easily installable via [sdkman](https://sdkman.io/)) to create an app choosing gradle or maven as the build tool. Once the app is created we can use both quarkus command line or gradle/maven.

More detailed guide to create your first application at https://quarkus.io/guides/getting-started

More guides at https://quarkus.io/guides/

TODO note about configuration files and default profiles
TODO note about "dev services"

To create a simple application with a reactive REST endpoint:
```shell
sdk install quarkus
quarkus create app org.rogervinas:quarkus-app --gradle-kotlin-dsl --extension='kotlin,resteasy-reactive-jackson'
```

TODO explain generated files

A gradle project is created with the following classes:
* src/main/kotlin/org.rogervinas.GreetingResource: the controller
* src/test/kotlin/org.rogervinas.GreetingResourceTest: the application integration test
* src/native-test/kotlin/org.rogervinas.GreetingResourceIT: the application integration test but using the docker image

We will transform these classes to make it look like the Spring Boot ones.

Just run it once to check everything is ok:
```shell
quarkus dev
```

And just make a request to the endpoint:
```shell
curl http://localhost:8080/hello
Hello from RESTEasy Reactive
```

TODO cleanup
* remove src/main/resources/META-INF directory
* remove README.md

## YAML configuration

To use yaml configuration files we need to add this extension:
```shell
quarkus extension add quarkus-config-yaml
```

Rename src/main/resources/application.properties to application.yaml and add our first property:
```yaml
greeting:
  name: "Bitelchus"
```

[Default profiles](https://quarkus.io/guides/config-reference#default-profiles) in quarkus are:
* dev - Activated when in development mode (i.e. quarkus:dev)
* test - Activated when running tests
* prod - The default profile when not running in development or test mode

So we will create a `application-prod.yaml` to put there all the production configuration properties.

## GreetingController

We transform GreetingResource to GreetingController:
```kotlin
@Path("/hello")
class GreetingController(
  private val repository: GreetingRepository,
  @ConfigProperty(name = "greeting.name") private val name: String,
  @ConfigProperty(name = "greeting.secret") private val secret: String
) {
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  fun hello() = "${repository.getGreeting()} my name is $name and my secret is $secret"
}
```

* We can inject dependencies via constructor.
* We can inject configuration properties using `@ConfigProperty` annotation.
* Everything is pretty similar to Spring Boot. Note that it uses standard JAX-RS annotations which is also possible in Spring Boot (but not by default).

## Database configuration

We will use the Reactive SQL Client https://quarkus.io/guides/reactive-sql-clients
```shell
quarkus extension add quarkus-reactive-pg-client
```

And we configure it for dev and test in application.yaml:
```yaml
quarkus:
  datasource:
    devservices:
      image-name: "postgres:14.5"
```

And for production in application-prod.yaml:
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

## Flyway migrations

To enable flyway https://quarkus.io/guides/flyway, add these dependencies manually (no extension?)
```kotlin
implementation("io.quarkus:quarkus-flyway")
implementation("io.quarkus:quarkus-jdbc-postgresql")
```

quarkus extension add jdbc-postgresql ???

We cannot use the same reactive datasource, so we will have to configure the standard one:
```yaml
quarkus:
  flyway:
    migrate-at-start: true
```

```yaml
quarkus:
  datasource:
    db-kind: "postgresql"
    username: "myuser"
    password: "mypassword"
    jdbc:
      url: "jdbc:postgresql://${DB_HOST:localhost}:5432/mydb"
      max-size: 20
    reactive:
      url: "postgresql://${DB_HOST:localhost}:5432/mydb"
      max-size: 20
```

JDBC driver used by flyway, Reactive driver used by our `GreetingRepository`.

And we add the migrations under db/migrations.

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

* `@ApplicationScoped` annotation https://quarkus.io/guides/cdi
* We inject the reactive client.

## Vault configuration

Following this guide https://quarkiverse.github.io/quarkiverse-docs/quarkus-vault/dev/index.html

Add the extension:
```shell
quarkus extension add vault
```

We configure the dev service in application.yaml:
```yaml
quarkus:
  vault:
    secret-config-kv-path: "myapp"
    devservices:
      image-name: "vault:1.12.1"
      init-commands:
        - "kv put secret/myapp greeting.secret=watermelon"
```

And for production in application-prod.yaml:
```yaml
quarkus:
  vault:
    url: "http://${VAULT_HOST:localhost}:8200"
    authentication:
      client-token: "mytoken"
```

## Optional: native image

Packaging https://quarkus.io/guides/building-native-image + https://quarkus.io/guides/building-native-image#testing-the-native-executable

https://quarkus.io/guides/building-native-image
sdk install java 22.3.r19-grl
gu install native-image
quarkus build --native -Dquarkus.native.container-build=true
https://quarkus.io/guides/building-native-image#multistage-docker

docker build -f src/main/docker/Dockerfile.native -t quarkus-app . ?

## Testing the controller

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

* `@QuarkusTest` will start all devservices, that we are not using.
* `@InjectMock` to mock the repository.
* RestAssured to test the endpoint.

## Testing the application

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

### Test

```shell
./gradlew test
```

### Run in dev

```shell
# start application with dev services
quarkus dev

# make requests
curl http://localhost:8080/hello
```

### Build a fatjar and run it

```shell
quarkus build

# start vault and database
docker compose up -d vault vault-cli db

# start application
java -jar build/quarkus-app/quarkus-run.jar

# make requests
curl http://localhost:8080/hello

# stop application with control-c

# stop vault and database
docker compose down
```

### Build a docker image and run it

```shell
quarkus build
docker build -f src/main/docker/Dockerfile.jvm -t quarkus-app . 

# start vault and database
docker compose up -d vault vault-cli db

# start application container
docker compose --profile quarkus up -d

# make requests
curl http://localhost:8080/hello

# stop all containers
docker compose --profile quarkus down
docker compose down
```
