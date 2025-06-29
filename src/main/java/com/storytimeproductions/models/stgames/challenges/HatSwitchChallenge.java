package com.storytimeproductions.models.stgames.challenges;

import com.storytimeproductions.models.stgames.Cuboid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * A challenge where players must open a chest and equip the golden helmet inside. The challenge
 * spawns chests in specified regions, and players must complete the challenge by equipping the
 * golden helmet.
 */
public class HatSwitchChallenge implements StoryBlitzChallenge, Listener {
  private final String description = "Equip the golden helmet!";
  private final Set<UUID> completed = new HashSet<>();
  private final List<Location> spawnLocations = new ArrayList<>();
  private final List<Location> chestLocations = new ArrayList<>();
  private Plugin plugin;

  /**
   * Constructs a HatSwitchChallenge using the provided spawn regions.
   *
   * @param spawnRegions List of Cuboid regions to spawn chests in
   */
  public HatSwitchChallenge(List<Cuboid> spawnRegions) {
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
    chestLocations.clear();

    int frameCount = Math.max(1, players.size() - 1);
    List<Location> available = new ArrayList<>(spawnLocations);
    Collections.shuffle(available);

    if (plugin == null) {
      plugin = Bukkit.getPluginManager().getPlugin("stweaks");
    }

    for (int i = 0; i < frameCount && !available.isEmpty(); i++) {
      Location frameLoc = available.remove(0).clone();
      // Place the item frame on the floor (facing up)
      frameLoc.setY(frameLoc.getBlockY()); // Place above the ground
      frameLoc
          .getWorld()
          .spawn(
              frameLoc,
              ItemFrame.class,
              f -> {
                f.setFacingDirection(BlockFace.UP, true);
                f.setItem(new ItemStack(Material.GOLDEN_HELMET));
                f.setFixed(true);
              });
      // Store the frame's location for cleanup
      chestLocations.add(frameLoc);
    }

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public boolean isCompleted(Player player) {
    ItemStack helmet = player.getInventory().getHelmet();
    return helmet != null && helmet.getType() == Material.GOLDEN_HELMET;
  }

  @Override
  public void cleanup(List<Player> players) {
    HandlerList.unregisterAll(this);
    completed.clear();
    // Remove all spawned item frames
    for (Location loc : chestLocations) {
      for (Entity entity : loc.getWorld().getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
        if (entity instanceof ItemFrame) {
          entity.remove();
        }
      }
    }
    chestLocations.clear();

    for (Location regionLoc : spawnLocations) {
      for (Entity entity : regionLoc.getWorld().getNearbyEntities(regionLoc, 1.5, 2.5, 1.5)) {
        if (entity instanceof ItemFrame) {
          entity.remove();
        }
      }
    }

    for (Player player : players) {
      ItemStack helmet = player.getInventory().getHelmet();
      if (helmet != null && helmet.getType() == Material.GOLDEN_HELMET) {
        player.getInventory().setHelmet(null);
      }
    }
  }

  /** Handles when a player equips the golden helmet from a chest. */
  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getWhoClicked();
    // Check if the player is equipping a golden helmet
    ItemStack currentItem = event.getCurrentItem();
    if (currentItem != null && currentItem.getType() == Material.GOLDEN_HELMET) {
      Bukkit.getScheduler()
          .runTaskLater(
              plugin,
              () -> {
                if (isCompleted(player)) {
                  completed.add(player.getUniqueId());
                }
              },
              1L);
    }
  }

  /**
   * Handles player interaction with item frames containing a golden helmet.
   *
   * @param event The PlayerInteractEntityEvent triggered when a player interacts with an entity
   */
  @EventHandler
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof ItemFrame)) {
      return;
    }
    ItemFrame frame = (ItemFrame) event.getRightClicked();
    // Only allow if the frame contains a golden helmet
    ItemStack item = frame.getItem();
    if (item != null && item.getType() == Material.GOLDEN_HELMET) {
      // Remove the item from the frame and give it to the player if their hand is
      // empty
      Player player = event.getPlayer();
      if (player.getInventory().firstEmpty() != -1) {
        frame.setItem(null);
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_HELMET));
        event.setCancelled(true);
      }
    }
  }
}
