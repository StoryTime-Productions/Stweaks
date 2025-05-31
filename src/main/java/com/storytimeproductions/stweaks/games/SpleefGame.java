package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Represents a Spleef game where players can dig blocks to make opponents fall. Implements the
 * Minigame interface for game lifecycle management.
 */
public class SpleefGame implements Minigame {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final Set<Location> platformBlocks = new HashSet<>();
  private boolean roundActive = false;
  private Player winner = null;
  private int initialPlayerCount = 0; // Add this field at the top of your class

  /**
   * Constructs a new Spleef game with the specified configuration.
   *
   * @param config the game configuration
   */
  public SpleefGame(GameConfig config) {
    this.config = config;
  }

  /** Initializes the Spleef game. */
  @Override
  public void onInit() {
    platformBlocks.clear();
    winner = null;
    spawnPlatform();
    initialPlayerCount = players.size();
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {
    for (Player player : players) {
      giveShovel(player);
    }
    roundActive = true;
  }

  /** Updates the game state. */
  @Override
  public void update() {
    if (!roundActive) {
      return;
    }
    int platformY = config.getGameArea().getBlockY();
    Iterator<Player> it = players.iterator();
    while (it.hasNext()) {
      Player player = it.next();
      if (player.getLocation().getBlockY() < platformY) {
        player.sendMessage(Component.text("You fell!", NamedTextColor.RED));
        it.remove();
        player.getInventory().remove(Material.DIAMOND_SHOVEL);
        if (config.getExitArea() != null) {
          player.teleport(config.getExitArea());
        }
      }
    }
    // Check for winner
    if (players.size() == 1 && winner == null) {
      winner = players.get(0);
      winner.sendMessage(Component.text("You win!", NamedTextColor.GOLD));
      winner.getInventory().remove(Material.DIAMOND_SHOVEL);
      if (initialPlayerCount > 0) {
        ItemStack tickets = new ItemStack(Material.NAME_TAG, initialPlayerCount);
        tickets.getItemMeta().displayName(Component.text("Time Ticket").color(NamedTextColor.GOLD));
        ItemMeta meta = tickets.getItemMeta();
        meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
        meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        tickets.setItemMeta(meta);
        winner.getInventory().addItem(tickets);
        winner.sendMessage(
            Component.text(
                "You received " + initialPlayerCount + " Time Ticket(s)!", NamedTextColor.YELLOW));
      }
      roundActive = false;
    }
  }

  /** Renders the game state. */
  @Override
  public void render() {
    // Optional: show action bar or effects
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    for (Location loc : platformBlocks) {
      Block block = loc.getBlock();
      block.setType(Material.OBSIDIAN);
    }
    for (Player player : players) {
      player.getInventory().remove(Material.DIAMOND_SHOVEL);
      player.getInventory().remove(Material.SNOWBALL);
    }
    players.clear();
    winner = null;
    roundActive = false;
    resetPlatform();
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
      player.teleport(getConfig().getGameArea().clone().add(0, 1, 0));
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
    player.getInventory().remove(Material.DIAMOND_SHOVEL);
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
    return !roundActive;
  }

  private void spawnPlatform() {
    Location center = config.getGameArea();
    World world = center.getWorld();
    int radius = 8;
    int y = center.getBlockY();
    for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
      for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {
        Location loc = new Location(world, x, y, z);
        Block block = world.getBlockAt(x, y, z);
        if (loc.distance(center) <= radius + 0.5) {
          if (block.getType() != Material.AIR && block.getType() != Material.BARRIER) {
            block.setType(Material.SNOW_BLOCK);
            platformBlocks.add(loc.clone());
          }
        }
      }
    }
  }

  private void resetPlatform() {
    for (Location loc : platformBlocks) {
      Block block = loc.getBlock();
      block.setType(Material.OBSIDIAN);
    }
    platformBlocks.clear();
  }

  private void giveShovel(Player player) {
    ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL, 1);
    ItemMeta meta = shovel.getItemMeta();
    meta.displayName(Component.text("Spleef Shovel", NamedTextColor.AQUA));
    shovel.setItemMeta(meta);
    player.getInventory().addItem(shovel);
  }
}
