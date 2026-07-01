package com.storytimeproductions.models.stgames;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

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

  // --- Event hooks (no-op by default; override in games that need them) ---

  /** Called when a player in this game interacts with a block or air. */
  default void onInteract(PlayerInteractEvent event) {}

  /** Called when a player in this game moves. */
  default void onMove(PlayerMoveEvent event) {}

  /** Called when a player in this game dies. */
  default void onDeath(PlayerDeathEvent event) {}

  /** Called when a player in this game damages another player in this game. */
  default void onDamage(EntityDamageByEntityEvent event) {}

  /** Called when a player in this game toggles sneak. */
  default void onSneak(PlayerToggleSneakEvent event) {}

  /** Called when a player in this game runs a command. */
  default void onCommand(PlayerCommandPreprocessEvent event) {}

  /**
   * Returns true if players can join this game while it is already running. Defaults to false
   * (players must wait for the previous round to finish).
   */
  default boolean allowsConcurrentJoins() {
    return false;
  }

  /**
   * Returns true if players should be teleported to the exit area when the game ends. Defaults to
   * true; override to false for games that manage their own exit flow.
   */
  default boolean shouldTeleportOnExit() {
    return true;
  }
}
