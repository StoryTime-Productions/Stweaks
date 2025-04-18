package com.storytimeproductions.stweaks.playtime;

/**
 * Represents the playtime data for a player.
 *
 * <p>This class stores the amount of time a player has played and whether the player is marked as
 * AFK (Away From Keyboard). It provides methods to update and retrieve the player's playtime and
 * AFK status.
 */
public class PlaytimeData {
  private double minutesPlayed;
  private boolean isAfk;

  /**
   * Adds one minute of playtime to the player's total minutes played, factoring in a multiplier.
   *
   * <p>This method increments the minutes played by one minute, multiplied by the provided
   * multiplier. This allows for adjusting the playtime based on conditions like weekends or special
   * events.
   *
   * @param multiplier The multiplier to apply to the playtime.
   */
  public void addMinute(double multiplier) {
    minutesPlayed += 1 * multiplier;
  }

  /**
   * Retrieves the total number of minutes the player has played.
   *
   * @return The total minutes played by the player.
   */
  public double getMinutesPlayed() {
    return minutesPlayed;
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
   * <p>This method updates the player's AFK status, marking them as either AFK or not AFK.
   *
   * @param afk true if the player is AFK, false otherwise.
   */
  public void setAfk(boolean afk) {
    isAfk = afk;
  }
}
