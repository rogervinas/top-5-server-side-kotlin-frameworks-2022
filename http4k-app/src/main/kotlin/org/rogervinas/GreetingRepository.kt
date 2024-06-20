package org.rogervinas

import java.sql.Connection

interface GreetingRepository {
  fun getGreeting(): String
}

class GreetingJdbcRepository(private val connection: Connection) : GreetingRepository {
  init {
    createGreetingsTable()
  }

  override fun getGreeting(): String =
    connection.createStatement().use { statement ->
      statement.executeQuery("SELECT greeting FROM greetings ORDER BY random() LIMIT 1").use { resultSet ->
        return if (resultSet.next()) {
          resultSet.getString("greeting")
        } else {
          throw Exception("No greetings found!")
        }
      }
    }

  private fun createGreetingsTable() {
    connection.createStatement().use {
      it.executeUpdate(
        """
        CREATE TABLE IF NOT EXISTS greetings (
            id serial,
            greeting varchar(100) NOT NULL,
            PRIMARY KEY (id)
        );
        INSERT INTO greetings (greeting) VALUES ('Hello');
        INSERT INTO greetings (greeting) VALUES ('Hola');
        INSERT INTO greetings (greeting) VALUES ('Hi');
        INSERT INTO greetings (greeting) VALUES ('Holi');
        INSERT INTO greetings (greeting) VALUES ('Bonjour');
        INSERT INTO greetings (greeting) VALUES ('Ni hao');
        INSERT INTO greetings (greeting) VALUES ('Bon dia');
        """.trimIndent(),
      )
    }
  }
}
