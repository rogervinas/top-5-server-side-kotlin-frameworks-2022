package org.rogervinas

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

interface GreetingRepository {
  fun getGreeting(): String
}

@Repository
class GreetingJdbcRepository(private val jdbcTemplate: JdbcTemplate) : GreetingRepository {

  override fun getGreeting(): String = jdbcTemplate
        .queryForObject("SELECT greeting FROM greetings ORDER BY random() LIMIT 1", String::class.java)!!
}
