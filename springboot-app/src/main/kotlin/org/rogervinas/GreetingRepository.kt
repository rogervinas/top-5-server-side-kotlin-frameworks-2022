package org.rogervinas

import org.springframework.stereotype.Repository

@Repository
class GreetingRepository(/*private val jdbcTemplate: JdbcTemplate*/) {

    fun getGreeting() = "Hola"
            //.query("SELECT greeting FROM greetings ORDER BY random() limit 1")
}