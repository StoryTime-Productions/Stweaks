package com.storytimeproductions.stweaks.util;

import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages Boss Bars that show each player's remaining playtime progress.
 *
 * <p>The BossBar updates every second and visualizes the remaining playtime in a "xxmxxs" format,
 * reflecting how close the player is to reaching the daily 60-minute playtime requirement.
 */
public class BossBarManager {

  private static JavaPlugin plugin;
  private static final Map<UUID, BossBar> playerBars = new HashMap<>();

  /**
   * Initializes the BossBarManager and starts periodic updates for all online players.
   *
   * @param pl The plugin instance.
   */
  public static void init(JavaPlugin pl) {
    plugin = pl;

    new BukkitRunnable() {
      @Override
      public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
          updateBossBar(player);
        }
      }
    }.runTaskTimer(plugin, 0L, 20L);
  }

  /**
   * Updates the Boss Bar for the given player to reflect their live playtime countdown.
   *
   * @param player The player to update.
   */
  public static void updateBossBar(Player player) {
    UUID uuid = player.getUniqueId();

    // If the player is in the lobby, show that their timer is paused
    if ("lobby".equalsIgnoreCase(player.getWorld().getName())) {
      BossBar bar =
          playerBars.computeIfAbsent(
              uuid,
              id -> {
                BossBar newBar =
                    BossBar.bossBar(
                        Component.text("Your timer is paused in the lobby. :)"),
                        0.0f,
                        Color.WHITE,
                        Overlay.PROGRESS);
                player.showBossBar(newBar);
                return newBar;
              });

      bar.name(Component.text("Your timer is paused in the lobby. :)"));
      bar.progress(0.0f);
      bar.color(Color.WHITE);
      return;
    }

    // Get total remaining seconds
    long totalSecondsLeft = PlaytimeTracker.getSeconds(uuid);
    totalSecondsLeft = Math.max(totalSecondsLeft, 0);

    // Break down into minutes and remaining seconds
    long minutesLeft = totalSecondsLeft / 60;
    long secondsLeft = totalSecondsLeft % 60;

    if (secondsLeft == 0 && (minutesLeft == 10 || minutesLeft == 5 || minutesLeft == 1)) {
      TablistManager.sendPlaytimeWarningTitle(player, (int) minutesLeft);
    }

    // Calculate progress as a percentage of 60 minutes (3600 seconds)
    double progress = (3600 - totalSecondsLeft) / 3600.0;
    progress = Math.min(1.0, Math.max(0.0, progress));

    if (progress >= 1.0) {
      Component kickMessage =
          Component.text("Your daily hour is up! Come back tomorrow.")
              .color(NamedTextColor.RED)
              .decorate(TextDecoration.BOLD);

      player.kick(kickMessage);
    }

    // AFK check
    boolean isAfk = false;
    if (PlaytimeTracker.getData(uuid) != null) {
      isAfk = PlaytimeTracker.getData(uuid).isAfk();
    }

    String timeFormatted = String.format("%02d:%02d", minutesLeft, secondsLeft);
    String status = "Your remaining time: " + timeFormatted + (isAfk ? " (AFK)" : "");

    final float finalProgress = (float) progress;

    BossBar bar =
        playerBars.computeIfAbsent(
            uuid,
            id -> {
              BossBar newBar =
                  BossBar.bossBar(
                      Component.text(status), finalProgress, Color.GREEN, Overlay.PROGRESS);
              player.showBossBar(newBar);
              return newBar;
            });

    bar.name(Component.text(status));
    bar.progress(finalProgress);

    if (finalProgress >= 1.0) {
      bar.color(Color.BLUE);
      bar.name(Component.text("✅ You've ran out of playtime!"));
    } else if (finalProgress >= 0.5) {
      bar.color(Color.YELLOW);
    } else {
      bar.color(Color.RED);
    }
  }

  /**
   * Removes a player's boss bar (e.g., on logout).
   *
   * @param player The player to remove the boss bar for.
   */
  public static void removeBossBar(Player player) {
    UUID uuid = player.getUniqueId();
    BossBar bar = playerBars.remove(uuid);
    if (bar != null) {
      player.hideBossBar(bar);
    }
  }
}
