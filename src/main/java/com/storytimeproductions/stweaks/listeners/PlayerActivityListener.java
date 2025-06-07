package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.stweaks.commands.StStatusCommand;
import com.storytimeproductions.stweaks.playtime.PlaytimeData;
import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import com.storytimeproductions.stweaks.util.BossBarManager;
import com.storytimeproductions.stweaks.util.TablistManager;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Listens for player movement and connection events to track activity and manage UI elements.
 *
 * <p>This listener also performs AFK detection based on player movement. Players who remain
 * inactive for a configured threshold (5 minutes) are marked as AFK in the {@link PlaytimeTracker}.
 */
public class PlayerActivityListener implements Listener {

  private static final HashMap<UUID, Long> lastMovement = new HashMap<>();
  private static final long AFK_THRESHOLD_MILLIS = 1 * 60 * 1000;

  /**
   * Constructs a new {@code PlayerActivityListener} and starts a repeating task to check AFK
   * statuses.
   *
   * @param plugin The main plugin instance used to schedule tasks.
   */
  public PlayerActivityListener(JavaPlugin plugin) {
    // Start the periodic AFK checker
    new BukkitRunnable() {
      @Override
      public void run() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
          TablistManager.updateTablist(player, PlaytimeTracker.getTotalMultiplier());
          UUID uuid = player.getUniqueId();
          updateBelowName(
              player, PlaytimeTracker.getData(player.getUniqueId()).getAvailableSeconds());
          long lastActive = lastMovement.getOrDefault(uuid, now);
          boolean afk = (now - lastActive) > AFK_THRESHOLD_MILLIS;
          PlaytimeTracker.setAfk(uuid, afk);
        }
      }
    }.runTaskTimer(plugin, 0L, 20); // Check every 30 seconds
  }

  /**
   * Handles the player movement event.
   *
   * <p>This method updates the last movement timestamp for the player and marks them as not AFK.
   *
   * @param event The {@link PlayerMoveEvent} containing movement information.
   */
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    lastMovement.put(uuid, System.currentTimeMillis());
    PlaytimeTracker.setAfk(uuid, false); // Reset AFK on movement
  }

  /**
   * Handles the player join event.
   *
   * <p>Initializes the player's movement timestamp, updates the BossBar and tablist UI, and makes
   * the player execute the /lobby command upon joining the server.
   *
   * @param event The {@link PlayerJoinEvent} containing the joining player's information.
   */
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    lastMovement.put(uuid, System.currentTimeMillis()); // Initialize last movement
    BossBarManager.updateBossBar(player);
    TablistManager.updateTablist(player, PlaytimeTracker.getTotalMultiplier());

    // Make the player execute /lobby on join
    Bukkit.getScheduler()
        .runTaskLater(
            Bukkit.getPluginManager().getPlugin("Stweaks"),
            () -> player.performCommand("lobby"),
            10L);
  }

  /**
   * Handles the player quit event.
   *
   * <p>Removes the player's last movement entry and cleans up their BossBar UI element.
   *
   * @param event The {@link PlayerQuitEvent} containing the quitting player's information.
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    lastMovement.remove(uuid);
    BossBarManager.removeBossBar(event.getPlayer());
    if (!event.getPlayer().getWorld().getName().startsWith("world")) {
      event
          .getPlayer()
          .getActivePotionEffects()
          .forEach(
              effect -> {
                event.getPlayer().removePotionEffect(effect.getType());
              });
    }
  }

  /**
   * Prevents users from moving items in the "Your Playtime Status" inventory.
   *
   * @param event The {@link InventoryClickEvent} containing inventory click information.
   */
  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getView() == null) {
      return;
    }
    String title = event.getView().title().toString();
    Player player = (Player) event.getWhoClicked();

    // Admin view: select player
    if (title.contains("Admin: Player Status")) {
      event.setCancelled(true);
      ItemStack clicked = event.getCurrentItem();
      if (clicked != null && clicked.getType() == Material.PLAYER_HEAD) {
        String playerName =
            PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        if (playerName == null || playerName.isEmpty()) {
          player.sendMessage("Could not determine player name from head.");
          return;
        }
        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null) {
          StStatusCommand.openPlayerManageInventory(
              player, target, Bukkit.getPluginManager().getPlugin("stweaks"));
        }
      }
    }

    // Player manage view: add/remove time, ticket/cash
    if (title.contains("Manage: ")) {
      String rawTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

      event.setCancelled(true);
      ItemStack clicked = event.getCurrentItem();
      if (clicked == null) {
        return;
      }
      String[] split = rawTitle.split(": ");
      if (split.length < 2) {
        return;
      }
      String playerName = split[1];
      Player target = Bukkit.getPlayerExact(playerName);
      if (target == null) {
        return;
      }
      PlaytimeData data = PlaytimeTracker.getData(target.getUniqueId());
      switch (event.getSlot()) {
        case 12: // Add 5 minutes
          data.addAvailableSeconds(300);
          player.sendMessage("Added 5 minutes to " + playerName);
          break;
        case 10: // Remove 5 minutes
          data.addAvailableSeconds(-300);
          player.sendMessage("Removed 5 minutes from " + playerName);
          break;
        case 14: // Give ticket
          player.performCommand("status ticket " + playerName);
          break;
        case 16: // Cash ticket
          player.performCommand("status cash " + playerName);
          break;
        default:
          break;
      }
      StStatusCommand.openPlayerManageInventory(
          player, target, Bukkit.getPluginManager().getPlugin("stweaks"));
    }

    if (title != null && title.contains("Your Playtime Status")) {
      event.setCancelled(true);

      // Handle add/remove banked chunk buttons
      if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) {
        return;
      }
      var meta = event.getCurrentItem().getItemMeta();
      var pdc = meta.getPersistentDataContainer();

      // Add banked chunk
      if (pdc.has(
          new org.bukkit.NamespacedKey("stweaks", "add_banked"),
          org.bukkit.persistence.PersistentDataType.STRING)) {
        var data = PlaytimeTracker.getData(player.getUniqueId());
        if (data == null) {
          return;
        }
        double secondsLeft = data.getAvailableSeconds();
        if (secondsLeft - 300 > 600) {
          data.setBankedTickets(data.getBankedTickets() + 1);
          data.addAvailableSeconds(-300);
          player.sendMessage("Added a 5-minute chunk to your bank!");
        } else {
          player.sendMessage(
              "You cannot bank more chunks (must keep more than 10 minutes remaining).");
        }
        player.performCommand("status");
        return;
      }

      // Remove banked chunk
      if (pdc.has(
          new org.bukkit.NamespacedKey("stweaks", "remove_banked"),
          org.bukkit.persistence.PersistentDataType.STRING)) {
        var data =
            com.storytimeproductions.stweaks.playtime.PlaytimeTracker.getData(player.getUniqueId());
        if (data == null) {
          return;
        }
        int banked = data.getBankedTickets();
        if (banked > 0) {
          if (data.getAvailableSeconds() < 3600 - 300) {
            data.setBankedTickets(banked - 1);
            data.addAvailableSeconds(300);
            player.sendMessage("Removed a 5-minute chunk from your bank!");
          } else {
            player.sendMessage("You can only add time if you have less than 55 minutes remaining.");
          }
        } else {
          player.sendMessage("You have no banked chunks to remove.");
        }
        player.performCommand("status");
        return;
      }
    }
  }

  /**
   * Updates the player's scoreboard to show the time left below their name.
   *
   * @param player The player whose scoreboard to update.
   * @param secondsLeft The number of seconds left to display.
   */
  public static void updateBelowName(Player player, double secondsLeft) {
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    Objective objective = scoreboard.getObjective("timeleft");
    if (objective == null) {
      objective =
          scoreboard.registerNewObjective("timeleft", Criteria.DUMMY, Component.text("s left"));
      objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    int totalSeconds = (int) secondsLeft;

    // Set the score for this player (do not reset others!)
    Score score = objective.getScore(player.getName());
    score.setScore(totalSeconds);

    // Assign the main scoreboard to the player (if not already)
    if (player.getScoreboard() != scoreboard) {
      player.setScoreboard(scoreboard);
    }
  }
}
