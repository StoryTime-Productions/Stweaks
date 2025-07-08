package com.storytimeproductions.stweaks.games.hunt;

/** Represents the different maps available for Hunt games. */
public enum HuntMap {
  CASTLE("Castle", "Medieval castle with towers and dungeons"),
  MOUNTAIN("Mountain", "Rocky mountain terrain with caves and cliffs"),
  MEDIEVAL("Medieval", "Ancient medieval town with cobblestone streets"),
  INDUSTRIAL("Industrial", "Industrial complex with machinery and warehouses"),
  OFFICE("Office", "Modern office building with cubicles and meeting rooms");

  private final String displayName;
  private final String description;

  HuntMap(String displayName, String description) {
    this.displayName = displayName;
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public String getWorldName() {
    return "hunt_" + name().toLowerCase();
  }
}
