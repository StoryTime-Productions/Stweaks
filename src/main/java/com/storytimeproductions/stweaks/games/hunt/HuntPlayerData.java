package com.storytimeproductions.stweaks.games.hunt;

import java.util.UUID;

/** Represents a player's preferences and selections for joining a Hunt game. */
public class HuntPlayerData {
  private final UUID playerId;
  private HuntTeam selectedTeam;
  private HunterClass selectedHunterClass;
  private HiderClass selectedHiderClass;
  private HuntMap preferredMap;
  private HuntGameMode preferredGameMode;
  private boolean isReady;
  private String selectedDisguise;
  private String selectedDisguiseSkin; // Store the actual skin name (e.g., "FoxyTheFierce")

  /**
   * Constructs a new HuntPlayerData for the given player UUID.
   *
   * @param playerId The UUID of the player
   */
  public HuntPlayerData(UUID playerId) {
    this.playerId = playerId;
    this.isReady = false;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public HuntTeam getSelectedTeam() {
    return selectedTeam;
  }

  public void setSelectedTeam(HuntTeam selectedTeam) {
    this.selectedTeam = selectedTeam;
  }

  public HunterClass getSelectedHunterClass() {
    return selectedHunterClass;
  }

  public void setSelectedHunterClass(HunterClass selectedHunterClass) {
    this.selectedHunterClass = selectedHunterClass;
  }

  public HiderClass getSelectedHiderClass() {
    return selectedHiderClass;
  }

  public void setSelectedHiderClass(HiderClass selectedHiderClass) {
    this.selectedHiderClass = selectedHiderClass;
  }

  public HuntMap getPreferredMap() {
    return preferredMap;
  }

  public void setPreferredMap(HuntMap preferredMap) {
    this.preferredMap = preferredMap;
  }

  public HuntGameMode getPreferredGameMode() {
    return preferredGameMode;
  }

  public void setPreferredGameMode(HuntGameMode preferredGameMode) {
    this.preferredGameMode = preferredGameMode;
  }

  public boolean isReady() {
    return isReady;
  }

  public void setReady(boolean ready) {
    this.isReady = ready;
  }

  /** Checks if the player has made all required selections to be ready. */
  public boolean hasValidSelections() {
    return selectedTeam != null
        && preferredMap != null
        && preferredGameMode != null
        && ((selectedTeam == HuntTeam.HUNTERS && selectedHunterClass != null)
            || (selectedTeam == HuntTeam.HIDERS && selectedHiderClass != null));
  }

  /**
   * Gets the selected disguise display name for this player (used for re-applying disguises).
   *
   * @return The disguise display name, or null if none selected.
   */
  public String getSelectedDisguise() {
    return selectedDisguise;
  }

  /**
   * Sets the selected disguise display name for this player (should be called when a disguise is
   * picked).
   *
   * @param disguise The disguise display name (e.g., "Slenderman")
   */
  public void setSelectedDisguise(String disguise) {
    this.selectedDisguise = disguise;
  }

  /**
   * Gets the selected disguise skin name for this player (the actual Minecraft skin).
   *
   * @return The disguise skin name (e.g., "FoxyTheFierce"), or null if none selected.
   */
  public String getSelectedDisguiseSkin() {
    return selectedDisguiseSkin;
  }

  /**
   * Sets the selected disguise skin name for this player (the actual Minecraft skin).
   *
   * @param skin The player skin name to use for the disguise (e.g., "FoxyTheFierce")
   */
  public void setSelectedDisguiseSkin(String skin) {
    this.selectedDisguiseSkin = skin;
  }
}
