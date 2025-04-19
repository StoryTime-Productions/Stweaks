package com.storytimeproductions.stweaks.playtime;

/**
 * Represents the playtime data for a player. Stores the total number of seconds a player has
 * actively played and their AFK (away-from-keyboard) status.
 */
public class PlaytimeData {

  /** Total number of seconds the player has played. */
  private double secondsPlayed;

  /** Indicates whether the player is currently AFK (away from keyboard). */
  private boolean isAfk;

  /**
   * Constructs a PlaytimeData object with a predefined number of seconds played.
   *
   * @param seconds The initial amount of seconds played.
   */
  public PlaytimeData(double seconds) {
    secondsPlayed = seconds;
  }

  /** Constructs a PlaytimeData object with zero seconds played and not AFK. */
  public PlaytimeData() {
    secondsPlayed = 0;
    isAfk = false;
  }

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
   * @return The total seconds played as a long.
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
   * @return The seconds part after converting to full minutes.
   */
  public long getRemainingSeconds() {
    return (long) secondsPlayed % 60;
  }

  /**
   * Checks if the player is currently marked as AFK.
   *
   * @return {@code true} if the player is AFK, {@code false} otherwise.
   */
  public boolean isAfk() {
    return isAfk;
  }

  /**
   * Sets the player's AFK status.
   *
   * @param afk {@code true} to mark the player as AFK, {@code false} otherwise.
   */
  public void setAfk(boolean afk) {
    isAfk = afk;
  }
}
