package org.rogervinas

import com.bettercloud.vault.Vault
import com.bettercloud.vault.VaultConfig
import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.EngineMain
import io.ktor.server.netty.Netty
import java.sql.DriverManager

fun main(args: Array<String>) {
  EngineMain.main(args)
  embeddedServer(
    factory = Netty,
    port = 8080,
    host = "0.0.0.0",
    module = Application::module,
  )
    .start(wait = true)
}

fun Application.module() {
  val environmentConfig = environment.config.withVault()
  val repository = greetingRepository(environmentConfig)
  greetingController(
    environmentConfig.property("greeting.name").getString(),
    environmentConfig.propertyOrNull("greeting.secret")?.getString() ?: "unknown",
    repository,
  )
}

private fun greetingRepository(config: ApplicationConfig): GreetingRepository {
  val host = config.property("database.host").getString()
  val port = config.property("database.port").getString()
  val name = config.property("database.name").getString()
  val username = config.property("database.username").getString()
  val password = config.property("database.password").getString()
  val connection = DriverManager.getConnection("jdbc:postgresql://$host:$port/$name", username, password)
  return GreetingJdbcRepository(connection)
}

private fun ApplicationConfig.withVault(): ApplicationConfig {
  val vaultProtocol = this.property("vault.protocol").getString()
  val vaultHost = this.property("vault.host").getString()
  val vaultPort = this.property("vault.port").getString()
  val vaultToken = this.property("vault.token").getString()
  val vaultPath = this.property("vault.path").getString()
  val vaultConfig =
    VaultConfig()
      .address("$vaultProtocol://$vaultHost:$vaultPort")
      .token(vaultToken)
      .build()
  val vaultData = Vault(vaultConfig).logical().read(vaultPath).data
  return this.mergeWith(MapApplicationConfig(vaultData.entries.map { Pair(it.key, it.value) }))
}
