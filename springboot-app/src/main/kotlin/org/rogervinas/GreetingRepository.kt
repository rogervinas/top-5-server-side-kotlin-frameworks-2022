package org.rogervinas

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class GreetingRepository(private val jdbcTemplate: JdbcTemplate) {

    fun getGreeting() = jdbcTemplate
            .queryForObject("SELECT greeting FROM greetings ORDER BY random() limit 1", String::class.java)
}