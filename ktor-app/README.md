# Ktor

To begin with you can follow the [Creating Ktor applications](https://ktor.io/create/) guide.

To create a Ktor project we have three alternatives:
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
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = GreetingApplication::module)
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

  override fun getGreeting(): String = connection.createStatement().use { statement ->
    statement.executeQuery("SELECT greeting FROM greetings ORDER BY random() LIMIT 1").use { resultSet ->
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
* As Ktor does not offer any specific database support:
  * We just use plain `java.sql` code (instead of any database connection library)
  * We just create the `greetings` table if it does not exist (instead of any database migration library like [flyway](https://flywaydb.org/))
* Adding Postgres plugin when creating the project should add two dependencies:
  * `org.postgresql:postgresql` for production.
  * `com.h2database:h2` for testing, that we can remove it as we will use [Testcontainers](https://www.testcontainers.org/)

Following Ktor conventions we create an Application extension to create the repository:
```kotlin
fun Application.greetingRepository(): GreetingRepository {
  val host = environment.config.property("database.host").getString()
  val port = environment.config.property("database.port").getString()
  val name = environment.config.property("database.name").getString()
  val username = environment.config.property("database.username").getString()
  val password = environment.config.property("database.password").getString()
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

Complete documentation at [Routing](https://ktor.io/docs/routing-in-ktor.html) guide.

### Vault configuration

Ktor does not support Vault. Just for this sample we will use [karlazzampersonal/ktor-vault](https://github.com/karlazzampersonal/ktor-vault) plugin.

### Application

As defined in `application.yaml` the only module loaded will be `org.rogervinas.GreetingApplicationKt.module` so we need to implement it:
```kotlin
// File GreetingApplication.kt

fun Application.module() {
  val repository = greetingRepository()
  greetingController(
    environment.config.property("greeting.name").getString(),
    environment.config.propertyOrNull("greeting.secret")?.getString() ?: "unknown",
    repository
  )
}
```
* It will create a `GreetingRepository` ...
* ... and a `GreetingController` injecting the repository created in the previous step.

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
      assertThat(bodyAsText()).matches(".+ my name is Bitelchus and my secret is unknown")
    }
  }
}
```
* We use [Testcontainers](https://www.testcontainers.org/) to test with **Postgres** and **Vault** containers.
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

Then:
```shell
# Build docker image
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

## Build a native executable and run it

Following [Generate a Micronaut Application Native Executable with GraalVM](https://guides.micronaut.io/latest/creating-your-first-micronaut-app-gradle-kotlin.html#generate-a-micronaut-application-native-executable-with-graalvm):
```shell
# Install GraalVM via sdkman
sdk install java 22.3.r19-grl
sdk default java 22.3.r19-grl
export GRAALVM_HOME=$JAVA_HOME

# Install the native-image
gu install native-image

# Build native executable
./gradlew nativeCompile

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application using native executable
MICRONAUT_ENVIRONMENTS=prod ./build/native/nativeCompile/micronaut-app 

# Make requests
curl http://localhost:8080/hello

# Stop Application with control-c

# Stop all containers
docker compose down
```

That's it! Happy coding! 💙




















# Ktor


or ktor intellij plugin
configuration in code, yaml, hocon file


vault no puc afegir
postgres puc afegir





l

https://ktor.io/docs/configuration-file.html#read-configuration-in-code

ktor + flyway es third party o sigui que res


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


pas a junit5