# Spring Boot

We can create a project using [Spring Initialzr](https://start.spring.io/#!type=gradle-project-kotlin&language=kotlin&platformVersion=3.0.1&packaging=jar&jvmVersion=17&groupId=org.rogervinas&artifactId=springboot-app&name=springboot-app&description=Demo%20project%20for%20Spring%20Boot&packageName=org.rogervinas.springboot-app&dependencies=webflux,cloud-starter-vault-config,jdbc) and download it locally.

A lot of documentation guides at [spring.io/spring-boot](https://spring.io/projects/spring-boot).

## Implementation

### YAML Configuration

By default, **Spring Initialzr** creates a template using `application.properties` file. We can just rename it to `application.yaml` and it will work the same.

We can put there our first configuration property:
```yaml
greeting:
  name: "Bitelchus"
```

More documentation about configuration sources at [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config) and [Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles).

### GreetingRepository

We will create a `GreetingRepository`:
```kotlin
interface GreetingRepository {
  fun getGreeting(): String
}

@Repository
class GreetingJdbcRepository(private val jdbcTemplate: JdbcTemplate): GreetingRepository {
  fun getGreeting(): String = jdbcTemplate
    .queryForObject("SELECT greeting FROM greetings ORDER BY random() LIMIT 1", String::class.java)!!
}
```

* The `@Repository` annotation will make **Spring Boot** to create a singleton instance at startup.
* We inject a `JdbcTemplate` (provided by the `spring-boot-starter-jdbc` autoconfiguration) to execute queries to the database.
* We use `queryForObject` and that SQL to retrieve one random `greeting` from the `greetings` table.

Additional to `spring-boot-starter-jdbc` we will need to add these extra dependencies:
```kotlin
implementation("org.postgresql:postgresql")
implementation("org.flywaydb:flyway-core")
```

And the following configuration in `application.yaml`:
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

And [Flyway](https://flywaydb.org/) migrations under [src/main/resources/db/migration](src/main/resources/db/migration) to create and populate `greetings` table.

### GreetingController

We will create a `GreetingController` serving `/hello` endpoint:
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

* `@RestController` annotation will make Spring Boot to create an instance on startup and wire it properly as a REST endpoint on `/hello` path, scanning its annotated methods.
* `@GetMapping` will map `hello` function answering to `GET /hello` requests.
* The controller expects a `GreetingRepository` to be injected as well as two configuration properties, no matter what property source they come from (environment variables, system properties, configuration files, **Vault**, ...).
* We expect to get `greeting.secret` from **Vault**, that is why we configure `unknown` as its default value, so it does not fail until we configure **Vault** properly.

### GreetingApplication

As a **Spring Boot** requirement, we need to create a main application:
```kotlin
@SpringBootApplication
class GreetingApplication

fun main(args: Array<String>) { 
  runApplication<GreetingApplication>(*args)
}
```

By convention, all classes under the same package of the main application will be scanned for annotations.

### Vault Configuration

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

Then we can access the configuration property `greeting.secret` stored in **Vault**.

You can check the documentation at [Spring Vault](https://spring.io/projects/spring-vault).

### Testing the endpoint

We can test the endpoint with a "slice test", meaning only the parts needed by the controller will be started:
```kotlin
@WebFluxTest
@TestPropertySource(properties = [
  "spring.cloud.vault.enabled=false",
  "greeting.secret=apple"
])
class GreetingControllerTest {

  @MockitoBean
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

* We use `WebTestClient` to execute requests to the endpoint.
* We mock the repository with `@MockitoBean`.
* We can use a `@TestPropertySource` to configure the `greeting.secret` property and `spring.cloud.vault.enabled=false` to disable **Vault**.

### Testing the application

To test the whole application we will use [Testcontainers](https://www.testcontainers.org/) and the docker compose file:
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
        assertThat(it.responseBody!!).matches(".+ my name is Bitelchus and my secret is watermelon")
      }
  }
}
```

* We use the shared docker compose to start the required three containers.
* We use `WebTestClient` again to test the endpoint.
* We use pattern matching to check the greeting, as it is random.
* As **Vault** is now enabled, the secret should be `watermelon`.

## Test

```shell
./gradlew test
```

## Run

```shell
# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
./gradlew bootRun

# Make requests
curl http://localhost:8080/hello

# Stop Application with control-c

# Stop all containers
docker compose down
```

## Build a fatjar and run it

```shell
# Build fatjar
./gradlew bootJar

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
./gradlew bootBuildImage

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
docker compose --profile springboot up -d

# Make requests
curl http://localhost:8080/hello

# Stop all containers
docker compose --profile springboot down
docker compose down
```

That's it! Happy coding! 💙
