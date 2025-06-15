package com.storytimeproductions.models.stgames.challenges;

import java.util.List;
import org.bukkit.entity.Player;

/**
 * Represents a challenge where players must break a block. This class implements the
 * StoryBlitzChallenge interface.
 */
public class BreakBlockChallenge implements StoryBlitzChallenge {

  private final String description;

  /**
   * Constructs a new BreakBlockChallenge with the specified description.
   *
   * @param description The description of the challenge (shown to players).
   */
  public BreakBlockChallenge(String description) {
    this.description = description;
  }

  /**
   * Checks if the player has completed the challenge.
   *
   * @param player The player to check.
   * @return true if the player has completed the challenge, false otherwise.
   */
  @Override
  public boolean isCompleted(Player player) {
    return false;
  }

  /**
   * Returns the description of the challenge.
   *
   * @return The description of the challenge.
   */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * Starts the challenge for the given players.
   *
   * @param players The players in the game.
   */
  @Override
  public void start(List<Player> players) {
    // Optional: implement challenge setup logic
  }

  /**
   * Cleans up resources when the challenge ends.
   *
   * @param players The players in the game.
   */
  @Override
  public void cleanup(List<Player> players) {
    // Optional: implement challenge cleanup logic
  }
}
