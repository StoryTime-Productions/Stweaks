package com.storytimeproductions.stweaks.config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A utility class for managing player-specific configuration settings.
 *
 * <p>This class provides methods to load the configuration file and retrieve values for various
 * settings such as required playtime, AFK threshold, and weekend multipliers, with the weekend
 * multiplier being shared across all players.
 */
public class SettingsManager {
  private static FileConfiguration config;
  private static JavaPlugin plugin;

  // Map to store per-player settings using UUID as key
  private static Map<UUID, PlayerSettings> playerSettingsMap = new HashMap<>();

  /**
   * Loads the plugin's configuration file.
   *
   * <p>This method initializes the configuration by getting the config from the provided JavaPlugin
   * instance. This must be called before accessing any configuration values.
   *
   * @param pl The plugin instance from which the configuration is loaded.
   */
  public static void load(JavaPlugin pl) {
    plugin = pl;
    config = plugin.getConfig();
    config.set("activeBoost.amount", 0.0);
  }

  /**
   * Reloads the plugin's configuration file.
   *
   * <p>This method reloads the configuration by getting the config from the provided JavaPlugin
   * instance. This must be called to refresh the configuration values.
   */
  public static void reload() {
    plugin.reloadConfig();
    config = plugin.getConfig();
  }

  /**
   * Retrieves the required playtime for a specific player.
   *
   * <p>This method fetches the "required_minutes" setting from the configuration file. If the
   * setting is not found, the default value of 60 minutes is returned.
   *
   * @param playerUuid The UUID of the player whose required playtime is being retrieved.
   * @return The required playtime in minutes for the specific player.
   */
  public static int getRequiredMinutes(UUID playerUuid) {
    PlayerSettings settings = getPlayerSettings(playerUuid);
    return settings.getRequiredMinutes();
  }

  /**
   * Retrieves the AFK threshold in seconds for a specific player.
   *
   * <p>This method fetches the "afk_threshold_seconds" setting from the configuration file. If the
   * setting is not found, the default value of 300 seconds (5 minutes) is returned.
   *
   * @param playerUuid The UUID of the player whose AFK threshold is being retrieved.
   * @return The AFK threshold in seconds for the specific player.
   */
  public static int getAfkThresholdSeconds(UUID playerUuid) {
    PlayerSettings settings = getPlayerSettings(playerUuid);
    return settings.getAfkThresholdSeconds();
  }

  /**
   * Retrieves the global weekend multiplier for all players.
   *
   * <p>This method fetches the "weekend_multiplier" setting from the configuration file. If the
   * setting is not found, the default value of 1.5 is returned.
   *
   * @return The global weekend playtime multiplier for all players.
   */
  public static double getWeekendMultiplier() {
    return config.getDouble("multipliers.weekend", 2.0);
  }

  /**
   * Retrieves the base multiplier for all players.
   *
   * <p>This method fetches the "multipliers.base" setting from the configuration file. If the
   * setting is not found, the default value of 1.0 is returned.
   *
   * @return The base playtime multiplier for all players.
   */
  public static double getBaseMultiplier() {
    return config.getDouble("multipliers.base", 1.0);
  }

  /**
   * Retrieves the social multiplier for all players.
   *
   * <p>This method fetches the "multipliers.social" setting from the configuration file. If the
   * setting is not found, the default value of 0.0 is returned.
   *
   * @return The social playtime multiplier for all players.
   */
  public static double getSocialMultiplier() {
    return config.getDouble("multipliers.social", 0.0);
  }

  /**
   * Retrieves the social distance for all players.
   *
   * <p>This method fetches the "multipliers.social-distance" setting from the configuration file.
   * If the setting is not found, the default value of 10.0 is returned.
   *
   * @return The social distance for all players.
   */
  public static double getSocialDistance() {
    return config.getDouble("multipliers.social-distance", 10.0);
  }

  /**
   * Retrieves the player settings for a specific player. If the player settings don't exist yet,
   * create a new `PlayerSettings` object and store it.
   *
   * @param playerUuid The UUID of the player whose settings are being retrieved.
   * @return The `PlayerSettings` object for the specific player.
   */
  private static PlayerSettings getPlayerSettings(UUID playerUuid) {
    if (!playerSettingsMap.containsKey(playerUuid)) {
      // Create and store default settings for the player
      PlayerSettings settings =
          new PlayerSettings(
              config.getInt("required_minutes", 60), config.getInt("afk_threshold_seconds", 300));
      playerSettingsMap.put(playerUuid, settings);
    }
    return playerSettingsMap.get(playerUuid);
  }

  /**
   * Retrieves the config.
   *
   * @return the config
   */
  public static FileConfiguration getConfig() {
    return config;
  }

  /** Saves the config. */
  public static void saveConfig() {
    plugin.saveConfig();
  }

  /** A helper class to store per-player settings. */
  private static class PlayerSettings {
    private int requiredMinutes;
    private int afkThresholdSeconds;

    public PlayerSettings(int requiredMinutes, int afkThresholdSeconds) {
      this.requiredMinutes = requiredMinutes;
      this.afkThresholdSeconds = afkThresholdSeconds;
    }

    public int getRequiredMinutes() {
      return requiredMinutes;
    }

    public int getAfkThresholdSeconds() {
      return afkThresholdSeconds;
    }
  }
}
