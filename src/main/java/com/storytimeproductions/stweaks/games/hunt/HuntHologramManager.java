package com.storytimeproductions.stweaks.games.hunt;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages holograms for the Hunt game system. Creates and manages class selection and map voting
 * holograms using DecentHolograms commands.
 */
public class HuntHologramManager {

  private final JavaPlugin plugin;
  private final Map<UUID, String> playerClassSelections;
  private final Map<UUID, String> playerMapVotes;
  private final Map<String, Integer> hologramLineCounts; // Track number of lines per hologram
  private final Map<UUID, Integer>
      playerClassLineNumbers; // Track which line each player is on for classes
  private final Map<UUID, Integer>
      playerMapLineNumbers; // Track which line each player is on for maps
  private final Map<UUID, Boolean>
      playerClassFlags; // Track double command prevention for class selection
  private final Map<UUID, Boolean> playerMapFlags; // Track double command prevention for map voting
  private HuntDisguiseManager disguiseManager; // Reference to disguise manager

  /**
   * Constructs a new HuntHologramManager.
   *
   * @param plugin The JavaPlugin instance
   */
  public HuntHologramManager(JavaPlugin plugin) {
    this.plugin = plugin;
    this.playerClassSelections = new HashMap<>();
    this.playerMapVotes = new HashMap<>();
    this.hologramLineCounts = new HashMap<>();
    this.playerClassLineNumbers = new HashMap<>();
    this.playerMapLineNumbers = new HashMap<>();
    this.playerClassFlags = new HashMap<>();
    this.playerMapFlags = new HashMap<>();

    // Initialize line counts for all holograms (assuming they start with 1 line -
    // the title)
    for (HunterClass hunterClass : HunterClass.values()) {
      hologramLineCounts.put("hunter_" + hunterClass.name().toLowerCase(), 1);
    }
    for (HiderClass hiderClass : HiderClass.values()) {
      hologramLineCounts.put("hider_" + hiderClass.name().toLowerCase(), 1);
    }
    for (HuntMap map : HuntMap.values()) {
      hologramLineCounts.put("map_" + map.name().toLowerCase(), 1);
    }
  }

  /**
   * Executes a DecentHolograms command.
   *
   * @param command The command to execute
   */
  private void logAndDispatchDhCommand(String command) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
  }

  /** Sets up class hologram titles with bold and colored names and clears all player names. */
  public void initializeClassHologramTitles() {
    // Clear all existing player data first
    clearAllHolograms();

    // Initialize hunter class holograms
    for (HunterClass hunterClass : HunterClass.values()) {
      String hologramId = "hunter_" + hunterClass.name().toLowerCase();
      String title = "&c&l" + hunterClass.getDisplayName(); // Bold red for hunters
      logAndDispatchDhCommand(String.format("dh l set %s 1 1 %s", hologramId, title));
    }

    // Initialize hider class holograms
    for (HiderClass hiderClass : HiderClass.values()) {
      String hologramId = "hider_" + hiderClass.name().toLowerCase();
      String title = "&9&l" + hiderClass.getDisplayName(); // Bold blue for hiders
      logAndDispatchDhCommand(String.format("dh l set %s 1 1 %s", hologramId, title));
    }

    // Initialize map holograms with titles (optional - you can customize these)
    for (HuntMap map : HuntMap.values()) {
      String hologramId = "map_" + map.name().toLowerCase();
      String title = "&e&l" + map.getDisplayName(); // Bold yellow for maps
      logAndDispatchDhCommand(String.format("dh l set %s 1 1 %s", hologramId, title));
    }

    plugin.getLogger().info("Initialized Hunt hologram titles and cleared all player names");
  }

  /**
   * Adds a player to a class hologram.
   *
   * @param playerId The UUID of the player
   * @param playerName The name of the player
   * @param className The class name (e.g., "brute", "nimble", "trickster")
   * @param isHunter Whether this is a hunter class
   */
  public void addPlayerToClass(
      UUID playerId, String playerName, String className, boolean isHunter) {
    // Check for double command prevention using flag approach
    Boolean flag = playerClassFlags.get(playerId);
    if (flag != null && flag) {
      // Second call - reset flag and do nothing
      playerClassFlags.put(playerId, false);
      plugin
          .getLogger()
          .info(playerName + " triggered double command prevention for class selection");
      return;
    }

    // First call - set flag and process command
    playerClassFlags.put(playerId, true);

    String hologramId = (isHunter ? "hunter_" : "hider_") + className.toLowerCase();

    // Check if player is already in this class - if so, remove them (toggle
    // behavior)
    String currentClass = playerClassSelections.get(playerId);
    if (hologramId.equals(currentClass)) {
      // Player is already in this class, remove them from it
      removePlayerFromCurrentClass(playerId);

      // Handle disguise removal if player is leaving a hunter class
      if (disguiseManager != null) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
          disguiseManager.handleClassChange(player, currentClass, null);
        }
      }

      plugin.getLogger().info(playerName + " left " + className + " class");
      return;
    }

    // Player is not in this class, add them to it
    // Remove player from any previous class by removing their specific line
    removePlayerFromCurrentClass(playerId);

    // Handle disguise removal if player is switching from hunter to hider or vice
    // versa
    if (disguiseManager != null) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        disguiseManager.handleClassChange(player, currentClass, hologramId);
      }
    }

    // Add a new line to the hologram
    int currentLines = hologramLineCounts.getOrDefault(hologramId, 1);
    int newLineNumber = currentLines + 1;
    // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
    String command = String.format("dh l add %s 1 &f\u2022 %s", hologramId, playerName);
    // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters
    logAndDispatchDhCommand(command);

    // Update tracking
    hologramLineCounts.put(hologramId, newLineNumber);
    playerClassSelections.put(playerId, hologramId);
    playerClassLineNumbers.put(playerId, newLineNumber);

    plugin
        .getLogger()
        .info(
            "Added "
                + playerName
                + " to "
                + className
                + " class hologram at line "
                + newLineNumber);
  }

  /**
   * Adds a player's vote to a map hologram.
   *
   * @param playerId The UUID of the player
   * @param playerName The name of the player
   * @param mapName The map name (e.g., "warehouse", "mansion")
   */
  public void addPlayerToMap(UUID playerId, String playerName, String mapName) {
    // Check for double command prevention using flag approach
    Boolean flag = playerMapFlags.get(playerId);
    if (flag != null && flag) {
      // Second call - reset flag and do nothing
      playerMapFlags.put(playerId, false);
      plugin.getLogger().info(playerName + " triggered double command prevention for map voting");
      return;
    }

    // First call - set flag and process command
    playerMapFlags.put(playerId, true);

    String hologramId = "map_" + mapName.toLowerCase();

    // Check if player has already voted for this map - if so, remove their vote
    // (toggle behavior)
    String currentMap = playerMapVotes.get(playerId);
    if (hologramId.equals(currentMap)) {
      // Player has already voted for this map, remove their vote
      removePlayerFromCurrentMap(playerId);
      plugin.getLogger().info(playerName + " removed vote for " + mapName + " map");
      return;
    }

    // Player hasn't voted for this map, add their vote
    // Remove player from any previous map vote by removing their specific line
    removePlayerFromCurrentMap(playerId);

    // Add a new line to the hologram
    int currentLines = hologramLineCounts.getOrDefault(hologramId, 1);
    int newLineNumber = currentLines + 1;
    // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
    String command = String.format("dh l add %s 1 &f\u2022 %s", hologramId, playerName);
    // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters
    logAndDispatchDhCommand(command);

    // Update tracking
    hologramLineCounts.put(hologramId, newLineNumber);
    playerMapVotes.put(playerId, hologramId);
    playerMapLineNumbers.put(playerId, newLineNumber);

    plugin
        .getLogger()
        .info(
            "Added "
                + playerName
                + " vote to "
                + mapName
                + " map hologram at line "
                + newLineNumber);
  }

  /**
   * Removes a player from their current class by removing their specific line.
   *
   * @param playerId The UUID of the player to remove
   */
  private void removePlayerFromCurrentClass(UUID playerId) {
    String currentClass = playerClassSelections.get(playerId);
    Integer lineNumber = playerClassLineNumbers.get(playerId);

    if (currentClass != null && lineNumber != null) {
      // Remove the specific line
      logAndDispatchDhCommand(String.format("dh l remove %s 1 %d", currentClass, lineNumber));

      // Update line counts for the hologram
      int currentLines = hologramLineCounts.getOrDefault(currentClass, 1);
      hologramLineCounts.put(currentClass, Math.max(1, currentLines - 1));

      // Update line numbers for players on lines after the removed one
      updateLineNumbersAfterRemoval(currentClass, lineNumber);

      // Remove tracking data
      playerClassSelections.remove(playerId);
      playerClassLineNumbers.remove(playerId);

      plugin.getLogger().info("Removed player from " + currentClass + " at line " + lineNumber);
    }
  }

  /**
   * Removes a player from their current map vote by removing their specific line.
   *
   * @param playerId The UUID of the player to remove
   */
  private void removePlayerFromCurrentMap(UUID playerId) {
    String currentMap = playerMapVotes.get(playerId);
    Integer lineNumber = playerMapLineNumbers.get(playerId);

    if (currentMap != null && lineNumber != null) {
      // Remove the specific line
      logAndDispatchDhCommand(String.format("dh l remove %s 1 %d", currentMap, lineNumber));

      // Update line counts for the hologram
      int currentLines = hologramLineCounts.getOrDefault(currentMap, 1);
      hologramLineCounts.put(currentMap, Math.max(1, currentLines - 1));

      // Update line numbers for players on lines after the removed one
      updateLineNumbersAfterRemoval(currentMap, lineNumber);

      // Remove tracking data
      playerMapVotes.remove(playerId);
      playerMapLineNumbers.remove(playerId);

      plugin.getLogger().info("Removed player from " + currentMap + " at line " + lineNumber);
    }
  }

  /**
   * Updates line numbers for all players after a line has been removed.
   *
   * @param hologramId The hologram that had a line removed
   * @param removedLineNumber The line number that was removed
   */
  private void updateLineNumbersAfterRemoval(String hologramId, int removedLineNumber) {
    // Update line numbers for all players who were on lines after the removed one
    for (Map.Entry<UUID, String> entry : playerClassSelections.entrySet()) {
      if (entry.getValue().equals(hologramId)) {
        UUID playerId = entry.getKey();
        Integer currentLine = playerClassLineNumbers.get(playerId);
        if (currentLine != null && currentLine > removedLineNumber) {
          playerClassLineNumbers.put(playerId, currentLine - 1);
        }
      }
    }

    for (Map.Entry<UUID, String> entry : playerMapVotes.entrySet()) {
      if (entry.getValue().equals(hologramId)) {
        UUID playerId = entry.getKey();
        Integer currentLine = playerMapLineNumbers.get(playerId);
        if (currentLine != null && currentLine > removedLineNumber) {
          playerMapLineNumbers.put(playerId, currentLine - 1);
        }
      }
    }
  }

  /** Clears all player lines from all holograms. */
  public void clearAllHolograms() {
    // Clear all class holograms using page add/remove (only during initialization)
    for (HunterClass hunterClass : HunterClass.values()) {
      String hologramId = "hunter_" + hunterClass.name().toLowerCase();
      clearHologramPlayersForInit(hologramId);
    }

    for (HiderClass hiderClass : HiderClass.values()) {
      String hologramId = "hider_" + hiderClass.name().toLowerCase();
      clearHologramPlayersForInit(hologramId);
    }

    // Clear all map holograms using page add/remove (only during initialization)
    for (HuntMap map : HuntMap.values()) {
      String hologramId = "map_" + map.name().toLowerCase();
      clearHologramPlayersForInit(hologramId);
    }

    // Clear tracking data
    playerClassSelections.clear();
    playerMapVotes.clear();
    playerClassLineNumbers.clear();
    playerMapLineNumbers.clear();
    playerClassFlags.clear();
    playerMapFlags.clear();

    plugin.getLogger().info("Cleared all Hunt hologram player lines");
  }

  /**
   * Clears all player lines from a specific hologram during initialization. Preserves page 1 but
   * clears its content and removes any additional pages.
   *
   * @param hologramId The hologram to clear
   */
  private void clearHologramPlayersForInit(String hologramId) {
    // Remove pages 2 and above (if they exist)
    for (int i = 10; i >= 2; i--) {
      logAndDispatchDhCommand(String.format("dh page remove %s %d", hologramId, i));
    }

    // Clear all lines on page 1 (but keep the page itself)
    for (int line = 2; line <= 10; line++) {
      logAndDispatchDhCommand(String.format("dh l remove %s 1 %d", hologramId, line));
    }

    // Set the title back on page 1
    String title = getTitleForHologram(hologramId);
    if (title != null) {
      logAndDispatchDhCommand(String.format("dh l set %s 1 1 %s", hologramId, title));
    }

    hologramLineCounts.put(hologramId, 1);
  }

  /**
   * Gets the appropriate title for a hologram based on its ID.
   *
   * @param hologramId The hologram identifier
   * @return The formatted title string, or null if not found
   */
  private String getTitleForHologram(String hologramId) {
    if (hologramId.startsWith("hunter_")) {
      String className = hologramId.substring("hunter_".length());
      for (HunterClass hunterClass : HunterClass.values()) {
        if (hunterClass.name().toLowerCase().equals(className)) {
          return "&c&l" + hunterClass.getDisplayName(); // Bold red for hunters
        }
      }
    } else if (hologramId.startsWith("hider_")) {
      String className = hologramId.substring("hider_".length());
      for (HiderClass hiderClass : HiderClass.values()) {
        if (hiderClass.name().toLowerCase().equals(className)) {
          return "&9&l" + hiderClass.getDisplayName(); // Bold blue for hiders
        }
      }
    } else if (hologramId.startsWith("map_")) {
      String mapName = hologramId.substring("map_".length());
      for (HuntMap map : HuntMap.values()) {
        if (map.name().toLowerCase().equals(mapName)) {
          return "&e&l" + map.getDisplayName(); // Bold yellow for maps
        }
      }
    }
    return null;
  }

  /**
   * Gets the current class selection for a player.
   *
   * @param playerId The UUID of the player
   * @return The class hologram ID, or null if no selection
   */
  public String getPlayerClassSelection(UUID playerId) {
    return playerClassSelections.get(playerId);
  }

  /**
   * Gets the current map vote for a player.
   *
   * @param playerId The UUID of the player
   * @return The map hologram ID, or null if no vote
   */
  public String getPlayerMapVote(UUID playerId) {
    return playerMapVotes.get(playerId);
  }

  /**
   * Removes a player completely from all holograms (both class and map). This should be called when
   * a player disconnects or leaves the hunt world.
   *
   * @param playerId The UUID of the player to remove
   */
  public void removePlayerFromAllHolograms(UUID playerId) {
    // Remove from class holograms
    removePlayerFromCurrentClass(playerId);

    // Remove from map holograms
    removePlayerFromCurrentMap(playerId);

    // Clear flags for this player
    playerClassFlags.remove(playerId);
    playerMapFlags.remove(playerId);

    plugin.getLogger().info("Removed player " + playerId + " from all Hunt holograms");
  }

  /**
   * Removes all players from all holograms. This should be called during server shutdown or plugin
   * disable.
   */
  public void removeAllPlayersFromHolograms() {
    // Clear all tracking data which will effectively remove all players
    playerClassSelections.clear();
    playerMapVotes.clear();
    playerClassLineNumbers.clear();
    playerMapLineNumbers.clear();
    playerClassFlags.clear();
    playerMapFlags.clear();

    plugin.getLogger().info("Removed all players from Hunt holograms during cleanup");
  }

  /**
   * Removes a player from their current class selection. This allows players to leave a class by
   * selecting it again.
   *
   * @param playerId The UUID of the player to remove from their class
   * @return true if the player was removed from a class, false if they weren't in any class
   */
  public boolean removePlayerFromClass(UUID playerId) {
    String currentClass = playerClassSelections.get(playerId);
    if (currentClass != null) {
      removePlayerFromCurrentClass(playerId);
      return true;
    }
    return false;
  }

  /**
   * Removes a player from their current map vote. This allows players to remove their vote by
   * selecting the same map again.
   *
   * @param playerId The UUID of the player to remove from their map vote
   * @return true if the player was removed from a map vote, false if they hadn't voted
   */
  public boolean removePlayerFromMapVote(UUID playerId) {
    String currentMap = playerMapVotes.get(playerId);
    if (currentMap != null) {
      removePlayerFromCurrentMap(playerId);
      return true;
    }
    return false;
  }

  /**
   * Sets the disguise manager reference for handling class change events.
   *
   * @param disguiseManager The HuntDisguiseManager instance
   */
  public void setDisguiseManager(HuntDisguiseManager disguiseManager) {
    this.disguiseManager = disguiseManager;
  }
}
