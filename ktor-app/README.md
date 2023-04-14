# Ktor

To begin with you can follow the [Creating Ktor applications](https://ktor.io/create/) guide.

To create a **Ktor** project we have three alternatives:
* Use [IntelliJ Idea plugin](https://ktor.io/docs/intellij-idea.html) 
* Use [start.ktor.io](https://start.ktor.io) web interface (similar to [Spring Initializr](https://start.spring.io/) for **Spring Boot**)
* Create a project [manually](https://ktor.io/docs/server-dependencies.html)

For example this project has been created using [start.ktor.io](https://start.ktor.io) and these options:
* Adjust project settings:
  * Build system = Gradle Kotlin
  * Engine = Netty
  * Configuration in = YAML file
  * Add sample code ✓
* Add plugins:
  * Postgres ✓

Once created you can run it to check everything is ok:
```shell
./gradlew run
```

And make a request to the sample endpoint:
```shell
curl http://localhost:8080
Hello World!
```

## Implementation

### YAML configuration

As we generated the project choosing "Configuration in YAML file" all is set, and we can add our custom property in `application.yaml`:
```yaml
greeting:
  name: "Bitelchus"
```

If we had chosen "Configuration in Code" we would need to make these changes:

1) Add `io.ktor:ktor-server-config-yaml` dependency.
2) Change Application's main method from:
  ```kotlin
  fun main() {
    embeddedServer(factory = Netty, port = 8080, host = "0.0.0.0", module = Application::module)
      .start(wait = true)
  }
  ```
  To:
  ```kotlin
  fun main() {
    EngineMain.main(args)
  }
  ```
3) Add Application's port and modules in `application.yaml`:
```yaml
ktor:
  deployment:
    port: 8080
  application:
    modules:
      - org.rogervinas.GreetingApplicationKt.module
```

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
* As **Ktor** does not offer any specific database support:
  * We just use plain `java.sql` code (instead of any database connection library)
  * We just create the `greetings` table if it does not exist (instead of any database migration library like [flyway](https://flywaydb.org/))
* Adding Postgres plugin when creating the project should add two dependencies:
  * `org.postgresql:postgresql` for production.
  * `com.h2database:h2` for testing, that we can remove it as we will use [Testcontainers](https://www.testcontainers.org/)

We create this function to create the repository:
```kotlin
private fun greetingRepository(config: ApplicationConfig): GreetingRepository {
  val host = config.property("database.host").getString()
  val port = config.property("database.port").getString()
  val name = config.property("database.name").getString()
  val username = config.property("database.username").getString()
  val password = config.property("database.password").getString()
  val connection = DriverManager.getConnection("jdbc:postgresql://$host:$port/$name", username, password)
  return GreetingJdbcRepository(connection)
}
```

And we add these properties in `application.yaml`:
```yaml
database:
  host: "$DB_HOST:localhost"
  port: 5432
  name: "mydb"
  username: "myuser"
  password: "mypassword"
```

Note that we allow to override `database.host` with the value of `DB_HOST` environment variable, or "localhost" if not set. This is only needed when running locally using docker compose.

### GreetingController

We create a `GreetingController` to serve the `/hello` endpoint:
```kotlin
fun Application.greetingController(
  name: String,
  secret: String,
  repository: GreetingRepository
) {
  routing {
    get("/hello") {
      call.respondText {
        "${repository.getGreeting()} my name is $name and my secret is $secret"
      }
    }
  }
}
```

We just name it `GreetingController` to follow the same convention as the other frameworks in this series, mainly **SpringBoot**.

Complete documentation at [Routing](https://ktor.io/docs/routing-in-ktor.html) guide.

### Vault configuration

**Ktor** does not support **Vault**, so we will simply use [BetterCloud/vault-java-driver](https://github.com/BetterCloud/vault-java-driver):
```kotlin
private fun ApplicationConfig.withVault(): ApplicationConfig {
  val vaultProtocol = this.property("vault.protocol").getString()
  val vaultHost = this.property("vault.host").getString()
  val vaultPort = this.property("vault.port").getString()
  val vaultToken = this.property("vault.token").getString()
  val vaultPath = this.property("vault.path").getString()
  val vaultConfig = VaultConfig()
    .address("$vaultProtocol://$vaultHost:$vaultPort")
    .token(vaultToken)
    .build()
  val vaultData = Vault(vaultConfig).logical().read(vaultPath).data
  return this.mergeWith(MapApplicationConfig(vaultData.entries.map { Pair(it.key, it.value) }))
}
```

With these properties in `application.yaml`:
```yaml
vault:
  protocol: "http"
  host: "$VAULT_HOST:localhost"
  port: 8200
  token: "mytoken"
  path: "secret/myapp"
```

Note that we allow to override `vault.host` with the value of `VAULT_HOST` environment variable, or "localhost" if not set. This is only needed when running locally using docker compose, same as with `database.host`.

As an alternative, we could also use [karlazzampersonal/ktor-vault](https://github.com/karlazzampersonal/ktor-vault) plugin. 

Next section will show how to use this `ApplicationConfig.withVault()` extension.

### Application

As defined in `application.yaml` the only module loaded will be `org.rogervinas.GreetingApplicationKt.module` so we need to implement it:
```kotlin
fun Application.module() {
  val environmentConfig = environment.config.withVault()
  val repository = greetingRepository(environmentConfig)
  greetingController(
    environmentConfig.property("greeting.name").getString(),
    environmentConfig.propertyOrNull("greeting.secret")?.getString() ?: "unknown",
    repository
  )
}
```
* It will merge default `ApplicationConfig` and override it with **Vault** values.
* It will create a `GreetingRepository` and a `GreetingController`.

## Testing the endpoint

We can test the endpoint this way:
```kotlin
class GreetingControllerTest {

  private val repository = mockk<GreetingRepository>().apply {
    every { getGreeting() } returns "Hello"
  }
  
  @Test
  fun `should say hello`() = testApplication {
    environment {
      config = MapApplicationConfig()
     }
     application {
       greetingController("Bitelchus", "apple", repository)
     }
     client.get("/hello").apply {
       assertThat(status).isEqualTo(OK)
       assertThat(bodyAsText()).isEqualTo("Hello my name is Bitelchus and my secret is apple")
     }
    }
}
```
* We use testApplication DSL with an empty configuration to just test the controller.
* We mock the repository with `mockk`.
* Complete documentation at [Testing](https://ktor.io/docs/testing.htm) guide. 

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
  }

  @Test
  fun `should say hello`() = testApplication {
    client.get("/hello").apply {
      assertThat(status).isEqualTo(OK)
      assertThat(bodyAsText()).matches(".+ my name is Bitelchus and my secret is watermelon")
    }
  }
}
```
* We use [Testcontainers](https://www.testcontainers.org/) to test with **Postgres** and **Vault** containers.
* We use pattern matching to check the greeting, as it is random.
* As this test uses **Vault**, the secret should be `watermelon`.
* Complete documentation at [Testing](https://ktor.io/docs/testing.htm) guide.

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

Note that main class is specified in `build.gradle.kts`:
```kotlin
application {
  mainClass.set("org.rogervinas.GreetingApplicationKt")
}
```

## Build a fatjar and run it

```shell
# Build fatjar
./gradlew buildFatJar

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
java -jar build/libs/ktor-app-all.jar

# Make requests
curl http://localhost:8080/hello

# Stop Application with control-c

# Stop all containers
docker compose down
```

More documentation at [Creating fat JARs](https://ktor.io/docs/fatjar.html) guide.

## Build a docker image and run it

```shell
# Build docker image and publish it to local registry
./gradlew publishImageToLocalRegistry

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
docker compose --profile ktor up -d

# Make requests
curl http://localhost:8080/hello

# Stop all containers
docker compose --profile ktor down
docker compose down
```

We can configure "Image name", "Image tag" and "JRE version" in `build.gradle.kts`:
```kotlin
ktor {
  docker {
    localImageName.set("${project.name}")
    imageTag.set("${project.version}")
    jreVersion.set(io.ktor.plugin.features.JreVersion.JRE_17)
  }
}
```

More documentation at [Docker](https://ktor.io/docs/docker.html) guide.

That's it! Happy coding! 💙
