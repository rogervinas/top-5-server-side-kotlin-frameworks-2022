package org.rogervinas

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
  EngineMain.main(args)
}

fun Application.module() {
  val repository = greetingRepository()
  greetingController(
        environment.config.property("greeting.name").getString(),
        environment.config.propertyOrNull("greeting.secret")?.getString() ?: "unknown",
        repository
  )
}
