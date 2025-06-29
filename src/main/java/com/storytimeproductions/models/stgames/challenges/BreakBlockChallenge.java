package com.storytimeproductions.models.stgames.challenges;

import com.storytimeproductions.models.stgames.Cuboid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Represents a challenge where players must break a block. This class implements the
 * StoryBlitzChallenge interface.
 */
public class BreakBlockChallenge implements StoryBlitzChallenge {

  private final String description;
  private final int requiredCount;
  private final Material targetBlockType;
  private final List<Location> spawnLocations = new ArrayList<>();
  private final Map<UUID, Integer> brokenCount = new HashMap<>();
  private final Set<Location> challengeBlocks = new HashSet<>();
  private BukkitRunnable cleanupTask;
  private Listener blockBreakListener;
  private final ItemStack challengePickaxe;

  /**
   * Constructs a new BreakBlockChallenge with a random block type and count.
   *
   * @param spawnRegions List of Cuboid regions to spawn blocks in.
   */
  public BreakBlockChallenge(List<Cuboid> spawnRegions) {
    this.description = "Break a block!";

    // Randomly pick required count between 1 and 5
    this.requiredCount = 1 + new Random().nextInt(5);

    // Define block type groups
    List<Material[]> groups =
        Arrays.asList(
            // Concretes
            new Material[] {
              Material.WHITE_CONCRETE, Material.ORANGE_CONCRETE,
              Material.MAGENTA_CONCRETE, Material.LIGHT_BLUE_CONCRETE,
              Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
              Material.PINK_CONCRETE, Material.GRAY_CONCRETE,
              Material.LIGHT_GRAY_CONCRETE, Material.CYAN_CONCRETE,
              Material.PURPLE_CONCRETE, Material.BLUE_CONCRETE,
              Material.BROWN_CONCRETE, Material.GREEN_CONCRETE,
              Material.RED_CONCRETE, Material.BLACK_CONCRETE
            },
            // Terracottas
            new Material[] {
              Material.WHITE_TERRACOTTA, Material.ORANGE_TERRACOTTA,
              Material.MAGENTA_TERRACOTTA, Material.LIGHT_BLUE_TERRACOTTA,
              Material.YELLOW_TERRACOTTA, Material.LIME_TERRACOTTA,
              Material.PINK_TERRACOTTA, Material.GRAY_TERRACOTTA,
              Material.LIGHT_GRAY_TERRACOTTA, Material.CYAN_TERRACOTTA,
              Material.PURPLE_TERRACOTTA, Material.BLUE_TERRACOTTA,
              Material.BROWN_TERRACOTTA, Material.GREEN_TERRACOTTA,
              Material.RED_TERRACOTTA, Material.BLACK_TERRACOTTA
            },
            // Wools
            new Material[] {
              Material.WHITE_WOOL, Material.ORANGE_WOOL,
              Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL,
              Material.YELLOW_WOOL, Material.LIME_WOOL,
              Material.PINK_WOOL, Material.GRAY_WOOL,
              Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL,
              Material.PURPLE_WOOL, Material.BLUE_WOOL,
              Material.BROWN_WOOL, Material.GREEN_WOOL,
              Material.RED_WOOL, Material.BLACK_WOOL
            });

    // Randomly pick a group, then a color from that group
    Random rand = new Random();
    Material[] group = groups.get(rand.nextInt(groups.size()));
    this.targetBlockType = group[rand.nextInt(group.length)];

    int minY = Integer.MAX_VALUE;
    for (Cuboid cuboid : spawnRegions) {
      minY = Math.min(minY, cuboid.y1);
    }
    for (Cuboid cuboid : spawnRegions) {
      for (int x = cuboid.x1; x <= cuboid.x2; x++) {
        for (int z = cuboid.z1; z <= cuboid.z2; z++) {
          spawnLocations.add(new Location(cuboid.world, x, minY, z));
        }
      }
    }

    // Create the challenge pickaxe
    challengePickaxe = new ItemStack(Material.IRON_PICKAXE);
    ItemMeta meta = challengePickaxe.getItemMeta();
    meta.displayName(Component.text("Challenge Pickaxe"));
    challengePickaxe.setItemMeta(meta);
  }

  /**
   * Returns the description of the challenge.
   *
   * @return The description of the challenge.
   */
  @Override
  public String getDescription() {
    return description
        + " (Break "
        + requiredCount
        + " "
        + targetBlockType.name().replace("_", " ").toLowerCase()
        + ")";
  }

  /**
   * Starts the challenge for the given players.
   *
   * @param players The players in the game.
   */
  @Override
  public void start(List<Player> players) {
    // Only use the lowest y-level among all spawnLocations
    if (spawnLocations.isEmpty()) {
      return;
    }

    List<Location> lowestLayer = new ArrayList<>(spawnLocations);

    Collections.shuffle(lowestLayer);
    challengeBlocks.clear();

    // Prepare a list of indices for target blocks
    List<Integer> indices = new ArrayList<>();
    for (int i = 0; i < lowestLayer.size(); i++) {
      indices.add(i);
    }
    Collections.shuffle(indices);

    // Place target blocks at random positions, rest filler
    int totalTargets = requiredCount * (players.size() - 1);
    Set<Integer> targetIndices =
        new HashSet<>(indices.subList(0, Math.min(totalTargets, indices.size())));
    List<Material> fillerBlocks =
        Arrays.asList(Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.SAND);

    for (int i = 0; i < lowestLayer.size(); i++) {
      Location loc = lowestLayer.get(i);
      Block block = loc.getBlock();
      if (targetIndices.contains(i)) {
        block.setType(targetBlockType);
        challengeBlocks.add(loc);
      } else {
        block.setType(fillerBlocks.get(new Random().nextInt(fillerBlocks.size())));
      }
    }
    brokenCount.clear();

    // Give each player a pickaxe
    for (Player p : players) {
      if (!p.getInventory().contains(challengePickaxe)) {
        p.getInventory().addItem(challengePickaxe.clone());
      }
    }

    // Register block break listener
    Plugin plugin = Bukkit.getPluginManager().getPlugin("stweaks");
    blockBreakListener =
        new Listener() {
          @EventHandler
          public void onBlockBreak(BlockBreakEvent event) {
            Player player = event.getPlayer();
            Block block = event.getBlock();
            if (challengeBlocks.contains(block.getLocation())
                && block.getType() == targetBlockType) {
              // Give the block to the player
              player.getInventory().addItem(new ItemStack(targetBlockType, 1));
              // Track progress
              brokenCount.put(
                  player.getUniqueId(), brokenCount.getOrDefault(player.getUniqueId(), 0) + 1);
              challengeBlocks.remove(block.getLocation());
            }
          }
        };
    Bukkit.getPluginManager().registerEvents(blockBreakListener, plugin);

    // Prepare cleanup task
    cleanupTask =
        new BukkitRunnable() {
          @Override
          public void run() {
            for (Location loc : challengeBlocks) {
              Block block = loc.getBlock();
              if (block.getType() == targetBlockType) {
                block.setType(Material.AIR);
              }
            }
            challengeBlocks.clear();
          }
        };
  }

  /**
   * Checks if the player has completed the challenge.
   *
   * @param player The player to check.
   * @return true if the player has completed the challenge, false otherwise.
   */
  @Override
  public boolean isCompleted(Player player) {
    return brokenCount.getOrDefault(player.getUniqueId(), 0) >= requiredCount;
  }

  /**
   * Cleans up resources when the challenge ends.
   *
   * @param players The players in the game.
   */
  @Override
  public void cleanup(List<Player> players) {
    // Remove any remaining challenge blocks (both target and filler)
    for (Location loc : spawnLocations) {
      Block block = loc.getBlock();
      block.setType(Material.AIR);
    }
    challengeBlocks.clear();
    brokenCount.clear();

    // Remove the pickaxe and target block from all players
    for (Player p : players) {
      p.getInventory().remove(Material.IRON_PICKAXE);
      p.getInventory().remove(targetBlockType);
    }

    // Unregister the block break listener if needed
    if (blockBreakListener != null) {
      BlockBreakEvent.getHandlerList().unregister(blockBreakListener);
      blockBreakListener = null;
    }

    // Cancel and clear cleanup task if running
    if (cleanupTask != null) {
      try {
        cleanupTask.cancel();
      } catch (IllegalStateException ignored) {
        // Task may have already been cancelled or completed
      }
      cleanupTask = null;
    }
  }
}
