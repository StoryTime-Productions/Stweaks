package com.storytimeproductions.models.stgames.challenges;

import com.storytimeproductions.models.stgames.Cuboid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * A challenge where players must shear a sheep. The number of sheep is the number of alive players
 * minus one. All sheep, wool (in inventory and on the ground), and shears are removed on cleanup.
 */
public class ShearSheepChallenge implements StoryBlitzChallenge, Listener {
  private final String description = "Shear a sheep!";
  private final Set<UUID> completed = new HashSet<>();
  private final List<Location> spawnLocations = new ArrayList<>();
  private final List<Sheep> sheepList = new ArrayList<>();
  private final Random random = new Random();
  private Plugin plugin;
  private List<Cuboid> spawnRegions = new ArrayList<>();

  /**
   * Constructs a ShearSheepChallenge using the provided spawn regions.
   *
   * @param spawnRegions List of Cuboid regions to spawn sheep in
   */
  public ShearSheepChallenge(List<Cuboid> spawnRegions) {
    this.spawnRegions = spawnRegions;
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
    sheepList.clear();

    int sheepCount = Math.max(1, players.size() - 1);
    List<Location> available = new ArrayList<>(spawnLocations);
    Collections.shuffle(available, random);

    // Spawn sheep at random locations
    for (int i = 0; i < sheepCount && !available.isEmpty(); i++) {
      Location loc = available.remove(0);
      Sheep sheep = loc.getWorld().spawn(loc, Sheep.class, s -> s.setSheared(false));
      sheepList.add(sheep);
    }

    // Give each player a shear
    for (Player p : players) {
      if (!p.getInventory().contains(Material.SHEARS)) {
        p.getInventory().addItem(new ItemStack(Material.SHEARS));
      }
    }

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

    // Remove all sheep
    for (Sheep sheep : sheepList) {
      if (sheep != null && !sheep.isDead()) {
        sheep.remove();
      }
    }
    sheepList.clear();

    // Remove all wool and shears from players' inventories
    for (Player p : players) {
      // Remove all wool colors
      for (Material mat : Material.values()) {
        if (mat.name().endsWith("_WOOL")) {
          p.getInventory().remove(mat);
        }
      }
      // Remove shears
      p.getInventory().remove(Material.SHEARS);
    }

    // Remove all dropped wool items within the spawn regions
    for (Cuboid region : spawnRegions) {
      World world = region.world;
      for (Entity entity : world.getEntities()) {
        if (entity instanceof Item) {
          Item item = (Item) entity;
          ItemStack stack = item.getItemStack();
          if (stack != null && stack.getType().name().endsWith("_WOOL")) {
            if (region.contains(item.getLocation())) {
              item.remove();
            }
          }
        }
      }
    }
  }

  /**
   * Handles the event when a player shears a sheep.
   *
   * @param event The PlayerShearEntityEvent triggered when a player shears a sheep
   */
  @EventHandler
  public void onPlayerShearSheep(PlayerShearEntityEvent event) {
    if (!(event.getEntity() instanceof Sheep)) {
      return;
    }
    Sheep sheep = (Sheep) event.getEntity();
    if (!sheepList.contains(sheep)) {
      return;
    }
    Player player = event.getPlayer();
    completed.add(player.getUniqueId());
  }
}
