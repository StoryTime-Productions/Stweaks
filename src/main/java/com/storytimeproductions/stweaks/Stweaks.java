package com.storytimeproductions.stweaks;

import io.papermc.lib.PaperLib;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by StoryTime Productions on 2025-04-17.
 *
 * @author Copyright (c) StoryTime Productions. All Rights Reserved.
 */
public class Stweaks extends JavaPlugin {

  @Override
  public void onEnable() {
    PaperLib.suggestPaper(this);

    saveDefaultConfig();
  }
}
