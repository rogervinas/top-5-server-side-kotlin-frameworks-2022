# Top 5 Server-Side Frameworks for Kotlin in 2022

This is a demo inspired by [Anton Arhipov](https://twitter.com/antonarhipov)'s [Top 5 Server-Side Frameworks for Kotlin in 2022 @ Kotlin by JetBrains](https://www.youtube.com/watch?v=pYK5KkuZ3aU) where, spoiler alert, the author shows this top 5 list:

1. [Spring Boot](https://spring.io/projects/spring-boot)
2. [Quarkus](https://quarkus.io/)
3. [Micronaut](https://micronaut.io/)
4. [Ktor](https://ktor.io/docs/welcome.html)
5. [http4k](https://www.http4k.org/)

I have a lot of experience in Spring Boot, so I wanted to take a look at the other ones.

To do so we will create a simple application with each one of these frameworks, implementing the following features:

* Configuration and profiles
* REST endpoint
* Database with migrations
* Secrets from Vault
* Testing
* Build as a docker image

TODO: detailed diagram of the final solution and shared parts

## Spring Boot

Quick start with [Spring Initialzr](https://start.spring.io/#!type=gradle-project-kotlin&language=kotlin&platformVersion=2.7.6&packaging=jar&jvmVersion=17&groupId=org.rogervinas&artifactId=springboot-app&name=springboot-app&description=Demo%20project%20for%20Spring%20Boot&packageName=org.rogervinas.springboot-app&dependencies=webflux,cloud-starter-vault-config,jdbc)

Documentation at https://spring.io/projects/spring-boot

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

Then we can access the property as `greeting.secret` stored in vault.

### Database Configuration

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

### GreetingRepository

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

### GreetingController

```kotlin
@RestController
@RequestMapping("/hello")
class GreetingController(
  private val repository: GreetingRepository, 
  @Value("\${greeting.name}") private val name: String,
  @Value("\${greeting.secret}") private val secret: String
) {
  @GetMapping(produces = [MediaType.TEXT_PLAIN_VALUE])
  fun hello() = "${repository.getGreeting()} my name is $name and my secret is $secret"
}
```

* `@RestController` annotation will tell Spring Boot to create an instance on startup and wire it properly as a REST endpoint on `/hello` path, inspecting its annotated methods.
* `@GetMapping` will map `hello` function on `GET /hello`.
* The controller expects a GreetingRepository to be injected as well as two configuration properties, no matter what property source they come from (environment variables, system properties, configuration files, vault, ...)

### GreetingApplication

We need a main application to put it all together:
```kotlin
@SpringBootApplication
class GreetingApplication

fun main(args: Array<String>) { 
  runApplication<GreetingApplication>(*args)
}
```

By convention, all classes under the same package of the main application will be scanned for annotations.

### Testing the controller

We can test the controller with a "slice test", meaning only the parts needed by the controller will be started:
```kotlin
@WebFluxTest
@TestPropertySource(properties = [
  "greeting.secret=Apple"
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
      .expectBody<String>().isEqualTo("Hello my name is Bitelchus and my secret is Apple")
    }
}
```

* We use `WebTestClient` to execute requests to the controller.
* We mock the `GreetingRepository`
* We can use a `@TestPropertySource` to configure the `greeting.secret` property, as in this test we do not have vault.

### Testing the application

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
        it.responseBody!!.matches(Regex(".+ my name is Bitelchus and my secret is Watermelon"))
      }
  }
}
```

* We use the shared docker compose to start three containers.
* We use `WebTestClient` again to test the endpoint.
* As the greeting is random, now we have to match.
* As now we are using vault the secret is Watermelon.

### Test

```shell
./gradlew test
```

### Run

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

### Build a fatjar and run it

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

### Build a docker image and run it

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

## Quarkus

* Quick start https://quarkus.io/get-started/
* First app https://quarkus.io/guides/getting-started
* We can use quarkus commands or gradle
* Packaging https://quarkus.io/guides/building-native-image + https://quarkus.io/guides/building-native-image#testing-the-native-executable
* Built-in Quarkus features available only in IntelliJ IDEA Ultimate https://quarkus.io/guides/ide-tooling 

```shell
sdk install quarkus
quarkus create app org.rogervinas:quarkus-app --gradle-kotlin-dsl --extension='kotlin,resteasy-reactive-jackson'
```

Configuration:
```shell
quarkus extension add quarkus-config-yaml
quarkus extension add quarkus-reactive-pg-client
```

quarkus extension add agroal
quarkus extension add jdbc-postgresql

Add the agroal extension plus one of jdbc-db2, jdbc-derby, jdbc-h2, jdbc-mariadb, jdbc-mssql, jdbc-mysql, jdbc-oracle or jdbc-postgresql.


afegir flyway https://quarkus.io/guides/flyway


export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=mytoken

❯ vault kv put -mount=secret myapp mysecret=123456
== Secret Path ==
secret/data/hello

======= Metadata =======
Key                Value
---                -----
created_time       2022-11-09T20:27:15.700705694Z
custom_metadata    <nil>
deletion_time      n/a
destroyed          false
version            1

quarkus extension add vault

vault kv get secret/myapp

https://quarkus.io/guides/building-native-image
sdk install java 22.3.r19-grl
gu install native-image

quarkus build --native -Dquarkus.native.container-build=true

https://quarkus.io/guides/building-native-image#multistage-docker

java -jar build/quarkus-app/quarkus-run.jar


docker build -f src/main/docker/Dockerfile.native -t quarkus-app .
docker build -f src/main/docker/Dockerfile.jvm -t quarkus-app .

docker compose up -d
docker compose --profile quarkus up -d
docker compose down

docker-compose up -d
docker-compose --profile springboot up -d
docker-compose down

./gradlew bootBuildImage