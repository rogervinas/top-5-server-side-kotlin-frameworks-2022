# Http4k

To begin with you can follow the [Quickstart](https://www.http4k.org/quickstart/) and [How-to](https://www.http4k.org/guide/howto/) guides.

To create a **Http4k** project we have three alternatives:
* Use [IntelliJ Idea plugin](https://github.com/http4k/intellij-settings)
* Use [Project Wizard](https://toolbox.http4k.org/project) web interface (similar to [Spring Initializr](https://start.spring.io/) for **Spring Boot**)
* Use [Toolbox CLI](https://toolbox.http4k.org/)

For example this project has been created using [Project Wizard](https://toolbox.http4k.org/project) and these options:
* What kind of app are you writing? **Server**
* Do you need server-side WebSockets or SSE? **No**
* Select a server engine: **Undertow** (http4k team's default choice)
* Select HTTP client library: **OkHttp** (http4k team's go-to HTTP Client)
* Select JSON serialisation library: **Jackson** (http4k team's pick for JSON library, although we will not need it for this sample)
* Select a templating library: **None**
* Select any other messaging formats used by the app: **None** (not needed for this sample but good to know)
* Select any integrations that catch your eye! **None** (not needed for this sample but good to know)
* Select any testing technologies to battle harden the app: **None** (not needed for this sample but good to know)
* Application identity:
  * Main class name: **GreetingApplication**
  * Base package name: **org.rogervinas**
* Select a build tool: **Gradle**
* Select a packaging type: **ShadowJar**

Note some features missing with [Project Wizard](https://toolbox.http4k.org/project):
* It generates Gradle Groovy (we cannot choose Gradle Kotlin DSL 😭)
* It configures Java 11
* It does not generate a .gitignore file

Once created you can run it to check everything is ok:
```shell
./gradlew run
```

And make a request to the sample endpoints:
```shell
curl http://localhost:9000/ping
pong

curl http://localhost:9000/formats/json/jackson
{"subject":"Barry","message":"Hello there!"}
```

## Implementation

### YAML configuration

**Http4k** does not support loading configuration values from YAML 😨

For this sample we will just enable [Cloud Native Configuration](https://www.http4k.org/guide/reference/cloud_native/) and get configuration values from environment variables using the `Environment` object.

### GreetingRepository

We will create a `GreetingRepository`:
```kotlin
interface GreetingRepository {
  fun getGreeting(): String
}

class GreetingJdbcRepository(private val connection: Connection) : GreetingRepository {
    
  init {
    createGreetingsTable() 
  }

  override fun getGreeting(): String = connection
    .createStatement()
    .use { statement ->
      statement
        .executeQuery("""
          SELECT greeting FROM greetings
          ORDER BY random() LIMIT 1
          """.trimIndent()
        )
        .use { resultSet ->
          return if (resultSet.next()) {
            resultSet.getString("greeting")
          } else {
            throw Exception("No greetings found!")
          }
        }
  }

  private fun createGreetingsTable() {
   connection.createStatement().use {
     it.executeUpdate("""
       CREATE TABLE IF NOT EXISTS greetings (
         id serial,
         greeting varchar(100) NOT NULL,
         PRIMARY KEY (id)
       );
       INSERT INTO greetings (greeting) VALUES ('Hello');
       INSERT INTO greetings (greeting) VALUES ('Hola');
       INSERT INTO greetings (greeting) VALUES ('Hi');
       INSERT INTO greetings (greeting) VALUES ('Holi');
       INSERT INTO greetings (greeting) VALUES ('Bonjour');
       INSERT INTO greetings (greeting) VALUES ('Ni hao');
       INSERT INTO greetings (greeting) VALUES ('Bon dia');
       """.trimIndent()
     )
   }
  }
}
```
* As **Http4k** does not offer any specific database support:
  * We just use plain `java.sql` code (instead of any database connection library)
  * We just create the `greetings` table if it does not exist (instead of any database migration library like [flyway](https://flywaydb.org/))
* As we plan to use **Postgres** we need to add `org.postgresql:postgresql` dependency in `build.gradle`.

### GreetingController

We create a `GreetingController` to serve the `/hello` endpoint:
```kotlin
fun greetingController(
  name: String,
  secret: String,
  repository: GreetingRepository
) = routes(
  "/hello" bind GET to {
    Response(Status.OK)
      .body("${repository.getGreeting()} my name is $name and my secret is $secret")
  }
)
```

We just name it `GreetingController` to follow the same convention as the other frameworks in this series, mainly **SpringBoot**.

### Vault configuration

**Http4k** does not support **Vault**, so we will simply use [BetterCloud/vault-java-driver](https://github.com/BetterCloud/vault-java-driver):
```kotlin
private fun Environment.withVault(): Environment {
  val vaultProtocol = EnvironmentKey.string().defaulted(VAULT_PROTOCOL, "http")(this)
  val vaultHost = EnvironmentKey.string().defaulted(VAULT_HOST, "localhost")(this)
  val vaultPort = EnvironmentKey.int().defaulted(VAULT_PORT, 8200)(this)
  val vaultToken = EnvironmentKey.string().defaulted(VAULT_TOKEN, "mytoken")(this)
  val vaultPath = EnvironmentKey.string().defaulted(VAULT_PATH, "secret/myapp")(this)
  val vaultConfig = VaultConfig()
    .address("$vaultProtocol://$vaultHost:$vaultPort")
    .token(vaultToken)
    .build()
  val vaultData = Vault(vaultConfig).logical().read(vaultPath).data
  return MapEnvironment.from(vaultData.toProperties()).overrides(this)
}
```

We use `Environment` from [Cloud Native Configuration](https://www.http4k.org/guide/reference/cloud_native/) to retrieve environment variables and override them with all values loaded from **Vault**.

Next section will show how to use this `Environment.withVault()` extension.

### Application

There is no particular convention in **Http4k** so we will make our own:

We will create a `GreetingRepository`:
```kotlin
private fun greetingRepository(env: Environment): GreetingRepository {
  val host = EnvironmentKey.string().defaulted(DB_HOST, "localhost")(env)
  val port = EnvironmentKey.int().defaulted(DB_PORT, 5432)(env)
  val name = EnvironmentKey.string().defaulted(DB_NAME, "mydb")(env)
  val username = EnvironmentKey.string().defaulted(DB_USERNAME, "myuser")(env)
  val password = EnvironmentKey.string().defaulted(DB_PASSWORD, "mypassword")(env)
  val connection = DriverManager.getConnection("jdbc:postgresql://$host:$port/$name", username, password)
  return GreetingJdbcRepository(connection)
}
```

And a `HttpHandler` (our `GreetingController`):
```kotlin
private fun greetingController(env: Environment, repository: GreetingRepository): HttpHandler {
  val name = EnvironmentKey.string().defaulted(GREETING_NAME, "Bitelchus")(env)
  val secret = EnvironmentKey.string().defaulted(GREETING_SECRET, "unknown")(env)
  return greetingController(name, secret, repository)
}
```

And a `Http4kServer` (our `GreetingApplication`):
```kotlin
fun greetingApplication(env: Environment): Http4kServer {
  val envWithVault = env.withVault()
  val port = EnvironmentKey.int().defaulted(SERVER_PORT, 8080)(envWithVault)
  val repository = greetingRepository(envWithVault)
  val controller = greetingController(envWithVault, repository)
  return PrintRequest().then(controller).asServer(Undertow(port))
}
```

And finally we will start it in the `main` method:
```kotlin
fun main() {
  println("Application starting ...")
  val application = greetingApplication(ENV)
  application.start()
  println("Application started on " + application.port())
}
```

Note that in `greetingApplication` function we override the `ENV` object with all the values loaded from **Vault**.

## Testing the endpoint

We can test the endpoint this way:
```kotlin
class GreetingControllerTest {

  private val repository = mockk<GreetingRepository>().apply {
    every { getGreeting() } returns "Hello"
  }

  private val controller = greetingController("Bitelchus", "apple", repository)

  @Test
  fun `should say hello`() {
    val response = controller(Request(GET, "/hello"))
    assertThat(response, hasStatus(Status.OK))
    assertThat(response, hasBody("Hello my name is Bitelchus and my secret is apple"))
  }
}
```
* We mock the repository with `mockk`.
* More documentation at [TDDing http4k](https://www.http4k.org/guide/tutorials/tdding_http4k/) and [Different test-types](https://www.http4k.org/guide/howto/write_different_test_types/) guides.

## Testing the application

We can test the whole application this way:
```kotlin
@Testcontainers
class GreetingApplicationTest {

  companion object {
    @Container
    private val container = DockerComposeContainer(File("../docker-compose.yaml"))
      .withServices("db", "vault", "vault-cli")
      .withLocalCompose(true)
      .waitingFor("db", forLogMessage(".*database system is ready to accept connections.*", 1))
      .waitingFor("vault", forLogMessage(".*Development mode.*", 1))
      .waitingFor("vault-cli", forLogMessage(".*created_time.*", 1))
  }

  private val client = OkHttp()
  private val application = greetingApplication(
    MapEnvironment.from(
      Properties().apply {
        this[SERVER_PORT] = 0
      }
    )
  )

  @BeforeEach
  fun start() {
    application.start()
  }

  @AfterEach
  fun stop() {
    application.stop()
  }

  @Test
  fun `should say hello`() {
    val response = client(Request(GET, "http://localhost:${application.port()}/hello"))
    assertThat(response, hasStatus(OK))
    assertThat(response, hasBody(Regex(".+ my name is Bitelchus and my secret is watermelon")))
  }
}
```
* We use [Testcontainers](https://www.testcontainers.org/) to test with **Postgres** and **Vault** containers.
* We can override configuration values passing a `MapEnvironment` to the `greetingApplication`. In this case we only override `SERVER_PORT` value to `0` to force a random port.
* We use pattern matching to check the greeting, as it is random.
* As this test uses **Vault**, the secret should be `watermelon`.

## Test

```shell
./gradlew test
```

## Run

```shell
# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
./gradlew run

# Make requests
curl http://localhost:8080/hello

# Stop Application with control-c

# Stop all containers
docker compose down
```

Note that main class is specified in `build.gradle`:
```groovy
mainClassName = "org.rogervinas.GreetingApplicationKt"
```

## Build a fatjar and run it

```shell
# Build fatjar
./gradlew shadowJar

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
java -jar build/libs/http4k-app.jar

# Make requests
curl http://localhost:8080/hello

# Stop Application with control-c

# Stop all containers
docker compose down
```

## Build a docker image and run it

**Http4k** does not support creating docker images out of the box, but for example we can create this `Dockerfile`:
```dockerfile
FROM registry.access.redhat.com/ubi8/openjdk-11:1.15

COPY build/libs/http4k-app.jar /app/

EXPOSE 8080

ENV JAVA_APP_JAR="/app/http4k-app.jar"
```

And then:
```shell
# Build fatjar
./gradlew shadowJar

# Build docker image and publish it to local registry
docker build . -t http4k-app

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
docker compose --profile http4k up -d

# Make requests
curl http://localhost:8080/hello

# Stop all containers
docker compose --profile http4k down
docker compose down
```

That's it! Happy coding! 💙
