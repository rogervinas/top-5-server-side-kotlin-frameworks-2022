# Micronaut

To begin with you can follow the [Creating your first Micronaut application](https://guides.micronaut.io/latest/creating-your-first-micronaut-app.html) guide. 

You will see that there are two alternatives:
* Use [Micronaut Command Line Interface](https://docs.micronaut.io/latest/guide/#cli) command line (easily installable via [sdkman](https://sdkman.io/))
* Use [Micronaut Launch](https://launch.micronaut.io/) web interface (similar to [Spring Initializr](https://start.spring.io/) for **Spring Boot**)

You can also check [all the other guides](https://guides.micronaut.io/latest/index.html) as well as [the user documentation](https://docs.micronaut.io/latest/guide/).

To create our sample gradle & kotlin application using the command line:
```shell
sdk install micronaut
mn create-app micronaut-app \
   --features=data-jdbc,postgres,flyway \
   --build=gradle_kotlin --lang=kotlin --java-version=11 --test=junit
```

Just run it once to check everything is ok:
```shell
./gradlew run
```

And make a request to the health endpoint:
```shell
curl http://localhost:8080/health
{"status":"UP"}
```

## Implementation

### YAML configuration

We can add to `application.yaml` our first configuration property:
```yaml
greeting:
  name: "Bitelchus"
```

We can have different "environments" active (similar to "profiles" in **Spring Boot**):
* By default, no environment is enabled, so only `application.yaml` file will be loaded.
* We can enable environments using `MICRONAUT_ENVIRONMENTS` environment variable or `micronaut.environments` system property.
* When executing tests `test` environment is enabled.

So we will create a `application-prod.yaml` file to put there all the production configuration properties.

More documentation about configuration sources at [Application Configuration](https://docs.micronaut.io/latest/guide/#config) guide.

### GreetingRepository

We will create a `GreetingRepository`:
```kotlin
interface GreetingRepository {
  fun getGreeting(): String
}

@Singleton
open class GreetingJdbcRepository(dataSource: DataSource) : GreetingRepository {
    
  private val jdbi = Jdbi.create(dataSource)

  @Transactional
  override fun getGreeting(): String = jdbi
    .open()
    .use {
      it.createQuery("SELECT greeting FROM greetings ORDER BY random() LIMIT 1")
        .mapTo(String::class.java)
        .first()
    }
}
```

* The `@Singleton` annotation will make **Micronaut** to create an instance at startup.
* We inject the `DataSource` provided by **Micronaut**.
* As seems that **Micronaut** does not include anything similar by default, we use [JDBI](https://jdbi.org/) and that SQL to retrieve one random `greeting` from the `greetings` table.
* We add the `@Transactional` annotation so **Micronaut** will execute the query within a database transaction. As **Micronaut** will instantiate a proxy class inheriting from `GreetingJdbcRepository` we are forced to "open" the class as all kotlin classes are final.

For this to work, we need some extra steps ...

Use a specific postresql driver version (just not do depend on [Micronaut BOM]()) and add the [JDBI](https://jdbi.org/) dependency:
```kotlin
implementation("org.postgresql:postgresql:42.5.1")
implementation("org.jdbi:jdbi3-core:3.36.0")
```

As we added `posgtresql` feature when creating the project, [Test Resources](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/) will magically start for us a **PostgreSQL** container. It is not required but we can configure a specific version of the container in `application.yaml`:
```yaml
test-resources:
  containers:
    postgres:
      image-name: "postgres:14.5"
```

We should also configure the database connection for `prod` environment in `application-prod.yaml`:
```yaml
datasources:
  default:
    url: "jdbc:postgresql://${DB_HOST:localhost}:5432/mydb"
    username: "myuser"
    password: "mypassword"
```

Configuring it will disable [Test Resources](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/) for **PostgreSQL**.

[Flyway](https://flywaydb.org/) is already enabled as we created the project adding the `flyway` feature, so we only need to add migrations under [src/main/resources/db/migration](src/main/resources/db/migration) to create and populate `greetings` table.

### GreetingController

We will create a `GreetingController` to serve the `/hello` endpoint:
```kotlin
@Controller("/hello")
class GreetingController(
  private val repository: GreetingRepository,
  @Property(name = "greeting.name") private val name: String,
  @Property(name = "greeting.secret", defaultValue = "unknown") private val secret: String
) {
    @Get
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = "${repository.getGreeting()} my name is $name and my secret is $secret"
}
```

* We can inject dependencies via constructor and configuration properties using `@Property` annotation.
* We expect to get `greeting.secret` from **Vault**, that is why we configure `unknown` as its default value, so it does not fail until we configure **Vault** properly.
* Everything is pretty similar to **Spring Boot**.

### Vault configuration

Following the [HashiCorp Vault Support](https://docs.micronaut.io/latest/guide/#distributedConfigurationVault) guide we have add this configuration to `bootstrap.yaml`:
```yaml
micronaut:
  application:
    name: "myapp"
  server:
    port: 8080
  config-client:
    enabled: true

vault:
  client:
    config:
      enabled: true
    kv-version: "V2"
    secret-engine-name: "secret"

test-resources:
  containers:
    postgres:
      image-name: "postgres:14.5"
    hashicorp-vault:
      image-name: "vault:1.12.1"
      path: "secret/myapp"
      secrets:
        - "greeting.secret=watermelon"
```

Note that some of this configuration was already set in `application.yaml` but we move it here, so it is available in the "bootstrap" phase.

Once thing not currently mentioned in the documentation is that we need to add this dependency to enable "bootstrap":
```kotlin
implementation("io.micronaut.discovery:micronaut-discovery-client")
```

[Test Resources for Hashicorp Vault](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/#modules-hashicorp-vault) allow us to populate **Vault**, so for dev and test it will start a ready-to-use **Vault** container 🥹

For `prod` environment we configure **Vault** in `bootstrap-prod.yaml`, and doing so we will disable 
```yaml
vault:
  client:
    uri: "http://${VAULT_HOST:localhost}:8200"
    token: "mytoken"
```

And as usual, configuring it will disable [Test Resources](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/) for **Vault**.

## Testing the endpoint

We can test the endpoint this way:
```kotlin
@MicronautTest
@Property(name = "greeting.secret", value = "apple")
class GreetingControllerTest {

  @Inject
  @field:Client("/")
  private lateinit var client: HttpClient

  @Inject
  private lateinit var repository: GreetingRepository

  @Test
  fun `should say hello`() {
    every { repository.getGreeting() } returns "Hello"

    val request: HttpRequest<Any> = HttpRequest.GET("/hello")
    val response = client.toBlocking().exchange(request, String::class.java)

    assertEquals(OK, response.status)
    assertEquals("Hello my name is Bitelchus and my secret is apple", response.body.get())
  }

    @MockBean(GreetingRepository::class)
    fun repository() = mockk<GreetingRepository>()
}
```

* `@MicronautTest` will start all "Test Resources" container, despite not needed 🤷
* We mock the repository with `@MockBean`.
* We use **Micronaut**'s `HttpClient` to test the endpoint.
* We set `greeting.secret` property just for this test (so we do not use the **Vault** value).

## Testing the application

We can test the whole application this way:
```kotlin
@MicronautTest
class GreetingApplicationTest {

  @Test
  fun `should say hello`(spec: RequestSpecification) {
    spec
      .`when`()
      .get("/hello")
      .then()
      .statusCode(200)
      .body(matchesPattern(".+ my name is Bitelchus and my secret is watermelon"))
    }
}
```

* `@MicronautTest` will start all "Test Resources" containers, now all of them are being used.
* For this one we use [Rest Assured](https://guides.micronaut.io/latest/micronaut-rest-assured.html) instead of `HttpClient`, just to show another way. We need to add `io.micronaut.test:micronaut-test-rest-assured` dependency.
* We use pattern matching to check the greeting, as it is random.
* As this test uses **Vault**, the secret should be `watermelon`.

## Test

```shell
./gradlew test
```

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
./gradlew shadowJar

# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
java -Dmicronaut.environments=prod -jar build/libs/micronaut-app-0.1-all.jar

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

That's it! Happy coding! 💙

https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/
https://docs.micronaut.io/latest/guide/#config
https://micronaut-projects.github.io/micronaut-gradle-plugin/snapshot/#_docker_support
https://micronaut-projects.github.io/micronaut-sql/latest/guide/#jasync
https://docs.micronaut.io/latest/guide/#distributedConfigurationVault
https://docs.micronaut.io/latest/guide/#bootstrap
https://guides.micronaut.io/latest/micronaut-rest-assured-gradle-kotlin.html
