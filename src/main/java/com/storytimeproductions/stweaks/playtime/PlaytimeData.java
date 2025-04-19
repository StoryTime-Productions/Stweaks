package com.storytimeproductions.stweaks.playtime;

/**
 * Represents the playtime data for a player. Stores the total number of seconds a player has
 * actively played and their AFK (away-from-keyboard) status.
 */
public class PlaytimeData {

  /** Total number of seconds the player has played. */
  private double secondsPlayed;

  private Long afkSince;

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

  /**
   * Gets the time in milliseconds since the player was last marked as AFK.
   *
   * <p>This method returns the time that has passed since the player was last considered AFK. It
   * can be used to determine how long the player has been inactive. If the player is currently not
   * AFK, it may return {@code null} or a default value indicating no AFK status.
   *
   * @return The time in milliseconds since the player was last marked AFK, or {@code null} if the
   *     player is not currently AFK.
   */
  public Long getAfkSince() {
    return afkSince;
  }
}
