package com.storytimeproductions.stweaks.playtime;

/**
 * Represents the playtime data for a player. Stores the available seconds a player has and their
 * AFK (away-from-keyboard) status.
 */
public class PlaytimeData {

  private Long afkSince;
  private boolean isAfk;
  private double availableSeconds;
  private int bankedTickets = 0;
  private Integer lastHourChecked = null;
  private boolean kickOnAfkTimeout = false;

  /**
   * Constructs a PlaytimeData object with a predefined number of available seconds.
   *
   * @param availableSeconds The initial amount of available seconds.
   */
  public PlaytimeData(double availableSeconds) {
    this.availableSeconds = availableSeconds;
  }

  /** Constructs a PlaytimeData object with zero available seconds and not AFK. */
  public PlaytimeData() {
    availableSeconds = 3600;
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
  public boolean addAvailableSeconds(double seconds) {
    this.availableSeconds += seconds;
    if (this.availableSeconds < 0) {
      this.availableSeconds = 0;
    }
    return true;
  }

  /**
   * Sets the total available seconds for the player.
   *
   * @param seconds The new total available seconds.
   */
  public void setAvailableSeconds(double seconds) {
    this.availableSeconds = seconds;
  }

  /**
   * Retrieves the total available seconds for the player.
   *
   * @return The total available seconds as a long.
   */
  public double getAvailableSeconds() {
    return availableSeconds;
  }

  /**
   * Retrieves the number of full minutes available.
   *
   * @return The total available minutes.
   */
  public double getAvailableMinutes() {
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

  /**
   * Gets the number of banked 5-minute tickets for the player.
   *
   * @return The number of banked tickets.
   */
  public int getBankedTickets() {
    return bankedTickets;
  }

  /**
   * Sets the number of banked 5-minute tickets for the player.
   *
   * @param tickets The number of tickets to set.
   */
  public void setBankedTickets(int tickets) {
    this.bankedTickets = tickets;
  }

  /**
   * Gets the last hour checked for daily playtime reset.
   *
   * @return the last checked hour, or null if never set
   */
  public Integer getLastHourChecked() {
    return lastHourChecked;
  }

  /**
   * Sets the last hour checked for daily playtime reset.
   *
   * @param hour the hour to set
   */
  public void setLastHourChecked(Integer hour) {
    this.lastHourChecked = hour;
  }

  /**
   * Checks if the player should be kicked on AFK timeout.
   *
   * @return {@code true} if the player should be kicked on AFK timeout, {@code false} otherwise.
   */
  public boolean isKickOnAfkTimeout() {
    return kickOnAfkTimeout;
  }

  /**
   * Sets whether the player should be kicked on AFK timeout.
   *
   * @param kick {@code true} to kick the player on AFK timeout, {@code false}
   */
  public void setKickOnAfkTimeout(boolean kick) {
    this.kickOnAfkTimeout = kick;
  }
}
