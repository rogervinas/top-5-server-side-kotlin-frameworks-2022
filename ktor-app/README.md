# Ktor

To begin with you can follow the [xxx](xxx) guide.

To create a Ktor project we have two alternatives:
* Use [IntelliJ Idea plugin]() 
* Use [](https://start.ktor.io) web interface (similar to [Spring Initializr](https://start.spring.io/) for **Spring Boot**)

--You can also check [all the other guides](https://guides.micronaut.io/latest/index.html) as well as [the user documentation](https://docs.micronaut.io/latest/guide/).

Crear amb start:
Configuration in YAML file
Add sample code
Add plugins postgres

Just run it once to check everything is ok:
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

As we've chosen configuration by YAML all is set si no necessitem afegir dep `io.ktor:ktor-server-config-yaml` i cridar a:
```kotlin
fun main(args: Array<String>) {
  EngineMain.main(args)
}
```

i els moduls s'han d'afegir per configuracio no per codi:
```yaml
ktor:
  deployment:
    port: 8080
  application:
    modules:
      - org.rogervinas.GreetingApplicationKt.module
```

We can add to `application.yaml` our first configuration property:
```yaml
greeting:
  name: "Bitelchus"
```

Si usem configuration per codi llavors no es llegeix el yaml i en el main:
```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}
```

Environments o profiles??

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

* No usem cap migration pk Ktor no incorpora, pero podriem usar flyway o similar standalone.
* Per simplificar tambe usem directament java.sql, pero podriem usar qualsevol third-party.

Com que hem afegit la plugin postgres ja tenim les deps:

```kotlin
implementation("org.postgresql:postgresql:$postgres_version")
```

Eliminem la part de h2 pk ho farem duna altra manera, 

Creem una extension per crear el repo:
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

amb aquesta config a application.yaml:
```yaml
database:
  host: "$DB_HOST:localhost"
  port: 5432
  name: "mydb"
  username: "myuser"
  password: "mypassword"
```

En el cas del host pren el valor de la variable d'entorn DB_HOST per bla bla bla

### GreetingController

We will create a `GreetingController` to serve the `/hello` endpoint:
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

Mes info a ...

### Vault configuration

https://github.com/karlazzampersonal/ktor-vault

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

* We use testApplication with an empty configuration to just test the controller
* We mock the repository with `mockk`.
* More https://ktor.io/docs/testing.htm

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

* Testcontainers to test with a real postgres and vault.
* El sample usa h2 pero millor un postgres
* We use pattern matching to check the greeting, as it is random.
* NO ENCARA As this test uses **Vault**, the secret should be `watermelon`.

## Test

```shell
./gradlew test
```

## Run

!! LA MAIN CLASS s-especifica al build.gradle.kts

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

## Build a docker image and run it

Configure a base docker image???

Then:
```shell
# Build docker image
./gradlew buildImage

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