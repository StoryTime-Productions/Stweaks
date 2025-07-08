package com.storytimeproductions.stweaks.games.hunt;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Represents the different classes available for hunters. */
public enum HunterClass {
  BRUTE(
      "Brute",
      "Heavy assault with powerful weapons",
      new ItemStack(Material.IRON_AXE), // Melee - War Axe
      new ItemStack(Material.CROSSBOW), // Ranged - Heavy Crossbow
      new ItemStack(Material.TOTEM_OF_UNDYING), // Utility - Resilience Totem
      new ItemStack(Material.IRON_CHESTPLATE),
      0.8f,
      1.5f),

  NIMBLE(
      "Nimble",
      "Swift strikes and quick movement",
      new ItemStack(Material.IRON_SWORD), // Melee - Swift Blade
      new ItemStack(Material.BOW), // Ranged - Hunter's Bow
      new ItemStack(Material.FEATHER), // Utility - Dash
      new ItemStack(Material.LEATHER_CHESTPLATE),
      1.3f,
      0.7f),

  SABOTEUR(
      "Saboteur",
      "Disrupts and reveals hidden enemies",
      new ItemStack(Material.IRON_HOE), // Melee - Saboteur's Rod (non-block-breaking)
      new ItemStack(Material.FISHING_ROD), // Ranged - Grappling Hook
      new ItemStack(Material.ENDER_EYE), // Utility - Scanner
      new ItemStack(Material.CHAINMAIL_CHESTPLATE),
      1.0f,
      1.0f);

  private final String displayName;
  private final String description;
  private final ItemStack meleeWeapon;
  private final ItemStack rangedWeapon;
  private final ItemStack utilityItem;
  private final ItemStack armor;
  private final float speedModifier;
  private final float damageModifier;

  HunterClass(
      String displayName,
      String description,
      ItemStack meleeWeapon,
      ItemStack rangedWeapon,
      ItemStack utilityItem,
      ItemStack armor,
      float speedModifier,
      float damageModifier) {
    this.displayName = displayName;
    this.description = description;
    this.meleeWeapon = meleeWeapon;
    this.rangedWeapon = rangedWeapon;
    this.utilityItem = utilityItem;
    this.armor = armor;
    this.speedModifier = speedModifier;
    this.damageModifier = damageModifier;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public ItemStack getMeleeWeapon() {
    return meleeWeapon.clone();
  }

  public ItemStack getRangedWeapon() {
    return rangedWeapon.clone();
  }

  public ItemStack getUtilityItem() {
    return utilityItem.clone();
  }

  public ItemStack getArmor() {
    return armor.clone();
  }

  public float getSpeedModifier() {
    return speedModifier;
  }

  public float getDamageModifier() {
    return damageModifier;
  }

  /**
   * Gets the weapon for this hunter class.
   *
   * @deprecated Use getMeleeWeapon() instead
   */
  @Deprecated
  public ItemStack getWeapon() {
    return getMeleeWeapon();
  }
}
