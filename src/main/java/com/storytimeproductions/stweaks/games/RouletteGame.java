package com.storytimeproductions.stweaks.games;

import com.storytimeproductions.models.stgames.BetInfo;
import com.storytimeproductions.models.stgames.GameConfig;
import com.storytimeproductions.models.stgames.Minigame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Roulette game with three tables, each represented by a wall block. Players left-click to confirm,
 * starting a 5s countdown hologram above the wall. Other players can right-click to join the table,
 * which interrupts the countdown. Only main hand interactions are allowed. The table is not picked
 * randomly; the table is determined by the block.
 */
public class RouletteGame implements Minigame {
  private final GameConfig config;
  private static final List<Player> players = new ArrayList<>();
  private final Map<Player, BetInfo> playerBets = new HashMap<>();
  private final Set<Player> lockedPlayers = new HashSet<>();
  private final int maxSlots = 8;
  private final int maxTables = 3;
  private final Map<Integer, Integer> tableSpinTasks = new HashMap<>();
  private Block leaveBlock;

  // Table blocks and their locations
  private final Map<Integer, Block> tableBlocks = new HashMap<>();
  private final Map<Integer, Location> tableHologramLocations = new HashMap<>();

  // Table state
  private final Map<Integer, Set<Player>> tablePlayers = new HashMap<>();
  private final Map<Integer, Integer> tableCountdownTasks = new HashMap<>();
  private final Map<Integer, Integer> tableCountdownSeconds = new HashMap<>();

  // Add these fields to track original hologram lines and slot state
  private final Map<String, List<String>> hologramOriginalLines = new HashMap<>();
  private final Map<String, List<String>> hologramCurrentLines = new HashMap<>();
  private final Map<Integer, Integer> tableCurrentSlot =
      new HashMap<>(); // table -> current slot index

  // Add this field to track player slot and lock state per table
  private final Map<Integer, Map<Player, Integer>> tablePlayerSlots =
      new HashMap<>(); // table -> (player -> slot)
  private final Map<Integer, Set<Player>> tableLockedPlayers =
      new HashMap<>(); // table -> locked players

  /**
   * Constructs a new RouletteGame with the specified configuration.
   *
   * @param config the game configuration containing properties and settings
   */
  public RouletteGame(GameConfig config) {
    this.config = config;

    // Obtain table blocks from config gameProperties as Location objects
    for (int i = 1; i <= maxTables; i++) {
      String key = "table" + i;
      String locString = config.getGameProperties().get(key);
      if (locString != null) {
        String[] parts = locString.split(",");
        if (parts.length == 4) {
          Location loc =
              new Location(
                  Bukkit.getWorld(parts[0]),
                  Double.parseDouble(parts[1]),
                  Double.parseDouble(parts[2]),
                  Double.parseDouble(parts[3]));
          Block block = loc.getBlock();
          tableBlocks.put(i, block);
          tableHologramLocations.put(i, loc.clone().add(0, 1.0, 0));
        }
      }
    }

    String leaveLocString = config.getGameProperties().get("leaveBlock");
    if (leaveLocString != null) {
      String[] parts = leaveLocString.split(",");
      if (parts.length == 4) {
        Location loc =
            new Location(
                Bukkit.getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]));
        leaveBlock = loc.getBlock();
      }
    }

    for (int i = 1; i <= maxTables; i++) {
      tablePlayers.put(i, new HashSet<>());
      tablePlayerSlots.put(i, new HashMap<>());
      tableLockedPlayers.put(i, new HashSet<>());
    }
  }

  @Override
  public void onInit() {
    playerBets.clear();
    lockedPlayers.clear();
    for (int i = 1; i <= maxTables; i++) {
      tablePlayers.get(i).clear();
      cancelTableCountdown(i);
      cancelSpinTask(i);
    }
    initializeSlotHologramLines();
  }

  private void cancelSpinTask(int table) {
    Integer taskId = tableSpinTasks.remove(table);
    if (taskId != null) {
      Bukkit.getScheduler().cancelTask(taskId);
    }
  }

  @Override
  public void afterInit() {
    startNewRound();
  }

  /**
   * Starts a new round of the roulette game. You can implement the logic to reset tables, notify
   * players, etc.
   */
  private void startNewRound() {
    for (int i = 1; i <= maxTables; i++) {
      tablePlayers.get(i).clear();
      cancelTableCountdown(i);
    }
    initializeSlotHologramLines();
  }

  /**
   * Initializes the hologram lines for each slot at each table. This is called when the game is
   * initialized or reset.
   */
  @Override
  public void update() {
    // No periodic logic needed for this version
  }

  /** Renders the game state. This could be used for action bars, holograms, */
  @Override
  public void render() {
    // Could be used for action bars, etc.
  }

  /**
   * Initializes the hologram lines for each slot at each table. This is called when the game is
   * initialized or reset.
   */
  @Override
  public void onDestroy() {
    playerBets.clear();
    lockedPlayers.clear();
    for (int i = 1; i <= maxTables; i++) {
      tablePlayers.get(i).clear();
      cancelTableCountdown(i);
    }
    initializeSlotHologramLines();
  }

  /**
   * Initializes the hologram lines for each slot at each table.
   *
   * @param player the player to initialize holograms for
   */
  @Override
  public void join(Player player) {
    if (!players.contains(player)) {
      players.add(player);
      Location spawnLocation = config.getGameArea();
      if (spawnLocation != null) {
        player.teleport(spawnLocation);
        player.showTitle(
            Title.title(
                Component.text("Roulette Table", NamedTextColor.GOLD),
                Component.text(
                    "Right-click a table wall to pick a slot. Left-click to confirm.",
                    NamedTextColor.YELLOW)));
      } else {
        Bukkit.getLogger()
            .warning("[RouletteGame][DEBUG] No spawn location set in game properties.");
      }
      for (int i = 1; i <= maxTables; i++) {
        ensureSlotHologramLinesCapacity(i);
      }
    }
  }

  /**
   * Removes the specified player from the game and updates holograms.
   *
   * @param player the player to remove
   */
  @Override
  public void leave(Player player) {
    players.remove(player);
    playerBets.remove(player);
    lockedPlayers.remove(player);
    for (int i = 1; i <= maxTables; i++) {
      tablePlayers.get(i).remove(player);
    }

    // Move all slot holograms down by 1 when a player leaves
    int playerCount = players.size();
    if (playerCount >= 2) {
      for (int table = 1; table <= maxTables; table++) {
        for (int slot = 1; slot <= maxSlots; slot++) {
          String color = (slot % 2 == 1) ? "R" : "B";
          String slotHolo = table + "-" + color + "-" + slot;
          // Only move down for each player above the second
          for (int i = 3; i <= playerCount + 1; i++) {
            logAndDispatchDhCommand("dh move " + slotHolo + " ~ -1 ~");
          }
        }
      }
    }
  }

  @Override
  public List<Player> getPlayers() {
    return players;
  }

  @Override
  public GameConfig getConfig() {
    return config;
  }

  @Override
  public boolean shouldQuit() {
    return players.isEmpty();
  }

  @Override
  public void removeItems(Player player) {}

  /** Handles player interaction with a table block. Only main hand interactions are allowed. */
  public void handleTableInteract(PlayerInteractEvent event) {
    if (event.getHand() == null || event.getHand().name().equals("OFF_HAND")) {
      return;
    }
    Player player = event.getPlayer();
    Block clicked = event.getClickedBlock();
    if (clicked == null) {
      return;
    }

    if (leaveBlock != null && clicked.getLocation().equals(leaveBlock.getLocation())) {
      player.performCommand("casino leave");
      event.setCancelled(true);
      return;
    }

    int table = getTableFromBlock(clicked);
    if (table == -1) {
      return;
    }

    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
      handleLeftClickTable(player, table);
    } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
      handleRightClickTable(player, table);
    }
  }

  /** Returns the table number for a given block, or -1 if not a table. */
  private int getTableFromBlock(Block block) {
    for (Map.Entry<Integer, Block> entry : tableBlocks.entrySet()) {
      if (entry.getValue().equals(block)) {
        return entry.getKey();
      }
    }
    return -1;
  }

  /** Handles left-click on a table: confirms and starts countdown if not already started. */
  private void handleLeftClickTable(Player player, int table) {
    Set<Player> tableSet = tablePlayers.get(table);
    Set<Player> locked = tableLockedPlayers.get(table);

    if (!tableSet.contains(player)) {
      player.sendMessage(
          Component.text("You must join the table first by right-clicking.", NamedTextColor.RED));
      return;
    }
    if (locked.contains(player)) {
      player.sendMessage(Component.text("You are already locked in.", NamedTextColor.RED));
      return;
    }

    // Lock the player in
    locked.add(player);

    Map<Player, Integer> playerSlots = tablePlayerSlots.get(table);
    int slot = playerSlots.get(player);
    updateSlotHologram(table, slot, player, "&a" + player.getName()); // green = ready
    player.sendMessage(
        Component.text("You are now locked in on slot " + slot + "!", NamedTextColor.GREEN));

    // Only start countdown if ALL joined players are locked in and at least one
    // player is present
    if (!tableCountdownTasks.containsKey(table)
        && !tableSet.isEmpty()
        && locked.containsAll(tableSet)) {
      startTableCountdown(table);
    } else if (!locked.containsAll(tableSet)) {
      broadcastToTable(
          table, Component.text("Waiting for all players to be ready...", NamedTextColor.YELLOW));
    }
  }

  /** Handles right-click on a table: joins the table and interrupts countdown if running. */
  private void handleRightClickTable(Player player, int table) {
    Set<Player> tableSet = tablePlayers.get(table);
    Map<Player, Integer> playerSlots = tablePlayerSlots.get(table);
    Set<Player> locked = tableLockedPlayers.get(table);

    // If player is locked, do nothing
    if (locked.contains(player)) {
      player.sendMessage(
          Component.text(
              "You are already locked in and cannot change your slot.", NamedTextColor.RED));
      return;
    }

    ensureSlotHologramLinesCapacity(table);

    // First time joining: assign to slot 1
    if (!tableSet.contains(player)) {
      // Check for time-ticket
      if (!consumeTimeTicket(player)) {
        player.sendMessage(Component.text("You need a time-ticket to join!", NamedTextColor.RED));
        return;
      }
      tableSet.add(player);
      playerSlots.put(player, 1);
      updateSlotHologram(table, 1, player, "&e" + player.getName()); // yellow = not ready
      playerBets.put(player, new BetInfo(table, "R", 1));

      // --- Update hologramCurrentLines for slot 1 ---
      String color = "R";
      String slotHolo = table + "-" + color + "-" + 1;
      List<String> curr = hologramCurrentLines.get(slotHolo);
      if (curr != null) {
        // Find the first "-" or empty line after the color tag
        int playerLine = -1;
        for (int i = 1; i < curr.size(); i++) {
          if (curr.get(i).equals("-") || curr.get(i).isEmpty()) {
            playerLine = i;
            break;
          }
        }
        if (playerLine == -1) {
          playerLine = curr.size() - 1;
        }
        curr.set(playerLine, "&e" + player.getName());
      }

      player.sendMessage(
          Component.text("You have joined table " + table + " on slot 1.", NamedTextColor.GREEN));
      // Play ding sound on join
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 2.0f);
      return;
    }

    // Already joined, increment slot (wrap around after 8)
    int currentSlot = playerSlots.get(player);
    int nextSlot = (currentSlot % maxSlots) + 1;

    // Remove name from previous slot
    updateSlotHologram(table, currentSlot, player, "-");

    // --- Update hologramCurrentLines for previous slot ---
    {
      String color = (currentSlot % 2 == 1) ? "R" : "B";
      String slotHolo = table + "-" + color + "-" + currentSlot;
      List<String> curr = hologramCurrentLines.get(slotHolo);
      if (curr != null) {
        for (int i = 1; i < curr.size(); i++) {
          if (curr.get(i).replaceAll("&[0-9a-fl-or]", "").equalsIgnoreCase(player.getName())) {
            curr.set(i, "-");
            break;
          }
        }
      }
    }

    // Add name to new slot
    playerSlots.put(player, nextSlot);
    updateSlotHologram(table, nextSlot, player, "&e" + player.getName());

    // --- Update hologramCurrentLines for new slot ---
    {
      String color = (nextSlot % 2 == 1) ? "R" : "B";
      String slotHolo = table + "-" + color + "-" + nextSlot;
      List<String> curr = hologramCurrentLines.get(slotHolo);
      if (curr != null) {
        // Find the first "-" or empty line after the color tag
        int playerLine = -1;
        for (int i = 1; i < curr.size(); i++) {
          if (curr.get(i).equals("-") || curr.get(i).isEmpty()) {
            playerLine = i;
            break;
          }
        }
        if (playerLine == -1) {
          playerLine = curr.size() - 1;
        }
        curr.set(playerLine, "&e" + player.getName());
      }
    }

    // Play ding sound when setting the next slot hologram
    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 2.0f);

    // Set/update BetInfo for this player
    String colorStr = (nextSlot % 2 == 1) ? "R" : "B";
    // Update this line to match your BetInfo constructor
    playerBets.put(player, new BetInfo(table, colorStr, nextSlot));

    // Interrupt countdown if running
    if (tableCountdownTasks.containsKey(table)) {
      cancelTableCountdown(table);

      // Set the countdown hologram to "Waiting..."
      String holoName = "roulette-table-" + table + "-countdown";
      logAndDispatchDhCommand("dh l set " + holoName + " 1 1 &eWaiting...");
    }
  }

  private void ensureSlotHologramLinesCapacity(int table) {
    int neededLines = players.size() + 1;
    for (int slot = 1; slot <= maxSlots; slot++) {
      String color = (slot % 2 == 1) ? "R" : "B";
      String slotHolo = table + "-" + color + "-" + slot;
      List<String> orig = hologramOriginalLines.get(slotHolo);
      List<String> curr = hologramCurrentLines.get(slotHolo);
      if (orig != null && orig.size() < neededLines) {
        while (orig.size() < neededLines) {
          orig.add("-");
          logAndDispatchDhCommand("dh l add " + slotHolo + " 1 &7-");
        }
      }
      if (curr != null && curr.size() < neededLines) {
        while (curr.size() < neededLines) {
          curr.add("-");
        }
      }
    }
  }

  /** Sends a message to all players at the specified table. */
  private void broadcastToTable(int table, Component message) {
    Set<Player> tableSet = tablePlayers.get(table);
    if (tableSet != null) {
      for (Player p : tableSet) {
        p.sendMessage(message);
      }
    }
  }

  /**
   * Starts a 5-second countdown hologram above the table wall. If another player right-clicks, the
   * countdown is interrupted.
   */
  private void startTableCountdown(int table) {
    tableCountdownSeconds.put(table, 5);
    broadcastToTable(
        table, Component.text("Countdown started! 5 seconds to lock in.", NamedTextColor.YELLOW));

    String holoName = "roulette-table-" + table + "-countdown";

    int taskId =
        Bukkit.getScheduler()
            .scheduleSyncRepeatingTask(
                Bukkit.getPluginManager().getPlugin("stweaks"),
                () -> {
                  Integer secondsObj = tableCountdownSeconds.get(table);
                  if (secondsObj == null) {
                    // Countdown was cancelled, exit task
                    return;
                  }
                  int seconds = secondsObj.intValue();

                  logAndDispatchDhCommand(
                      "dh l set " + holoName + " 1 1 &eCountdown: " + seconds + "s");

                  if (seconds <= 1) {
                    finishTableCountdown(table);
                    cancelTableCountdown(table);
                  } else {
                    tableCountdownSeconds.put(table, seconds - 1);
                  }
                },
                0L,
                20L);
    tableCountdownTasks.put(table, taskId);
  }

  /** Cancels the countdown for a table and removes the hologram. */
  private void cancelTableCountdown(int table) {
    Integer taskId = tableCountdownTasks.remove(table);
    if (taskId != null) {
      Bukkit.getScheduler().cancelTask(taskId);
    }
    tableCountdownSeconds.remove(table);
  }

  /** Called when the countdown finishes for a table. */
  private void finishTableCountdown(int table) {
    // Set the countdown hologram to "Rolling..."
    String holoName = "roulette-table-" + table + "-countdown";

    // For each slot, set all lines to "-"
    for (int slot = 1; slot <= maxSlots; slot++) {
      String color = (slot % 2 == 1) ? "R" : "B";
      String slotHolo = table + "-" + color + "-" + slot;
      List<String> orig = hologramOriginalLines.get(slotHolo);
      if (orig != null) {
        for (int i = 1; i <= orig.size(); i++) {
          logAndDispatchDhCommand("dh l set " + slotHolo + " 1 " + i + " &7-");
        }
      }
    }

    // Start the roulette spin animation
    spinRouletteForTable(table); // This will use tableSpinTasks
    logAndDispatchDhCommand("dh l set " + holoName + " 1 1 &eRolling...");
  }

  private void spinRouletteForTable(int table) {
    if (tableSpinTasks.containsKey(table)) {
      Bukkit.getLogger()
          .warning(
              "[RouletteGame][DEBUG] Spin already running for table "
                  + table
                  + ", not starting another.");
      return;
    }
    int slots = maxSlots;
    int spinDuration = 5 + new Random().nextInt(5); // 5-9 seconds
    int[] delays = new int[slots * spinDuration];
    int delay = 2;
    for (int i = 0; i < delays.length; i++) {
      delays[i] = delay;
      if (i > delays.length * 0.6) {
        delay += 1;
      }
      if (delay > 8) {
        delay = 8;
      }
    }

    tableCurrentSlot.put(table, 1);
    int[] spinIndex = {0};
    final int[] lastSlot = {-1};

    int taskId =
        Bukkit.getScheduler()
            .scheduleSyncRepeatingTask(
                Bukkit.getPluginManager().getPlugin("stweaks"),
                new Runnable() {
                  @Override
                  public void run() {
                    if (spinIndex[0] >= delays.length) {

                      // Determine the final slot and color
                      int finalSlot = lastSlot[0];
                      String finalColor = (finalSlot % 2 == 1) ? "R" : "B";
                      String finalSlotHolo = table + "-" + finalColor + "-" + finalSlot;

                      // Highlight the winning slot for 3 seconds
                      List<String> orig = hologramOriginalLines.get(finalSlotHolo);
                      if (orig != null) {
                        for (int i = 1; i <= orig.size(); i++) {
                          logAndDispatchDhCommand(
                              "dh l set " + finalSlotHolo + " 1 " + i + " &a" + orig.get(i - 1));
                        }
                      }

                      // Play sound for all players at the table
                      Set<Player> tableSet = tablePlayers.get(table);
                      if (tableSet != null) {
                        for (Player player : tableSet) {
                          BetInfo bet = playerBets.get(player);
                          if (bet != null
                              && bet.table == table
                              && bet.slot == finalSlot
                              && bet.color.equalsIgnoreCase(finalColor)) {
                            // Winner sound
                            player.playSound(
                                player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                          } else {
                            // Loser sound
                            player.playSound(
                                player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                          }
                        }
                      }

                      // Wait 3 seconds, then restore all hologram lines and reward players
                      Bukkit.getScheduler()
                          .runTaskLater(
                              Bukkit.getPluginManager().getPlugin("stweaks"),
                              () -> {
                                // Restore all hologram lines for every slot at this table
                                for (int slot = 1; slot <= slots; slot++) {
                                  String color = (slot % 2 == 1) ? "R" : "B";
                                  String sh = table + "-" + color + "-" + slot;
                                  List<String> origLines = hologramOriginalLines.get(sh);
                                  if (origLines != null) {
                                    for (int i = 1; i <= origLines.size(); i++) {
                                      logAndDispatchDhCommand(
                                          "dh l set "
                                              + sh
                                              + " 1 "
                                              + i
                                              + " "
                                              + origLines.get(i - 1));
                                    }
                                  }
                                }

                                // Rewarding logic
                                List<Player> winners = new ArrayList<>();
                                List<Player> losers = new ArrayList<>();
                                for (Player player : tableSet) {
                                  BetInfo bet = playerBets.get(player);
                                  if (bet != null
                                      && bet.table == table
                                      && bet.slot == finalSlot
                                      && bet.color.equalsIgnoreCase(finalColor)) {
                                    winners.add(player);
                                  } else {
                                    losers.add(player);
                                  }
                                }
                                int reward = losers.size() + 1;
                                for (Player winner : winners) {
                                  winner.sendMessage(
                                      Component.text(
                                          "You won! You receive " + reward + " tickets!",
                                          NamedTextColor.GREEN));
                                  rewardWinner(winner, reward);
                                }
                                for (Player loser : losers) {
                                  loser.sendMessage(
                                      Component.text(
                                          "You did not win this round.", NamedTextColor.RED));
                                }
                                Integer spinTaskId = tableSpinTasks.remove(table);

                                String holoName = "roulette-table-" + table + "-countdown";

                                if (spinTaskId != null) {
                                  Bukkit.getScheduler().cancelTask(spinTaskId);
                                  logAndDispatchDhCommand(
                                      "dh l set " + holoName + " 1 1 &eWaiting...");
                                }

                                // After restoring all hologram lines and rewarding players, clear
                                // all players
                                // from the slots:
                                for (int slot = 1; slot <= slots; slot++) {
                                  String color = (slot % 2 == 1) ? "R" : "B";
                                  String slotHolo = table + "-" + color + "-" + slot;
                                  List<String> origLines = hologramOriginalLines.get(slotHolo);
                                  if (origLines != null) {
                                    for (int i = 1; i < origLines.size(); i++) {
                                      origLines.set(i, "-");
                                      logAndDispatchDhCommand(
                                          "dh l set " + slotHolo + " 1 " + (i + 1) + " &7-");
                                    }
                                  }
                                }

                                // Also clear player slot tracking for this table
                                tablePlayers.get(table).clear();
                                tablePlayerSlots.get(table).clear();
                                tableLockedPlayers.get(table).clear();
                              },
                              60L);

                      return;
                    }

                    // Turn off previous slot
                    if (lastSlot[0] != -1) {
                      String color = (lastSlot[0] % 2 == 1) ? "R" : "B";
                      String slotHolo = table + "-" + color + "-" + lastSlot[0];
                      List<String> orig = hologramOriginalLines.get(slotHolo);
                      if (orig != null) {
                        for (int i = 1; i <= orig.size(); i++) {
                          logAndDispatchDhCommand("dh l set " + slotHolo + " 1 " + i + " &7-");
                        }
                      }
                    }

                    // Turn on current slot
                    int currentSlot = (spinIndex[0] % slots) + 1;
                    tableCurrentSlot.put(table, currentSlot);
                    String color = (currentSlot % 2 == 1) ? "R" : "B";
                    String slotHolo = table + "-" + color + "-" + currentSlot;
                    List<String> orig = hologramOriginalLines.get(slotHolo);
                    if (orig != null) {
                      for (int i = 1; i <= orig.size(); i++) {
                        logAndDispatchDhCommand(
                            "dh l set " + slotHolo + " 1 " + i + " " + orig.get(i - 1));
                      }
                    }

                    Set<Player> tableSet = tablePlayers.get(table);
                    if (tableSet != null) {
                      for (Player p : tableSet) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 2.0f);
                      }
                    }

                    lastSlot[0] = currentSlot;
                    spinIndex[0]++;
                  }
                },
                0L,
                4L);
    tableSpinTasks.put(table, taskId);
  }

  private void rewardWinner(Player winner, int amount) {
    ItemStack tickets = new ItemStack(org.bukkit.Material.NAME_TAG, amount);
    ItemMeta meta = tickets.getItemMeta();
    meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
    tickets.setItemMeta(meta);
    winner.getInventory().addItem(tickets);
  }

  // Add this utility method to check and consume a time-ticket:
  private boolean consumeTimeTicket(Player player) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.getType() == org.bukkit.Material.NAME_TAG && item.hasItemMeta()) {
        ItemMeta meta = item.getItemMeta();
        if (meta.hasDisplayName() && meta.displayName().toString().contains("5-minute ticket")) {
          // Consume one ticket
          item.setAmount(item.getAmount() - 1);
          player.updateInventory();
          return true;
        }
      }
    }
    return false;
  }

  // Add this method to initialize the original lines for all slot holograms at
  // game start or round start
  private void initializeSlotHologramLines() {
    int playerCount = players.size();

    for (int table = 1; table <= maxTables; table++) {
      for (int slot = 1; slot <= maxSlots; slot++) {
        String color = (slot % 2 == 1) ? "R" : "B";
        String slotHolo = table + "-" + color + "-" + slot;
        List<String> lines = new ArrayList<>();
        // First line: R or B with color
        if (color.equals("R")) {
          lines.add("&c&lR");
        } else {
          lines.add("&0&lB");
        }
        // The rest of the lines are empty, will be filled with player names as bets are
        // placed
        for (int i = 1; i < players.size() + 1; i++) {
          lines.add("-");
        }
        hologramOriginalLines.put(slotHolo, new ArrayList<>(lines));
        hologramCurrentLines.put(slotHolo, new ArrayList<>(lines));
        clearSlotHologramPlayers(table, slot);

        if (playerCount >= 3) {
          for (int i = 3; i <= playerCount; i++) {
            logAndDispatchDhCommand("dh move " + slotHolo + " ~ 1 ~");
          }
        }
      }
    }
  }

  private void clearSlotHologramPlayers(int table, int slot) {
    String color = (slot % 2 == 1) ? "R" : "B";
    String slotHolo = table + "-" + color + "-" + slot;
    List<String> orig = hologramOriginalLines.get(slotHolo);
    if (orig == null) {
      Bukkit.getLogger()
          .warning("[RouletteGame][DEBUG] Tried to clear non-existent hologram: " + slotHolo);
      return;
    }

    // Ensure the first line is always the correct color tag
    String colorTag = color.equals("R") ? "&c&lR" : "&0&lB";
    logAndDispatchDhCommand("dh l set " + slotHolo + " 1 1 " + colorTag);

    // Remove all player lines (lines 2 to 2 + players.size())
    for (int i = 2; i <= 2 + players.size(); i++) {
      logAndDispatchDhCommand("dh l remove " + slotHolo + " 1 " + i);
    }

    // Set lines 2 to 2 + players.size() - 1 to "-"
    for (int i = 2; i <= 2 + players.size() - 1; i++) {
      logAndDispatchDhCommand("dh l add " + slotHolo + " 1 &7-");
    }
  }

  /**
   * Updates the slot hologram for a given table and slot, replacing the player's name line. If
   * content is "-", removes the player's name from the slot using the remove command.
   */
  private void updateSlotHologram(int table, int slot, Player player, String content) {
    String color = (slot % 2 == 1) ? "R" : "B";
    String slotHolo = table + "-" + color + "-" + slot;
    List<String> orig = hologramOriginalLines.get(slotHolo);
    if (orig == null) {
      Bukkit.getLogger()
          .warning("[RouletteGame][DEBUG] Tried to update non-existent hologram: " + slotHolo);
      return;
    }

    // Try to find the player's line first
    int playerLine = -1;
    for (int i = 1; i < orig.size(); i++) {
      String line = orig.get(i);
      if (line.replaceAll("&[0-9a-fl-or]", "").equalsIgnoreCase(player.getName())) {
        playerLine = i;
        break;
      }
    }
    // If not found, find the first empty line ("-" or "")
    if (playerLine == -1 && !content.equals("-")) {
      for (int i = 1; i < orig.size(); i++) {
        String line = orig.get(i);
        if (line.equals("-") || line.isEmpty()) {
          playerLine = i;
          break;
        }
      }
    }
    // If still not found, fallback to last line
    if (playerLine == -1) {
      playerLine = orig.size() - 1;
    }

    if (content.equals("-")) {
      // Only clear the player's own line
      if (orig.get(playerLine).replaceAll("&[0-9a-fl-or]", "").equalsIgnoreCase(player.getName())) {
        orig.set(playerLine, "-");
        logAndDispatchDhCommand("dh l set " + slotHolo + " 1 " + (playerLine + 1) + " &7-");
      }
    } else {
      orig.set(playerLine, content);
      logAndDispatchDhCommand("dh l set " + slotHolo + " 1 " + (playerLine + 1) + " " + content);
    }
  }

  // Utility method for logging and dispatching DecentHolograms commands
  private void logAndDispatchDhCommand(String command) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
  }
}
