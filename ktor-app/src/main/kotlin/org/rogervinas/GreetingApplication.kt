package org.rogervinas

import io.ktor.server.application.*
import io.ktor.server.netty.*
import java.sql.DriverManager

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
  val host = environment.config.property("database.host").getString()
  val port = environment.config.property("database.port").getString()
  val name = environment.config.property("database.name").getString()
  val username = environment.config.property("database.username").getString()
  val password = environment.config.property("database.password").getString()
  val connection = DriverManager.getConnection("jdbc:postgresql://$host:$port/$name", username, password)
  val repository = GreetingJdbcRepository(connection)

  greetingController(
        environment.config.property("greeting.name").getString(),
        environment.config.propertyOrNull("greeting.secret")?.getString() ?: "unknown",
        repository
  )
}
