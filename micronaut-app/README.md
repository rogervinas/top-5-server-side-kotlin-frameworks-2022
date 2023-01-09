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