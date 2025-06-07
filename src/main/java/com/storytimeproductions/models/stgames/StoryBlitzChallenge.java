package com.storytimeproductions.models.stgames;

import java.util.List;
import org.bukkit.entity.Player;

/**
 * Represents a single challenge in StoryBlitz. Extend this class to implement custom challenge
 * logic.
 */
public interface StoryBlitzChallenge {

  /**
   * Returns the description of the challenge, which is shown to players.
   *
   * @return The description of the challenge.
   */
  public String getDescription();

  /**
   * Called when the challenge starts for the given team.
   *
   * @param teamPlayers The players in the team.
   */
  public void start(List<Player> teamPlayers);

  /**
   * Checks if the player has completed the challenge.
   *
   * @param player The player to check.
   * @return true if the player has completed the challenge, false otherwise.
   */
  public abstract boolean isCompleted(Player player);

  /**
   * Called when the challenge ends (cleanup).
   *
   * @param teamPlayers The players in the team.
   */
  public void cleanup(List<Player> teamPlayers);
}
