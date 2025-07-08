package com.storytimeproductions.stweaks.games.hunt;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages item kits for Hunt game classes. Handles giving and removing class-specific items to
 * players.
 */
public class HuntKitManager {
  /**
   * Applies hunter abilities (e.g., potion effects, attribute modifiers) for the given class.
   * Called after teleporting to the map.
   */
  public void applyHunterAbilities(Player player, HunterClass hunterClass) {
    // Don't apply abilities to spectators
    if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
      plugin
          .getLogger()
          .info("[DEBUG] Skipped applying hunter abilities to spectator: " + player.getName());
      return;
    }

    plugin
        .getLogger()
        .info(
            "[DEBUG] Applying hunter abilities for "
                + player.getName()
                + " as "
                + hunterClass.name());

    // Clear any existing potion effects first to avoid conflicts
    player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

    // Example: Give speed, strength, or other effects based on class
    switch (hunterClass) {
      case BRUTE:
        player.addPotionEffect(
            new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
        plugin
            .getLogger()
            .info("[DEBUG] Applied STRENGTH effect to Brute hunter: " + player.getName());
        break;
      case NIMBLE:
        player.addPotionEffect(
            new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        plugin
            .getLogger()
            .info("[DEBUG] Applied SPEED effect to Nimble hunter: " + player.getName());
        break;
      case SABOTEUR:
        player.addPotionEffect(
            new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.NIGHT_VISION,
                Integer.MAX_VALUE,
                0,
                false,
                false));
        plugin
            .getLogger()
            .info("[DEBUG] Applied NIGHT_VISION effect to Saboteur hunter: " + player.getName());
        break;
      default:
        plugin.getLogger().warning("[DEBUG] Unknown hunter class for abilities: " + hunterClass);
        break;
    }
  }

  /**
   * Applies hider passives (e.g., potion effects) for the given class. Called after teleporting to
   * the map.
   */
  public void applyHiderPassives(Player player, HiderClass hiderClass) {
    // Don't apply passives to spectators
    if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
      return;
    }

    switch (hiderClass) {
      case TRICKSTER:
        player.addPotionEffect(
            new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.LUCK, Integer.MAX_VALUE, 0, false, false));
        break;
      case PHASER:
        player.addPotionEffect(
            new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE,
                Integer.MAX_VALUE,
                0,
                false,
                false));
        break;
      case CLOAKER:
        player.addPotionEffect(
            new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.INVISIBILITY,
                20 * 10,
                0,
                false,
                false)); // 10s invis as example
        break;
      default:
        break;
    }
  }

  private final JavaPlugin plugin;

  /**
   * Constructs a new HuntKitManager.
   *
   * @param plugin The JavaPlugin instance
   */
  public HuntKitManager(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Gives a player the kit for their selected hunter class.
   *
   * @param player The player to give the kit to
   * @param hunterClass The hunter class
   */
  public void giveHunterKit(Player player, HunterClass hunterClass) {
    clearPlayerInventory(player);

    // Give all three items with custom names: melee, ranged, utility
    player
        .getInventory()
        .addItem(
            createNamedItem(
                hunterClass.getMeleeWeapon(),
                getMeleeWeaponName(hunterClass),
                getMeleeWeaponLore(hunterClass)));
    player
        .getInventory()
        .addItem(
            createNamedItem(
                hunterClass.getRangedWeapon(),
                getRangedWeaponName(hunterClass),
                getRangedWeaponLore(hunterClass)));
    player
        .getInventory()
        .addItem(
            createNamedItem(
                hunterClass.getUtilityItem(),
                getUtilityItemName(hunterClass),
                getUtilityItemLore(hunterClass)));

    // Note: Hunters do not receive armor to maintain fair gameplay balance

    // Special case: Give arrows for bow users (infinite arrows via game mechanics)
    if (hunterClass == HunterClass.NIMBLE) {
      player.getInventory().addItem(new ItemStack(org.bukkit.Material.ARROW, 1));
    }

    // Apply speed and damage modifiers (this would be handled by the game logic)
    // For now, we'll just log it
    plugin
        .getLogger()
        .info("Gave " + player.getName() + " the " + hunterClass.getDisplayName() + " hunter kit");
  }

  /**
   * Gives a player the kit for their selected hider class.
   *
   * @param player The player to give the kit to
   * @param hiderClass The hider class
   */
  public void giveHiderKit(Player player, HiderClass hiderClass) {
    clearPlayerInventory(player);

    // Give utility item with custom name and lore
    player
        .getInventory()
        .addItem(
            createNamedItem(
                hiderClass.getTool(),
                getHiderUtilityItemName(hiderClass),
                getHiderUtilityItemLore(hiderClass)));

    // Note: Hiders do not receive armor to maintain stealth gameplay

    plugin
        .getLogger()
        .info("Gave " + player.getName() + " the " + hiderClass.getDisplayName() + " hider kit");
  }

  /**
   * Removes all kit items from a player's inventory.
   *
   * @param player The player to clear
   */
  public void removePlayerKit(Player player) {
    clearPlayerInventory(player);
    plugin.getLogger().info("Removed kit from " + player.getName());
  }

  /**
   * Clears a player's inventory completely.
   *
   * @param player The player whose inventory to clear
   */
  private void clearPlayerInventory(Player player) {
    player.getInventory().clear();
    player.getInventory().setHelmet(null);
    player.getInventory().setChestplate(null);
    player.getInventory().setLeggings(null);
    player.getInventory().setBoots(null);
  }

  /**
   * Checks if a player currently has any hunt kit items.
   *
   * @param player The player to check
   * @return true if the player has any items that appear to be from a hunt kit
   */
  public boolean hasKitItems(Player player) {
    // Check for any items in inventory or armor slots
    ItemStack[] contents = player.getInventory().getContents();
    for (ItemStack item : contents) {
      if (item != null) {
        return true;
      }
    }

    ItemStack[] armor = player.getInventory().getArmorContents();
    for (ItemStack item : armor) {
      if (item != null) {
        return true;
      }
    }

    return false;
  }

  /**
   * Creates a named item with custom display name and lore.
   *
   * @param item The base item
   * @param displayName The display name
   * @param lore The lore lines
   * @return The customized item
   */
  private ItemStack createNamedItem(ItemStack item, String displayName, String[] lore) {
    ItemStack namedItem = item.clone();
    ItemMeta meta = namedItem.getItemMeta();
    if (meta != null) {
      // Use MiniMessage to parse the display name
      MiniMessage mm = MiniMessage.miniMessage();
      Component nameComponent =
          mm.deserialize(displayName).decoration(TextDecoration.ITALIC, false);
      meta.displayName(nameComponent);

      if (lore.length > 0) {
        Component[] loreComponents = new Component[lore.length];
        for (int i = 0; i < lore.length; i++) {
          loreComponents[i] = mm.deserialize(lore[i]).decoration(TextDecoration.ITALIC, false);
        }
        meta.lore(Arrays.asList(loreComponents));
      }
      namedItem.setItemMeta(meta);
    }
    return namedItem;
  }

  /** Gets the display name for a hunter class melee weapon. */
  private String getMeleeWeaponName(HunterClass hunterClass) {
    switch (hunterClass) {
      case BRUTE:
        return "<red><bold>War Axe</bold></red>";
      case NIMBLE:
        return "<green><bold>Swift Blade</bold></green>";
      case SABOTEUR:
        return "<yellow><bold>Saboteur's Rod</bold></yellow>";
      default:
        return "<gray>Unknown Weapon</gray>";
    }
  }

  /** Gets the lore for a hunter class melee weapon. */
  private String[] getMeleeWeaponLore(HunterClass hunterClass) {
    switch (hunterClass) {
      case BRUTE:
        return new String[] {
          "<gray>A heavy axe that deals</gray>", "<gray>devastating damage to enemies.</gray>"
        };
      case NIMBLE:
        return new String[] {
          "<gray>A lightweight sword perfect</gray>", "<gray>for quick, precise strikes.</gray>"
        };
      case SABOTEUR:
        return new String[] {
          "<gray>A specialized tool for</gray>", "<gray>disrupting enemy operations.</gray>"
        };
      default:
        return new String[] {"<gray>Unknown weapon</gray>"};
    }
  }

  /** Gets the display name for a hunter class ranged weapon. */
  private String getRangedWeaponName(HunterClass hunterClass) {
    switch (hunterClass) {
      case BRUTE:
        return "<red><bold>Heavy Crossbow</bold></red>";
      case NIMBLE:
        return "<green><bold>Hunter's Bow</bold></green>";
      case SABOTEUR:
        return "<yellow><bold>Grappling Hook</bold></yellow>";
      default:
        return "<gray>Unknown Weapon</gray>";
    }
  }

  /** Gets the lore for a hunter class ranged weapon. */
  private String[] getRangedWeaponLore(HunterClass hunterClass) {
    switch (hunterClass) {
      case BRUTE:
        return new String[] {
          "<gray>A powerful crossbow that</gray>", "<gray>pierces through armor.</gray>"
        };
      case NIMBLE:
        return new String[] {
          "<gray>A precise bow for accurate</gray>", "<gray>long-range shots.</gray>"
        };
      case SABOTEUR:
        return new String[] {
          "<gray>Can be used to pull enemies</gray>", "<gray>or traverse terrain.</gray>"
        };
      default:
        return new String[] {"<gray>Unknown weapon</gray>"};
    }
  }

  /** Gets the display name for a hunter class utility item. */
  private String getUtilityItemName(HunterClass hunterClass) {
    switch (hunterClass) {
      case BRUTE:
        return "<red><bold>Resilience Totem</bold></red>";
      case NIMBLE:
        return "<green><bold>Dash</bold></green>";
      case SABOTEUR:
        return "<yellow><bold>Scanner</bold></yellow>";
      default:
        return "<gray>Unknown Item</gray>";
    }
  }

  /** Gets the lore for a hunter class utility item. */
  private String[] getUtilityItemLore(HunterClass hunterClass) {
    switch (hunterClass) {
      case BRUTE:
        return new String[] {
          "<gray>Right-click to activate</gray>",
          "<gray>temporary resistance and</gray>",
          "<gray>regeneration effects.</gray>"
        };
      case NIMBLE:
        return new String[] {
          "<gray>Right-click to dash forward</gray>", "<gray>with incredible speed.</gray>"
        };
      case SABOTEUR:
        return new String[] {
          "<gray>Right-click to reveal nearby</gray>", "<gray>hiders for a short time.</gray>"
        };
      default:
        return new String[] {"<gray>Unknown ability</gray>"};
    }
  }

  /** Gets the display name for a hider class utility item. */
  private String getHiderUtilityItemName(HiderClass hiderClass) {
    switch (hiderClass) {
      case TRICKSTER:
        return "<dark_purple><bold>Tripwire Trap</bold></dark_purple>";
      case PHASER:
        return "<aqua><bold>Phase Pearl</bold></aqua>";
      case CLOAKER:
        return "<gray><bold>Invisibility Potion</bold></gray>";
      default:
        return "<gray>Unknown Item</gray>";
    }
  }

  /** Gets the lore for a hider class utility item. */
  private String[] getHiderUtilityItemLore(HiderClass hiderClass) {
    switch (hiderClass) {
      case TRICKSTER:
        return new String[] {
          "<gray>Right-click to place a</gray>",
          "<gray>tripwire trap that will</gray>",
          "<gray>slow trapped hunters and</gray>",
          "<gray>highlight them for you.</gray>"
        };
      case PHASER:
        return new String[] {
          "<gray>Right-click while facing a wall</gray>",
          "<gray>to teleport through it to</gray>",
          "<gray>the other side. Perfect for</gray>",
          "<gray>quick escapes!</gray>"
        };
      case CLOAKER:
        return new String[] {
          "<gray>Right-click to become</gray>",
          "<gray>invisible for a short time.</gray>",
          "<gray>Stay hidden from hunters!</gray>"
        };
      default:
        return new String[] {"<gray>Unknown ability</gray>"};
    }
  }
}
