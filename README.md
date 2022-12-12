# Top 5 Server-Side Frameworks for Kotlin in 2022

This is a demo inspired by [Anton Arhipov](https://twitter.com/antonarhipov)'s [Top 5 Server-Side Frameworks for Kotlin in 2022 @ Kotlin by JetBrains](https://www.youtube.com/watch?v=pYK5KkuZ3aU) where, **spoiler alert**, the author shares this top 5 list:

1. [Spring Boot](https://spring.io/projects/spring-boot)
2. [Quarkus](https://quarkus.io/)
3. [Micronaut](https://micronaut.io/)
4. [Ktor](https://ktor.io/docs/welcome.html)
5. [http4k](https://www.http4k.org/)

I have a lot of experience in Spring Boot, so I wanted to take a look at the other ones 😜

<p align="center">
  <img align="center" src="doc/meme.png">
</p>

To do so we will create a simple application with each one of these frameworks, implementing the following scenario:

<p align="center">
  <img align="center" src="doc/scenario.png">
</p>

We will use this [docker-compose.yaml](docker-compose.yaml) to start locally [vault](https://www.vaultproject.io/) and [postgresql](https://www.postgresql.org/), as well as the application containers.

In order to put a `greeting.secret` in vault we will start another vault container overriding its entrypoint to just put the secret using vault-cli and die afterwards (maybe there is another more elegant way to do it but this one works).

Please find below a step-by-step guide for each one of the top 5 frameworks. Here we go!

1. [Spring Boot](springboot-app)
2. [Quarkus](quarkus-app)
