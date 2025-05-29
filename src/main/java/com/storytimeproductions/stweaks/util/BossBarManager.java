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
    if (!player.getWorld().getName().startsWith("world")) {
      double totalSecondsLeftRaw = PlaytimeTracker.getData(uuid).getAvailableSeconds();
      final double totalSecondsLeft = Math.max(totalSecondsLeftRaw, 0);

      int h = (int) (totalSecondsLeft / 3600);
      int m = (int) ((totalSecondsLeft % 3600) / 60);
      int s = (int) (totalSecondsLeft % 60);
      String timeString = String.format("%02d:%02d:%02d", h, m, s);

      String pausedStatus = "Your remaining time: " + timeString + " (PAUSED)";

      BossBar bar =
          playerBars.computeIfAbsent(
              uuid,
              id -> {
                BossBar newBar =
                    BossBar.bossBar(
                        Component.text(pausedStatus), 0.0f, Color.WHITE, Overlay.PROGRESS);
                player.showBossBar(newBar);
                return newBar;
              });

      bar.name(Component.text(pausedStatus));
      bar.progress(0.0f);
      bar.color(Color.WHITE);
      return;
    }

    // Always use 3600 as the baseline for the progress bar
    final double baselineSeconds = 3600.0;

    // Get total remaining seconds
    double totalSecondsLeftRaw = PlaytimeTracker.getData(uuid).getAvailableSeconds();
    final double totalSecondsLeft = Math.max(totalSecondsLeftRaw, 0);

    // If player has 0 or less seconds, teleport them to the lobby world using
    if (totalSecondsLeft <= 0 && player.getWorld().getName().startsWith("world")) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lobby");
      player.sendMessage(
          Component.text("Your daily hour is up! Come back tomorrow.")
              .color(NamedTextColor.RED)
              .decorate(TextDecoration.BOLD));
      return;
    }

    // Break down into hours, minutes, and remaining seconds
    double hoursLeft = totalSecondsLeft / 3600;
    double minutesLeft = (totalSecondsLeft % 3600) / 60;
    double secondsLeft = totalSecondsLeft % 60;

    // Only show warning if hoursLeft is 0
    if (hoursLeft == 0
        && secondsLeft == 0
        && (minutesLeft == 10 || minutesLeft == 5 || minutesLeft == 1)) {
      TablistManager.sendPlaytimeWarningTitle(player, (int) minutesLeft);
    }

    // Progress logic: Only start decreasing when <= 3600 seconds left
    double progress;
    if (totalSecondsLeft > baselineSeconds) {
      progress = 1.0;
    } else {
      progress = totalSecondsLeft / baselineSeconds;
      progress = Math.max(0.0, Math.min(1.0, progress));
    }

    // AFK check
    boolean isAfk = false;
    if (PlaytimeTracker.getData(uuid) != null) {
      isAfk = PlaytimeTracker.getData(uuid).isAfk();
    }

    String timeFormatted;
    if (hoursLeft > 0) {
      timeFormatted =
          String.format("%02d:%02d:%02d", (int) hoursLeft, (int) minutesLeft, (int) secondsLeft);
    } else {
      timeFormatted = String.format("%02d:%02d", (int) minutesLeft, (int) secondsLeft);
    }
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
