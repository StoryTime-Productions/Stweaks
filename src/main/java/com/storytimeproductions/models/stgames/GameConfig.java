package com.storytimeproductions.models.stgames;

import java.util.Map;
import org.bukkit.Location;

/** Represents the configuration for a game, including its properties and settings. */
public class GameConfig {
  private final String gameId;
  private final Location joinBlock;
  private final int ticketCost;
  private final Location gameArea;
  private final Location exitArea;
  private final int playerLimit;
  private final String joinSuccessMessage;
  private final String joinFailMessage;
  private final String winMessage;
  private final String loseMessage;
  private final Map<String, String> gameProperties;

  /**
   * Constructs a new GameConfig with the specified parameters.
   *
   * @param gameId the unique identifier for the game
   * @param joinBlock the location of the join block
   * @param ticketCost the cost to join the game
   * @param gameArea the location representing the game area
   * @param exitArea the location representing the exit area
   * @param playerLimit the maximum number of players allowed
   * @param joinSuccessMessage the message shown when joining succeeds
   * @param joinFailMessage the message shown when joining fails
   * @param winMessage the message shown when a player wins
   * @param loseMessage the message shown when a player loses
   * @param gameProperties additional game-specific properties
   */
  public GameConfig(
      String gameId,
      Location joinBlock,
      int ticketCost,
      Location gameArea,
      Location exitArea,
      int playerLimit,
      String joinSuccessMessage,
      String joinFailMessage,
      String winMessage,
      String loseMessage,
      Map<String, String> gameProperties) {
    this.gameId = gameId;
    this.joinBlock = joinBlock;
    this.ticketCost = ticketCost;
    this.gameArea = gameArea;
    this.exitArea = exitArea;
    this.playerLimit = playerLimit;
    this.joinSuccessMessage = joinSuccessMessage;
    this.joinFailMessage = joinFailMessage;
    this.winMessage = winMessage;
    this.loseMessage = loseMessage;
    this.gameProperties = gameProperties;
  }

  /**
   * Gets the unique identifier for the game.
   *
   * @return the game ID
   */
  public String getGameId() {
    return gameId;
  }

  /**
   * Gets the location of the join block.
   *
   * @return the join block location
   */
  public Location getJoinBlock() {
    return joinBlock;
  }

  /**
   * Gets the cost to join the game.
   *
   * @return the ticket cost
   */
  public int getTicketCost() {
    return ticketCost;
  }

  /**
   * Gets the location representing the game area.
   *
   * @return the game area location
   */
  public Location getGameArea() {
    return gameArea;
  }

  /**
   * Gets the location representing the exit area.
   *
   * @return the exit area location
   */
  public Location getExitArea() {
    return exitArea;
  }

  /**
   * Gets the maximum number of players allowed in the game.
   *
   * @return the player limit
   */
  public int getPlayerLimit() {
    return playerLimit;
  }

  /**
   * Gets the message shown when joining the game succeeds.
   *
   * @return the join success message
   */
  public String getJoinSuccessMessage() {
    return joinSuccessMessage;
  }

  /**
   * Gets the message shown when joining the game fails.
   *
   * @return the join fail message
   */
  public String getJoinFailMessage() {
    return joinFailMessage;
  }

  /**
   * Gets the message shown when a player wins the game.
   *
   * @return the win message
   */
  public String getWinMessage() {
    return winMessage;
  }

  /**
   * Gets the message shown when a player loses the game.
   *
   * @return the lose message
   */
  public String getLoseMessage() {
    return loseMessage;
  }

  /**
   * Gets additional game-specific properties.
   *
   * @return a map of game properties
   */
  public Map<String, String> getGameProperties() {
    return gameProperties;
  }
}
