# Spring Boot

Quick start with [Spring Initialzr](https://start.spring.io/#!type=gradle-project-kotlin&language=kotlin&platformVersion=2.7.6&packaging=jar&jvmVersion=17&groupId=org.rogervinas&artifactId=springboot-app&name=springboot-app&description=Demo%20project%20for%20Spring%20Boot&packageName=org.rogervinas.springboot-app&dependencies=webflux,cloud-starter-vault-config,jdbc)

Documentation at https://spring.io/projects/spring-boot

## YAML Configuration

By default Spring Boot template is created using an `application.properties` file, but we can rename it to `application.yaml` and it will work the same. We can put there our first configuration property:
```yaml
greeting:
  name: "Bitelchus"
```

More about profiles at https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config and https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles

## GreetingRepository

We just add the dependencies:
```
org.springframework.boot:spring-boot-starter-jdbc
org.postgresql:postgresql:42.5.1
org.flywaydb:flyway-core:9.8.3
```

and the following configuration in `application.yaml`:
```yaml
spring:
  datasource:
    url: "jdbc:postgresql://${DB_HOST:localhost}:5432/mydb"
    username: "myuser"
    password: "mypassword"
    driver-class-name: "org.postgresql.Driver"
  flyway:
    enabled: true
```

and the flyway migrations under `src/main/resources/db/migrations`.

Then we can implement `GreetingRepository` as:
```kotlin
@Repository
class GreetingRepository(private val jdbcTemplate: JdbcTemplate) {
  fun getGreeting() = jdbcTemplate
    .queryForObject("SELECT greeting FROM greetings ORDER BY random() limit 1", String::class.java)
}
```

* The `@Repository` annotation will tell Spring Boot to create an instance on startup.
* We inject a `JdbcTemplate` (provided by the spring-boot-starter-jdbc autoconfiguration) to execute queries to the database.
* We use `queryForObject` and that SQL to retrieve one random `greeting` from the `greetings` table.

## GreetingController

```kotlin
@RestController
@RequestMapping("/hello")
class GreetingController(
  private val repository: GreetingRepository, 
  @Value("\${greeting.name}") private val name: String,
  @Value("\${greeting.secret:unknown}") private val secret: String
) {
  @GetMapping(produces = [MediaType.TEXT_PLAIN_VALUE])
  fun hello() = "${repository.getGreeting()} my name is $name and my secret is $secret"
}
```

* `@RestController` annotation will tell Spring Boot to create an instance on startup and wire it properly as a REST endpoint on `/hello` path, inspecting its annotated methods.
* `@GetMapping` will map `hello` function on `GET /hello`.
* The controller expects a GreetingRepository to be injected as well as two configuration properties, no matter what property source they come from (environment variables, system properties, configuration files, vault, ...)
* We expect to get `greeting.secret` from vault, that is why we configure `unknown` as its default value, so it does not fail until we configure vault properly.

## GreetingApplication

We need to create a main application:
```kotlin
@SpringBootApplication
class GreetingApplication

fun main(args: Array<String>) { 
  runApplication<GreetingApplication>(*args)
}
```

By convention, all classes under the same package of the main application will be scanned for annotations.

## Vault Configuration

We just add the dependency `org.springframework.cloud:spring-cloud-starter-vault-config` and we add the following configuration in `application.yaml`:
```yaml
spring:
  cloud:
    vault:
      enabled: true
      uri: "http://${VAULT_HOST:localhost}:8200"
      authentication: "TOKEN"
      token: "mytoken"
      kv:
        enabled: true
        backend: "secret"
        default-context: "myapp"
        application-name: "myapp"
  config:
    import: optional:vault://
```

Then we can access the property as `greeting.secret` stored in vault.

## Testing the controller

We can test the controller with a "slice test", meaning only the parts needed by the controller will be started:
```kotlin
@WebFluxTest
@TestPropertySource(properties = [
  "greeting.secret=apple"
])
class GreetingControllerTest {

  @MockBean
  private lateinit var repository: GreetingRepository

  @Autowired
  private lateinit var client: WebTestClient

  @Test
  fun `should say hello`() {
    doReturn("Hello").`when`(repository).getGreeting()

    client
      .get().uri("/hello")
      .exchange()
      .expectStatus().isOk
      .expectBody<String>().isEqualTo("Hello my name is Bitelchus and my secret is apple")
    }
}
```

* We use `WebTestClient` to execute requests to the controller.
* We mock the `GreetingRepository`
* We can use a `@TestPropertySource` to configure the `greeting.secret` property, as in this test we do not have vault.

## Testing the application

To test the whole application we will use Testcontainers and the docker compose file.

```kotlin
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class GreetingApplicationTest {
    
  companion object {
    @Container
	private val container = DockerComposeContainer(File("../docker-compose.yaml"))
	  .withServices("db", "vault", "vault-cli")
	  .withLocalCompose(true)
	  .waitingFor("db", forLogMessage(".*database system is ready to accept connections.*", 1))
	  .waitingFor("vault", forLogMessage(".*Development mode.*", 1))
  }

  @Autowired
  private lateinit var client: WebTestClient

  @Test
  fun `should say hello`() { 
    client
      .get().uri("/hello")
	  .exchange()
      .expectStatus().isOk
	  .expectBody<String>().consumeWith {
        it.responseBody!!.matches(Regex(".+ my name is Bitelchus and my secret is watermelon"))
      }
  }
}
```

* We use the shared docker compose to start three containers.
* We use `WebTestClient` again to test the endpoint.
* As the greeting is random, now we have to match.
* As now we are using vault the secret is `watermelon`.

## Test

```shell
./gradlew test
```

## Run

```shell
# start vault and database
docker compose up -d vault vault-cli db

# start application
./gradlew bootRun

# make requests
curl http://localhost:8080/hello

# stop application with control-c

# stop vault and database
docker compose down
```

## Build a fatjar and run it

```shell
./gradlew bootJar

# start vault and database
docker compose up -d vault vault-cli db

# start application
java -jar build/libs/springboot-app-0.0.1-SNAPSHOT.jar

# make requests
curl http://localhost:8080/hello

# stop application with control-c

# stop vault and database
docker compose down
```

## Build a docker image and run it

```shell
./gradlew bootBuildImage

# start vault and database
docker compose up -d vault vault-cli db

# start application container
docker compose --profile springboot up -d

# make requests
curl http://localhost:8080/hello

# stop all containers
docker compose down
```