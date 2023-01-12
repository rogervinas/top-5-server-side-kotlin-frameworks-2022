# Micronaut

To begin with you can follow the [Creating your first Micronaut application](https://guides.micronaut.io/latest/creating-your-first-micronaut-app.html) guide. 

You will see that there are two alternatives:
* Use [Micronaut Command Line Interface](https://docs.micronaut.io/latest/guide/#cli) command line (easily installable via [sdkman](https://sdkman.io/))
* Use [Micronaut Launch](https://launch.micronaut.io/) web interface (similar to [Spring Initializr](https://start.spring.io/) for Spring Boot)

You can also check [all the other guides](https://guides.micronaut.io/latest/index.html) as well as [the user documentation](https://docs.micronaut.io/latest/guide/).

To create our sample application using the command line:
```shell
sdk install micronaut
mn create-app com.rogervinas \
   --features=data-jdbc,postgres,flyway \
   --build=gradle_kotlin --lang=kotlin --java-version=11 --test=junit
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
interface GreetingRepository {
  fun getGreeting(): String
}

@ApplicationScoped
class GreetingJdbcRepository(private val client: PgPool): GreetingRepository {
  override fun getGreeting(): String = client
    .query("SELECT greeting FROM greetings ORDER BY random() LIMIT 1")
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

Note that here we can use these `init-commands` to populate **Vault** 🥹

For `prod` profile we configure **Vault** in `application-prod.yaml`:
```yaml
quarkus:
  vault:
    url: "http://${VAULT_HOST:localhost}:8200"
    authentication:
      client-token: "mytoken"
```

## Testing the endpoint

We rename the original `GreetingResourceTest` to `GreetingControllerTest` and we modify it this way:
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

## Build a docker image and run it

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

## Build a native executable and run it

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

That's it! Happy coding! 💙









## Micronaut 3.7.4 Documentation

- [User Guide](https://docs.micronaut.io/3.7.4/guide/index.html)
- [API Reference](https://docs.micronaut.io/3.7.4/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/3.7.4/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Shadow Gradle Plugin](https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow)
## Feature http-client documentation

- [Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#httpClient)

sdk install micronaut

## STEPS

https://guides.micronaut.io/latest/creating-your-first-micronaut-app-gradle-kotlin.html
+ native

https://micronaut.io/launch

https://micronaut-projects.github.io/micronaut-test/latest/guide/#junit5

IoC https://docs.micronaut.io/latest/guide/#ioc

Configuration https://guides.micronaut.io/latest/micronaut-configuration-gradle-kotlin.html

Spring to Micronaut https://guides.micronaut.io/latest/spring-boot-to-micronaut-application-class-gradle-kotlin.html

https://guides.micronaut.io/latest/micronaut-data-jdbc-repository-gradle-kotlin.html

https://guides.micronaut.io/latest/micronaut-flyway-gradle-kotlin.html

Add features data-jdbc, flyway, postgres 
```
mn create-app com.rogervinas \
   --features=data-jdbc,postgres,flyway \
   --build=gradle_kotlin --lang=kotlin --java-version=11 --test=junit
```


https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/

https://docs.micronaut.io/latest/guide/#config

./gradlew dockerBuild

https://micronaut-projects.github.io/micronaut-gradle-plugin/snapshot/#_docker_support

## Run

```shell
# Start Application
./gradlew run

# Make requests
curl http://localhost:8080/hello

# Stop Application with control-c

# Stop all containers
docker compose down
```

## Build a fatjar and run it

```shell
# Build fatjar
./gradlew jar

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
java -jar build/libs/springboot-app-0.0.1-SNAPSHOT.jar

# Make requests
curl http://localhost:8080/hello

# Stop Application with control-c

# Stop all containers
docker compose down
```

## Build a docker image and run it

```shell
# Build docker image
./gradlew dockerBuild

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
docker compose --profile micronaut up -d

# Make requests
curl http://localhost:8080/hello

# Stop all containers
docker compose --profile micronaut down
docker compose down
```

https://micronaut-projects.github.io/micronaut-sql/latest/guide/#jasync

https://docs.micronaut.io/latest/guide/#distributedConfigurationVault

https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/#modules-hashicorp-vault

https://docs.micronaut.io/latest/guide/#bootstrap

