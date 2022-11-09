package org.rogervinas

import io.vertx.mutiny.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class GreetingRepository(private val client: PgPool) {

    fun getGreeting() = client
            .query("SELECT greeting FROM greetings ORDER BY random() limit 1")
            .executeAndAwait()
            .map { r -> r.get(String::class.java, "greeting") }
            .first()
}