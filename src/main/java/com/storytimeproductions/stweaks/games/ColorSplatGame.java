package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Represents a Color Splat game where players can slap each other with fish. Implements the
 * Minigame interface for game lifecycle management.
 */
public class ColorSplatGame implements Minigame, Listener {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final Map<Player, Material> playerColors = new HashMap<>();
  private final Set<Location> poolBlocks = new HashSet<>();
  private boolean gameInProgress = false;
  private Location poolOrigin; // bottom northwest corner of pool

  /**
   * Constructs a new Color Splat game with the specified configuration.
   *
   * @param config the game configuration
   */
  public ColorSplatGame(GameConfig config) {
    this.config = config;
  }

  /** Initializes the Color Splat game. */
  @Override
  public void onInit() {
    // Setup pool location from config, e.g. "pool-origin": "world,x,y,z"
    String[] parts = config.getGameProperties().get("pool-origin").split(",");
    poolOrigin =
        new Location(
            Bukkit.getWorld(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3]));
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {
    Material[] colors = {
      Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL, Material.YELLOW_WOOL
    };
    for (int i = 0; i < players.size(); i++) {
      playerColors.put(players.get(i), colors[i % colors.length]);
    }
    // Fill pool with water and record locations
    poolBlocks.clear();
    for (int dx = 0; dx < 5; dx++) {
      for (int dz = 0; dz < 5; dz++) {
        Location loc = poolOrigin.clone().add(dx, 0, dz);
        Block b = loc.getBlock();
        b.setType(Material.WATER);
        poolBlocks.add(b.getLocation());
      }
    }
    gameInProgress = true;
  }

  /** Updates the game state. */
  @Override
  public void update() {
    // Not needed for this simple game
  }

  /** Renders the game state. */
  @Override
  public void render() {
    // Not needed for this simple game
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    players.clear();
    playerColors.clear();
    poolBlocks.clear();
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
      player.teleport(config.getGameArea());
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
    playerColors.remove(player);
  }

  /**
   * Gets the list of players currently in the game.
   *
   * @return a list of players in the game
   */
  @Override
  public List<Player> getPlayers() {
    return players;
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
    // No items to remove for this game
  }

  /**
   * Handles player movement events to detect when they land in the pool.
   *
   * @param event the PlayerMoveEvent
   */
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!gameInProgress) {
      return;
    }
    Player player = event.getPlayer();
    if (!players.contains(player)) {
      return;
    }

    Location to = event.getTo();
    Location from = event.getFrom();
    Block toBlock = to.getBlock();
    Block blockBelow = toBlock.getRelative(BlockFace.DOWN);
    boolean isOnGround = blockBelow.getType().isSolid();

    // Only trigger when the player lands (Y velocity negative, on ground, and moved
    // to a new block)
    if (from.getBlockY() >= to.getBlockY() && isOnGround) {
      // Require a minimum downward velocity (e.g., -0.6 or lower)
      if (player.getVelocity().getY() > -0.06) {
        Bukkit.getLogger()
            .info(
                "[ColorSplat] "
                    + player.getName()
                    + " landed but did not fall fast enough (velocity: "
                    + player.getVelocity().getY()
                    + ").");
        return;
      }

      Block landed = toBlock;
      if (landed.getType() == Material.WATER && poolBlocks.contains(landed.getLocation())) {
        Bukkit.getLogger()
            .info(
                "[ColorSplat] "
                    + player.getName()
                    + " landed in the pool at "
                    + landed.getLocation());
        // Replace water with player's color
        Material color = playerColors.get(player);
        landed.setType(color);
        Bukkit.getLogger()
            .info("[ColorSplat] Block at " + landed.getLocation() + " set to " + color);

        // Color adjacent blocks if they are not already the player's color
        for (BlockFace face :
            new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
          Block adj = landed.getRelative(face);
          if (poolBlocks.contains(adj.getLocation())
              && adj.getType() != color
              && adj.getType() != Material.WATER) {
            adj.setType(color);
            Bukkit.getLogger()
                .info("[ColorSplat] Adjacent block at " + adj.getLocation() + " set to " + color);
          }
        }

        // Check if pool is finished
        if (isPoolFull()) {
          Bukkit.getLogger().info("[ColorSplat] Pool is full. Ending game and awarding winner.");
          endGameAndAwardWinner();
        }
      }
    }
  }

  private boolean isPoolFull() {
    for (Location loc : poolBlocks) {
      Material type = loc.getBlock().getType();
      if (type == Material.WATER) {
        return false;
      }
    }
    return true;
  }

  private void endGameAndAwardWinner() {
    gameInProgress = false;
    // Tally colors
    Map<Player, Integer> scores = new HashMap<>();
    for (Player p : players) {
      scores.put(p, 0);
    }
    for (Location loc : poolBlocks) {
      Material type = loc.getBlock().getType();
      for (Map.Entry<Player, Material> entry : playerColors.entrySet()) {
        if (entry.getValue() == type) {
          scores.put(entry.getKey(), scores.get(entry.getKey()) + 1);
        }
      }
    }
    // Find winner (highest score, first in case of tie)
    int max = Collections.max(scores.values());
    Player winner = null;
    for (Map.Entry<Player, Integer> entry : scores.entrySet()) {
      if (entry.getValue() == max) {
        winner = entry.getKey();
        break;
      }
    }
    // Build readable leaderboard
    StringBuilder leaderboard = new StringBuilder("\n");
    scores.entrySet().stream()
        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
        .forEach(
            entry ->
                leaderboard
                    .append(entry.getKey().getName())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n"));

    // Announce and reward
    for (Player p : players) {
      p.sendMessage(
          Component.text("Game over! ", NamedTextColor.GOLD)
              .append(
                  Component.text(
                      winner != null ? winner.getName() + " wins!" : "No winner!",
                      NamedTextColor.WHITE))
              .append(Component.text(" Final scores:" + leaderboard, NamedTextColor.GRAY)));
    }
    if (winner != null) {
      rewardWinner(winner);
    }
  }

  private void rewardWinner(Player winner) {
    ItemStack tickets = new ItemStack(org.bukkit.Material.NAME_TAG, players.size());
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);
    winner.getInventory().addItem(tickets);
    winner.sendMessage(Component.text("Congratulations! You win!", NamedTextColor.GOLD));
  }
}
