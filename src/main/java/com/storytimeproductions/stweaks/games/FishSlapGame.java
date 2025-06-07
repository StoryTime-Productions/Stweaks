package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Represents a Fish Slap game where players can slap each other with fish. Implements the Minigame
 * interface for game lifecycle management.
 */
public class FishSlapGame implements Minigame, Listener {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final Map<Player, Integer> consecutiveWins = new HashMap<>();
  private final Set<Player> outPlayers = new HashSet<>();
  private boolean roundInProgress = false;
  private Player lastWinner = null;
  private Location waitingLoc;

  /**
   * Constructs a new Fish Slap game with the specified configuration.
   *
   * @param config the game configuration
   */
  public FishSlapGame(GameConfig config) {
    this.config = config;

    String[] parts = config.getGameProperties().get("temp-waiting-area").split(",");
    waitingLoc =
        new Location(
            Bukkit.getWorld(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3]));
  }

  /** Initializes the Battleship game. */
  @Override
  public void onInit() {
    Bukkit.getPluginManager().registerEvents(this, Bukkit.getPluginManager().getPlugin("Stweaks"));
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {
    for (Player p : players) {
      giveFish(p);
      consecutiveWins.put(p, 0);
    }
    startRound();
  }

  /** Updates the game state. */
  @Override
  public void update() {
    if (!roundInProgress) {
      return;
    }
    for (Player player : players) {
      if (outPlayers.contains(player)) {
        continue;
      }
      Block blockBelow = player.getLocation().clone().subtract(0, 1, 0).getBlock();
      if (blockBelow.getType() == Material.BLACK_CONCRETE) {
        outPlayers.add(player);
        player.sendMessage(Component.text("You are out for this round!", NamedTextColor.RED));
        player.teleport(waitingLoc);

        if (outPlayers.size() == players.size() - 1) {
          // Find the winner
          for (Player p : players) {
            if (!outPlayers.contains(p)) {
              endRound(p);
              break;
            }
          }
        }
      }
    }
  }

  /** Renders the game state. */
  @Override
  public void render() {
    for (Player p : players) {
      showActionBar(p);
    }
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    for (Player p : players) {
      p.getInventory().remove(Material.COD);
    }
    players.clear();
    consecutiveWins.clear();
    outPlayers.clear();
    lastWinner = null;
  }

  /**
   * Adds the specified player to the game.
   *
   * @param player the player to add
   */
  @Override
  public void join(Player player) {
    if (!players.contains(player)) {
      players.add(player);
      consecutiveWins.put(player, 0);
      giveFish(player);
      player.teleport(config.getGameArea());
    }
  }

  /**
   * Removes the specified player from the game.
   *
   * @param player the player to remove
   */
  @Override
  public void leave(Player player) {
    players.remove(player);
    consecutiveWins.remove(player);
    outPlayers.remove(player);
    player.getInventory().remove(Material.COD);
  }

  /**
   * Gets the list of players currently in the game.
   *
   * @return a list of players in the game
   */
  @Override
  public List<Player> getPlayers() {
    return players;
  }

  /**
   * Gets the game configuration.
   *
   * @return the game configuration
   */
  @Override
  public GameConfig getConfig() {
    return config;
  }

  /**
   * Determines if the game should quit.
   *
   * @return true if the game should quit, false otherwise
   */
  @Override
  public boolean shouldQuit() {
    // The game should quit if any player has 3 or more consecutive wins
    for (int wins : consecutiveWins.values()) {
      if (wins >= 3) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void removeItems(Player player) {
    player.getInventory().remove(Material.COD);
  }

  private void giveFish(Player player) {
    ItemStack fish = new ItemStack(Material.COD, 1);
    ItemMeta meta = fish.getItemMeta();
    meta.displayName(Component.text("Fish Slapper", NamedTextColor.AQUA));
    meta.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 1, true);
    fish.setItemMeta(meta);
    player.getInventory().addItem(fish);
  }

  /**
   * Handles the event when a player is damaged by another entity.
   *
   * @param event the EntityDamageByEntityEvent
   */
  @EventHandler
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!roundInProgress) {
      return;
    }
    if (!(event.getDamager() instanceof Player damager)) {
      return;
    }
    if (!(event.getEntity() instanceof Player victim)) {
      return;
    }
    if (!players.contains(damager) || !players.contains(victim)) {
      return;
    }

    // Check if the damager is holding the Fish Slapper
    ItemStack item = damager.getInventory().getItemInMainHand();
    if (item.getType() == Material.COD
        && item.hasItemMeta()
        && item.getItemMeta().hasDisplayName()
        && Component.text("Fish Slapper", NamedTextColor.AQUA)
            .equals(item.getItemMeta().displayName())) {
      victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_SLIME_BLOCK_STEP, 1.0f, 1.0f);
    }
  }

  private void startRound() {
    roundInProgress = false;
    if (lastWinner == null) {
      for (Player p : players) {
        giveFish(p);
      }
    } else {
      for (Player p : outPlayers) {
        p.teleport(config.getGameArea());
        showActionBar(p);
      }
      outPlayers.clear();
    }

    new BukkitRunnable() {
      int seconds = 3;

      @Override
      public void run() {
        if (seconds > 0) {
          for (Player p : players) {
            p.showTitle(
                Title.title(
                    Component.text("Round starting in", NamedTextColor.YELLOW),
                    Component.text(String.valueOf(seconds), NamedTextColor.GOLD)));
            p.playSound(
                p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f + (3 - seconds) * 0.2f);
          }
          seconds--;
        } else {
          for (Player p : players) {
            p.showTitle(
                Title.title(Component.text("Go!", NamedTextColor.GREEN), Component.empty()));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
          }
          roundInProgress = true;
          this.cancel();
        }
      }
    }.runTaskTimer(Bukkit.getPluginManager().getPlugin("Stweaks"), 0L, 20L);
  }

  private void endRound(Player winner) {
    roundInProgress = false;
    for (Player p : players) {
      if (p.equals(winner)) {
        consecutiveWins.put(p, consecutiveWins.getOrDefault(p, 0) + 1);
      } else {
        consecutiveWins.put(p, 0);
      }
    }
    for (Player p : players) {
      showActionBar(p);
    }
    if (consecutiveWins.get(winner) >= 3) {
      for (Player p : players) {
        p.showTitle(
            Title.title(
                Component.text(winner.getName() + " wins the game!", NamedTextColor.GOLD),
                Component.text("Congratulations!", NamedTextColor.YELLOW)));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
      }
      broadcastToGamePlayers(
          Component.text(winner.getName() + " wins the game!", NamedTextColor.GOLD));
      rewardWinner(winner);
    } else {
      for (Player p : players) {
        p.showTitle(
            net.kyori.adventure.title.Title.title(
                Component.text(winner.getName() + " wins the round!", NamedTextColor.GREEN),
                Component.empty()));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
      }
      broadcastToGamePlayers(
          Component.text(winner.getName() + " wins the round!", NamedTextColor.GREEN));
      lastWinner = winner;
      Bukkit.getScheduler()
          .runTaskLater(Bukkit.getPluginManager().getPlugin("stweaks"), this::startRound, 60L);
    }
  }

  private void rewardWinner(Player winner) {
    ItemStack tickets = new ItemStack(org.bukkit.Material.NAME_TAG, players.size());
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);
    winner.getInventory().addItem(tickets);
    winner.sendMessage(Component.text("Congratulations! You win!", NamedTextColor.GOLD));
  }

  // CHECKSTYLE:OFF: AvoidEscapedUnicodeCharacters
  private void showActionBar(Player player) {
    int wins = consecutiveWins.getOrDefault(player, 0);
    Component bar = Component.empty();
    for (int i = 0; i < 3; i++) {
      if (i < wins) {
        bar = bar.append(Component.text("\u25A0", NamedTextColor.GREEN));
      } else {
        bar = bar.append(Component.text("\u25A0", NamedTextColor.RED));
      }
      if (i < 2) {
        bar = bar.append(Component.space());
      }
    }
    player.sendActionBar(bar);
  }
  // CHECKSTYLE:ON: AvoidEscapedUnicodeCharacters

  private void broadcastToGamePlayers(Component message) {
    for (Player p : players) {
      p.sendMessage(message);
    }
  }
}
