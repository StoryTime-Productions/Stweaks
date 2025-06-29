package com.storytimeproductions.models.stgames.challenges;

import com.storytimeproductions.models.stgames.Cuboid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/** Represents a challenge where players must empty their hotbar. */
public class EmptyInventoryChallenge implements StoryBlitzChallenge, Listener {
  private final String description = "Empty your hotbar!";
  private final Set<UUID> completed = new HashSet<>();
  private final Random random = new Random();
  private List<Cuboid> spawnRegions = new ArrayList<>();

  /** Constructs a new EmptyInventoryChallenge. */
  public EmptyInventoryChallenge(List<Cuboid> spawnRegions) {
    this.spawnRegions = spawnRegions;
  }

  /** Returns the name of the challenge. */
  @Override
  public String getDescription() {
    return getDescriptionWithGoob(description);
  }

  /** Returns the name of the challenge. */
  @Override
  public void start(List<Player> players) {
    completed.clear();

    // Fill each player's hotbar with random items
    Material[] materials = Material.values();
    for (Player p : players) {
      for (int slot = 0; slot < 9; slot++) {
        Material mat;
        do {
          mat = materials[random.nextInt(materials.length)];
        } while (!mat.isItem() || mat == Material.AIR);
        p.getInventory().setItem(slot, new ItemStack(mat, 1 + random.nextInt(16)));
      }
    }
  }

  /**
   * Handles the event when a player empties their hotbar.
   *
   * @param player the player who emptied their hotbar
   */
  @Override
  public boolean isCompleted(Player player) {
    // Hotbar slots are 0-8
    for (int slot = 0; slot < 9; slot++) {
      ItemStack item = player.getInventory().getItem(slot);
      if (item != null && item.getType() != Material.AIR) {
        return false;
      }
    }
    return true;
  }

  /** Prevents item drops and removes the item from the player's inventory instead. */
  @EventHandler
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    Player player = event.getPlayer();
    event.setCancelled(true);
    ItemStack dropped = event.getItemDrop().getItemStack();
    player.getInventory().removeItem(dropped);
  }

  /**
   * Cleans up the challenge by unregistering the event listener and clearing completed players.
   *
   * @param players the list of players who participated in the challenge
   */
  @Override
  public void cleanup(List<Player> players) {
    HandlerList.unregisterAll(this);
    completed.clear();
    // Optionally clear hotbar after challenge
    for (Player p : players) {
      for (int slot = 0; slot < 9; slot++) {
        p.getInventory().setItem(slot, null);
      }
    }
    // Remove all dropped items within the spawn regions
    for (Cuboid region : spawnRegions) {
      World world = region.world;
      for (org.bukkit.entity.Entity entity : world.getEntities()) {
        if (entity instanceof Item) {
          Location loc = entity.getLocation();
          if (region.contains(loc)) {
            entity.remove();
          }
        }
      }
    }
  }
}
