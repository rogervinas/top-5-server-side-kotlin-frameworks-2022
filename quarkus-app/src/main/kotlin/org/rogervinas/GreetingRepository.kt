package org.rogervinas

import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped

interface GreetingRepository {
  fun getGreeting(): String
}

@ApplicationScoped
class GreetingJdbcRepository(
  private val dataSource: AgroalDataSource,
) : GreetingRepository {
  override fun getGreeting(): String {
    dataSource.connection.use { connection ->
      connection.createStatement().use { statement ->
        val rs = statement.executeQuery("SELECT greeting FROM greetings ORDER BY random() LIMIT 1")
        rs.next()
        return rs.getString("greeting")
      }
    }
  }
}
