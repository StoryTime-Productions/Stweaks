package com.storytimeproductions.stweaks;

import com.storytimeproductions.stweaks.commands.BiomeTrackerCommand;
import com.storytimeproductions.stweaks.commands.CosmeticsMenuCommand;
import com.storytimeproductions.stweaks.commands.PetsMenuCommand;
import com.storytimeproductions.stweaks.commands.QuestMenuCommand;
import com.storytimeproductions.stweaks.commands.StBoostCommand;
import com.storytimeproductions.stweaks.commands.StLobbyCommand;
import com.storytimeproductions.stweaks.commands.StSpawnCommand;
import com.storytimeproductions.stweaks.commands.StStatusCommand;
import com.storytimeproductions.stweaks.config.SettingsManager;
import com.storytimeproductions.stweaks.events.EventManager;
import com.storytimeproductions.stweaks.listeners.BiomeNotifier;
import com.storytimeproductions.stweaks.listeners.CosmeticsListener;
import com.storytimeproductions.stweaks.listeners.CowSkinnerListener;
import com.storytimeproductions.stweaks.listeners.FbiDiscListener;
import com.storytimeproductions.stweaks.listeners.IllegalWaterListener;
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
import com.storytimeproductions.stweaks.util.PetsManager;
import com.storytimeproductions.stweaks.util.QuestsManager;
import io.papermc.lib.PaperLib;
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

    BiomeTrackerManager trackerManager = new BiomeTrackerManager(dbManager, this);
    QuestsManager questsManager = new QuestsManager(dbManager, this);
    PetsManager petsManager = new PetsManager(this);
    CosmeticsManager cosmeticsManager = new CosmeticsManager(this);

    QuestMenuCommand questMenuCommand = new QuestMenuCommand(questsManager);
    PetsMenuCommand petsMenuCommand = new PetsMenuCommand(this, petsManager);
    CosmeticsMenuCommand cosmeticsMenuCommand = new CosmeticsMenuCommand(cosmeticsManager);

    // Register event listeners
    getServer().getPluginManager().registerEvents(new PlayerActivityListener(this), this);
    getServer().getPluginManager().registerEvents(new CowSkinnerListener(), this);
    getServer().getPluginManager().registerEvents(new FbiDiscListener(), this);
    getServer().getPluginManager().registerEvents(new IllegalWaterListener(this), this);
    getServer().getPluginManager().registerEvents(new LebronArmorListener(this), this);
    getServer().getPluginManager().registerEvents(new BiomeNotifier(this, trackerManager), this);
    getServer()
        .getPluginManager()
        .registerEvents(new QuestMenuListener(this, questsManager, questMenuCommand), this);
    getServer()
        .getPluginManager()
        .registerEvents(new PetsMenuListener(this, petsManager, petsMenuCommand), this);
    getServer().getPluginManager().registerEvents(new PetsListener(this, petsManager), this);
    getServer().getPluginManager().registerEvents(new CosmeticsListener(), this);

    // Register commands
    getCommand("ststatus").setExecutor(new StStatusCommand());
    getCommand("stboost").setExecutor(new StBoostCommand());
    getCommand("stlobby").setExecutor(new StLobbyCommand(getConfig()));
    getCommand("spawn").setExecutor(new StSpawnCommand(getConfig()));
    getCommand("biometracker").setExecutor(new BiomeTrackerCommand(trackerManager, this));
    getCommand("stquests").setExecutor(questMenuCommand);
    getCommand("stpets").setExecutor(petsMenuCommand);
    getCommand("stcosmetics").setExecutor(cosmeticsMenuCommand);

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
    PlaytimeTracker.saveToDatabase(dbManager.getConnection());
    getLogger().info("Stweaks disabled!");
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
}
