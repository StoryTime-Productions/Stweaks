package com.storytimeproductions.models.stgames.challenges;

import java.util.List;
import java.util.Random;
import org.bukkit.entity.Player;

/**
 * Represents a single challenge in StoryBlitz. Implement this interface to provide custom challenge
 * logic.
 */
public interface StoryBlitzChallenge {
  /**
   * Returns the description of the challenge, which is shown to players.
   *
   * @return The description of the challenge.
   */
  String getDescription();

  /**
   * Called when the challenge starts for the given players.
   *
   * @param players The players in the game.
   */
  default void start(List<Player> players) {
    // Optional: implement to set up challenge state, hints, etc.
  }

  /**
   * Checks if the player has completed the challenge.
   *
   * @param player The player to check.
   * @return true if the player has completed the challenge, false otherwise.
   */
  boolean isCompleted(Player player);

  /**
   * Called when the challenge ends (cleanup).
   *
   * @param players The players in the game.
   */
  default void cleanup(List<Player> players) {
    // Optional: implement to clean up challenge state.
  }

  /**
   * Returns a description with a chance to include "goob" for fun.
   *
   * @param normalDescription The normal description of the challenge.
   * @return The modified description, possibly including "goob".
   */
  default String getDescriptionWithGoob(String normalDescription) {
    if (new Random().nextInt(100) == 0) {
      return "goob";
    }
    return normalDescription;
  }
}
