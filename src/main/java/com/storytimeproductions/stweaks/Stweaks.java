package com.storytimeproductions.stweaks;

import com.storytimeproductions.stweaks.commands.StBoostCommand;
import com.storytimeproductions.stweaks.commands.StStatusCommand;
import com.storytimeproductions.stweaks.config.SettingsManager;
import com.storytimeproductions.stweaks.events.EventManager;
import com.storytimeproductions.stweaks.listeners.AfkListener;
import com.storytimeproductions.stweaks.listeners.PlayerActivityListener;
import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import com.storytimeproductions.stweaks.util.BossBarManager;
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

    // Register event listeners
    getServer().getPluginManager().registerEvents(new PlayerActivityListener(), this);
    getServer().getPluginManager().registerEvents(new AfkListener(), this);

    // Register commands
    getCommand("ststatus").setExecutor(new StStatusCommand());
    getCommand("stboost").setExecutor(new StBoostCommand());

    // Initialize playtime tracker and event manager
    PlaytimeTracker.init(this);
    EventManager.init(this);
    BossBarManager.init(this);

    getLogger().info("Stweaks enabled!");
  }

  /**
   * Called when the plugin is disabled. This method is responsible for shutting down services like
   * the playtime tracker.
   */
  @Override
  public void onDisable() {
    PlaytimeTracker.shutdown();
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
