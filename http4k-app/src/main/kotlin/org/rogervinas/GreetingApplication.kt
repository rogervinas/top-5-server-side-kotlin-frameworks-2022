package org.rogervinas

import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.Environment.Companion.ENV
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.rogervinas.GreetingApplication.Companion.greetingApplication
import java.sql.DriverManager

class GreetingApplication {
  companion object {
    const val DATABASE_HOST = "DATABASE_HOST"
    const val DATABASE_PORT = "DATABASE_PORT"
    const val DATABASE_NAME = "DATABASE_NAME"
    const val DATABASE_USERNAME = "DATABASE_USERNAME"
    const val DATABASE_PASSWORD = "DATABASE_PASSWORD"
    const val GREETING_NAME = "GREETING_NAME"
    const val GREETING_SECRET = "GREETING_SECRET"
    const val SERVER_PORT = "SERVER_PORT"

    fun greetingRepository(env: Environment): GreetingRepository {
      val host = EnvironmentKey.string().defaulted(DATABASE_HOST, "localhost")(env)
      val port = EnvironmentKey.int().defaulted(DATABASE_PORT, 5432)(env)
      val name = EnvironmentKey.string().defaulted(DATABASE_NAME, "mydb")(env)
      val username = EnvironmentKey.string().defaulted(DATABASE_USERNAME, "myuser")(env)
      val password = EnvironmentKey.string().defaulted(DATABASE_PASSWORD, "mypassword")(env)
      val connection = DriverManager.getConnection("jdbc:postgresql://$host:$port/$name", username, password)
      return GreetingJdbcRepository(connection)
    }

    fun greetingController(env: Environment, repository: GreetingRepository): HttpHandler {
      val name = EnvironmentKey.string().defaulted(GREETING_NAME, "Bitelchus")(env)
      val secret = EnvironmentKey.string().defaulted(GREETING_SECRET, "unknown")(env)
      return greetingController(name, secret, repository)
    }

    fun greetingApplication(env: Environment): Http4kServer {
      val port = EnvironmentKey.int().defaulted(SERVER_PORT, 8080)(env)
      val repository = greetingRepository(env)
      val controller = greetingController(env, repository)
      return PrintRequest().then(controller).asServer(Undertow(port))
    }
  }
}

fun main() {
  println("Application starting ...")
  val application = greetingApplication(ENV)
  application.start()
  println("Application started on " + application.port())
}
