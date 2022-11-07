package org.rogervinas

import io.vertx.mutiny.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class GreetingRepository(private val client: PgPool) {

    fun getVersion() = client
            .query("SELECT version() as version")
            .executeAndAwait()
            .map { r -> r.get(String::class.java, "version") }
            .first()
}