package com.storytimeproductions.models.stgames.challenges;

import com.storytimeproductions.models.stgames.Cuboid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

/** A challenge where players must climb a spawned ladder tower and reach the top. */
public class LadderClimbChallenge implements StoryBlitzChallenge, Listener {
  private final Set<UUID> completed = new HashSet<>();
  private final String description = "Climb the ladder and reach the top!";
  private Location towerBase = null;
  private int towerHeight = 5;
  private Cuboid region;
  private Plugin plugin;
  private List<Player> players;
  private Location topPlatformCenter = null;

  /**
   * Constructor for the LadderClimbChallenge.
   *
   * @param spawnRegions the spawnRegions from which the tower will be spawned in.
   */
  public LadderClimbChallenge(List<Cuboid> spawnRegions) {
    this.plugin = Bukkit.getPluginManager().getPlugin("stweaks");
    if (!spawnRegions.isEmpty()) {
      this.region = spawnRegions.get(0);
      // Use the lowest y-level for the base
      int minY = Math.min(region.y1, region.y2);
      int centerX = (region.x1 + region.x2) / 2;
      int centerZ = (region.z1 + region.z2) / 2;
      World world = region.world;
      towerBase = new Location(world, centerX, minY, centerZ);
      topPlatformCenter = new Location(world, centerX, minY + towerHeight + 1, centerZ);
    }
  }

  @Override
  public String getDescription() {
    return getDescriptionWithGoob(description);
  }

  @Override
  public void start(List<Player> players) {
    this.players = players;
    completed.clear();
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

    // Teleport all players to the base of the tower
    if (towerBase != null) {
      for (Player p : players) {
        p.teleport(towerBase.clone().add(0, 1, 0));
        p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        p.setFoodLevel(20);
      }
    }
  }

  @Override
  public boolean isCompleted(Player player) {
    return completed.contains(player.getUniqueId());
  }

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

  /** Handles player movement to check if they have reached the top of the ladder. */
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (players == null || !players.contains(player)) {
      return;
    }
    if (completed.contains(player.getUniqueId())) {
      return;
    }
    if (topPlatformCenter != null) {
      Location loc = event.getTo();
      // Check if player is standing on the platform (within 1 block radius)
      if (loc.getWorld().equals(topPlatformCenter.getWorld())
          && Math.abs(loc.getBlockX() - topPlatformCenter.getBlockX()) <= 1
          && Math.abs(loc.getBlockY() - topPlatformCenter.getBlockY()) <= 1
          && Math.abs(loc.getBlockZ() - topPlatformCenter.getBlockZ()) <= 1) {
        completed.add(player.getUniqueId());
        player.sendMessage("You reached the top of the ladder!");
      }
    }
  }
}
