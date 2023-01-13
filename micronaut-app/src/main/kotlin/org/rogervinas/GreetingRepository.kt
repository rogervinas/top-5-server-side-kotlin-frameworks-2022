package org.rogervinas

import jakarta.inject.Singleton
import org.jdbi.v3.core.Jdbi
import javax.sql.DataSource
import javax.transaction.Transactional

interface GreetingRepository {
  fun getGreeting(): String
}

@Singleton
open class GreetingJdbcRepository(dataSource: DataSource) : GreetingRepository {

  private val jdbi = Jdbi.create(dataSource)

  @Transactional
  override fun getGreeting(): String = jdbi
        .open()
        .use {
          it.createQuery("SELECT greeting FROM greetings ORDER BY random() LIMIT 1")
                .mapTo(String::class.java)
                .first()
        }
}
