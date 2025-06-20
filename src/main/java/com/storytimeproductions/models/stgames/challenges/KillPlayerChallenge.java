package com.storytimeproductions.models.stgames.challenges;

import com.storytimeproductions.models.stgames.Cuboid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

/**
 * Represents a challenge where players must kill themselves by jumping off a tower. This class
 * implements the StoryBlitzChallenge interface and handles player deaths and damage events to
 * determine if the challenge is completed.
 */
public class KillPlayerChallenge implements StoryBlitzChallenge, Listener {
  private final Set<UUID> completed = new HashSet<>();
  private final String description = "Kill yourself by jumping off the tower!";
  private Location towerBase = null;
  private int towerHeight = 5;
  private Cuboid region;
  private Plugin plugin;
  private List<Player> players;

  /**
   * Constructor for the KillPlayerChallenge.
   *
   * @param spawnRegions the spawnRegions from which the tower will be spawned in.
   */
  public KillPlayerChallenge(List<Cuboid> spawnRegions) {
    this.plugin = Bukkit.getPluginManager().getPlugin("stweaks");
    if (!spawnRegions.isEmpty()) {
      this.region = spawnRegions.get(0);
      // Use the lowest y-level for the base
      int minY = Math.min(region.y1, region.y2);
      int centerX = (region.x1 + region.x2) / 2;
      int centerZ = (region.z1 + region.z2) / 2;
      World world = region.world;
      towerBase = new Location(world, centerX, minY, centerZ);
    }
  }

  /**
   * Returns the description of the challenge.
   *
   * @return the description of the challenge
   */
  @Override
  public String getDescription() {
    return getDescriptionWithGoob(description);
  }

  /**
   * Starts the challenge by teleporting players to the top of the tower and registering the event
   * listener.
   *
   * @param players the list of players participating in the challenge
   */
  @Override
  public void start(List<Player> players) {
    this.players = players;
    completed.clear();
    // Register event listener
    Bukkit.getPluginManager().registerEvents(this, plugin);

    // Build the tower every round
    if (towerBase != null && region != null) {
      int minY = towerBase.getBlockY();
      int centerX = towerBase.getBlockX();
      int centerZ = towerBase.getBlockZ();
      World world = towerBase.getWorld();

      // Build the tower: ladder on one side, platform on top
      for (int y = minY; y < minY + towerHeight; y++) {
        Block block = world.getBlockAt(centerX, y, centerZ);
        block.setType(Material.OAK_PLANKS);
        // Place ladder on the south side
        Block ladderBlock = world.getBlockAt(centerX, y, centerZ + 1);
        ladderBlock.setType(Material.LADDER);
        ladderBlock.setBlockData(Bukkit.createBlockData("minecraft:ladder[facing=south]"));
      }
      // Platform on top, but leave the block above the topmost ladder open for access
      int platformY = minY + towerHeight;
      for (int x = centerX - 1; x <= centerX + 1; x++) {
        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
          // Leave opening at (centerX, platformY, centerZ + 1) for ladder access
          if (x == centerX && z == centerZ + 1) {
            continue;
          }
          world.getBlockAt(x, platformY, z).setType(Material.OAK_PLANKS);
        }
      }
    }

    // Teleport all players to the top of the tower
    if (towerBase != null) {
      for (Player p : players) {
        p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
        p.setFoodLevel(20);
      }
    }
  }

  /**
   * Checks if the challenge is completed by the player.
   *
   * @param player the player to check
   * @return true if the player has completed the challenge, false otherwise
   */
  @Override
  public boolean isCompleted(Player player) {
    return completed.contains(player.getUniqueId());
  }

  /**
   * Cleans up the challenge by unregistering the event listener and removing the tower.
   *
   * @param players the list of players who participated in the challenge
   */
  @Override
  public void cleanup(List<Player> players) {
    HandlerList.unregisterAll(this);
    completed.clear();

    if (towerBase != null && region != null) {
      int minY = towerBase.getBlockY();
      int centerX = towerBase.getBlockX();
      int centerZ = towerBase.getBlockZ();
      World world = towerBase.getWorld();

      // Remove the tower (vertical planks and ladders)
      for (int y = minY; y < minY + towerHeight; y++) {
        // Remove central plank
        world.getBlockAt(centerX, y, centerZ).setType(Material.AIR);
        // Remove ladder on the south side
        world.getBlockAt(centerX, y, centerZ + 1).setType(Material.AIR);
      }
      // Remove the platform on top
      for (int x = centerX - 1; x <= centerX + 1; x++) {
        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
          world.getBlockAt(x, minY + towerHeight, z).setType(Material.AIR);
        }
      }
    }
  }

  /**
   * Handles player death events to check if the player has killed themselves.
   *
   * @param event the PlayerDeathEvent triggered when a player dies
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    if (players.contains(player)) {
      event.setCancelled(true);
      completed.add(player.getUniqueId());
      player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
      player.setFoodLevel(20);
      player.sendMessage(
          Component.text(
              "You have completed the challenge by killing yourself!", NamedTextColor.GREEN));
    }
  }

  /**
   * Handles player damage events to check if the player has killed themselves.
   *
   * @param event the EntityDamageEvent triggered when a player takes damage
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getEntity();
    if (players.contains(player)) {
      if (event.getFinalDamage() >= player.getHealth()) {
        event.setCancelled(true);
        completed.add(player.getUniqueId());
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.sendMessage(
            Component.text(
                "You have completed the challenge by killing yourself!", NamedTextColor.GREEN));
      }
    }
  }
}
