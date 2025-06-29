package com.storytimeproductions.models.stgames.challenges;

import com.storytimeproductions.models.stgames.Cuboid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Represents a challenge where players must craft a specific item. */
public class CraftItemChallenge implements StoryBlitzChallenge {

  private final String description;
  private final Material targetItem;
  private final Map<Material, Integer> requiredIngredients;
  private final List<Material> distractorItems;
  private final Random random = new Random();
  private final List<Location> spawnLocations = new ArrayList<>();
  private Location craftingTableLocation = null;

  private static final Map<Material, Map<Material, Integer>> RECIPES = new HashMap<>();

  static {
    RECIPES.put(Material.CRAFTING_TABLE, Map.of(Material.OAK_PLANKS, 4));
    RECIPES.put(Material.STICK, Map.of(Material.OAK_PLANKS, 2));
    RECIPES.put(Material.TORCH, Map.of(Material.COAL, 1, Material.STICK, 1));
    RECIPES.put(Material.STONE_PICKAXE, Map.of(Material.STICK, 2, Material.COBBLESTONE, 3));
    RECIPES.put(Material.FURNACE, Map.of(Material.COBBLESTONE, 8));
    RECIPES.put(Material.BREAD, Map.of(Material.WHEAT, 3));
    RECIPES.put(Material.CHEST, Map.of(Material.OAK_PLANKS, 8));
    RECIPES.put(Material.LEVER, Map.of(Material.COBBLESTONE, 1, Material.STICK, 1));
  }

  /**
   * Constructs a new CraftItemChallenge with a random item and its ingredients.
   *
   * @param spawnRegions List of Cuboid regions to spawn crafting table in.
   */
  public CraftItemChallenge(List<Cuboid> spawnRegions) {
    // Pick a random craftable item
    List<Material> craftables = new ArrayList<>(RECIPES.keySet());
    this.targetItem = craftables.get(random.nextInt(craftables.size()));
    this.requiredIngredients = RECIPES.get(targetItem);

    // Pick some distractor items (not needed for this recipe)
    Set<Material> allMaterials = new HashSet<>();
    for (Map<Material, Integer> recipe : RECIPES.values()) {
      allMaterials.addAll(recipe.keySet());
    }
    allMaterials.removeAll(requiredIngredients.keySet());
    distractorItems = new ArrayList<>(allMaterials);

    this.description = "Craft a " + targetItem.name().replace("_", " ").toLowerCase();

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
  }

  @Override
  public String getDescription() {
    return getDescriptionWithGoob(description);
  }

  @Override
  public void start(List<Player> players) {
    // Give each player the required ingredients and distractors
    for (Player p : players) {
      for (Material mat : requiredIngredients.keySet()) {
        p.getInventory().remove(mat);
      }
      for (Map.Entry<Material, Integer> entry : requiredIngredients.entrySet()) {
        p.getInventory().addItem(new ItemStack(entry.getKey(), entry.getValue()));
      }
      Collections.shuffle(distractorItems, random);
      int distractorCount = 2 + random.nextInt(3);
      for (int i = 0; i < distractorCount && i < distractorItems.size(); i++) {
        Material mat = distractorItems.get(i);
        p.getInventory().addItem(new ItemStack(mat, 1 + random.nextInt(3)));
      }
    }

    // Place a single crafting table at a random lowest y-level location
    if (!spawnLocations.isEmpty()) {
      Collections.shuffle(spawnLocations, random);
      craftingTableLocation = spawnLocations.get(0);
      Block block = craftingTableLocation.getBlock();
      block.setType(Material.CRAFTING_TABLE);
    }
  }

  @Override
  public boolean isCompleted(Player player) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.getType() == targetItem && item.getAmount() > 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void cleanup(List<Player> players) {
    // Remove all challenge-related items and crafting table
    for (Player p : players) {
      p.getInventory().remove(targetItem);
      for (Material mat : requiredIngredients.keySet()) {
        p.getInventory().remove(mat);
      }
      for (Material mat : distractorItems) {
        p.getInventory().remove(mat);
      }
    }
    if (craftingTableLocation != null) {
      Block block = craftingTableLocation.getBlock();
      if (block.getType() == Material.CRAFTING_TABLE) {
        block.setType(Material.AIR);
      }
      craftingTableLocation = null;
    }
  }
}
