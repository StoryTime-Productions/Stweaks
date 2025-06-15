package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import com.storytimeproductions.models.stgames.challenges.BreakBlockChallenge;
import com.storytimeproductions.models.stgames.challenges.StoryBlitzChallenge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Represents a Tic Tac Toe game where players can place Xs and Os on a grid. Implements the
 * Minigame interface for game lifecycle management.
 */
public class StoryBlitz implements Minigame {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final Map<Player, Integer> lives = new HashMap<>();
  private final List<StoryBlitzChallenge> challenges = new ArrayList<>();
  private StoryBlitzChallenge currentChallenge = null;
  private int cooldown = 5;
  private int cooldownTicks = 0;
  private final int minCooldown = 1;
  private boolean gameInProgress = false;

  /**
   * Constructs a new StoryBlitz game with the specified configuration.
   *
   * @param config the game configuration
   */
  public StoryBlitz(GameConfig config) {
    this.config = config;
  }

  /** Initializes the Battleship game. */
  @Override
  public void onInit() {
    // Initialize lives and challenges
    for (Player p : players) {
      lives.put(p, 5);
    }
    challenges.add(new BreakBlockChallenge("Break a block!"));
    Collections.shuffle(challenges);
    gameInProgress = true;
    cooldown = 5;
    cooldownTicks = cooldown * 20;
    nextChallenge();
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {}

  /** Updates the game state. */
  @Override
  public void update() {
    if (!gameInProgress) {
      return;
    }
    if (cooldownTicks > 0) {
      cooldownTicks--;
      return;
    }
    if (currentChallenge != null) {
      // Check completion
      List<Player> incomplete = new ArrayList<>();
      for (Player p : players) {
        if (lives.getOrDefault(p, 0) > 0 && !currentChallenge.isCompleted(p)) {
          incomplete.add(p);
        }
      }
      if (incomplete.size() == 1) {
        Player loser = incomplete.get(0);
        int remaining = lives.getOrDefault(loser, 1) - 1;
        lives.put(loser, remaining);
        loser.sendMessage(
            Component.text("You lost a life! Lives left: " + remaining, NamedTextColor.RED));
        if (remaining <= 0) {
          loser.teleport(config.getExitArea());
          loser.sendMessage(Component.text("You are out!", NamedTextColor.GRAY));
        }
        // Reduce cooldown if player fell, min 1s
        cooldown = Math.max(minCooldown, cooldown - 1);
      }
      // End challenge and go to next
      currentChallenge.cleanup(players);
      nextChallenge();
    }
  }

  /** Renders the game state. */
  @Override
  public void render() {
    // Show action bar with cooldown
    for (Player p : players) {
      if (lives.getOrDefault(p, 0) > 0) {
        p.sendActionBar(
            Component.text(
                "Next challenge in " + (cooldownTicks / 20) + "s", NamedTextColor.YELLOW));
      }
    }
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    // Reward winner(s)
    List<Player> alive = new ArrayList<>();
    for (Player p : players) {
      if (lives.getOrDefault(p, 0) > 0) {
        alive.add(p);
      }
    }
    if (alive.size() == 1) {
      rewardWinner(alive.get(0), players.size());
    }
    for (Player p : players) {
      removeItems(p);
    }
    players.clear();
    lives.clear();
    challenges.clear();
    currentChallenge = null;
    gameInProgress = false;
  }

  /**
   * Adds the specified player to the game.
   *
   * @param player the player to add
   */
  @Override
  public void join(Player player) {
    if (!players.contains(player)) {
      players.add(player);
      lives.put(player, 5);
    }
  }

  /**
   * Removes the specified player from the game.
   *
   * @param player the player to remove
   */
  @Override
  public void leave(Player player) {
    players.remove(player);
    lives.remove(player);
  }

  /**
   * Gets the list of players currently in the game.
   *
   * @return a list of players in the game
   */
  @Override
  public List<Player> getPlayers() {
    return new ArrayList<>(players);
  }

  /**
   * Gets the game configuration.
   *
   * @return the game configuration
   */
  @Override
  public GameConfig getConfig() {
    return config;
  }

  /**
   * Determines if the game should quit.
   *
   * @return true if the game should quit, false otherwise
   */
  @Override
  public boolean shouldQuit() {
    return !gameInProgress;
  }

  @Override
  public void removeItems(Player player) {
    // Remove all challenge-related items if needed
  }

  // --- Helper methods ---

  private void nextChallenge() {
    if (challenges.isEmpty()
        || players.stream().filter(p -> lives.getOrDefault(p, 0) > 0).count() <= 1) {
      gameInProgress = false;
      return;
    }
    // Pick a random challenge, but not the same as the last one
    StoryBlitzChallenge previous = currentChallenge;
    StoryBlitzChallenge next = null;
    List<StoryBlitzChallenge> available = new ArrayList<>(challenges);
    if (previous != null && available.size() > 1) {
      available.remove(previous);
    }
    Collections.shuffle(available);
    next = available.get(0);
    currentChallenge = next;
    currentChallenge.start(players);
    cooldownTicks = cooldown * 20;
    for (Player p : players) {
      if (lives.getOrDefault(p, 0) > 0) {
        p.sendMessage(
            Component.text("Challenge: " + currentChallenge.getDescription(), NamedTextColor.AQUA));
      }
    }
  }

  private void rewardWinner(Player winner, int ticketCount) {
    ItemStack tickets = new ItemStack(Material.NAME_TAG, ticketCount);
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);
    winner.getInventory().addItem(tickets);
  }
}
