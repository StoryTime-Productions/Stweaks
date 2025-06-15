package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Represents a MobHunt game where players are divided into hunters and innocents. Hunters must find
 * and eliminate innocents disguised as sheep, while innocents must survive the hunt. Implements the
 * Minigame interface for game lifecycle management.
 */
public class MobHunt implements Minigame {
  private final GameConfig config;
  private final List<Player> players = new ArrayList<>();
  private final Set<Player> hunters = new HashSet<>();
  private final Set<Player> innocents = new HashSet<>();
  private boolean gameInProgress = false;
  private long gameStartTime = 0L;
  private final int gameDurationTicks = 20 * 60 * 3; // 3 minutes
  private BossBar timerBar = null;
  private int initialInnocentCount = 0;

  /**
   * Constructs a new MobHunt game with the specified configuration.
   *
   * @param config the game configuration
   */
  public MobHunt(GameConfig config) {
    this.config = config;
  }

  /** Initializes the MobHunt game. */
  @Override
  public void onInit() {
    // Teleport all players to game area and assign roles
    List<Player> allPlayers = new ArrayList<>(players);
    Collections.shuffle(allPlayers);
    int hunterCount = Math.max(1, allPlayers.size() / 5); // 1 hunter per 5 players, at least 1
    for (int i = 0; i < allPlayers.size(); i++) {
      Player p = allPlayers.get(i);
      p.teleport(config.getGameArea());
      if (i < hunterCount) {
        hunters.add(p);
        ItemStack axe = new ItemStack(Material.STONE_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.displayName(Component.text("Hunter's Axe", NamedTextColor.RED));
        axe.setItemMeta(meta);
        p.getInventory().addItem(axe);
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60, 1, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false));
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(), "disguiseplayer " + p.getName() + " wolf");
        p.showTitle(
            Title.title(
                Component.text("You are the HUNTER!", NamedTextColor.RED),
                Component.text("Find and eliminate the innocents!", NamedTextColor.GRAY)));
      } else {
        innocents.add(p);
        // Randomly pick a disguise for the innocent: cow, pig, or sheep
        String[] animals = {"cow", "pig", "sheep"};
        String disguise = animals[(int) (Math.random() * animals.length)];
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(), "disguiseplayer " + p.getName() + " " + disguise);
        p.showTitle(
            Title.title(
                Component.text("You are INNOCENT!", NamedTextColor.GREEN),
                Component.text("Hide and survive!", NamedTextColor.GRAY)));
      }
    }
    gameInProgress = true;
    gameStartTime = System.currentTimeMillis();
    initialInnocentCount = innocents.size();
    // Remove blindness after title disappears (about 3 seconds)
    Bukkit.getScheduler()
        .runTaskLater(
            Bukkit.getPluginManager().getPlugin("stweaks"),
            () -> {
              for (Player p : players) {
                p.removePotionEffect(PotionEffectType.BLINDNESS);
              }
            },
            60L);
  }

  /** Called after the game has been initialized. */
  @Override
  public void afterInit() {}

  /** Updates the game state. */
  @Override
  public void update() {
    if (!gameInProgress) {
      return;
    }

    // Remove dead players from sets
    hunters.removeIf(p -> !p.isOnline() || p.isDead());
    innocents.removeIf(p -> !p.isOnline() || p.isDead());

    // End game if only hunters or only innocents remain, or time is up
    if (innocents.isEmpty()
        || hunters.isEmpty()
        || (System.currentTimeMillis() - gameStartTime)
            > (gameDurationTicks * 50L / 1000L * 1000L)) {
      endGame();
    }
  }

  private void endGame() {
    gameInProgress = false;
    if (timerBar != null) {
      timerBar.removeAll();
      timerBar = null;
    }
    // Reveal all disguises
    for (Player p : players) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "undisguiseplayer " + p.getName());
      p.removePotionEffect(PotionEffectType.BLINDNESS);
    }
    // Announce result using kyori adventure and play sounds
    if (innocents.isEmpty()) {
      Bukkit.broadcast(Component.text("The hunters have won!", NamedTextColor.RED));
      for (Player p : players) {
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
      }
      rewardWinners(hunters);
    } else if (hunters.isEmpty()) {
      Bukkit.broadcast(Component.text("The innocents have won!", NamedTextColor.GREEN));
      for (Player p : players) {
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
      }
      rewardWinners(innocents);
    } else {
      Bukkit.broadcast(Component.text("Time's up! The innocents win!", NamedTextColor.YELLOW));
      for (Player p : players) {
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
      }
      rewardWinners(innocents);
    }
  }

  // Reward all winners (simple example: give a diamond)
  private void rewardWinners(Set<Player> winners) {
    if (initialInnocentCount <= 0) {
      return;
    }
    ItemStack tickets = new ItemStack(org.bukkit.Material.NAME_TAG, initialInnocentCount);
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);

    for (Player p : winners) {
      p.getInventory().addItem(tickets);
    }
  }

  /** Renders the game state. */
  @Override
  public void render() {
    if (!gameInProgress) {
      if (timerBar != null) {
        timerBar.removeAll();
        timerBar = null;
      }
      return;
    }
    long elapsed = (System.currentTimeMillis() - gameStartTime) / 1000;
    long total = (gameDurationTicks / 20);
    long remaining = Math.max(0, total - elapsed);
    long min = remaining / 60;
    long sec = remaining % 60;
    double progress = Math.max(0, Math.min(1, (double) remaining / total));

    if (timerBar == null) {
      timerBar =
          Bukkit.createBossBar(
              String.format("Time left: %d:%02d", min, sec), BarColor.YELLOW, BarStyle.SOLID);
    } else {
      timerBar.setTitle(String.format("Time left: %d:%02d", min, sec));
    }
    timerBar.setProgress(progress);

    // Add all players to the boss bar
    for (Player p : players) {
      timerBar.addPlayer(p);
    }
  }

  /** Cleans up resources when the game is destroyed. */
  @Override
  public void onDestroy() {
    if (timerBar != null) {
      timerBar.removeAll();
      timerBar = null;
    }
    for (Player p : players) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "undisguiseplayer " + p.getName());
      p.removePotionEffect(PotionEffectType.BLINDNESS);
    }
    for (Player p : hunters) {
      removeItems(p);
    }
    hunters.clear();
    innocents.clear();
    players.clear();
    gameInProgress = false;
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
    hunters.remove(player);
    innocents.remove(player);
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "undisguiseplayer " + player.getName());
    player.removePotionEffect(PotionEffectType.BLINDNESS);
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
    return !gameInProgress;
  }

  /**
   * Removes items from the player's inventory when they leave the game.
   *
   * @param player the player whose items should be removed
   */
  @Override
  public void removeItems(Player player) {
    if (hunters.contains(player)) {
      // Remove only the hunter's axe
      ItemStack[] contents = player.getInventory().getContents();
      for (int i = 0; i < contents.length; i++) {
        ItemStack item = contents[i];
        if (item != null
            && item.getType() == Material.STONE_AXE
            && item.hasItemMeta()
            && Component.text("Hunter's Axe", NamedTextColor.RED)
                .equals(item.getItemMeta().displayName())) {
          player.getInventory().setItem(i, null);
        }
      }
    }
  }

  /**
   * Handles player death events to teleport them to the game exit.
   *
   * @param event the PlayerDeathEvent
   */
  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    if (!players.contains(player)) {
      return;
    }
    innocents.remove(player);
    hunters.remove(player);

    Location exit = config.getExitArea();
    event.setCancelled(true);
    Bukkit.getScheduler()
        .runTaskLater(
            Bukkit.getPluginManager().getPlugin("stweaks"), () -> player.teleport(exit), 1L);
  }
}
