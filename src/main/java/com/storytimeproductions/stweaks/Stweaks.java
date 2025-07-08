package com.storytimeproductions.stweaks;

import com.storytimeproductions.stweaks.commands.BiomeTrackerCommand;
import com.storytimeproductions.stweaks.commands.CosmeticsMenuCommand;
import com.storytimeproductions.stweaks.commands.DisguiseCommand;
import com.storytimeproductions.stweaks.commands.HuntCommand;
import com.storytimeproductions.stweaks.commands.PetsMenuCommand;
import com.storytimeproductions.stweaks.commands.QuestMenuCommand;
import com.storytimeproductions.stweaks.commands.StCasinoCommand;
import com.storytimeproductions.stweaks.commands.StLobbyCommand;
import com.storytimeproductions.stweaks.commands.StSpawnCommand;
import com.storytimeproductions.stweaks.commands.StStatusCommand;
import com.storytimeproductions.stweaks.config.SettingsManager;
import com.storytimeproductions.stweaks.events.EventManager;
import com.storytimeproductions.stweaks.games.hunt.HiderUtilityListener;
import com.storytimeproductions.stweaks.games.hunt.HuntCleanupListener;
import com.storytimeproductions.stweaks.games.hunt.HuntDisguiseListener;
import com.storytimeproductions.stweaks.games.hunt.HuntDisguiseManager;
import com.storytimeproductions.stweaks.games.hunt.HuntDisguisePassiveListener;
import com.storytimeproductions.stweaks.games.hunt.HuntHologramManager;
import com.storytimeproductions.stweaks.games.hunt.HuntKitManager;
import com.storytimeproductions.stweaks.games.hunt.HuntLobbyListener;
import com.storytimeproductions.stweaks.games.hunt.HuntLobbyManager;
import com.storytimeproductions.stweaks.games.hunt.HuntPlayerJoinListener;
import com.storytimeproductions.stweaks.games.hunt.HuntPrepPhaseManager;
import com.storytimeproductions.stweaks.games.hunt.HuntUtilityListener;
import com.storytimeproductions.stweaks.listeners.BiomeNotifier;
import com.storytimeproductions.stweaks.listeners.CosmeticsListener;
import com.storytimeproductions.stweaks.listeners.CowSkinnerListener;
import com.storytimeproductions.stweaks.listeners.FbiDiscListener;
import com.storytimeproductions.stweaks.listeners.GameManagerListener;
import com.storytimeproductions.stweaks.listeners.IllegalWaterListener;
import com.storytimeproductions.stweaks.listeners.ItemConsumableListener;
import com.storytimeproductions.stweaks.listeners.LebronArmorListener;
import com.storytimeproductions.stweaks.listeners.PetsListener;
import com.storytimeproductions.stweaks.listeners.PetsMenuListener;
import com.storytimeproductions.stweaks.listeners.PlayerActivityListener;
import com.storytimeproductions.stweaks.listeners.QuestMenuListener;
import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import com.storytimeproductions.stweaks.util.BiomeTrackerManager;
import com.storytimeproductions.stweaks.util.BossBarManager;
import com.storytimeproductions.stweaks.util.CosmeticsManager;
import com.storytimeproductions.stweaks.util.DbManager;
import com.storytimeproductions.stweaks.util.EntityDisguiseManager;
import com.storytimeproductions.stweaks.util.PetsManager;
import com.storytimeproductions.stweaks.util.QuestsManager;
import io.papermc.lib.PaperLib;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for the Stweaks, a plugin designed to manage and track player playtime, enforce
 * playtime requirements, and handle other gameplay enhancements. This class is responsible for
 * initializing the plugin, loading configurations, registering commands, listeners, and events, as
 * well as shutting down services on plugin disable. Created by StoryTime Productions on 2025-04-17.
 */
public class Stweaks extends JavaPlugin {

  private static Stweaks instance;
  private DbManager dbManager;
  private HuntHologramManager huntHologramManager;
  private HuntDisguiseManager huntDisguiseManager;
  private HuntLobbyManager huntLobbyManager;
  private HuntPrepPhaseManager huntPrepPhaseManager;

  /**
   * Called when the plugin is enabled. This method is responsible for setting up the plugin,
   * including loading the configuration, registering event listeners, and initializing the playtime
   * tracker and event manager.
   */
  @Override
  public void onEnable() {
    PaperLib.suggestPaper(this);

    saveDefaultConfig();
    SettingsManager.load(this);

    dbManager = new DbManager();
    dbManager.connect();

    // Initialize playtime tracker and event manager
    PlaytimeTracker.loadFromDatabase(dbManager.getConnection());
    PlaytimeTracker.init(this);
    EventManager.init(this);
    BossBarManager.init(this);

    huntHologramManager = new HuntHologramManager(this);
    huntDisguiseManager = new HuntDisguiseManager(this);

    // Set the disguise manager reference in the hologram manager for class change
    // handling
    huntHologramManager.setDisguiseManager(huntDisguiseManager);

    // Initialize hunt hologram titles after server fully starts
    Bukkit.getScheduler()
        .runTaskLater(
            this, huntHologramManager::initializeClassHologramTitles, 20L); // 1 second delay

    // Initialize hunt disguise stands after server fully starts
    Bukkit.getScheduler()
        .runTaskLater(this, huntDisguiseManager::spawnDisguiseStands, 40L); // 2 second delay

    final QuestsManager questsManager = new QuestsManager(dbManager, this);
    final PetsManager petsManager = new PetsManager(this);
    final CosmeticsManager cosmeticsManager = new CosmeticsManager(this);
    huntLobbyManager = new HuntLobbyManager();

    // Create HuntKitManager before HuntPrepPhaseManager
    HuntKitManager huntKitManager = new HuntKitManager(this);

    // Load hunt configuration for prep phase manager
    File huntConfigFile = new File(getDataFolder(), "hunt.yml");
    if (!huntConfigFile.exists()) {
      saveResource("hunt.yml", false);
    }
    org.bukkit.configuration.file.FileConfiguration huntConfig =
        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(huntConfigFile);

    huntPrepPhaseManager =
        new HuntPrepPhaseManager(
            this,
            huntLobbyManager,
            huntHologramManager,
            huntDisguiseManager,
            huntKitManager,
            huntConfig);

    // Set the prep phase manager reference in the disguise manager for hologram
    // updates
    huntDisguiseManager.setPrepPhaseManager(huntPrepPhaseManager);

    QuestMenuCommand questMenuCommand = new QuestMenuCommand(questsManager);
    PetsMenuCommand petsMenuCommand = new PetsMenuCommand(this, petsManager);
    CosmeticsMenuCommand cosmeticsMenuCommand = new CosmeticsMenuCommand(cosmeticsManager);

    // Register event listeners
    getServer().getPluginManager().registerEvents(new PlayerActivityListener(this), this);
    getServer().getPluginManager().registerEvents(new CowSkinnerListener(), this);
    getServer().getPluginManager().registerEvents(new FbiDiscListener(), this);
    getServer().getPluginManager().registerEvents(new IllegalWaterListener(this), this);
    getServer().getPluginManager().registerEvents(new LebronArmorListener(this), this);

    BiomeTrackerManager trackerManager = new BiomeTrackerManager(dbManager, this);
    getServer().getPluginManager().registerEvents(new BiomeNotifier(this, trackerManager), this);

    getServer()
        .getPluginManager()
        .registerEvents(new QuestMenuListener(this, questsManager, questMenuCommand), this);
    getServer()
        .getPluginManager()
        .registerEvents(new PetsMenuListener(this, petsManager, petsMenuCommand), this);
    getServer().getPluginManager().registerEvents(new PetsListener(this, petsManager), this);
    getServer().getPluginManager().registerEvents(new CosmeticsListener(), this);
    getServer().getPluginManager().registerEvents(new GameManagerListener(this), this);
    getServer().getPluginManager().registerEvents(new ItemConsumableListener(), this);

    // Register Hunt-related listeners
    getServer().getPluginManager().registerEvents(new HuntLobbyListener(huntLobbyManager), this);
    getServer()
        .getPluginManager()
        .registerEvents(new HuntPlayerJoinListener(huntDisguiseManager, this), this);

    // Register Hunt disguise listener for armor stand interactions
    getServer()
        .getPluginManager()
        .registerEvents(new HuntDisguiseListener(huntDisguiseManager, huntHologramManager), this);

    // Register Hunt utility listener for hunter special abilities
    HuntUtilityListener huntUtilityListener = new HuntUtilityListener(huntConfig, huntLobbyManager);
    getServer().getPluginManager().registerEvents(huntUtilityListener, this);

    // Register Hunt disguise passive listener for passive abilities
    HuntDisguisePassiveListener huntDisguisePassiveListener =
        new HuntDisguisePassiveListener(this, huntDisguiseManager, huntLobbyManager);
    getServer().getPluginManager().registerEvents(huntDisguisePassiveListener, this);

    // Register Hunt hider utility listener for hider special abilities
    HiderUtilityListener hiderUtilityListener = new HiderUtilityListener(this, huntLobbyManager);
    getServer().getPluginManager().registerEvents(hiderUtilityListener, this);

    // Register Hunt cleanup listener with all required dependencies
    getServer()
        .getPluginManager()
        .registerEvents(
            new HuntCleanupListener(
                this,
                huntHologramManager,
                huntKitManager,
                huntDisguiseManager,
                huntUtilityListener,
                huntDisguisePassiveListener,
                hiderUtilityListener,
                huntPrepPhaseManager,
                "hunt"),
            this);

    // Register commands
    getCommand("ststatus").setExecutor(new StStatusCommand(this));
    getCommand("stcasino").setExecutor(new StCasinoCommand(getConfig()));
    getCommand("stlobby").setExecutor(new StLobbyCommand(getConfig()));
    getCommand("spawn").setExecutor(new StSpawnCommand(getConfig()));
    getCommand("biometracker").setExecutor(new BiomeTrackerCommand(trackerManager, this));
    getCommand("stquests").setExecutor(questMenuCommand);
    getCommand("stpets").setExecutor(petsMenuCommand);
    getCommand("stcosmetics").setExecutor(cosmeticsMenuCommand);

    EntityDisguiseManager disguiseManager = new EntityDisguiseManager(this);
    getCommand("stdisguise").setExecutor(new DisguiseCommand(disguiseManager));

    // Create Hunt command with all required dependencies
    HuntCommand huntCommand =
        new HuntCommand(
            this,
            huntLobbyManager,
            huntHologramManager,
            huntKitManager,
            huntDisguiseManager,
            huntPrepPhaseManager,
            hiderUtilityListener);
    getCommand("hunt").setExecutor(huntCommand);

    // Initialize the HuntDeathHandler
    initHuntDeathHandler();

    getLogger().info("");
    getLogger().info("   _____ _                      _        ");
    getLogger().info("  / ____| |                    | |       ");
    getLogger().info(" | (___ | |___      _____  __ _| | _____ ");
    getLogger().info("  \\___ \\| __\\ \\ /\\ / / _ \\/ _` | |/ / __|");
    getLogger().info("  ____) | |_ \\ V  V /  __/ (_| |   <\\__ \\");
    getLogger().info(" |_____/ \\__| \\_/\\_/ \\___|\\__,_|_|\\_\\___/");
    getLogger().info("");
  }

  /**
   * Called when the plugin is disabled. This method is responsible for shutting down services like
   * the playtime tracker.
   */
  @Override
  public void onDisable() {
    // Reset size for all disguised hunters before cleaning up disguises
    if (huntDisguiseManager != null) {
      resetDisguisedHunterSizes();
      huntDisguiseManager.removeAllDisguises();
      huntDisguiseManager.clearDisguiseStands();
    }

    // Clean up Hunt holograms before saving other data
    if (huntHologramManager != null) {
      huntHologramManager.removeAllPlayersFromHolograms();
    }

    PlaytimeTracker.saveToDatabase(dbManager.getConnection());
    getLogger().info("Stweaks disabled!");
  }

  /**
   * Resets the size of all currently disguised hunters to normal (1.0) on server shutdown. This
   * ensures that when players reconnect, they don't retain the enlarged size from their disguise.
   */
  private void resetDisguisedHunterSizes() {
    if (huntDisguiseManager == null) {
      return;
    }

    for (java.util.UUID playerId : huntDisguiseManager.getDisguisedPlayers()) {
      org.bukkit.entity.Player player = Bukkit.getPlayer(playerId);
      if (player != null && player.isOnline()) {
        // Reset player size to normal using entitysize command
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(), "entitysize player " + player.getName() + " 1.0");
        getLogger().info("Reset size for disguised hunter: " + player.getName());
      }
    }
  }

  /**
   * Gets the instance of the Stweaks plugin. This method provides access to the plugin's main
   * instance, allowing other parts of the code to interact with the plugin.
   *
   * @return The current instance of the Stweaks plugin.
   */
  public static Stweaks getInstance() {
    return instance;
  }

  /**
   * Registers a Bukkit event listener for this plugin.
   *
   * <p>This method registers the provided listener with the Bukkit event system, allowing the
   * listener to handle events for this plugin.
   *
   * @param listener The event listener to register.
   */
  public void registerListener(Listener listener) {
    Bukkit.getPluginManager().registerEvents(listener, this);
  }

  /**
   * Initializes the HuntDeathHandler for handling player deaths in the Hunt game.
   *
   * <p>This method creates a HuntDeathHandler with references to the necessary managers.
   */
  private void initHuntDeathHandler() {
    try {
      // Create a file reference for the hunt.yml config
      File huntConfigFile = new File(getDataFolder(), "hunt.yml");
      if (!huntConfigFile.exists()) {
        saveResource("hunt.yml", false);
      }

      // Create the death handler with the existing managers
      com.storytimeproductions.stweaks.games.hunt.HuntDeathHandler deathHandler =
          new com.storytimeproductions.stweaks.games.hunt.HuntDeathHandler(
              this, huntPrepPhaseManager, huntLobbyManager);

      // Register the death handler
      getServer().getPluginManager().registerEvents(deathHandler, this);

      // Set the death handler in the prep phase manager for tracking hider counts
      if (huntPrepPhaseManager != null) {
        huntPrepPhaseManager.setDeathHandler(deathHandler);
        getLogger().info("HuntDeathHandler linked to HuntPrepPhaseManager successfully!");
      }

      // Set the disguise manager in the death handler for undisguising eliminated
      // hiders
      if (huntDisguiseManager != null) {
        deathHandler.setDisguiseManager(huntDisguiseManager);
        getLogger().info("HuntDisguiseManager linked to HuntDeathHandler successfully!");
      }

      getLogger().info("HuntDeathHandler has been registered successfully!");
    } catch (Exception e) {
      // This is a more robust error handling approach that shows the full stack trace
      getLogger().severe("Failed to initialize HuntDeathHandler: " + e.getMessage());
      e.printStackTrace();

      // Create a simplified version that doesn't rely on managers
      try {
        // Create a very simplified version as a fallback
        com.storytimeproductions.stweaks.games.hunt.HuntDeathHandler deathHandler =
            new com.storytimeproductions.stweaks.games.hunt.HuntDeathHandler(this, null, null);
        getServer().getPluginManager().registerEvents(deathHandler, this);
        getLogger()
            .warning("Registered HuntDeathHandler with null managers (limited functionality)");
      } catch (Exception ex) {
        getLogger()
            .severe(
                "Failed to initialize HuntDeathHandler with fallback approach: " + ex.getMessage());
      }
    }
  }
}
