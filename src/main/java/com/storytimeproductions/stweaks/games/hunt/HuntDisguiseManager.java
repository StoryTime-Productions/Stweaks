package com.storytimeproductions.stweaks.games.hunt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.PlayerWatcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages disguise functionality for the Hunt game. Creates armor stands with player disguises at
 * configured locations and handles player interactions to apply disguises.
 */
public class HuntDisguiseManager {
  private final JavaPlugin plugin;
  private final Map<Location, ArmorStand> disguiseStands;
  private final Map<Location, String> standSkins;
  private final Map<Location, String> standDisplayNames;
  private final Map<Location, Double> standScales;
  private final Map<UUID, Long> playerCooldowns;
  private final Map<UUID, Long> playerScreechCooldowns; // Track screech cooldowns
  private final Map<UUID, BukkitTask> disguiseTasks;
  private final Map<UUID, Location> playerDisguiseStands;
  private final Map<UUID, String> playerDisguiseTypes; // Track display names for passive abilities
  private FileConfiguration huntConfig;
  private HuntPrepPhaseManager
      prepPhaseManager; // Reference to prep phase manager for hologram updates

  /**
   * Constructs a new HuntDisguiseManager.
   *
   * @param plugin The JavaPlugin instance
   */
  public HuntDisguiseManager(JavaPlugin plugin) {
    this.plugin = plugin;
    this.disguiseStands = new HashMap<>();
    this.standSkins = new HashMap<>();
    this.standDisplayNames = new HashMap<>();
    this.standScales = new HashMap<>();
    this.playerCooldowns = new HashMap<>();
    this.playerScreechCooldowns = new HashMap<>();
    this.disguiseTasks = new HashMap<>();
    this.playerDisguiseStands = new HashMap<>();
    this.playerDisguiseTypes = new HashMap<>();

    loadConfiguration();
  }

  /** Loads the hunt configuration from hunt.yml. */
  private void loadConfiguration() {
    try {
      // Create hunt.yml if it doesn't exist
      plugin.saveResource("hunt.yml", false);

      // Load the configuration
      huntConfig =
          YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), "hunt.yml"));

      plugin.getLogger().info("Loaded hunt configuration");

      // Log all configured disguises on startup for debugging
      ConfigurationSection locations =
          huntConfig.getConfigurationSection("hunt.disguise-locations");
      if (locations != null) {
        plugin.getLogger().info("Available disguises in hunt.yml:");
        for (String locationId : locations.getKeys(false)) {
          ConfigurationSection locationConfig = locations.getConfigurationSection(locationId);
          if (locationConfig != null) {
            String displayName = locationConfig.getString("display-name", "unknown");
            String skinName = locationConfig.getString("player-skin", "unknown");
            plugin.getLogger().info(" - " + displayName + " -> " + skinName);
          }
        }
      } else {
        plugin.getLogger().warning("No hunt.disguise-locations section found in hunt.yml!");
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to load hunt configuration: " + e.getMessage());
      // Create a default configuration
      huntConfig = new YamlConfiguration();
    }
  }

  /** Spawns all disguise armor stands based on the configuration. */
  public void spawnDisguiseStands() {
    // Clear existing stands first
    clearDisguiseStands();

    String worldName = huntConfig.getString("hunt.world", "world");
    World world = Bukkit.getWorld(worldName);

    if (world == null) {
      plugin.getLogger().warning("World '" + worldName + "' not found for hunt disguise stands");
      return;
    }

    ConfigurationSection locations = huntConfig.getConfigurationSection("hunt.disguise-locations");
    if (locations == null) {
      plugin.getLogger().warning("No disguise locations configured in hunt.yml");
      return;
    }

    for (String locationId : locations.getKeys(false)) {
      ConfigurationSection locationConfig = locations.getConfigurationSection(locationId);
      if (locationConfig == null || !locationConfig.getBoolean("enabled", true)) {
        continue;
      }

      double x = locationConfig.getDouble("x");
      double y = locationConfig.getDouble("y");
      double z = locationConfig.getDouble("z");
      float yaw = (float) locationConfig.getDouble("yaw", 0.0);
      float pitch = (float) locationConfig.getDouble("pitch", 0.0);
      String playerSkin = locationConfig.getString("player-skin", "Steve");
      String displayName = locationConfig.getString("display-name", playerSkin);

      Location location = new Location(world, x, y, z, yaw, pitch);

      // Spawn armor stand
      ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
      armorStand.setGravity(false);
      armorStand.setCanPickupItems(false);
      armorStand.setInvulnerable(true);
      armorStand.setPersistent(true);

      // Apply player disguise to the armor stand
      try {
        PlayerDisguise disguise = new PlayerDisguise(playerSkin);
        disguise.setReplaceSounds(false);
        disguise.setModifyBoundingBox(false);
        disguise.setName(displayName);

        PlayerWatcher watcher = (PlayerWatcher) disguise.getWatcher();
        double playerScale = huntConfig.getDouble("hunt.disguise.scale", 1.3);
        watcher.setScale(playerScale);
        watcher.setCapeEnabled(false);

        DisguiseAPI.disguiseEntity(armorStand, disguise);

        plugin
            .getLogger()
            .info(
                "Spawned disguise stand at "
                    + location
                    + " (yaw: "
                    + yaw
                    + ", pitch: "
                    + pitch
                    + ")"
                    + " with skin "
                    + playerSkin
                    + " and display name "
                    + displayName);
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to disguise armor stand: " + e.getMessage());
      }

      // Store the armor stand and its data
      disguiseStands.put(location, armorStand);
      standSkins.put(location, playerSkin);
      standDisplayNames.put(location, displayName);

      double scale = locationConfig.getDouble("scale", 1.3);
      standScales.put(location, scale);
    }

    plugin.getLogger().info("Spawned " + disguiseStands.size() + " disguise armor stands");
  }

  /** Clears all existing disguise armor stands. */
  public void clearDisguiseStands() {
    for (ArmorStand stand : disguiseStands.values()) {
      if (stand != null && !stand.isDead()) {
        // Remove disguise first
        if (DisguiseAPI.isDisguised(stand)) {
          DisguiseAPI.undisguiseToAll(stand);
        }
        stand.remove();
      }
    }

    disguiseStands.clear();
    standSkins.clear();
    standDisplayNames.clear();
    standScales.clear();

    plugin.getLogger().info("Cleared all disguise armor stands");
  }

  /**
   * Handles a player left-clicking on a disguise armor stand.
   *
   * @param player The player who clicked
   * @param armorStand The armor stand that was clicked
   * @param hologramManager The hologram manager to check if player is a hunter
   */
  public void handleDisguiseInteraction(
      Player player, ArmorStand armorStand, HuntHologramManager hologramManager) {

    // Check if player is a hunter
    String playerClass = hologramManager.getPlayerClassSelection(player.getUniqueId());
    if (playerClass == null || !playerClass.startsWith("hunter_")) {
      return;
    }

    // Check cooldown
    long currentTime = System.currentTimeMillis();
    Long lastUse = playerCooldowns.get(player.getUniqueId());
    int cooldownSeconds = huntConfig.getInt("hunt.disguise.cooldown", 30);

    if (lastUse != null && (currentTime - lastUse) < (cooldownSeconds * 1000L)) {
      return;
    }

    // Find which disguise stand this is
    Location standLocation = null;
    for (Map.Entry<Location, ArmorStand> entry : disguiseStands.entrySet()) {
      if (entry.getValue().equals(armorStand)) {
        standLocation = entry.getKey();
        break;
      }
    }

    if (standLocation == null) {
      player.sendMessage(
          Component.text("Invalid disguise stand!", NamedTextColor.RED, TextDecoration.BOLD));
      return;
    }

    String skinName = standSkins.get(standLocation);
    String displayName = standDisplayNames.get(standLocation);
    if (skinName == null || displayName == null) {
      player.sendMessage(
          Component.text("Failed to get disguise data!", NamedTextColor.RED, TextDecoration.BOLD));
      return;
    }

    // Check if player is already disguised from this same stand - if so, undisguise
    // them
    Location currentDisguiseStand = playerDisguiseStands.get(player.getUniqueId());
    if (currentDisguiseStand != null && currentDisguiseStand.equals(standLocation)) {
      // Player clicked the same stand they're disguised from - remove disguise
      removeDisguise(player);
      playerCooldowns.put(player.getUniqueId(), currentTime);
      return;
    }

    // Remove any existing disguise
    if (DisguiseAPI.isDisguised(player)) {
      DisguiseAPI.undisguiseToAll(player);
    }

    // Cancel any existing disguise task
    BukkitTask existingTask = disguiseTasks.get(player.getUniqueId());
    if (existingTask != null) {
      existingTask.cancel();
      disguiseTasks.remove(player.getUniqueId());
    }

    try {

      // Apply disguise
      PlayerDisguise disguise = new PlayerDisguise(skinName);
      disguise.setReplaceSounds(false);
      disguise.setModifyBoundingBox(false);

      // Get the watcher to set scale
      PlayerWatcher watcher = (PlayerWatcher) disguise.getWatcher();
      watcher.setNameVisible(false);
      watcher.setCapeEnabled(false);

      DisguiseAPI.disguiseToAll(player, disguise);

      // Apply entity size scaling using command
      double playerScale = huntConfig.getDouble("hunt.disguise.player-scale", 1.3);
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(),
          String.format("entitysize player %s %.1f", player.getName(), playerScale));

      // Play scary transformation sound
      player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f);
      player.playSound(player.getLocation(), Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 0.8f);

      // Set cooldown
      playerCooldowns.put(player.getUniqueId(), currentTime);

      // Track which stand this player used for disguise
      playerDisguiseStands.put(player.getUniqueId(), standLocation);

      // Track the display name for passive abilities
      playerDisguiseTypes.put(player.getUniqueId(), displayName);

      // Update player data with selected disguise and skin, with extra debug logging
      try {
        HuntPlayerData playerData =
            Bukkit.getServicesManager()
                .getRegistration(HuntLobbyManager.class)
                .getProvider()
                .getPlayerData(player.getUniqueId());
        if (playerData != null) {
          playerData.setSelectedDisguise(displayName);
          playerData.setSelectedDisguiseSkin(skinName);
          plugin
              .getLogger()
              .info(
                  "[DEBUG] Saved to player data: displayName='"
                      + displayName
                      + "', skinName='"
                      + skinName
                      + "' for "
                      + player.getName());
        } else {
          plugin
              .getLogger()
              .warning(
                  "[DEBUG] Could not save disguise selection for "
                      + player.getName()
                      + " - player data not found");
        }
      } catch (Exception e) {
        plugin
            .getLogger()
            .warning("[DEBUG] Failed to save disguise to player data: " + e.getMessage());
      }

      // Schedule removal if duration is set
      int duration = huntConfig.getInt("hunt.disguise.duration", 0);
      if (duration > 0) {
        BukkitTask task =
            Bukkit.getScheduler()
                .runTaskLater(
                    plugin,
                    () -> {
                      removeDisguise(player);
                      disguiseTasks.remove(player.getUniqueId());
                    },
                    duration * 20L); // Convert seconds to ticks

        disguiseTasks.put(player.getUniqueId(), task);

        player.sendMessage(
            Component.text("Disguised as ", NamedTextColor.GREEN)
                .append(Component.text(displayName, NamedTextColor.WHITE))
                .append(Component.text(" for " + duration + " seconds!", NamedTextColor.GREEN)));
      } else {
        player.sendMessage(
            Component.text("Disguised as ", NamedTextColor.GREEN)
                .append(Component.text(displayName, NamedTextColor.WHITE))
                .append(Component.text("!", NamedTextColor.GREEN)));
      }

      plugin
          .getLogger()
          .info(player.getName() + " disguised as " + displayName + " (skin: " + skinName + ")");

      // Update prep phase hologram if available
      if (prepPhaseManager != null) {
        plugin
            .getLogger()
            .info(
                "Calling updateDisguiseSelections after disguise applied for " + player.getName());
        prepPhaseManager.updateDisguiseSelections();
      } else {
        plugin
            .getLogger()
            .warning(
                "prepPhaseManager is null when trying to update disguise selections for "
                    + player.getName());
      }

    } catch (Exception e) {
      player.sendMessage(
          Component.text("Failed to apply disguise!", NamedTextColor.RED, TextDecoration.BOLD));
      plugin
          .getLogger()
          .warning("Failed to disguise player " + player.getName() + ": " + e.getMessage());
    }
  }

  /**
   * Removes a player's disguise and resets their size.
   *
   * @param player The player to remove disguise from
   */
  public void removeDisguise(Player player) {
    if (DisguiseAPI.isDisguised(player)) {
      DisguiseAPI.undisguiseToAll(player);

      // Reset entity size back to normal
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), String.format("entitysize player %s 1.0", player.getName()));

      // Clear the selectedDisguise in player data if available
      try {
        // Get player data from lobby manager
        HuntPlayerData playerData =
            Bukkit.getServicesManager()
                .getRegistration(HuntLobbyManager.class)
                .getProvider()
                .getPlayerData(player.getUniqueId());
        if (playerData != null) {
          // Clear both the saved disguise and skin
          playerData.setSelectedDisguise(null);
          playerData.setSelectedDisguiseSkin(null);
          plugin
              .getLogger()
              .fine("Cleared selected disguise and skin in player data for " + player.getName());
        }
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to clear disguise in player data: " + e.getMessage());
      }

      player.sendMessage(Component.text("Disguise removed!", NamedTextColor.YELLOW));
    }

    // Cancel any pending removal task
    BukkitTask task = disguiseTasks.get(player.getUniqueId());
    if (task != null) {
      task.cancel();
      disguiseTasks.remove(player.getUniqueId());
    }

    // Clear all disguise-related data
    playerDisguiseStands.remove(player.getUniqueId());
    playerDisguiseTypes.remove(player.getUniqueId());
    playerScreechCooldowns.remove(player.getUniqueId());

    // Update prep phase hologram if available
    if (prepPhaseManager != null) {
      prepPhaseManager.updateDisguiseSelections();
    }
  }

  /** Removes all disguises from all players and cleans up. */
  public void removeAllDisguises() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (DisguiseAPI.isDisguised(player)) {
        removeDisguise(player);
      }
    }

    // Cancel all tasks
    for (BukkitTask task : disguiseTasks.values()) {
      task.cancel();
    }
    disguiseTasks.clear();
    playerCooldowns.clear();
    playerDisguiseStands.clear();
    playerDisguiseTypes.clear();
    playerScreechCooldowns.clear();

    plugin.getLogger().info("Removed all hunt disguises");
  }

  /**
   * Checks if an armor stand is a disguise stand.
   *
   * @param armorStand The armor stand to check
   * @return true if it's a disguise stand, false otherwise
   */
  public boolean isDisguiseStand(ArmorStand armorStand) {
    return disguiseStands.containsValue(armorStand);
  }

  /**
   * Gets the disguise type (display name) of the disguise stand that a player is currently using.
   *
   * @param playerId The UUID of the player
   * @return The display name of the disguise, or null if the player is not disguised
   */
  public String getPlayerDisguiseSkin(UUID playerId) {
    if (!DisguiseAPI.isDisguised(Bukkit.getPlayer(playerId))) {
      return null;
    }

    String disguiseType = playerDisguiseTypes.get(playerId);
    return disguiseType;
  }

  /**
   * Checks if a player is currently disguised.
   *
   * @param playerId The UUID of the player
   * @return true if the player is disguised, false otherwise
   */
  public boolean isPlayerDisguised(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) {
      return false;
    }

    boolean apiDisguised = DisguiseAPI.isDisguised(player);
    boolean hasDisguiseType = playerDisguiseTypes.containsKey(playerId);
    return apiDisguised && hasDisguiseType;
  }

  /**
   * Gets all currently disguised players.
   *
   * @return A set of UUIDs of all currently disguised players
   */
  public Set<UUID> getDisguisedPlayers() {
    return new HashSet<>(playerDisguiseTypes.keySet());
  }

  /**
   * Plays a character-specific screech sound for a disguised hunter.
   *
   * @param player The disguised player
   */
  public void playCharacterScreech(Player player) {
    String disguiseType = playerDisguiseTypes.get(player.getUniqueId());
    if (disguiseType == null) {
      return;
    }

    // Check screech cooldown (3 seconds)
    UUID playerId = player.getUniqueId();
    long currentTime = System.currentTimeMillis();

    // Check if player has a screech cooldown
    if (playerScreechCooldowns.containsKey(playerId)) {
      long lastScreechTime = playerScreechCooldowns.get(playerId);
      long timeSinceLastScreech = currentTime - lastScreechTime;

      // If less than 3 seconds have passed, ignore the screech
      if (timeSinceLastScreech < 3000) {
        // Silently return - no spam message needed
        return;
      }
    }

    // Set the screech cooldown
    playerScreechCooldowns.put(playerId, currentTime);

    // Map disguise types to their screech sounds (audible to all nearby players)
    Location playerLoc = player.getLocation();
    switch (disguiseType) {
      case "Springtrap":
        // Mechanical/robotic screech
        player.getWorld().playSound(playerLoc, Sound.BLOCK_METAL_BREAK, 2.0f, 0.3f);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 1.8f);
        break;
      case "Herobrine":
        // Ghostly/supernatural screech
        player.getWorld().playSound(playerLoc, Sound.ENTITY_GHAST_SCREAM, 1.5f, 1.2f);
        player.getWorld().playSound(playerLoc, Sound.AMBIENT_CAVE, 1.2f, 2.0f);
        break;
      case "Slenderman":
        // Static/distorted screech
        player.getWorld().playSound(playerLoc, Sound.BLOCK_BEACON_DEACTIVATE, 1.8f, 0.1f);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_ENDERMAN_SCREAM, 1.5f, 0.7f);
        break;
      case "Cryptid":
        // Animalistic/beast screech
        player.getWorld().playSound(playerLoc, Sound.ENTITY_WOLF_HOWL, 1.8f, 0.8f);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_RAVAGER_ROAR, 1.4f, 1.4f);
        break;
      case "Jigsaw":
        // Sinister/mechanical screech
        player.getWorld().playSound(playerLoc, Sound.BLOCK_GRINDSTONE_USE, 1.7f, 0.5f);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_VEX_CHARGE, 1.5f, 0.9f);
        break;
      case "Scarecrow":
        // Wind/rustling screech - fabric and hay sounds
        player.getWorld().playSound(playerLoc, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.7f, 0.6f);
        player.getWorld().playSound(playerLoc, Sound.BLOCK_GRASS_BREAK, 1.4f, 0.8f);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_PHANTOM_AMBIENT, 1.6f, 1.3f);
        break;
      default:
        // Default screech if disguise type is unknown
        player.getWorld().playSound(playerLoc, Sound.ENTITY_ZOMBIE_AMBIENT, 1.5f, 0.5f);
        break;
    }
  }

  /**
   * Resets a player's entity size to normal. This should be called when a player joins the server
   * to ensure they don't retain any modified size from previous sessions.
   *
   * @param player The player whose size should be reset
   */
  public void resetPlayerSize(Player player) {
    Bukkit.dispatchCommand(
        Bukkit.getConsoleSender(), String.format("entitysize player %s 1.0", player.getName()));
  }

  /** Reloads the configuration and respawns disguise stands. */
  public void reload() {
    clearDisguiseStands();
    loadConfiguration();
    spawnDisguiseStands();
    plugin.getLogger().info("Reloaded hunt disguise system");
  }

  /**
   * Handles player class changes and removes disguises if necessary. If a player who is disguised
   * leaves a hunter class or joins a hider class, their disguise will be removed.
   *
   * @param player The player whose class changed
   * @param oldClass The old class (can be null if no previous class)
   * @param newClass The new class (can be null if leaving all classes)
   */
  public void handleClassChange(Player player, String oldClass, String newClass) {
    // Check if player is currently disguised
    if (!DisguiseAPI.isDisguised(player)) {
      return; // Player is not disguised, nothing to do
    }

    // Check if player was in a hunter class and is now leaving or joining a hider
    // class
    boolean wasHunter = oldClass != null && oldClass.startsWith("hunter_");
    boolean isNowHider = newClass != null && newClass.startsWith("hider_");
    boolean leftAllClasses = newClass == null;

    if (wasHunter && (isNowHider || leftAllClasses)) {
      // Player was a hunter with a disguise and is now leaving hunter class or
      // joining hider
      removeDisguise(player);
      plugin
          .getLogger()
          .info(
              "Removed disguise from "
                  + player.getName()
                  + " due to class change (was: "
                  + oldClass
                  + ", now: "
                  + newClass
                  + ")");
    }
  }

  /**
   * Sets the prep phase manager reference for hologram updates.
   *
   * @param prepPhaseManager The prep phase manager instance
   */
  public void setPrepPhaseManager(HuntPrepPhaseManager prepPhaseManager) {
    this.prepPhaseManager = prepPhaseManager;
  }
}
