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
   * Creates the playtime and biome tracking tables if they do not exist. The playtime table stores
   * UUIDs, seconds played, and last update timestamp. The discovered_biomes table tracks which
   * biomes each player has visited.
   */
  private void createTableIfNotExists() {
    String playtimeSql =
        """
        CREATE TABLE IF NOT EXISTS playtime (
            uuid TEXT PRIMARY KEY,
            seconds_played INTEGER NOT NULL,
            updated_last DATE NOT NULL
        );
        """;

    String biomeSql =
        """
        CREATE TABLE IF NOT EXISTS discovered_biomes (
            uuid TEXT NOT NULL,
            biome_key TEXT NOT NULL,
            PRIMARY KEY (uuid, biome_key)
        );
        """;

    String questCompletionSql =
        """
        CREATE TABLE IF NOT EXISTS completed_quests (
            uuid TEXT NOT NULL,
            quest_id TEXT NOT NULL,
            completion_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (uuid, quest_id)
        );
        """;

    try (Statement stmt = connection.createStatement()) {
      stmt.execute(playtimeSql);
      stmt.execute(biomeSql);
      stmt.execute(questCompletionSql);
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
