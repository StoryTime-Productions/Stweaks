package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import com.storytimeproductions.stweaks.games.BattleshipGame;
import com.storytimeproductions.stweaks.games.BlockBreakGame;
import com.storytimeproductions.stweaks.games.BlockPartyGame;
import com.storytimeproductions.stweaks.games.ConnectFourGame;
import com.storytimeproductions.stweaks.games.GuessWhoGame;
import com.storytimeproductions.stweaks.games.KothTagGame;
import com.storytimeproductions.stweaks.games.MemoryPairsGame;
import com.storytimeproductions.stweaks.games.MinesweeperGame;
import com.storytimeproductions.stweaks.games.RockPaperScissorsGame;
import com.storytimeproductions.stweaks.games.RouletteGame;
import com.storytimeproductions.stweaks.games.SpeedCraftingGame;
import com.storytimeproductions.stweaks.games.SpleefGame;
import com.storytimeproductions.stweaks.games.SumoWrestlingGame;
import com.storytimeproductions.stweaks.games.TicTacToeGame;
import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages the lifecycle and player interactions for all minigames. Handles loading games from
 * configuration, player joining, and game state.
 */
public class GameManagerListener implements Listener {
  public static final Map<String, Minigame> activeGames = new HashMap<>();

  private static final Map<String, BukkitRunnable> joinTimers = new ConcurrentHashMap<>();
  private static final Map<String, Set<UUID>> joinedPlayers = new ConcurrentHashMap<>();
  private static final Map<String, Boolean> gameActive = new ConcurrentHashMap<>();

  private static JavaPlugin plugin;

  private static final Map<String, Function<GameConfig, Minigame>> gameFactories = new HashMap<>();

  static {
    gameFactories.put("roulette", RouletteGame::new);
    gameFactories.put("rock_paper_scissors", RockPaperScissorsGame::new);
    gameFactories.put("sumo_wrestling", SumoWrestlingGame::new);
    gameFactories.put("guess_who", GuessWhoGame::new);
    gameFactories.put("battleship", BattleshipGame::new);
    gameFactories.put("spleef", SpleefGame::new);
    gameFactories.put("blockbreak", BlockBreakGame::new);
    gameFactories.put("tictactoe", TicTacToeGame::new);
    gameFactories.put("memorypairs", MemoryPairsGame::new);
    gameFactories.put("connectfour", ConnectFourGame::new);
    gameFactories.put("block_party", BlockPartyGame::new);
    gameFactories.put("speedcrafting", SpeedCraftingGame::new);
    gameFactories.put("kothtag", KothTagGame::new);
    gameFactories.put("minesweeper", MinesweeperGame::new);
  }

  /**
   * Constructor to initialize the GameManagerListener.
   *
   * @param plugin the JavaPlugin instance to use for scheduling tasks
   */
  public GameManagerListener(JavaPlugin plugin) {
    GameManagerListener.plugin = plugin;

    File gamesFile = new File(plugin.getDataFolder(), "games.yml");

    if (gamesFile.exists()) {
      FileConfiguration gamesConfig = YamlConfiguration.loadConfiguration(gamesFile);
      loadGamesFromConfig(gamesConfig);
    }
  }

  /**
   * Gets the map of currently active minigames.
   *
   * @return a map of game IDs to active Minigame instances
   */
  public static Map<String, Minigame> getActiveGames() {
    return activeGames;
  }

  /**
   * Loads games from the provided configuration file. Cancels any running join timers, clears
   * previous state, and initializes new games.
   *
   * @param config the configuration file containing game definitions
   */
  public static void loadGamesFromConfig(FileConfiguration config) {
    for (BukkitRunnable timer : joinTimers.values()) {
      timer.cancel();
    }
    joinTimers.clear();

    for (String gameId : gameActive.keySet()) {
      gameActive.put(gameId, false);
    }
    joinedPlayers.clear();

    for (Minigame minigame : activeGames.values()) {
      minigame.onDestroy();
    }

    activeGames.clear();

    if (config == null) {
      return;
    }
    if (!config.isConfigurationSection("games")) {
      return;
    }

    ConfigurationSection gamesSection = config.getConfigurationSection("games");
    for (String gameId : gamesSection.getKeys(false)) {
      ConfigurationSection section = gamesSection.getConfigurationSection(gameId);

      // Parse locations from comma-separated strings
      Location joinBlockLoc = parseLocation(section.getString("join-block"));
      Location gameAreaLoc = parseLocation(section.getString("game-area"));
      Location exitAreaLoc = parseLocation(section.getString("exit-area"));

      int ticketCost = section.getInt("ticket-cost", 1);
      int playerLimit = section.getInt("player-limit", 1);
      String joinSuccessMessage = section.getString("join-success-message", "Joined!");
      String joinFailMessage = section.getString("join-fail-message", "Failed to join!");
      String winMessage = section.getString("win-message", "You win!");
      String loseMessage = section.getString("lose-message", "You lose!");

      // Parse gameProperties as a map
      ConfigurationSection propsSection = section.getConfigurationSection("gameProperties");
      Map<String, String> gameProperties = new HashMap<>();
      if (propsSection != null) {
        for (String key : propsSection.getKeys(false)) {
          gameProperties.put(key, String.valueOf(propsSection.get(key)));
        }
      }

      GameConfig configObj =
          new GameConfig(
              gameId,
              joinBlockLoc,
              ticketCost,
              gameAreaLoc,
              exitAreaLoc,
              playerLimit,
              joinSuccessMessage,
              joinFailMessage,
              winMessage,
              loseMessage,
              gameProperties);

      Function<GameConfig, Minigame> factory = gameFactories.get(gameId.toLowerCase());
      if (factory != null) {
        Minigame minigame = factory.apply(configObj);
        activeGames.put(gameId, minigame);
      } else {
        Bukkit.getLogger().warning("[STweaks] Unknown game id in config: " + gameId);
      }
    }
  }

  /**
   * Handles player right-click events to allow players to join games by interacting with join
   * blocks.
   *
   * @param event the PlayerInteractEvent triggered in the world
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
      return;
    }
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    Block block = event.getClickedBlock();
    if (block == null) {
      return;
    }
    Player player = event.getPlayer();
    for (Minigame minigame : activeGames.values()) {
      Location joinLoc = minigame.getConfig().getJoinBlock();
      if (joinLoc == null) {
        continue;
      }
      if (!block.getLocation().equals(joinLoc)) {
        continue;
      }

      tryJoinGame(minigame, player, joinLoc);
      event.setCancelled(true);
      break;
    }
  }

  private void tryJoinGame(Minigame minigame, Player player, Location joinLoc) {
    String gameId = minigame.getConfig().getGameId();
    if (gameActive.getOrDefault(gameId, false)) {
      player.sendMessage(NamedTextColor.RED + "Game is already running!");
      return;
    }

    joinedPlayers.putIfAbsent(gameId, new HashSet<>());
    Set<UUID> players = joinedPlayers.get(gameId);
    if (players.size() >= minigame.getConfig().getPlayerLimit()) {
      setJoinIndicator(joinLoc, false);
      player.sendMessage(
          Component.text(minigame.getConfig().getJoinFailMessage(), NamedTextColor.RED));
      return;
    }

    if (!hasTicket(player, minigame.getConfig().getTicketCost())) {
      player.sendMessage(Component.text("You need a Time Ticket to join!", NamedTextColor.RED));
      return;
    }

    consumeTicket(player, minigame.getConfig().getTicketCost());
    players.add(player.getUniqueId());
    minigame.join(player);
    player.displayName(Component.text(player.getName(), NamedTextColor.GREEN));
    player.teleport(minigame.getConfig().getGameArea().clone().add(0, 1, 0));
    player.sendMessage(
        Component.text(minigame.getConfig().getJoinSuccessMessage(), NamedTextColor.GREEN));
    player.sendMessage(
        Component.text(
            "Type /casino leave to leave the game before it begins.", NamedTextColor.YELLOW));
    setJoinIndicator(joinLoc, true);

    if (players.size() < 2) {
      return;
    }

    if (joinTimers.containsKey(gameId)) {
      joinTimers.get(gameId).cancel();
    }
    BukkitRunnable timer =
        new BukkitRunnable() {
          int seconds = 10;

          @Override
          public void run() {
            if (players.size() >= minigame.getConfig().getPlayerLimit()) {
              setJoinIndicator(joinLoc, false);
              this.cancel();
              startGame(minigame);
              return;
            }
            if (seconds <= 0) {
              this.cancel();
              setJoinIndicator(joinLoc, false);
              startGame(minigame);
              return;
            }
            if (players.size() > 1 && players.size() <= minigame.getConfig().getPlayerLimit()) {
              setJoinIndicator(joinLoc, true);
              for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                  p.showTitle(
                      Title.title(
                          Component.text("Game starting in " + seconds, NamedTextColor.YELLOW),
                          Component.empty(),
                          Times.times(
                              java.time.Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)));
                  if (seconds <= 3) {
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
                  }
                }
              }
              seconds--;
            }
          }
        };
    joinTimers.put(gameId, timer);
    timer.runTaskTimer(plugin, 0L, 20L);
  }

  // Helper to parse "world,x,y,z"
  private static Location parseLocation(String str) {
    if (str == null) {
      return null;
    }
    String[] parts = str.split(",");
    if (parts.length != 4) {
      return null;
    }
    World world = Bukkit.getWorld(parts[0]);
    double x = Double.parseDouble(parts[1]);
    double y = Double.parseDouble(parts[2]);
    double z = Double.parseDouble(parts[3]);
    return new Location(world, x, y, z);
  }

  private void setJoinIndicator(Location joinLoc, boolean canJoin) {
    Block above = joinLoc.clone().add(0, 1, 0).getBlock();
    above.setType(canJoin ? Material.LIME_CONCRETE : Material.RED_CONCRETE);
  }

  private boolean hasTicket(Player player, int amount) {
    int found = 0;
    NamespacedKey timeTicketKey = new NamespacedKey("storytime", "time_ticket");
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.hasItemMeta() && item.getItemMeta().hasItemModel()) {
        if (timeTicketKey.equals(item.getItemMeta().getItemModel())) {
          found += item.getAmount();
          if (found >= amount) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void consumeTicket(Player player, int amount) {
    int toRemove = amount;
    NamespacedKey timeTicketKey = new NamespacedKey("storytime", "time_ticket");
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.hasItemMeta() && item.getItemMeta().hasItemModel()) {
        if (timeTicketKey.equals(item.getItemMeta().getItemModel())) {
          int stackAmount = item.getAmount();
          if (stackAmount > toRemove) {
            item.setAmount(stackAmount - toRemove);
            return;
          } else {
            item.setAmount(0);
            toRemove -= stackAmount;
            if (toRemove <= 0) {
              return;
            }
          }
        }
      }
    }
  }

  private void startGame(Minigame minigame) {
    String gameId = minigame.getConfig().getGameId();
    gameActive.put(gameId, true);

    // Call game lifecycle methods
    minigame.onInit();
    minigame.afterInit();

    // Game loop
    BukkitRunnable gameLoop =
        new BukkitRunnable() {
          @Override
          public void run() {
            // If game signals quit, stop loop
            if (minigameShouldQuit(minigame)) {
              this.cancel();
              minigame.onDestroy();
              joinedPlayers.get(gameId).clear();
              gameActive.put(gameId, false);
              setJoinIndicator(minigame.getConfig().getJoinBlock(), true);
              return;
            }
            minigame.update();
            minigame.render();
          }
        };
    gameLoop.runTaskTimer(plugin, 0L, 20L);
  }

  private boolean minigameShouldQuit(Minigame minigame) {
    return minigame.shouldQuit();
  }

  /**
   * Handles player tagging in KothTagGame when one player damages another.
   *
   * @param event the EntityDamageByEntityEvent triggered when a player damages another player
   */
  @EventHandler
  public void onPlayerTag(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
      return;
    }
    Player tagger = (Player) event.getDamager();
    Player target = (Player) event.getEntity();
    for (Minigame minigame : activeGames.values()) {
      if (minigame instanceof KothTagGame) {
        List<Player> tagPlayers = minigame.getPlayers();
        if (tagPlayers.contains(tagger) && tagPlayers.contains(target)) {
          ((KothTagGame) minigame).tag(tagger, target);
        }
      }
    }
  }
}
