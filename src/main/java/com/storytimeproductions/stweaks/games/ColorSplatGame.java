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
  private Location poolOrigin;
  private Location ladderLoc;
  private final Map<Player, Boolean> heightRequirement = new HashMap<>();
  private final Map<Player, Integer> lastYlevel = new HashMap<>();
  private final Map<Player, Boolean> wasOnGround = new HashMap<>();
  private final Map<Player, Boolean> hasJumped = new HashMap<>();

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

    String[] ladderParts = config.getGameProperties().get("ladderLocation").split(",");
    ladderLoc =
        new Location(
            Bukkit.getWorld(ladderParts[0]),
            Double.parseDouble(ladderParts[1]),
            Double.parseDouble(ladderParts[2]),
            Double.parseDouble(ladderParts[3]));
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {
    Material[] colors = {
      Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL, Material.YELLOW_WOOL
    };
    for (int i = 0; i < players.size(); i++) {
      playerColors.put(players.get(i), colors[i % colors.length]);
      heightRequirement.put(players.get(i), false);
      wasOnGround.put(players.get(i), true);
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

    // Track Y-level and on-ground status every tick (1/20s)
    Bukkit.getScheduler()
        .runTaskTimer(
            Bukkit.getPluginManager().getPlugin("stweaks"),
            () -> {
              if (!gameInProgress) {
                return;
              }
              for (Player player : players) {
                int y = player.getLocation().getBlockY();
                lastYlevel.put(player, y);
                boolean onGround =
                    player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid();
                boolean wasGround = wasOnGround.getOrDefault(player, true);
                int poolY = poolOrigin.getBlockY();
                // Set heightRequirement true if player jumps from ground and is at least 10
                // blocks above pool
                if (!onGround && wasGround && y >= poolY + 10) {
                  heightRequirement.put(player, true);
                }
                wasOnGround.put(player, onGround);
              }
            },
            0L,
            1L);
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
    if (to == null) {
      return;
    }

    int poolY = poolOrigin.getBlockY();
    int y = player.getLocation().getBlockY();

    // Set heightRequirement true if player is at least 10 blocks above pool
    if (y >= poolY + 10) {
      heightRequirement.put(player, true);
      hasJumped.put(player, false); // Reset jump state
    }

    // Detect spacebar press (jump) while heightRequirement is true and player is
    // airborne
    if (heightRequirement.getOrDefault(player, false)
        && !hasJumped.getOrDefault(player, false)
        && player.getVelocity().getY() > 0.1) {
      hasJumped.put(player, true);
    }

    // Replace all uses of 'toBlock' in the splat logic with 'blockBelow'
    Block blockBelow = to.clone().add(0, -1, 0).getBlock();
    // Instantly teleport the player if the block below is a color block
    if (playerColors.containsValue(blockBelow.getType())) {
      heightRequirement.put(player, false);
      hasJumped.put(player, false);
      player.teleport(ladderLoc);
    }

    // Only allow splat if player had heightRequirement, has jumped, and lands in
    // pool
    if (heightRequirement.getOrDefault(player, false)
        && hasJumped.getOrDefault(player, false)
        && (to.getBlockY() == poolY || to.getBlockY() == poolY + 1)) {
      Block toBlock = to.getBlock();

      if (toBlock.getType() == Material.WATER && poolBlocks.contains(toBlock.getLocation())) {
        // Landed in the pool: color it and reset requirement
        Material color = playerColors.get(player);
        toBlock.setType(color);

        // Color adjacent blocks if they are not already the player's color
        for (BlockFace face :
            new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
          Block adj = toBlock.getRelative(face);
          if (poolBlocks.contains(adj.getLocation())
              && adj.getType() != color
              && adj.getType() != Material.WATER) {
            adj.setType(color);
          }
        }

        // Check if pool is finished
        if (isPoolFull()) {
          endGameAndAwardWinner();
        }
      }

      // Reset requirements so they must jump out and up again
      heightRequirement.put(player, false);
      hasJumped.put(player, false);

      player.teleport(ladderLoc);
    } else if (hasJumped.getOrDefault(player, false)
        && (playerColors.containsValue(blockBelow.getType())
            || blockBelow.getType() == Material.STRIPPED_OAK_LOG)) {
      // Instantly reset requirements and teleport player if they land on a colored or
      // pale oak log block
      heightRequirement.put(player, false);
      hasJumped.put(player, false);
      player.teleport(ladderLoc);
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
