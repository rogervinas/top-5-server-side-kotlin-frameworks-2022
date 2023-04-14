package org.rogervinas

import com.bettercloud.vault.Vault
import com.bettercloud.vault.VaultConfig
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.Environment.Companion.ENV
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.cloudnative.env.MapEnvironment
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
    const val DB_HOST = "DB_HOST"
    const val DB_PORT = "DB_PORT"
    const val DB_NAME = "DB_NAME"
    const val DB_USERNAME = "DB_USERNAME"
    const val DB_PASSWORD = "DB_PASSWORD"

    const val GREETING_NAME = "GREETING_NAME"
    const val GREETING_SECRET = "GREETING_SECRET"

    const val VAULT_PROTOCOL = "VAULT_PROTOCOL"
    const val VAULT_HOST = "VAULT_HOST"
    const val VAULT_PORT = "VAULT_PORT"
    const val VAULT_TOKEN = "VAULT_TOKEN"
    const val VAULT_PATH = "VAULT_PATH"

    const val SERVER_PORT = "SERVER_PORT"

    private fun Environment.withVault(): Environment {
      val vaultProtocol = EnvironmentKey.string().defaulted(VAULT_PROTOCOL, "http")(this)
      val vaultHost = EnvironmentKey.string().defaulted(VAULT_HOST, "localhost")(this)
      val vaultPort = EnvironmentKey.int().defaulted(VAULT_PORT, 8200)(this)
      val vaultToken = EnvironmentKey.string().defaulted(VAULT_TOKEN, "mytoken")(this)
      val vaultPath = EnvironmentKey.string().defaulted(VAULT_PATH, "secret/myapp")(this)
      val vaultConfig = VaultConfig()
        .address("$vaultProtocol://$vaultHost:$vaultPort")
        .token(vaultToken)
        .build()
      val vaultData = Vault(vaultConfig).logical().read(vaultPath).data
      return MapEnvironment.from(vaultData.toProperties()).overrides(this)
    }

    private fun greetingRepository(env: Environment): GreetingRepository {
      val host = EnvironmentKey.string().defaulted(DB_HOST, "localhost")(env)
      val port = EnvironmentKey.int().defaulted(DB_PORT, 5432)(env)
      val name = EnvironmentKey.string().defaulted(DB_NAME, "mydb")(env)
      val username = EnvironmentKey.string().defaulted(DB_USERNAME, "myuser")(env)
      val password = EnvironmentKey.string().defaulted(DB_PASSWORD, "mypassword")(env)
      val connection = DriverManager.getConnection("jdbc:postgresql://$host:$port/$name", username, password)
      return GreetingJdbcRepository(connection)
    }

    private fun greetingController(env: Environment, repository: GreetingRepository): HttpHandler {
      val name = EnvironmentKey.string().defaulted(GREETING_NAME, "Bitelchus")(env)
      val secret = EnvironmentKey.string().defaulted(GREETING_SECRET, "unknown")(env)
      return greetingController(name, secret, repository)
    }

    fun greetingApplication(env: Environment): Http4kServer {
      val envWithVault = env.withVault()
      val port = EnvironmentKey.int().defaulted(SERVER_PORT, 8080)(envWithVault)
      val repository = greetingRepository(envWithVault)
      val controller = greetingController(envWithVault, repository)
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
