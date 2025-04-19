package com.storytimeproductions.stweaks.playtime;

/** Represents the playtime data for a player. */
public class PlaytimeData {
  private double secondsPlayed; // Store total seconds played
  private boolean isAfk;

  /**
   * Adds seconds to the player's total time played.
   *
   * @param seconds The seconds played to add.
   */
  public void addSeconds(double seconds) {
    this.secondsPlayed += seconds;
  }

  /**
   * Retrieves the total seconds played by the player (rounded down).
   *
   * @return The total seconds played.
   */
  public long getTotalSecondsPlayed() {
    return (long) secondsPlayed;
  }

  /**
   * Retrieves the number of full minutes played.
   *
   * @return The total minutes played.
   */
  public long getMinutesPlayed() {
    return (long) (secondsPlayed / 60);
  }

  /**
   * Retrieves the remaining seconds after full minutes.
   *
   * @return The seconds part after minutes.
   */
  public long getRemainingSeconds() {
    return (long) secondsPlayed % 60;
  }

  /**
   * Checks if the player is currently marked as AFK.
   *
   * @return true if the player is AFK, false otherwise.
   */
  public boolean isAfk() {
    return isAfk;
  }

  /**
   * Sets the player's AFK status.
   *
   * @param afk true if the player is AFK, false otherwise.
   */
  public void setAfk(boolean afk) {
    isAfk = afk;
  }
}
