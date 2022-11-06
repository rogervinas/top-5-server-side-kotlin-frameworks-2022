# Top 5 Server-Side Frameworks for Kotlin in 2022

WORK IN PROGRESS!

Inspired by [Anton Arhipov](https://twitter.com/antonarhipov) [Top 5 Server-Side Frameworks for Kotlin in 2022 @ Kotlin by JetBrains](https://www.youtube.com/watch?v=pYK5KkuZ3aU)

1. [Spring Boot](https://spring.io/projects/spring-boot)
2. [Quarkus](https://quarkus.io/)
3. [Micronaut](https://micronaut.io/)
4. [Ktor](https://ktor.io/docs/welcome.html)
5. [http4k](https://www.http4k.org/)

WIP

Features to compare:
* Configuration + Secrets
* Profiles
* Dependency Injection
* Testing
* Packaging
* Build
* Integration with third-party libraries
* Performance?

Sample:

API ==> Database

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
quarkus extension add 'quarkus-config-yaml'
```