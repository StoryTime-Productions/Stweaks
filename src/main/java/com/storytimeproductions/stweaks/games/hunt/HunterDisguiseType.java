package com.storytimeproductions.stweaks.games.hunt;

/** Represents the different disguise types and their passive abilities for hunters. */
public enum HunterDisguiseType {
  SPRINGTRAP("Springtrap", "Springtrap", "Enhanced speed burst when chasing targets"),

  HEROBRINE("Herobrine", "Herobrine", "Night vision and reduced fall damage"),

  SLENDERMAN("Slenderman", "Slenderman", "Temporary invisibility when standing still"),

  CRYPTID("Cryptid", "Cryptid", "Enhanced tracking abilities and silent movement"),

  JIGSAW("Jigsaw", "Jigsaw", "Trap immunity and reduced damage from environmental hazards"),

  SCARECROW("Scarecrow", "Scarecrow", "Fear aura that slows nearby hiders");

  private final String skinName;
  private final String displayName;
  private final String passiveDescription;

  HunterDisguiseType(String skinName, String displayName, String passiveDescription) {
    this.skinName = skinName;
    this.displayName = displayName;
    this.passiveDescription = passiveDescription;
  }

  public String getSkinName() {
    return skinName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getPassiveDescription() {
    return passiveDescription;
  }

  /**
   * Gets the disguise type by skin name.
   *
   * @param skinName The skin name to match
   * @return The matching disguise type, or null if not found
   */
  public static HunterDisguiseType fromSkinName(String skinName) {
    for (HunterDisguiseType type : values()) {
      if (type.skinName.equalsIgnoreCase(skinName)) {
        return type;
      }
    }
    return null;
  }

  /**
   * Gets the disguise type by display name.
   *
   * @param displayName The display name to match
   * @return The matching disguise type, or null if not found
   */
  public static HunterDisguiseType fromDisplayName(String displayName) {
    for (HunterDisguiseType type : values()) {
      if (type.displayName.equalsIgnoreCase(displayName)) {
        return type;
      }
    }
    return null;
  }
}
