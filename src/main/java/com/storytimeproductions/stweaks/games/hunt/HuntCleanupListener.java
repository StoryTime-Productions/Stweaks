package com.storytimeproductions.stweaks.games.hunt;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles cleanup events for the Hunt game system. Removes players from holograms and clears their
 * items when they disconnect or change worlds.
 */
public class HuntCleanupListener implements Listener {

  private final JavaPlugin plugin;
  private final HuntHologramManager hologramManager;
  private final HuntKitManager kitManager;
  private final HuntDisguiseManager disguiseManager;
  private final HuntUtilityListener utilityListener;
  private final HuntDisguisePassiveListener passiveListener;
  private final HiderUtilityListener hiderUtilityListener;
  private final HuntPrepPhaseManager prepPhaseManager;
  private final String huntWorldName;
  private final Set<String> huntWorlds;

  /**
   * Constructs a new HuntCleanupListener.
   *
   * @param plugin The JavaPlugin instance
   * @param hologramManager The hologram manager instance
   * @param kitManager The kit manager instance
   * @param disguiseManager The disguise manager instance
   * @param utilityListener The utility listener instance
   * @param passiveListener The passive listener instance
   * @param hiderUtilityListener The hider utility listener instance
   * @param prepPhaseManager The prep phase manager instance
   * @param huntWorldName The name of the hunt world (null to handle all worlds)
   */
  public HuntCleanupListener(
      JavaPlugin plugin,
      HuntHologramManager hologramManager,
      HuntKitManager kitManager,
      HuntDisguiseManager disguiseManager,
      HuntUtilityListener utilityListener,
      HuntDisguisePassiveListener passiveListener,
      HiderUtilityListener hiderUtilityListener,
      HuntPrepPhaseManager prepPhaseManager,
      String huntWorldName) {
    this.plugin = plugin;
    this.hologramManager = hologramManager;
    this.kitManager = kitManager;
    this.disguiseManager = disguiseManager;
    this.utilityListener = utilityListener;
    this.passiveListener = passiveListener;
    this.hiderUtilityListener = hiderUtilityListener;
    this.prepPhaseManager = prepPhaseManager;
    this.huntWorldName = huntWorldName;
    this.huntWorlds = new HashSet<>();

    // Load all hunt world names from config
    loadHuntWorlds();
  }

  /**
   * Loads all hunt world names from the hunt.yml configuration file. This includes both the main
   * hunt world and all map worlds.
   */
  private void loadHuntWorlds() {
    try {
      File huntConfigFile = new File(plugin.getDataFolder(), "hunt.yml");
      if (!huntConfigFile.exists()) {
        plugin.getLogger().warning("hunt.yml not found, using default hunt world detection");
        return;
      }

      FileConfiguration config = YamlConfiguration.loadConfiguration(huntConfigFile);

      // Add the main hunt world
      String mainWorld = config.getString("hunt.world");
      if (mainWorld != null && !mainWorld.isEmpty()) {
        huntWorlds.add(mainWorld);
        plugin.getLogger().info("[DEBUG] Added main hunt world: " + mainWorld);
      }

      // Add all map worlds
      ConfigurationSection mapsSection = config.getConfigurationSection("hunt.maps");
      if (mapsSection != null) {
        for (String mapKey : mapsSection.getKeys(false)) {
          String mapWorld = mapsSection.getString(mapKey + ".world");
          if (mapWorld != null && !mapWorld.isEmpty()) {
            huntWorlds.add(mapWorld);
            plugin
                .getLogger()
                .info("[DEBUG] Added hunt map world: " + mapWorld + " (from map: " + mapKey + ")");
          }
        }
      }

      plugin
          .getLogger()
          .info("[DEBUG] Loaded " + huntWorlds.size() + " hunt worlds from configuration");
    } catch (Exception e) {
      plugin.getLogger().severe("Error loading hunt worlds from configuration: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Handles player quit events. Removes the player from all holograms when they disconnect.
   *
   * @param event The PlayerQuitEvent
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    cleanupPlayer(player, "disconnected");
  }

  /**
   * Handles player world change events. Removes the player from holograms and clears items when
   * they leave the hunt world.
   *
   * @param event The PlayerChangedWorldEvent
   */
  @EventHandler
  public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
    Player player = event.getPlayer();
    String fromWorldName = event.getFrom().getName();
    String toWorldName = player.getWorld().getName();

    // Check if both worlds are hunt-related worlds
    boolean fromIsHuntWorld = isHuntWorld(fromWorldName);
    boolean toIsHuntWorld = isHuntWorld(toWorldName);

    // If moving between hunt worlds, don't clean up
    if (fromIsHuntWorld && toIsHuntWorld) {
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin
            .getLogger()
            .info(
                "Player "
                    + player.getName()
                    + " moved between hunt worlds ("
                    + fromWorldName
                    + " -> "
                    + toWorldName
                    + "), preserving hunt data");
      }
      return;
    }

    // Only clean up when moving from a hunt world to a non-hunt world
    if (fromIsHuntWorld && !toIsHuntWorld) {
      cleanupPlayer(player, "left hunt world (" + fromWorldName + " -> " + toWorldName + ")");
    }
  }

  /**
   * Checks if a world name is a hunt-related world.
   *
   * @param worldName The world name to check
   * @return true if it's a hunt world, false otherwise
   */
  private boolean isHuntWorld(String worldName) {
    if (worldName == null) {
      return false;
    }

    // First check our loaded hunt worlds from config
    if (huntWorlds.contains(worldName)) {
      return true;
    }

    // Check if it matches the configured hunt world name exactly (legacy support)
    if (huntWorldName != null && worldName.equals(huntWorldName)) {
      return true;
    }

    // If no worlds were loaded from config, fall back to name-based detection
    if (huntWorlds.isEmpty()) {
      // Check if it contains "hunt" in the name (case-insensitive)
      if (worldName.toLowerCase().contains("hunt")) {
        plugin.getLogger().info("[DEBUG] Detected hunt world via naming convention: " + worldName);
        return true;
      }

      // Also check for world names that match our map naming scheme
      if (worldName.startsWith("HuntWorld")) {
        plugin
            .getLogger()
            .info("[DEBUG] Detected hunt world via map naming convention: " + worldName);
        return true;
      }
    }

    return false;
  }

  /**
   * Performs cleanup operations for a player.
   *
   * @param player The player to clean up
   * @param reason The reason for cleanup (for logging)
   */
  private void cleanupPlayer(Player player, String reason) {
    // Remove from all holograms
    hologramManager.removePlayerFromAllHolograms(player.getUniqueId());

    // Remove from prep phase if active
    if (prepPhaseManager != null) {
      prepPhaseManager.removePlayer(player.getUniqueId());
    }

    // Clear their kit items
    kitManager.removePlayerKit(player);

    // Remove their disguise and reset entity size
    disguiseManager.removeDisguise(player);

    // Clear utility ability cooldowns
    utilityListener.clearPlayerCooldowns(player.getUniqueId());

    // Clear hider utility ability cooldowns
    hiderUtilityListener.clearPlayerCooldowns(player.getUniqueId());

    // Clear passive ability effects and tasks
    passiveListener.clearPlayerPassiveEffects(player.getUniqueId());

    plugin
        .getLogger()
        .info(String.format("Cleaned up Hunt data for %s (reason: %s)", player.getName(), reason));
  }
}
