package com.storytimeproductions.stweaks.playtime;

import java.time.LocalDate;

/**
 * Represents the playtime data for a player. Stores the available seconds a player has and their
 * AFK (away-from-keyboard) status.
 */
public class PlaytimeData {

  private Long afkSince;

  /** Indicates whether the player is currently AFK (away from keyboard). */
  private boolean isAfk;

  private LocalDate lastHourGrantDate;
  private long availableSeconds;

  /**
   * Constructs a PlaytimeData object with a predefined number of available seconds.
   *
   * @param availableSeconds The initial amount of available seconds.
   */
  public PlaytimeData(long availableSeconds) {
    this.availableSeconds = availableSeconds;
  }

  /** Constructs a PlaytimeData object with zero available seconds and not AFK. */
  public PlaytimeData() {
    availableSeconds = 0;
    isAfk = false;
  }

  /** Resets the available seconds to zero. */
  public void reset() {
    this.availableSeconds = 0;
  }

  /**
   * Adds or subtracts seconds from the player's available time.
   *
   * @param seconds The seconds to add (can be negative).
   */
  public void addAvailableSeconds(long seconds) {
    this.availableSeconds += seconds;
  }

  /**
   * Retrieves the total available seconds for the player.
   *
   * @return The total available seconds as a long.
   */
  public long getAvailableSeconds() {
    return availableSeconds;
  }

  /**
   * Retrieves the number of full minutes available.
   *
   * @return The total available minutes.
   */
  public long getAvailableMinutes() {
    return availableSeconds / 60;
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
   * @return The time in milliseconds since the player was last marked AFK, or {@code null} if the
   *     player is not currently AFK.
   */
  public Long getAfkSince() {
    return afkSince;
  }

  public LocalDate getLastHourGrantDate() {
    return lastHourGrantDate;
  }

  /**
   * Sets the date when the player was last granted an hour of playtime.
   *
   * @param date The date to set as the last hour grant date.
   */
  public void setLastHourGrantDate(LocalDate date) {
    this.lastHourGrantDate = date;
  }
}
