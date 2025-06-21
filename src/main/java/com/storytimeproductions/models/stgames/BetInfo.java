package com.storytimeproductions.models.stgames;

/**
 * Represents a bet placed in a game, containing information about the table, color, slot, and
 * whether the bet is locked.
 */
public class BetInfo {
  public int table;
  public String color;
  public int slot;
  public boolean locked;

  /**
   * Constructor to create a BetInfo object with the specified table, color, and slot.
   *
   * @param table the table number
   * @param color the color of the bet (e.g., "red", "black")
   * @param slot the slot number
   */
  public BetInfo(int table, String color, int slot) {
    this.table = table;
    this.color = color;
    this.slot = slot;
    this.locked = false;
  }
}
