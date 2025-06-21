package com.storytimeproductions.models.stgames.challenges;

import com.storytimeproductions.models.stgames.Cuboid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

/** A challenge where players must ring a single bell placed in the spawn region. */
public class BellRingChallenge implements StoryBlitzChallenge, Listener {
  private final String description = "Ring the bell!";
  private final Set<UUID> completed = new HashSet<>();
  private final List<Location> spawnLocations = new ArrayList<>();
  private Location bellLocation = null;
  private Plugin plugin;

  /**
   * Constructs a BellRingChallenge using the provided spawn regions.
   *
   * @param spawnRegions List of Cuboid regions to spawn the bell in
   */
  public BellRingChallenge(List<Cuboid> spawnRegions) {
    // Collect all lowest layer locations from all cuboids
    for (Cuboid cuboid : spawnRegions) {
      int minY = Math.min(cuboid.y1, cuboid.y2);
      for (int x = Math.min(cuboid.x1, cuboid.x2); x <= Math.max(cuboid.x1, cuboid.x2); x++) {
        for (int z = Math.min(cuboid.z1, cuboid.z2); z <= Math.max(cuboid.z1, cuboid.z2); z++) {
          spawnLocations.add(new Location(cuboid.world, x, minY, z));
        }
      }
    }
  }

  @Override
  public String getDescription() {
    return getDescriptionWithGoob(description);
  }

  @Override
  public void start(List<Player> players) {
    completed.clear();
    // Place a bell at a random lowest-layer location
    if (!spawnLocations.isEmpty()) {
      Location loc = spawnLocations.get(new Random().nextInt(spawnLocations.size()));
      Block block = loc.getBlock();
      block.setType(Material.BELL);
      bellLocation = block.getLocation();
    }
    // Register event listener
    if (plugin == null) {
      plugin = Bukkit.getPluginManager().getPlugin("stweaks");
    }
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public boolean isCompleted(Player player) {
    return completed.contains(player.getUniqueId());
  }

  @Override
  public void cleanup(List<Player> players) {
    HandlerList.unregisterAll(this);
    completed.clear();
    // Remove the bell
    if (bellLocation != null) {
      Block block = bellLocation.getBlock();
      if (block.getType() == Material.BELL) {
        block.setType(Material.AIR);
      }
      bellLocation = null;
    }
  }

  /**
   * Handles player interaction with the bell.
   *
   * @param event The PlayerInteractEvent triggered when a player interacts with a block
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (bellLocation == null) {
      return;
    }
    if (event.getClickedBlock() == null) {
      return;
    }
    if (!event.getClickedBlock().getLocation().equals(bellLocation)) {
      return;
    }
    if (event.getClickedBlock().getType() != Material.BELL) {
      return;
    }
    Player player = event.getPlayer();
    completed.add(player.getUniqueId());
  }
}
