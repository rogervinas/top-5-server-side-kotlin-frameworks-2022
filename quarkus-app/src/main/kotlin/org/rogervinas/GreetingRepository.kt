package org.rogervinas

import io.vertx.mutiny.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped

interface GreetingRepository {
    fun getGreeting(): String
}

@ApplicationScoped
class GreetingJdbcRepository(private val client: PgPool): GreetingRepository {

    override fun getGreeting(): String = client
            .query("SELECT greeting FROM greetings ORDER BY random() LIMIT 1")
            .executeAndAwait()
            .map { r -> r.get(String::class.java, "greeting") }
            .first()
}