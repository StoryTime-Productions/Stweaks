package com.storytimeproductions.stweaks.games.hunt;

/** Represents the different game modes available in the Hunt system. */
public enum HuntGameMode {
  PROP_HUNT("Prop Hunt", "Hide as props and avoid detection"),
  IMPOSTER_HUNT("Imposter Hunt", "Find the imposters among us"),
  NEXTBOT_HUNT("NextBot Hunt", "Survive the relentless pursuit");

  private final String displayName;
  private final String description;

  HuntGameMode(String displayName, String description) {
    this.displayName = displayName;
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }
}
