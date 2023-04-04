package org.rogervinas

import com.bettercloud.vault.Vault
import com.bettercloud.vault.VaultConfig
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
  EngineMain.main(args)
}

fun Application.module() {
  val vaultData = vaultData()
  val repository = greetingRepository()
  greetingController(
        environment.config.property("greeting.name").getString(),
        vaultData["greeting.secret"] ?: "unknown",
        repository
  )
}

private fun Application.vaultData(): Map<String, String> {
  val vaultProtocol = environment.config.property("vault.protocol").getString()
  val vaultHost = environment.config.property("vault.host").getString()
  val vaultPort = environment.config.property("vault.port").getString()
  val vaultToken = environment.config.property("vault.token").getString()
  val vaultPath = environment.config.property("vault.path").getString()
  val vaultConfig = VaultConfig()
    .address("$vaultProtocol://$vaultHost:$vaultPort")
    .token(vaultToken)
    .build()
  return Vault(vaultConfig).logical().read(vaultPath).data.toMap()
}
