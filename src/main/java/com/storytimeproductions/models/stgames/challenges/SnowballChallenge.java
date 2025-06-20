package com.storytimeproductions.models.stgames.challenges;

import com.storytimeproductions.models.stgames.Cuboid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * A challenge where players must hit another player with a snowball. Players are given a stack of
 * snowballs. When a player hits another player with a snowball, they are marked as completed and
 * their snowballs are removed. On cleanup, all snowballs on the ground and in inventories are
 * removed.
 */
public class SnowballChallenge implements StoryBlitzChallenge, Listener {
  private final String description = "Hit another player with a snowball!";
  private final Set<UUID> completed = new HashSet<>();
  private final List<Cuboid> spawnRegions = new ArrayList<>();
  private Plugin plugin;

  /**
   * Constructs a SnowballChallenge using the provided spawn regions.
   *
   * @param spawnRegions List of Cuboid regions to spawn snowballs in
   */
  public SnowballChallenge(List<Cuboid> spawnRegions) {
    this.spawnRegions.addAll(spawnRegions);
  }

  @Override
  public String getDescription() {
    return getDescriptionWithGoob(description);
  }

  @Override
  public void start(List<Player> players) {
    completed.clear();
    // Give each player a stack of snowballs
    for (Player p : players) {
      p.getInventory().addItem(new ItemStack(Material.SNOWBALL, 16));
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
    // Remove all snowballs from players' inventories
    for (Player p : players) {
      p.getInventory().remove(Material.SNOWBALL);
    }
    // Remove all dropped snowballs within the spawn regions
    for (Cuboid region : spawnRegions) {
      World world = region.world;
      for (Entity entity : world.getEntities()) {
        if (entity instanceof Item) {
          Item item = (Item) entity;
          ItemStack stack = item.getItemStack();
          if (stack != null && stack.getType() == Material.SNOWBALL) {
            if (region.contains(item.getLocation())) {
              item.remove();
            }
          }
        }
      }
    }
  }

  /** Handles when a player is hit by a snowball thrown by another player. */
  @EventHandler
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player)) {
      return;
    }
    if (!(event.getDamager() instanceof Snowball)) {
      return;
    }
    Snowball snowball = (Snowball) event.getDamager();
    if (!(snowball.getShooter() instanceof Player)) {
      return;
    }
    Player shooter = (Player) snowball.getShooter();
    if (completed.contains(shooter.getUniqueId())) {
      return;
    }
    completed.add(shooter.getUniqueId());
    shooter.getInventory().remove(Material.SNOWBALL);
    shooter.sendMessage("You hit a player with a snowball!");
  }
}
