package com.storytimeproductions.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Material;

/**
 * Represents a quest that a player can complete in the game. A quest contains a name, description
 * (lore), item requirements, rewards, an optional deadline, and optional player-specific
 * participation conditions.
 */
public class Quest {

  private final String id;
  private final String name;
  private final String lore;
  private final List<String> itemRequirements;
  private final List<String> statRequirements;
  private final List<String> rewards;
  private final List<UUID> requiredPlayers; // Empty if quest is for everyone
  private final LocalDateTime deadline; // Null if no deadline
  private final Material icon;
  DateTimeFormatter displayFormatter =
      DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

  /**
   * Constructs a new Quest object with the specified parameters.
   *
   * @param id the unique identifier of the quest
   * @param name the display name of the quest
   * @param lore the lore or description of the quest
   * @param requirements a list of item requirements formatted as "ITEM:AMOUNT"
   * @param rewards a list of rewards formatted as "TYPE:AMOUNT"
   * @param requiredPlayers a list of player UUIDs required to complete this quest
   * @param deadline an optional deadline for the quest
   * @param icon the icon representing the quest in the GUI
   */
  public Quest(
      String id,
      String name,
      String lore,
      List<String> requirements,
      List<String> statRequirements,
      List<String> rewards,
      List<UUID> requiredPlayers,
      LocalDateTime deadline,
      Material icon) {
    this.id = id;
    this.name = name;
    this.lore = lore;
    this.itemRequirements = requirements;
    this.statRequirements = statRequirements;
    this.rewards = rewards;
    this.requiredPlayers = requiredPlayers;
    this.deadline = deadline;
    this.icon = icon;
  }

  /**
   * Gets the icon representing the quest in the GUI.
   *
   * @return the quest icon
   */
  public Material getIcon() {
    return icon;
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
   * Gets the list of stat requirements.
   *
   * @return a list of requirements formatted as "STAT:AMOUNT"
   */
  public List<String> getStatRequirements() {
    return statRequirements;
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
   * Gets the optional deadline for the quest.
   *
   * @return the deadline as a {@link LocalDateTime}, or null if the quest is indefinite
   */
  public String getDeadline() {
    if (deadline == null) {
      return null;
    }
    return deadline.format(displayFormatter);
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
   * Checks whether the quest has expired based on the current date and time.
   *
   * @return true if the current time is after the deadline, false otherwise (or if no deadline)
   */
  public boolean isExpired() {
    return deadline != null && deadline.isBefore(LocalDateTime.now());
  }

  /**
   * Returns a string representation of the quest, including ID, name, lore, requirements, rewards,
   * required players, and deadline.
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
        + ", deadline="
        + deadline
        + '}';
  }
}
