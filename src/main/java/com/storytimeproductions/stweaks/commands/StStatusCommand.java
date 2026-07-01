package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.stweaks.config.SettingsManager;
import com.storytimeproductions.stweaks.playtime.PlaytimeData;
import com.storytimeproductions.stweaks.playtime.PlaytimeTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Command for checking the player's current playtime status.
 *
 * <p>This command retrieves and displays the amount of time a player has played for the current day
 * and compares it against the required playtime as set in the plugin settings for that specific
 * player.
 */
public class StStatusCommand implements CommandExecutor {

  private final JavaPlugin plugin;

  /**
   * Constructor for the StStatusCommand class.
   *
   * @param plugin The JavaPlugin instance.
   */
  public StStatusCommand(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // /status boost <boostAmount>
    if (args.length == 2 && args[0].equalsIgnoreCase("boost")) {
      if (!sender.hasPermission("stweaks.status.boost")) {
        sender.sendMessage(
            Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                .append(
                    Component.text(
                        "You don't have permission to boost the multiplier.", NamedTextColor.RED)));
        return true;
      }
      try {
        double boostAmount = Double.parseDouble(args[1]);
        double currentBoost = 0.0;
        if (SettingsManager.getConfig().contains("activeBoost.amount")) {
          currentBoost = SettingsManager.getConfig().getDouble("activeBoost.amount");
        }
        double newBoost = currentBoost + boostAmount;

        SettingsManager.reload();
        SettingsManager.getConfig().set("activeBoost.amount", newBoost);
        SettingsManager.saveConfig();

        Bukkit.getServer()
            .sendMessage(
                Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                    .append(
                        Component.text(
                            (sender instanceof Player p ? p.getName() : sender.getName())
                                + " has added "
                                + boostAmount
                                + " to the timer multiplier! Expires 8 AM EST.",
                            NamedTextColor.YELLOW)));
      } catch (NumberFormatException e) {
        sender.sendMessage(
            Component.text("[Stweaks] ", NamedTextColor.YELLOW)
                .append(
                    Component.text(
                        "Invalid boost amount. Please enter a number.", NamedTextColor.RED)));
      }
      return true;
    }

    // /ststatus reset <player>
    if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
      if (!sender.hasPermission("stweaks.ststatus.reset")) {
        sender.sendMessage("§cYou don't have permission to reset playtime.");
        return true;
      }

      String targetName = args[1];
      OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
      UUID targetIdentifer = target.getUniqueId();

      if (targetIdentifer == null) {
        sender.sendMessage("§cCould not find player: " + targetName);
        return true;
      }

      if (PlaytimeTracker.getData(targetIdentifer) != null) {
        PlaytimeTracker.resetPlaytime(targetIdentifer);
        sender.sendMessage("§aReset playtime for §f" + targetName + "§a.");
      } else {
        sender.sendMessage("§eNo playtime data found for " + targetName + ".");
      }
      return true;
    }

    // /ststatus add <player> <seconds>
    if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
      if (!sender.hasPermission("stweaks.ststatus.add")) {
        sender.sendMessage("You don't have permission to add playtime.");
        return true;
      }

      long secondsToAdd;
      try {
        secondsToAdd = Long.parseLong(args[2]);
      } catch (NumberFormatException e) {
        sender.sendMessage("Invalid number of seconds: " + args[2]);
        return false;
      }

      List<Player> targets = resolveTargets(sender, args[1]);
      if (targets == null) {
        return false;
      }

      for (Player target : targets) {
        UUID targetIdentifer = target.getUniqueId();
        PlaytimeData data = PlaytimeTracker.getData(targetIdentifer);
        if (data == null) {
          data = new PlaytimeData();
          PlaytimeTracker.setPlaytime(targetIdentifer, data);
        }

        boolean success = data.addAvailableSeconds(secondsToAdd);
        sender.sendMessage("Added " + secondsToAdd + " seconds to " + target.getName() + ".");

        if (!success) {
          sender.sendMessage(
              "Failed to add seconds. The player must have at least 10 minutes of playtime.");
        }
      }
      return true;
    }

    // /ststatus ticket <player>
    if (args.length == 2 && args[0].equalsIgnoreCase("ticket")) {
      if (!sender.hasPermission("stweaks.ststatus.ticket")) {
        sender.sendMessage("You don't have permission to use this command.");
        return true;
      }

      List<Player> targets = resolveTargets(sender, args[1]);
      if (targets == null) {
        return false;
      }

      boolean anySuccess = false;
      for (Player target : targets) {
        PlaytimeData data = PlaytimeTracker.getData(target.getUniqueId());
        if (data == null) {
          sender.sendMessage("No playtime data found for " + target.getName() + ".");
          continue;
        }
        if (data.getAvailableSeconds() - 300 >= 600) {
          data.addAvailableSeconds(-300);
          ItemStack ticket = new ItemStack(Material.NAME_TAG);
          ItemMeta meta = ticket.getItemMeta();
          meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
          meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
          meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
          meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
          ticket.setItemMeta(meta);
          target.getInventory().addItem(ticket);
          target.sendMessage("You received a 5-minute ticket!");
          anySuccess = true;
        } else {
          sender.sendMessage(
              target.getName()
                  + " must have more than 10 minutes remaining after removing 5 minutes.");
          target.sendMessage("Ticketing failed. You must have more than 10 minutes remaining.");
        }
      }
      return anySuccess;
    }

    // /ststatus cash <player>
    if (args.length == 2 && args[0].equalsIgnoreCase("cash")) {
      if (!sender.hasPermission("stweaks.ststatus.cash")) {
        sender.sendMessage("You don't have permission to use this command.");
        return true;
      }

      List<Player> targets = resolveTargets(sender, args[1]);
      if (targets == null) {
        return false;
      }

      boolean anySuccess = false;
      for (Player target : targets) {
        PlaytimeData data = PlaytimeTracker.getData(target.getUniqueId());
        if (data == null) {
          sender.sendMessage("No playtime data found for " + target.getName() + ".");
          continue;
        }
        if (data.getAvailableSeconds() <= 3300) {
          ItemStack[] contents = target.getInventory().getContents();
          boolean found = false;
          for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.NAME_TAG && item.hasItemMeta()) {
              ItemMeta meta = item.getItemMeta();
              NamespacedKey key = new NamespacedKey("storytime", "time_ticket");
              NamespacedKey model = meta.getItemModel();
              if (key.equals(model)) {
                if (item.getAmount() > 1) {
                  item.setAmount(item.getAmount() - 1);
                } else {
                  target.getInventory().setItem(i, null);
                }
                data.addAvailableSeconds(300);
                target.sendMessage("You cashed in a 5-minute ticket!");
                sender.sendMessage("Cashed a 5-minute ticket for " + target.getName() + ".");
                found = true;
                anySuccess = true;
                break;
              }
            }
          }
          if (!found) {
            sender.sendMessage(target.getName() + " does not have a 5-minute ticket to cash.");
            target.sendMessage("You do not have a 5-minute ticket to cash.");
          }
        } else {
          sender.sendMessage(
              target.getName()
                  + " cannot cash a ticket unless they have 55 minutes or less remaining.");
          target.sendMessage(
              "You can only cash a ticket if you have 55 minutes or less remaining.");
        }
      }
      return anySuccess;
    }

    if (args.length == 1 && args[0].equalsIgnoreCase("afk")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage("Only players can use this command.");
        return true;
      }
      PlaytimeData data = PlaytimeTracker.getData(player.getUniqueId());
      boolean newValue = !data.isKickOnAfkTimeout();
      data.setKickOnAfkTimeout(newValue);
      player.sendMessage(
          "AFK handling set to: "
              + (newValue ? "Kick on AFK timeout" : "Timer decreases while AFK"));
      return true;
    }

    // /ststatus [admin]
    if (sender instanceof Player player) {
      boolean isAdmin = args.length > 0 && args[0].equalsIgnoreCase("admin");
      if (isAdmin && player.isOp()) {
        openAdminStatusInventory(player, 0);
        return true;
      } else {
        openStatusInventory(player);
        return true;
      }
    }
    return false;
  }

  /**
   * Resolves "@a", "@p", or a player name to a list of online players. Returns null and sends an
   * error message to the sender if the target cannot be resolved.
   */
  private List<Player> resolveTargets(CommandSender sender, String targetName) {
    List<Player> targets = new ArrayList<>();

    if (targetName.equalsIgnoreCase("@a")) {
      targets.addAll(Bukkit.getOnlinePlayers());
      return targets;
    }

    if (targetName.equalsIgnoreCase("@p")) {
      Player nearest = null;
      if (sender instanceof BlockCommandSender blockSender) {
        double minDist = Double.MAX_VALUE;
        for (Player p : Bukkit.getOnlinePlayers()) {
          if (p.getWorld().equals(blockSender.getBlock().getWorld())) {
            double dist =
                p.getLocation().distance(blockSender.getBlock().getLocation().add(0.5, 0.5, 0.5));
            if (dist < minDist) {
              minDist = dist;
              nearest = p;
            }
          }
        }
      } else if (sender instanceof Player playerSender) {
        nearest = playerSender;
      } else {
        nearest = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
      }
      if (nearest != null) {
        targets.add(nearest);
      }
      return targets;
    }

    Player player = Bukkit.getPlayerExact(targetName);
    if (player == null) {
      sender.sendMessage("Could not find player: " + targetName);
      return null;
    }
    targets.add(player);
    return targets;
  }

  /**
   * Schedules a repeating task that updates a single inventory slot every second while the player
   * keeps a given inventory open. Cancels automatically when the inventory is closed.
   */
  private void runWhileOpen(Player player, Component expectedTitle, Runnable update) {
    new BukkitRunnable() {
      @Override
      public void run() {
        if (!expectedTitle.equals(player.getOpenInventory().title())) {
          cancel();
          return;
        }
        update.run();
      }
    }.runTaskTimer(plugin, 20L, 20L);
  }

  private void openAdminStatusInventory(Player admin, int page) {
    int maxItems = 14;
    List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
    int totalPages = (int) Math.ceil(onlinePlayers.size() / (double) maxItems);
    if (page < 0) {
      page = 0;
    }
    if (page >= totalPages) {
      page = totalPages - 1;
    }

    final int currentPage = page;

    Inventory inv =
        Bukkit.createInventory(
            null,
            54,
            Component.text(
                "Admin: Player Status (Page "
                    + (currentPage + 1)
                    + "/"
                    + Math.max(1, totalPages)
                    + ")"));

    ItemStack borderPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta paneMeta = borderPane.getItemMeta();
    paneMeta.displayName(Component.text(" "));
    borderPane.setItemMeta(paneMeta);
    for (int i = 0; i < 54; i++) {
      if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
        inv.setItem(i, borderPane);
      }
    }

    final int[] itemsPlacedArr = {0};
    int startIndex = page * maxItems;
    int endIndex = Math.min(startIndex + maxItems, onlinePlayers.size());
    for (int y = 1; y <= 4; y++) {
      for (int x = 1; x <= 7; x++) {
        if ((x + y) % 2 == 0 && startIndex + itemsPlacedArr[0] < endIndex) {
          Player target = onlinePlayers.get(startIndex + itemsPlacedArr[0]);
          inv.setItem(y * 9 + x, buildSkullItem(target));
          itemsPlacedArr[0]++;
        }
      }
    }

    ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta grayMeta = grayPane.getItemMeta();
    grayMeta.displayName(Component.text(" "));
    grayPane.setItemMeta(grayMeta);

    if (page > 0) {
      ItemStack prev = new ItemStack(Material.ARROW);
      ItemMeta prevMeta = prev.getItemMeta();
      prevMeta.displayName(Component.text("Previous Page", NamedTextColor.YELLOW));
      prev.setItemMeta(prevMeta);
      inv.setItem(46, prev);
    } else {
      inv.setItem(46, grayPane);
    }
    if (page < totalPages - 1) {
      ItemStack next = new ItemStack(Material.ARROW);
      ItemMeta nextMeta = next.getItemMeta();
      nextMeta.displayName(Component.text("Next Page", NamedTextColor.YELLOW));
      next.setItemMeta(nextMeta);
      inv.setItem(52, next);
    } else {
      inv.setItem(52, grayPane);
    }

    admin.openInventory(inv);

    String expectedTitle =
        "Admin: Player Status (Page " + (currentPage + 1) + "/" + Math.max(1, totalPages) + ")";
    new BukkitRunnable() {
      @Override
      public void run() {
        if (!admin.getOpenInventory().title().toString().contains(expectedTitle)) {
          cancel();
          return;
        }
        Inventory openInv = admin.getOpenInventory().getTopInventory();
        int[] placed = {0};
        for (int y = 1; y <= 4; y++) {
          for (int x = 1; x <= 7; x++) {
            if ((x + y) % 2 == 0 && startIndex + placed[0] < endIndex) {
              Player target = onlinePlayers.get(startIndex + placed[0]);
              openInv.setItem(y * 9 + x, buildSkullItem(target));
              placed[0]++;
            }
          }
        }
      }
    }.runTaskTimer(plugin, 20L, 20L);
  }

  private static ItemStack buildSkullItem(Player target) {
    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
    ItemMeta meta = skull.getItemMeta();
    meta.displayName(Component.text(target.getName()).color(NamedTextColor.AQUA));
    if (meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
      skullMeta.setOwningPlayer(target);
    }
    PlaytimeData data = PlaytimeTracker.getData(target.getUniqueId());
    double secondsLeft = data != null ? data.getAvailableSeconds() : 0;
    int h = (int) (secondsLeft / 3600);
    int m = (int) ((secondsLeft % 3600) / 60);
    int s = (int) (secondsLeft % 60);
    meta.lore(
        List.of(
            Component.text(
                "Time Left: " + String.format("%02d:%02d:%02d", h, m, s), NamedTextColor.GREEN)));
    skull.setItemMeta(meta);
    return skull;
  }

  /**
   * Opens a 27-slot inventory for the admin to manage another player's playtime.
   *
   * @param admin The player who is managing the other player's playtime.
   * @param target The player whose playtime is being managed.
   * @param plugin The JavaPlugin instance.
   */
  public static void openPlayerManageInventory(Player admin, Player target, Plugin plugin) {
    Inventory inv = Bukkit.createInventory(null, 27, Component.text("Manage: " + target.getName()));

    ItemStack add = new ItemStack(Material.LIME_WOOL);
    ItemMeta addMeta = add.getItemMeta();
    addMeta.displayName(Component.text("Add 5 Minutes").color(NamedTextColor.GREEN));
    add.setItemMeta(addMeta);
    inv.setItem(12, add);

    ItemStack remove = new ItemStack(Material.RED_WOOL);
    ItemMeta removeMeta = remove.getItemMeta();
    removeMeta.displayName(Component.text("Remove 5 Minutes").color(NamedTextColor.RED));
    remove.setItemMeta(removeMeta);
    inv.setItem(10, remove);

    ItemStack ticket = new ItemStack(Material.NAME_TAG);
    ItemMeta ticketMeta = ticket.getItemMeta();
    ticketMeta.displayName(Component.text("Give 5-Minute Ticket").color(NamedTextColor.GOLD));
    ticket.setItemMeta(ticketMeta);
    inv.setItem(14, ticket);

    ItemStack cash = new ItemStack(Material.PAPER);
    ItemMeta cashMeta = cash.getItemMeta();
    cashMeta.displayName(Component.text("Cash 5-Minute Ticket").color(NamedTextColor.YELLOW));
    cash.setItemMeta(cashMeta);
    inv.setItem(16, cash);

    inv.setItem(4, buildTimeItem(target));
    admin.openInventory(inv);

    Component title = Component.text("Manage: " + target.getName());
    new BukkitRunnable() {
      @Override
      public void run() {
        if (!title.equals(admin.getOpenInventory().title())) {
          cancel();
          return;
        }
        admin.getOpenInventory().setItem(4, buildTimeItem(target));
      }
    }.runTaskTimer(plugin, 20L, 20L);

    admin.openInventory(inv);
  }

  private static ItemStack buildTimeItem(Player target) {
    PlaytimeData data = PlaytimeTracker.getData(target.getUniqueId());
    double secondsLeft = data != null ? data.getAvailableSeconds() : 0;
    int h = (int) (secondsLeft / 3600);
    int m = (int) ((secondsLeft % 3600) / 60);
    int s = (int) (secondsLeft % 60);
    String timeString = String.format("%02d:%02d:%02d", h, m, s);

    ItemStack timeLeft = new ItemStack(Material.CLOCK);
    ItemMeta timeMeta = timeLeft.getItemMeta();
    timeMeta.displayName(Component.text("Time Left").color(NamedTextColor.WHITE));
    timeMeta.lore(List.of(Component.text("Time Left: " + timeString, NamedTextColor.GREEN)));
    timeLeft.setItemMeta(timeMeta);
    return timeLeft;
  }

  /**
   * Opens a 54-slot inventory for the player, with black panes on the edges and information items
   * in the center.
   */
  private void openStatusInventory(Player player) {
    Component title = Component.text("Your Playtime Status");
    Inventory inv = Bukkit.createInventory(null, 54, title);

    ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta paneMeta = blackPane.getItemMeta();
    paneMeta.displayName(Component.text(" "));
    blackPane.setItemMeta(paneMeta);
    for (int i = 0; i < 9; i++) {
      inv.setItem(i, blackPane);
      inv.setItem(45 + i, blackPane);
    }
    for (int i = 1; i < 5; i++) {
      inv.setItem(i * 9, blackPane);
      inv.setItem(i * 9 + 8, blackPane);
    }

    PlaytimeData data = PlaytimeTracker.getData(player.getUniqueId());
    double secondsLeft = data != null ? data.getAvailableSeconds() : 0;

    double hours = secondsLeft / 3600;
    double minutes = (secondsLeft % 3600) / 60;
    double seconds = secondsLeft % 60;
    String formatted = formatTimeLeft(hours, minutes, seconds);

    ItemStack timeLeft = new ItemStack(Material.CLOCK);
    ItemMeta timeMeta = timeLeft.getItemMeta();
    timeMeta.displayName(
        Component.text("Time Left")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    timeMeta.lore(
        wrapLoreLine(formatted, 25).stream()
            .map(
                line ->
                    Component.text(line, NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, true))
            .toList());
    timeLeft.setItemMeta(timeMeta);
    inv.setItem(20, timeLeft);

    double baseMultiplier = PlaytimeTracker.getBaseMultiplier();
    ItemStack multiplierItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
    ItemMeta multiplierMeta = multiplierItem.getItemMeta();
    multiplierMeta.displayName(
        Component.text("Server Multiplier")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    List<String> multiplierLoreRaw = new ArrayList<>();
    multiplierLoreRaw.add("Base Multiplier: " + baseMultiplier + "x");
    multiplierLoreRaw.add(" ");
    double weekendMultiplier = PlaytimeTracker.getWeekendMultiplier();
    boolean isWeekend = PlaytimeTracker.isWeekend();
    String weekendStatus = isWeekend ? "active" : "inactive";
    multiplierLoreRaw.add("Weekend Multiplier: " + weekendMultiplier + "x (" + weekendStatus + ")");
    multiplierLoreRaw.add(" ");
    double socialMultiplier = PlaytimeTracker.computeGlobalSocialMultiplier();
    multiplierLoreRaw.add("Social Multiplier: " + socialMultiplier + "x");
    multiplierLoreRaw.add(" ");
    multiplierLoreRaw.add("Total Multiplier: " + PlaytimeTracker.getTotalMultiplier() + "x");
    List<Component> multiplierLore = new ArrayList<>();
    for (String line : multiplierLoreRaw) {
      wrapLoreLine(line, 25)
          .forEach(
              l ->
                  multiplierLore.add(
                      Component.text(l, NamedTextColor.GREEN)
                          .decoration(TextDecoration.ITALIC, true)));
    }
    multiplierMeta.lore(multiplierLore);
    multiplierItem.setItemMeta(multiplierMeta);
    inv.setItem(22, multiplierItem);

    ItemStack socialInfo = new ItemStack(Material.PLAYER_HEAD);
    ItemMeta socialMetaInfo = socialInfo.getItemMeta();
    socialMetaInfo.displayName(
        Component.text("Social Multiplier Info")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    List<Component> socialLoreInfo = new ArrayList<>();
    for (String line :
        List.of(
            "If you remain in proximity to other players, the social multiplier may increase!",
            " ",
            "The activation distance is "
                + ((int) PlaytimeTracker.getSocialDistance())
                + " blocks.")) {
      wrapLoreLine(line, 25)
          .forEach(
              l ->
                  socialLoreInfo.add(
                      Component.text(l, NamedTextColor.GREEN)
                          .decoration(TextDecoration.ITALIC, true)));
    }
    socialMetaInfo.lore(socialLoreInfo);
    socialInfo.setItemMeta(socialMetaInfo);
    inv.setItem(30, socialInfo);

    ItemStack dailyHour = new ItemStack(Material.SLIME_BALL);
    ItemMeta dailyMeta = dailyHour.getItemMeta();
    dailyMeta.displayName(
        Component.text("Daily Hour Reset")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    List<Component> dailyLore = new ArrayList<>();
    for (String line :
        List.of(
            "Every day at 4 AM, playtime is reset back to an hour.",
            " ",
            "You can use tickets to fill your playtime bar back up!")) {
      wrapLoreLine(line, 25)
          .forEach(
              l ->
                  dailyLore.add(
                      Component.text(l, NamedTextColor.GREEN)
                          .decoration(TextDecoration.ITALIC, true)));
    }
    dailyMeta.lore(dailyLore);
    dailyHour.setItemMeta(dailyMeta);
    inv.setItem(32, dailyHour);

    int tickets = secondsLeft > 600 ? (int) ((secondsLeft - 900) / 300) : 0;
    ItemStack ticketItem = new ItemStack(Material.PAPER);
    ItemMeta ticketMeta = ticketItem.getItemMeta();
    ticketMeta.displayName(
        Component.text("5-Minute Casino Tickets")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    List<String> ticketLoreRaw =
        new ArrayList<>(
            wrapLoreLine("You can convert your remaining time into " + tickets + " tickets.", 25));
    ticketLoreRaw.add(" ");
    ticketLoreRaw.add("Tickets can be used to gamble playtime at the casino! (/casino)");
    List<Component> ticketLoreLines = new ArrayList<>();
    for (String line : ticketLoreRaw) {
      wrapLoreLine(line, 25)
          .forEach(
              l ->
                  ticketLoreLines.add(
                      Component.text(l, NamedTextColor.GREEN)
                          .decoration(TextDecoration.ITALIC, true)));
    }
    ticketMeta.lore(ticketLoreLines);
    ticketItem.setItemMeta(ticketMeta);
    inv.setItem(24, ticketItem);

    int bankedTickets = data != null ? data.getBankedTickets() : 0;

    ItemStack bankedItem = new ItemStack(Material.ENDER_CHEST);
    ItemMeta bankedMeta = bankedItem.getItemMeta();
    bankedMeta.displayName(
        Component.text("Banked 5-Minute Chunks")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    List<Component> bankedLore = new ArrayList<>();
    wrapLoreLine(
            "You have "
                + bankedTickets
                + " banked 5-minute chunk"
                + (bankedTickets == 1 ? "" : "s")
                + ".",
            25)
        .forEach(
            l ->
                bankedLore.add(
                    Component.text(l, NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, true)));
    bankedLore.add(Component.text(" "));
    int maxBankable = secondsLeft > 600 ? (int) ((secondsLeft - 900) / 300) : 0;
    wrapLoreLine(
            "You can bank up to " + maxBankable + " more chunk" + (maxBankable == 1 ? "" : "s"), 25)
        .forEach(
            l ->
                bankedLore.add(
                    Component.text(l, NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, true)));
    bankedMeta.lore(bankedLore);
    bankedItem.setItemMeta(bankedMeta);
    inv.setItem(49, bankedItem);

    ItemStack removeBanked = new ItemStack(Material.RED_STAINED_GLASS_PANE);
    ItemMeta removeMeta = removeBanked.getItemMeta();
    removeMeta.displayName(
        Component.text("Remove 5-minutes from Bank")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.RED));
    removeMeta
        .getPersistentDataContainer()
        .set(
            new NamespacedKey(plugin, "remove_banked"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "removeBanked");
    removeBanked.setItemMeta(removeMeta);
    inv.setItem(48, removeBanked);

    ItemStack addBanked = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
    ItemMeta addMeta = addBanked.getItemMeta();
    addMeta.displayName(
        Component.text("Add 5-minutes from Bank")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.GREEN));
    addMeta
        .getPersistentDataContainer()
        .set(
            new NamespacedKey(plugin, "add_banked"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "addBanked");
    addBanked.setItemMeta(addMeta);
    inv.setItem(50, addBanked);

    player.openInventory(inv);

    runWhileOpen(
        player,
        title,
        () -> {
          PlaytimeData d = PlaytimeTracker.getData(player.getUniqueId());
          double sl = d != null ? d.getAvailableSeconds() : 0;
          double h = sl / 3600;
          double m = (sl % 3600) / 60;
          double s = sl % 60;
          String f = formatTimeLeft(h, m, s);
          ItemStack clock = new ItemStack(Material.CLOCK);
          ItemMeta cm = clock.getItemMeta();
          cm.displayName(
              Component.text("Time Left")
                  .decoration(TextDecoration.ITALIC, false)
                  .color(NamedTextColor.WHITE));
          cm.lore(
              wrapLoreLine(f, 25).stream()
                  .map(
                      line ->
                          Component.text(line, NamedTextColor.GREEN)
                              .decoration(TextDecoration.ITALIC, true))
                  .toList());
          clock.setItemMeta(cm);
          player.getOpenInventory().setItem(20, clock);
        });
  }

  /** Wraps a string into lines of maxLineLength, not breaking words. */
  private static List<String> wrapLoreLine(String text, int maxLineLength) {
    List<String> lines = new ArrayList<>();
    if (text.trim().isEmpty()) {
      lines.add(" ");
      return lines;
    }
    String[] words = text.split(" ");
    StringBuilder line = new StringBuilder();
    for (String word : words) {
      if (line.length() + word.length() + 1 > maxLineLength) {
        lines.add(line.toString());
        line = new StringBuilder();
      }
      if (line.length() > 0) {
        line.append(" ");
      }
      line.append(word);
    }
    if (!line.isEmpty()) {
      lines.add(line.toString());
    }
    return lines;
  }

  /** Formats the time left as a human-readable string. */
  private static String formatTimeLeft(double hours, double minutes, double seconds) {
    int h = (int) hours;
    int m = (int) minutes;
    int s = (int) seconds;
    StringBuilder sb = new StringBuilder("You have ");
    boolean hasHours = h > 0;
    boolean hasMinutes = m > 0;
    boolean hasSeconds = s > 0;

    if (hasHours) {
      sb.append(h).append(" hour").append(h == 1 ? "" : "s");
    }
    if (hasMinutes) {
      if (hasHours) {
        sb.append(", ");
      }
      sb.append(m).append(" minute").append(m == 1 ? "" : "s");
    }
    if (hasSeconds) {
      if (hasHours || hasMinutes) {
        sb.append(" and ");
      }
      sb.append(s).append(" second").append(s == 1 ? "" : "s");
    }
    sb.append(" left");
    return sb.toString();
  }
}
