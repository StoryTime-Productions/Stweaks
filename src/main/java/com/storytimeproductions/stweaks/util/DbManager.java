package com.storytimeproductions.stweaks.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Manages connection and setup for the SQLite database used to store player playtime. */
public class DbManager {

  private static final String DATABASE_URL = "jdbc:sqlite:plugins/Stweaks/playtime.db";

  private Connection connection;

  /**
   * Establishes a connection to the SQLite database and creates the playtime table if it doesn't
   * exist.
   */
  public void connect() {
    try {
      connection = DriverManager.getConnection(DATABASE_URL);
      createTableIfNotExists();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Creates the playtime table if it does not exist. The table stores UUIDs, seconds played, and
   * the last update timestamp.
   */
  private void createTableIfNotExists() {
    String sql =
        """
                CREATE TABLE IF NOT EXISTS playtime (
                    uuid TEXT PRIMARY KEY,
                    seconds_played INTEGER NOT NULL,
                    updated_last DATE NOT NULL
                );
                """;
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(sql);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets the current database connection.
   *
   * @return The active Connection object.
   */
  public Connection getConnection() {
    return connection;
  }

  /** Closes the database connection if it is open. */
  public void disconnect() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
