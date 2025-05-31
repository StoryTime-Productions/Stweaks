package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Represents a King of the Hill Tag game where players compete to control a designated area.
 * Implements the Minigame interface for game lifecycle management.
 */
public class KothTagGame implements Minigame {
  private final GameConfig config;
  private static List<Player> players = new ArrayList<>();
  private final Map<UUID, Integer> holdTimes = new HashMap<>();
  private Player currentIt = null;
  private int secondsLeft = 60;
  private boolean roundActive = false;
  private Player winner = null;

  private Scoreboard scoreboard;
  private Objective objective;

  private BukkitRunnable itArrowTask = null;

  /**
   * Constructs a new King of the Hill Tag game with the specified configuration.
   *
   * @param config the game configuration
   */
  public KothTagGame(GameConfig config) {
    this.config = config;
  }

  /** Initializes the Battleship game. */
  @Override
  public void onInit() {
    Bukkit.getLogger().info("[KothTag] Initializing game...");
    holdTimes.clear();
    currentIt = null;
    secondsLeft = 60;
    roundActive = false;
    winner = null;
    Bukkit.getLogger().info("[KothTag] Game initialized.");
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {
    Bukkit.getLogger().info("[KothTag] afterInit: Players: " + players.size());
    if (players.size() < 2) {
      for (Player p : players) {
        p.sendMessage(Component.text("Need at least 2 players for KOTH Tag!", NamedTextColor.RED));
      }
      roundActive = false;
      Bukkit.getLogger().info("[KothTag] Not enough players to start.");
      return;
    }
    // Setup scoreboard
    scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    objective =
        scoreboard.registerNewObjective(
            "koth_time", Criteria.DUMMY, Component.text("Tag Score", NamedTextColor.YELLOW));
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    // Pick a random player to be "it"
    currentIt = players.get(new Random().nextInt(players.size()));
    Bukkit.getLogger().info("[KothTag] " + currentIt.getName() + " is initially IT.");
    for (Player p : players) {
      holdTimes.put(p.getUniqueId(), 0);
      Score score = objective.getScore(p.getName());
      score.setScore(0);
      p.setScoreboard(scoreboard);
      if (p.equals(currentIt)) {
        p.sendMessage(Component.text("You are IT! Tag someone else!", NamedTextColor.GOLD));
        giveItItems(p); // Give bow and arrow to the first IT
        // Start repeating arrow task for first IT
        if (itArrowTask != null) {
          itArrowTask.cancel();
        }
        itArrowTask =
            new BukkitRunnable() {
              @Override
              public void run() {
                if (!roundActive || currentIt == null) {
                  this.cancel();
                  return;
                }
                boolean hasArrow = false;
                for (ItemStack item : currentIt.getInventory().getContents()) {
                  if (item != null && item.getType() == Material.TIPPED_ARROW) {
                    hasArrow = true;
                    break;
                  }
                }
                if (!hasArrow) {
                  ItemStack arrow = new ItemStack(Material.TIPPED_ARROW, 1);
                  PotionMeta arrowMeta = (PotionMeta) arrow.getItemMeta();
                  arrowMeta.setBasePotionType(org.bukkit.potion.PotionType.SLOWNESS);
                  arrowMeta.addCustomEffect(
                      new PotionEffect(PotionEffectType.SLOWNESS, 60, 2), true);
                  arrowMeta.displayName(Component.text("Slowness Arrow", NamedTextColor.AQUA));
                  arrow.setItemMeta(arrowMeta);
                  currentIt.getInventory().addItem(arrow);
                  Bukkit.getLogger()
                      .info("[KothTag] Gave slowness arrow to " + currentIt.getName());
                }
              }
            };
        itArrowTask.runTaskTimer(
            Bukkit.getPluginManager().getPlugin("stweaks"), 60L, 60L); // every 3 seconds
      } else {
        p.sendMessage(Component.text(currentIt.getName() + " is IT! Run!", NamedTextColor.YELLOW));
      }
    }
    roundActive = true;
    Bukkit.getLogger().info("[KothTag] Game started.");
  }

  /** Updates the game state. */
  @Override
  public void update() {
    if (!roundActive) {
      return;
    }
    if (secondsLeft <= 0) {
      roundActive = false;
      // Find the player(s) with the highest hold time
      int max = holdTimes.values().stream().max(Integer::compareTo).orElse(0);
      List<Player> winners = new ArrayList<>();
      for (Player p : players) {
        if (holdTimes.getOrDefault(p.getUniqueId(), 0) == max) {
          winners.add(p);
        }
      }
      Bukkit.getLogger()
          .info(
              "[KothTag] Game ended. Winner(s): " + winners.stream().map(Player::getName).toList());
      for (Player p : players) {
        if (winners.contains(p)) {
          givePrize(p, players.size());
          p.sendMessage(
              Component.text(
                  "You win KOTH Tag! Held IT for " + max + " seconds.", NamedTextColor.GOLD));
        } else {
          p.sendMessage(
              Component.text(
                  "Game over! " + winners.get(0).getName() + " wins!", NamedTextColor.YELLOW));
        }
      }
      winner = winners.isEmpty() ? null : winners.get(0);
      // Dispose scoreboard for all players
      for (Player p : players) {
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
      }
      scoreboard = null;
      objective = null;
      return;
    }
    // Give "it" player a title and increment their hold time
    if (currentIt != null && players.contains(currentIt)) {
      int newTime = holdTimes.getOrDefault(currentIt.getUniqueId(), 0) + 1;
      holdTimes.put(currentIt.getUniqueId(), newTime);
      if (objective != null) {
        Score score = objective.getScore(currentIt.getName());
        score.setScore(newTime);
      }
      currentIt.sendActionBar(Component.text("You are IT! Hold on!", NamedTextColor.RED));
    }
    secondsLeft--;
  }

  /** Renders the game state. */
  @Override
  public void render() {
    // No need for action bar, scoreboard shows times
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    Bukkit.getLogger().info("[KothTag] Destroying game and cleaning up.");
    if (itArrowTask != null) {
      itArrowTask.cancel();
      itArrowTask = null;
    }
    if (currentIt != null) {
      removeItItems(currentIt);
    }
    // Dispose scoreboard for all players
    for (Player p : players) {
      p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
    scoreboard = null;
    objective = null;
    players.clear();
    holdTimes.clear();
    currentIt = null;
    winner = null;
    roundActive = false;
    Bukkit.getLogger().info("[KothTag] Cleanup complete.");
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
    holdTimes.remove(player.getUniqueId());
    if (currentIt != null && currentIt.equals(player) && !players.isEmpty()) {
      // Pick a new "it" if the current one leaves
      currentIt = players.get(new Random().nextInt(players.size()));
    }
  }

  /**
   * Gets the list of players currently playing.
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
    return !roundActive;
  }

  private void givePrize(Player player, int amount) {
    ItemStack tickets = new ItemStack(Material.NAME_TAG, amount);
    tickets.getItemMeta().displayName(Component.text("Time Ticket").color(NamedTextColor.GOLD));
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    tickets.setItemMeta(meta);
    player.getInventory().addItem(tickets);
    player.sendMessage(
        Component.text("You win! +" + amount + " Time Tickets!", NamedTextColor.GOLD));
    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
  }

  /**
   * Tags a player as "it" in the game.
   *
   * @param tagger the player who is tagging
   * @param target the player being tagged
   */
  public void tag(Player tagger, Player target) {
    if (!roundActive) {
      return;
    }
    // Only allow a non-IT player to tag the current IT (steal IT status)
    if (currentIt == null || !players.contains(tagger) || !players.contains(target)) {
      return;
    }
    if (tagger.equals(currentIt)) {
      // IT cannot tag anyone
      return;
    }
    if (!target.equals(currentIt)) {
      // Only the current IT can be tagged
      return;
    }

    Bukkit.getLogger().info("[KothTag] " + tagger.getName() + " stole IT from " + target.getName());

    // Remove IT items from previous IT
    removeItItems(currentIt);

    // Set new IT to the tagger
    currentIt = tagger;

    // Give IT items to new IT
    giveItItems(currentIt);

    // Start repeating arrow task for new IT
    if (itArrowTask != null) {
      itArrowTask.cancel();
    }
    itArrowTask =
        new BukkitRunnable() {
          @Override
          public void run() {
            if (!roundActive || currentIt == null) {
              this.cancel();
              return;
            }
            boolean hasArrow = false;
            for (ItemStack item : currentIt.getInventory().getContents()) {
              if (item != null && item.getType() == Material.TIPPED_ARROW) {
                hasArrow = true;
                break;
              }
            }
            if (!hasArrow) {
              ItemStack arrow = new ItemStack(Material.TIPPED_ARROW, 1);
              PotionMeta arrowMeta = (PotionMeta) arrow.getItemMeta();
              arrowMeta.setBasePotionType(org.bukkit.potion.PotionType.SLOWNESS);
              arrowMeta.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2), true);
              arrowMeta.displayName(Component.text("Slowness Arrow", NamedTextColor.AQUA));
              arrow.setItemMeta(arrowMeta);
              currentIt.getInventory().addItem(arrow);
              Bukkit.getLogger().info("[KothTag] Gave slowness arrow to " + currentIt.getName());
            }
          }
        };
    itArrowTask.runTaskTimer(
        Bukkit.getPluginManager().getPlugin("stweaks"), 60L, 60L); // every 3 seconds

    for (Player p : players) {
      if (p.equals(currentIt)) {
        p.sendMessage(Component.text("You stole IT! Now you're IT!", NamedTextColor.GOLD));
      } else if (p.equals(target)) {
        p.sendMessage(
            Component.text(tagger.getName() + " stole IT from you!", NamedTextColor.YELLOW));
      } else {
        p.sendMessage(Component.text(currentIt.getName() + " is now IT!", NamedTextColor.YELLOW));
      }
    }
  }

  // Utility to give infinity bow and a single slowness arrow
  private void giveItItems(Player player) {
    Bukkit.getLogger().info("[KothTag] Giving IT items to " + player.getName());
    // Give infinity bow
    ItemStack bow = new ItemStack(Material.BOW, 1);
    ItemMeta bowMeta = bow.getItemMeta();
    bowMeta.addEnchant(Enchantment.INFINITY, 1, true);
    bowMeta.displayName(Component.text("Tag Bow", NamedTextColor.LIGHT_PURPLE));
    bowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    bow.setItemMeta(bowMeta);
    player.getInventory().addItem(bow);

    // Give a single slowness arrow (3 seconds)
    ItemStack arrow = new ItemStack(Material.TIPPED_ARROW, 1);
    PotionMeta arrowMeta = (PotionMeta) arrow.getItemMeta();
    arrowMeta.setBasePotionType(org.bukkit.potion.PotionType.SLOWNESS);
    arrowMeta.addCustomEffect(
        new PotionEffect(PotionEffectType.SLOWNESS, 60, 2), true); // 60 ticks = 3 seconds
    arrowMeta.displayName(Component.text("Slowness Arrow", NamedTextColor.AQUA));
    arrow.setItemMeta(arrowMeta);
    player.getInventory().addItem(arrow);
  }

  // Utility to remove bow and slowness arrows
  private void removeItItems(Player player) {
    Bukkit.getLogger().info("[KothTag] Removing IT items from " + player.getName());
    player.getInventory().remove(Material.BOW);
    player.getInventory().remove(Material.TIPPED_ARROW);
  }
}
