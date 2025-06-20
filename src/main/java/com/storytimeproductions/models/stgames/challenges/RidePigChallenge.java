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
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;

/**
 * A challenge where players must mount a pig with a saddle. The challenge is completed when a
 * player successfully mounts any of the spawned pigs.
 */
public class RidePigChallenge implements StoryBlitzChallenge, Listener {
  private final String description = "Mount a pig with a saddle!";
  private final Set<UUID> completed = new HashSet<>();
  private final List<Pig> pigs = new ArrayList<>();
  private final List<Location> spawnLocations = new ArrayList<>();
  private final Random random = new Random();
  private Plugin plugin;
  private int pigCount = 0;

  /**
   * Constructs a RidePigChallenge using the provided spawn regions.
   *
   * @param spawnRegions List of Cuboid regions to spawn pigs in
   */
  public RidePigChallenge(List<Cuboid> spawnRegions) {
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

  /**
   * Gets the description of the challenge.
   *
   * @return Challenge description
   */
  @Override
  public String getDescription() {
    return getDescriptionWithGoob(description);
  }

  /**
   * Starts the challenge by spawning pigs at random locations.
   *
   * @param players List of players participating in the challenge
   */
  @Override
  public void start(List<Player> players) {
    completed.clear();
    pigs.clear();
    pigCount = Math.max(1, players.size() - 1);

    // Pick random spawn locations for pigs
    List<Location> pigSpawns = new ArrayList<>();
    if (spawnLocations.size() >= pigCount) {
      List<Location> copy = new ArrayList<>(spawnLocations);
      for (int i = 0; i < pigCount; i++) {
        int idx = random.nextInt(copy.size());
        pigSpawns.add(copy.remove(idx));
      }
    } else if (!spawnLocations.isEmpty()) {
      for (int i = 0; i < pigCount; i++) {
        pigSpawns.add(spawnLocations.get(random.nextInt(spawnLocations.size())));
      }
    } else {
      Location fallback = Bukkit.getWorlds().get(0).getSpawnLocation();
      for (int i = 0; i < pigCount; i++) {
        pigSpawns.add(fallback);
      }
    }

    // Spawn pigs with saddles and AI enabled
    for (Location loc : pigSpawns) {
      Pig pig =
          loc.getWorld()
              .spawn(
                  loc,
                  Pig.class,
                  spawnedPig -> {
                    spawnedPig.setSaddle(true);
                    spawnedPig.setAI(true);
                  });
      pigs.add(pig);
    }

    // Register event listener
    if (plugin == null) {
      plugin = Bukkit.getPluginManager().getPlugin("stweaks");
    }
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  /**
   * Checks if the challenge is completed for a player.
   *
   * @param player The player to check
   * @return true if the player has completed the challenge, false otherwise
   */
  @Override
  public boolean isCompleted(Player player) {
    return completed.contains(player.getUniqueId());
  }

  /** Cleans up the challenge by unregistering the event listener and removing all pigs. */
  @Override
  public void cleanup(List<Player> players) {
    HandlerList.unregisterAll(this);
    completed.clear();
    // Despawn all pigs
    for (Pig pig : pigs) {
      if (pig != null && !pig.isDead()) {
        pig.remove();
      }
    }
    pigs.clear();
  }

  /**
   * Handles the event when a player mounts a pig.
   *
   * @param event The PlayerInteractEntityEvent triggered when a player interacts with a pig
   */
  @EventHandler
  public void onPlayerMountPig(PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof Pig)) {
      return;
    }
    Pig pig = (Pig) event.getRightClicked();
    if (!pigs.contains(pig)) {
      return;
    }
    Player player = event.getPlayer();
    // Mark as completed when player mounts a pig
    if (!completed.contains(player.getUniqueId())) {
      completed.add(player.getUniqueId());
      player.sendMessage("You mounted a pig!");
    }
  }
}
