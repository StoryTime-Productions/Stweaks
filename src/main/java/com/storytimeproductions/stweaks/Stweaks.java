package com.storytimeproductions.stweaks;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by StoryTime Productions on 2025-04-17.
 * 
 * 
 */

public class Stweaks extends JavaPlugin {

  private static Stweaks instance;

  @Override
  public void onEnable() {
    instance = this;

    // Suggest using Paper
    PaperLib.suggestPaper(this);

    // Load configuration
    saveDefaultConfig();

    // Register commands
    registerCommands();

    // Register events
    registerEvents();

    getLogger().info("STweaks has been enabled.");
  }

  @Override
  public void onDisable() {
    getLogger().info("STweaks has been disabled.");
  }

  public static Stweaks getInstance() {
    return instance;
  }

  private void registerCommands() {
    PluginCommand sampleCommand = getCommand("stweaks");
    if (sampleCommand != null) {
    } else {
      getLogger().warning("Command 'stweaks' is not defined in plugin.yml");
    }
  }

  private void registerEvents() {
    // Example: registerListener(new ExampleListener());
  }

  /**
   * Registers a Bukkit event listener for this plugin.
   *
   * @param listener The event listener to register.
   */
  public void registerListener(Listener listener) {
    Bukkit.getPluginManager().registerEvents(listener, this);
  }

}
