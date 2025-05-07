package com.storytimeproductions.models;

import java.util.List;
import java.util.UUID;

/**
 * Represents a quest that a player can complete in the game. A quest contains a name, description
 * (lore), item requirements, rewards, and optional player-specific participation conditions.
 */
public class Quest {

  private final String id;
  private final String name;
  private final String lore;
  private final List<String> itemRequirements; // Example: ["OAK_LOG:10"]
  private final List<String> rewards; // Example: ["MONEY:100", "XP:50"]
  private final List<UUID> requiredPlayers; // Empty if quest is for everyone

  /**
   * Constructs a new Quest instance.
   *
   * @param id unique identifier of the quest
   * @param name display name of the quest
   * @param lore lore or description of the quest
   * @param itemRequirements list of item requirements, formatted as "ITEM:AMOUNT"
   * @param rewards list of rewards, formatted as "TYPE:AMOUNT"
   * @param requiredPlayers list of UUIDs of players required to complete this quest; empty if quest
   *     is open to all
   */
  public Quest(
      String id,
      String name,
      String lore,
      List<String> itemRequirements,
      List<String> rewards,
      List<UUID> requiredPlayers) {
    this.id = id;
    this.name = name;
    this.lore = lore;
    this.itemRequirements = itemRequirements;
    this.rewards = rewards;
    this.requiredPlayers = requiredPlayers;
  }

  /**
   * Gets the unique identifier of the quest.
   *
   * @return the quest ID
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the display name of the quest.
   *
   * @return the quest name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the lore or description of the quest.
   *
   * @return the quest lore
   */
  public String getLore() {
    return lore;
  }

  /**
   * Gets the list of item requirements.
   *
   * @return a list of requirements formatted as "ITEM:AMOUNT"
   */
  public List<String> getItemRequirements() {
    return itemRequirements;
  }

  /**
   * Gets the list of rewards for completing the quest.
   *
   * @return a list of rewards formatted as "TYPE:AMOUNT"
   */
  public List<String> getRewards() {
    return rewards;
  }

  /**
   * Gets the list of players required to complete this quest.
   *
   * @return a list of player UUIDs or an empty list if the quest is open to all
   */
  public List<UUID> getRequiredPlayers() {
    return requiredPlayers;
  }

  /**
   * Determines if the quest is available to all players (i.e., not limited to specific players).
   *
   * @return true if the quest is open to all players, false if restricted to specific players
   */
  public boolean isDefaultQuest() {
    return requiredPlayers == null || requiredPlayers.isEmpty();
  }

  /**
   * Checks whether a specific player is eligible to undertake the quest.
   *
   * @param playerUuid the UUID of the player
   * @return true if the quest is either default or specifically assigned to the player
   */
  public boolean isAvailableTo(UUID playerUuid) {
    return isDefaultQuest() || requiredPlayers.contains(playerUuid);
  }

  /**
   * Returns a string representation of the quest, including ID, name, lore, requirements, rewards,
   * and required players.
   *
   * @return a string representation of this quest
   */
  @Override
  public String toString() {
    return "Quest{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", lore='"
        + lore
        + '\''
        + ", itemRequirements="
        + itemRequirements
        + ", rewards="
        + rewards
        + ", requiredPlayers="
        + requiredPlayers
        + '}';
  }
}
