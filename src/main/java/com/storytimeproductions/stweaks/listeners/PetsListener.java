package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.models.Pet;
import com.storytimeproductions.stweaks.util.PetsManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener that manages pet activity, hunger, and perks for players.
 *
 * <p>This class tracks which pets are "active" (present in a player's hotbar or offhand with the
 * correct model), manages their hunger, periodically gives rewards or plays ambient effects, and
 * handles feeding events.
 *
 * <ul>
 *   <li>Pets can provide either item or potion effect perks.
 *   <li>Pets must be fed to remain active and provide perks.
 *   <li>Ambient sounds/messages and rewards are given at random intervals.
 * </ul>
 */
public class PetsListener implements Listener {

  private final Map<UUID, Map<String, Integer>> petHunger = new HashMap<>();
  private PetsManager petsManager;

  // Hunger settings
  private static final int MAX_HUNGER = 5;
  private static final int HUNGER_COST_PER_ACTION = 1;

  /**
   * Constructs the PetsListener and starts the periodic pet action task.
   *
   * @param plugin The JavaPlugin instance.
   * @param petsManager The PetsManager for looking up pets.
   */
  public PetsListener(JavaPlugin plugin, PetsManager petsManager) {
    this.petsManager = petsManager;

    // Periodic task for pet actions
    new BukkitRunnable() {
      @Override
      public void run() {
        try {
          for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            String petId = null;
            if (offhand != null && isPetItem(offhand)) {
              Pet pet = petsManager.getPetByItem(offhand);
              if (pet != null) {
                petId = pet.getId();

                // Initialize hunger if needed
                petHunger
                    .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .putIfAbsent(petId, MAX_HUNGER);

                int hunger = petHunger.get(player.getUniqueId()).get(petId);
                if (hunger > 0) {
                  // Randomly decide to give item or play sound/message
                  if (shouldGiveItem()) {
                    givePetReward(player, pet);
                    decreaseHunger(player, petId);
                  } else if (shouldPlayAmbient()) {
                    playAmbientEffect(player, pet);
                  }

                  // Ensure hunger is defined before using it
                  if (petId != null) {
                    hunger = petHunger.get(player.getUniqueId()).getOrDefault(petId, 0);
                    String hungerState = getHungerState(hunger);
                    updatePetLore(offhand, hungerState);
                  }
                } else {
                  player.sendActionBar(
                      Component.text(
                          "Your "
                              + (petId != null
                                  ? petId.replace("_", " ").toLowerCase()
                                  : "unknown pet")
                              + " is hungry!"));
                }

                // Try to feed the pet automatically from inventory
                ItemStack foodStack = findAndConsumeFood(player, pet.getFood());
                if (foodStack != null) {
                  petHunger.get(player.getUniqueId()).put(petId, MAX_HUNGER);
                  player.sendMessage(
                      Component.text(
                          "Your "
                              + (petId != null
                                  ? petId.replace("_", " ").toLowerCase()
                                  : "unknown pet")
                              + " ate some "
                              + pet.getFood().name().toLowerCase()
                              + "!"));
                }
              }
            }

            // Ensure hunger is defined before using it
            Map<String, Integer> hungerMap = petHunger.get(player.getUniqueId());
            int hunger = 0;
            if (hungerMap != null && petId != null) {
              hunger = hungerMap.getOrDefault(petId, 0);
            }
            String hungerState = getHungerState(hunger);
            updatePetLore(offhand, hungerState);
          }
        } catch (Exception e) {
          Bukkit.getLogger().severe("Error in PetsListener task: " + e.getMessage());
          e.printStackTrace();
        }
      }
    }.runTaskTimer(plugin, 40L, 40L);
  }

  private String getHungerState(int hunger) {
    if (hunger >= 4) {
      return "Full";
    }
    if (hunger >= 2) {
      return "Peckish";
    }
    if (hunger == 1) {
      return "Hungry";
    }
    return "Starving";
  }

  private void updatePetLore(ItemStack item, String hungerState) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return;
    }
    List<Component> lore = meta.lore() != null ? meta.lore() : new ArrayList<>();
    // Remove old hunger line if present
    lore.removeIf(line -> line.toString().contains("Hunger:"));

    // Determine color based on hunger state
    TextColor color;
    switch (hungerState) {
      case "Full" -> color = NamedTextColor.GREEN;
      case "Peckish" -> color = NamedTextColor.YELLOW;
      case "Hungry" -> color = NamedTextColor.GOLD;
      default -> color = NamedTextColor.RED;
    }

    // Compose the lore line: "Hunger: " (white) + state (italic, colored)
    Component hungerComponent =
        Component.text("Hunger: ", NamedTextColor.WHITE)
            .append(Component.text(hungerState, color, TextDecoration.ITALIC));
    lore.add(hungerComponent);

    // Add favorite food line if not present
    boolean hasFoodLine = lore.stream().anyMatch(line -> line.toString().contains("I eat"));
    if (!hasFoodLine) {
      // Try to get the pet's food type from the persistent data or fallback
      String foodName = "unknown food";
      if (isPetItem(item)) {
        Pet pet = null;
        // Try to get the petId from the item
        NamespacedKey itemModel = meta.getItemModel();
        if (itemModel != null && itemModel.getNamespace().equals("hotbar_pets")) {
          String petId = itemModel.getKey();
          pet = petsManager.getPetById(petId);
        }
        if (pet != null && pet.getFood() != null) {
          foodName = formatMaterialName(pet.getFood().name());
        }
      }
      lore.add(
          Component.text("I eat " + foodName + "!", NamedTextColor.GRAY, TextDecoration.ITALIC));
    }

    meta.lore(lore);
    item.setItemMeta(meta);
  }

  // Helper to format material names nicely
  private String formatMaterialName(String materialName) {
    String formatted = materialName.replace('_', ' ').toLowerCase();
    String[] words = formatted.split(" ");
    StringBuilder sb = new StringBuilder();
    for (String word : words) {
      if (!word.isEmpty()) {
        sb.append(Character.toUpperCase(word.charAt(0)));
        if (word.length() > 1) {
          sb.append(word.substring(1));
        }
        sb.append(" ");
      }
    }
    return sb.toString().trim();
  }

  /**
   * Checks if the item is a pet (has hotbar_pets model).
   *
   * @param item The ItemStack to check.
   * @return true if the item is a pet, false otherwise.
   */
  private boolean isPetItem(ItemStack item) {
    if (!item.hasItemMeta()) {
      return false;
    }

    ItemMeta meta = item.getItemMeta();
    NamespacedKey itemModel = meta.getItemModel();

    if (itemModel == null) {
      return false;
    }

    return itemModel.getNamespace().equals("hotbar_pets");
  }

  /**
   * Gives the pet's reward to the player (item or potion effect), and sends a random pet message.
   *
   * @param player The player to reward.
   * @param pet The pet providing the reward.
   */
  private void givePetReward(Player player, Pet pet) {
    Object perk = pet.getPerk();
    if (perk instanceof ItemStack reward) {
      player.getInventory().addItem(reward);
      // Send a random message or burp sound from the pet
      pet.sendRandomPetMessage(player);
    } else if (perk instanceof org.bukkit.potion.PotionEffect effect) {
      player.addPotionEffect(effect);
      pet.sendRandomPetMessage(player);
    } else if (perk instanceof String effectStr && effectStr.startsWith("effect:")) {
      // If perk is a string like "effect:SPEED"
      String effectName = effectStr.substring(7);
      PotionEffectType type =
          org.bukkit.Registry.POTION_EFFECT_TYPE.get(
              NamespacedKey.minecraft(effectName.toLowerCase()));
      if (type != null) {
        org.bukkit.potion.PotionEffect effect =
            new org.bukkit.potion.PotionEffect(type, 20 * 60, 0);
        player.addPotionEffect(effect);
        pet.sendRandomPetMessage(player);
      }
    }
  }

  /**
   * Plays a random burp sound and sends a random quote message from the pet.
   *
   * @param player The player to play the effect for.
   * @param pet The pet providing the effect.
   */
  private void playAmbientEffect(Player player, Pet pet) {
    List<String> burps = pet.getBurpSounds();
    if (!burps.isEmpty()) {
      String burp = burps.get(new Random().nextInt(burps.size()));
      player.playSound(player.getLocation(), burp, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
  }

  /**
   * Randomly decides if the pet should give an item (3-10 min interval).
   *
   * @return true if the pet should give an item, false otherwise.
   */
  private boolean shouldGiveItem() {
    return Math.random() < (1.0 / (3 + new Random().nextInt(8))); // ~1 per 3-10 mins
  }

  /**
   * Randomly decides if the pet should play an ambient effect.
   *
   * @return true if the pet should play an ambient effect, false otherwise.
   */
  private boolean shouldPlayAmbient() {
    return Math.random() < 0.1; // 10% chance per tick
  }

  /**
   * Decreases the hunger of the specified pet for the player, preventing negative values.
   *
   * @param player The player owning the pet.
   * @param petId The pet's unique identifier.
   */
  private void decreaseHunger(Player player, String petId) {
    Map<String, Integer> hungerMap = petHunger.get(player.getUniqueId());
    if (hungerMap != null) {
      hungerMap.put(petId, Math.max(0, hungerMap.get(petId) - HUNGER_COST_PER_ACTION));
    }
  }

  private ItemStack findAndConsumeFood(Player player, org.bukkit.Material foodType) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.getType() == foodType && item.getAmount() > 0) {
        item.setAmount(item.getAmount() - 1);
        return item;
      }
    }
    return null;
  }
}
