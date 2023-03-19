package org.rogervinas

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

/*
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}
*/

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
