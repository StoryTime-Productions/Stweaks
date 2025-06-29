package com.storytimeproductions.models.stgames;

import java.util.List;
import org.bukkit.entity.Player;

/**
 * Represents a minigame that can be played within the game environment. Each minigame has its own
 * lifecycle methods and player management.
 */
public interface Minigame {
  /** Initializes the minigame. */
  void onInit();

  /** Called after the minigame has been initialized. */
  void afterInit();

  /** Updates the minigame state. */
  void update();

  /** Renders the minigame state. */
  void render();

  /** Cleans up resources when the minigame is destroyed. */
  void onDestroy();

  /**
   * Determines if the minigame should quit.
   *
   * @return true if the minigame should quit, false otherwise
   */
  boolean shouldQuit();

  /**
   * Adds the specified player to the minigame.
   *
   * @param player the player to add
   */
  void join(Player player);

  /**
   * Removes the specified player from the minigame.
   *
   * @param player the player to remove
   */
  void leave(Player player);

  /**
   * Gets the list of players currently in the minigame.
   *
   * @return a list of players in the minigame
   */
  List<Player> getPlayers();

  /**
   * Gets the minigame configuration.
   *
   * @return the minigame configuration
   */
  GameConfig getConfig();

  /**
   * Removes all game-related items from the player's inventory.
   *
   * @param player the player whose items will be removed
   */
  void removeItems(Player player);
}
