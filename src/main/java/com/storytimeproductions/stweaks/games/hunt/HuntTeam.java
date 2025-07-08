package com.storytimeproductions.stweaks.games.hunt;

/** Represents the teams players can join in Hunt games. */
public enum HuntTeam {
  HUNTERS("Hunters", "Find and eliminate the hiders"),
  HIDERS("Hiders", "Hide and survive until time runs out");

  private final String displayName;
  private final String description;

  HuntTeam(String displayName, String description) {
    this.displayName = displayName;
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public HuntTeam getOpposite() {
    return this == HUNTERS ? HIDERS : HUNTERS;
  }
}
