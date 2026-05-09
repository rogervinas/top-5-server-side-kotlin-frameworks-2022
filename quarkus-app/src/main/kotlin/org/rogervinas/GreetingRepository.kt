package org.rogervinas

import jakarta.enterprise.context.ApplicationScoped
import javax.sql.DataSource

interface GreetingRepository {
  fun getGreeting(): String
}

@ApplicationScoped
class GreetingJdbcRepository(
  private val dataSource: DataSource,
) : GreetingRepository {
  override fun getGreeting(): String =
    dataSource.connection.use { connection ->
      connection.prepareStatement("SELECT greeting FROM greetings ORDER BY random() LIMIT 1").use { statement ->
        statement.executeQuery().use { resultSet ->
          resultSet.next()
          resultSet.getString("greeting")
        }
      }
    }
}
