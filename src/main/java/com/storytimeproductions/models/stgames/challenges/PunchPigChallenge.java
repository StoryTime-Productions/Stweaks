package com.storytimeproductions.models.stgames.challenges;

import com.storytimeproductions.models.stgames.Cuboid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

/**
 * A challenge where players must punch five named pigs. Each player must punch all five pigs once
 * to complete the challenge. All pigs are despawned on cleanup.
 */
public class PunchPigChallenge implements StoryBlitzChallenge, Listener {
  private final String description = "Punch all five pigs!";
  private final Set<UUID> completed = new HashSet<>();
  private final Map<UUID, Set<UUID>> playerPunchedPigs = new HashMap<>();
  private final List<Pig> pigs = new ArrayList<>();
  private Plugin plugin;
  private final List<Location> spawnLocations = new ArrayList<>();
  private final Random random = new Random();

  /**
   * Constructs a PunchPigChallenge using spawn regions.
   *
   * @param spawnRegions List of Cuboid regions to spawn pigs in
   */
  public PunchPigChallenge(List<Cuboid> spawnRegions) {
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
   * Starts the challenge by spawning five named pigs and registering the event listener.
   *
   * @param players The list of players participating in the challenge
   */
  @Override
  public void start(List<Player> players) {
    completed.clear();
    playerPunchedPigs.clear();
    pigs.clear();

    // Pick 5 random positions from spawnLocations for the pigs
    List<Location> pigSpawns = new ArrayList<>();
    if (spawnLocations.size() >= 5) {
      List<Location> copy = new ArrayList<>(spawnLocations);
      for (int i = 0; i < 5; i++) {
        int idx = random.nextInt(copy.size());
        pigSpawns.add(copy.remove(idx));
      }
    } else if (!spawnLocations.isEmpty()) {
      // Not enough unique spots, just reuse
      for (int i = 0; i < 5; i++) {
        pigSpawns.add(spawnLocations.get(random.nextInt(spawnLocations.size())));
      }
    } else {
      // Fallback: use world's spawn
      Location fallback = Bukkit.getWorlds().get(0).getSpawnLocation();
      for (int i = 0; i < 5; i++) {
        pigSpawns.add(fallback);
      }
    }

    // Summon 5 named pigs at chosen locations
    for (int i = 0; i < 5; i++) {
      final int pigIndex = i;
      Location pigLoc = pigSpawns.get(pigIndex);
      Pig pig =
          pigLoc
              .getWorld()
              .spawn(
                  pigLoc,
                  Pig.class,
                  spawnedPig -> {
                    spawnedPig.customName(Component.text("Punch Me!"));
                    spawnedPig.setCustomNameVisible(true);
                    spawnedPig.setInvulnerable(false);
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
   * Checks if the player has punched all five pigs.
   *
   * @param player The player to check
   * @return true if the player has punched all pigs, false otherwise
   */
  @Override
  public boolean isCompleted(Player player) {
    Set<UUID> punched = playerPunchedPigs.get(player.getUniqueId());
    return punched != null && punched.size() == pigs.size();
  }

  /**
   * Cleans up the challenge by despawning all pigs and unregistering the event listener.
   *
   * @param players The list of players who participated in the challenge
   */
  @Override
  public void cleanup(List<Player> players) {
    HandlerList.unregisterAll(this);
    completed.clear();
    playerPunchedPigs.clear();
    // Despawn all pigs
    for (Pig pig : pigs) {
      if (pig != null && !pig.isDead()) {
        pig.remove();
      }
    }
    pigs.clear();
  }

  /**
   * Handles pig punch events. Tracks which pigs each player has punched and prevents pig death.
   *
   * @param event The EntityDamageByEntityEvent triggered when a player punches a pig
   */
  @EventHandler
  public void onPigPunch(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Pig)) {
      return;
    }
    Pig pig = (Pig) event.getEntity();
    if (!pigs.contains(pig)) {
      return;
    }
    if (!(event.getDamager() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getDamager();

    // Track which pigs this player has punched
    playerPunchedPigs
        .computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
        .add(pig.getUniqueId());

    // Optional: feedback
    player.sendMessage(Component.text("You punched pig #" + (pigs.indexOf(pig) + 1) + "!"));

    // Prevent pig from dying
    event.setCancelled(true);
    double maxHealth = pig.getAttribute(Attribute.MAX_HEALTH).getValue();
    pig.setHealth(maxHealth);
  }
}
