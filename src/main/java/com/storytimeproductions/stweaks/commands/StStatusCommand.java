package com.storytimeproductions.stweaks.commands;

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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

      String targetName = args[1];
      long secondsToAdd;
      try {
        secondsToAdd = Long.parseLong(args[2]);
      } catch (NumberFormatException e) {
        sender.sendMessage("Invalid number of seconds: " + args[2]);
        return false;
      }

      List<Player> targets = new ArrayList<>();
      if (targetName.equalsIgnoreCase("@a")) {
        targets.addAll(Bukkit.getOnlinePlayers());
      } else if (targetName.equalsIgnoreCase("@p")) {
        Player nearest = null;
        if (sender instanceof BlockCommandSender blockSender) {
          // Find nearest player to the command block
          double minDist = Double.MAX_VALUE;
          for (Player p : Bukkit.getOnlinePlayers()) {
            double dist =
                p.getLocation().distance(blockSender.getBlock().getLocation().add(0.5, 0.5, 0.5));
            if (dist < minDist) {
              minDist = dist;
              nearest = p;
            }
          }
        } else if (sender instanceof Player playerSender) {
          nearest = playerSender;
        } else {
          // Console fallback: pick a random online player
          nearest = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        }
        if (nearest != null) {
          targets.add(nearest);
        }
      } else {
        Player player = Bukkit.getPlayerExact(targetName);
        if (player != null) {
          targets.add(player);
        } else {
          sender.sendMessage("Could not find player: " + targetName);
          return false;
        }
      }

      boolean anySuccess = false;
      for (Player target : targets) {
        UUID targetIdentifer = target.getUniqueId();
        PlaytimeData data = PlaytimeTracker.getData(targetIdentifer);
        boolean success;
        if (data == null) {
          data = new PlaytimeData();
          PlaytimeTracker.setPlaytime(targetIdentifer, data);
        }

        success = data.addAvailableSeconds(secondsToAdd);
        sender.sendMessage("Added " + secondsToAdd + " seconds to " + target.getName() + ".");

        if (!success) {
          sender.sendMessage(
              "Failed to add seconds. The player must have at least 10 minutes of playtime.");
        }
      }
      return anySuccess;
    }

    // /ststatus ticket <player>
    if (args.length == 2 && args[0].equalsIgnoreCase("ticket")) {
      if (!sender.hasPermission("stweaks.ststatus.ticket")) {
        sender.sendMessage("You don't have permission to use this command.");
        return true;
      }
      String targetName = args[1];
      List<Player> targets = new ArrayList<>();
      if (targetName.equalsIgnoreCase("@a")) {
        targets.addAll(Bukkit.getOnlinePlayers());
      } else if (targetName.equalsIgnoreCase("@p")) {
        Player nearest = null;
        if (sender instanceof BlockCommandSender blockSender) {
          double minDist = Double.MAX_VALUE;
          for (Player p : Bukkit.getOnlinePlayers()) {
            double dist =
                p.getLocation().distance(blockSender.getBlock().getLocation().add(0.5, 0.5, 0.5));
            if (dist < minDist) {
              minDist = dist;
              nearest = p;
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
      } else {
        Player player = Bukkit.getPlayerExact(targetName);
        if (player != null) {
          targets.add(player);
        } else {
          sender.sendMessage("Could not find player: " + targetName);
          return false;
        }
      }

      boolean anySuccess = false;
      for (Player target : targets) {
        PlaytimeData data = PlaytimeTracker.getData(target.getUniqueId());
        if (data == null) {
          sender.sendMessage("No playtime data found for " + target.getName() + ".");
          continue;
        }
        // Only allow if after removing 5 minutes, available seconds will be > 600
        if (data.getAvailableSeconds() - 300 >= 600) {
          data.addAvailableSeconds(-300);
          // Give the player a nametag named "5-minute ticket" with custom model data
          ItemStack ticket = new ItemStack(Material.NAME_TAG);
          ItemMeta meta = ticket.getItemMeta();
          meta.displayName(Component.text("5-minute ticket").color(NamedTextColor.GOLD));
          // Set custom item_model using PersistentDataContainer
          meta.setItemModel(new NamespacedKey("storytime", "time_ticket"));
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
      String targetName = args[1];
      List<Player> targets = new ArrayList<>();
      if (targetName.equalsIgnoreCase("@a")) {
        targets.addAll(Bukkit.getOnlinePlayers());
      } else if (targetName.equalsIgnoreCase("@p")) {
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
      } else {
        Player player = Bukkit.getPlayerExact(targetName);
        if (player != null) {
          targets.add(player);
        } else {
          sender.sendMessage("Could not find player: " + targetName);
          return false;
        }
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
                // Remove one ticket
                if (item.getAmount() > 1) {
                  item.setAmount(item.getAmount() - 1);
                } else {
                  target.getInventory().setItem(i, null);
                }
                // Add 5 minutes (300 seconds)
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

    if (sender instanceof Player player) {
      openStatusInventory(player);
      return true;
    }
    return false;
  }

  /**
   * Opens a 54-slot inventory for the player, with black panes on the edges and information items
   * in the center.
   */
  private void openStatusInventory(Player player) {
    Inventory inv =
        Bukkit.createInventory(
            null, 54, net.kyori.adventure.text.Component.text("Your Playtime Status"));

    // Fill edges with black stained glass panes
    ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta paneMeta = blackPane.getItemMeta();
    paneMeta.displayName(net.kyori.adventure.text.Component.text(" "));
    blackPane.setItemMeta(paneMeta);

    // Top and bottom rows
    for (int i = 0; i < 9; i++) {
      inv.setItem(i, blackPane);
      inv.setItem(45 + i, blackPane);
    }
    // Left and right columns
    for (int i = 1; i < 5; i++) {
      inv.setItem(i * 9, blackPane);
      inv.setItem(i * 9 + 8, blackPane);
    }

    // Get playtime data
    PlaytimeData data = PlaytimeTracker.getData(player.getUniqueId());
    double secondsLeft = data != null ? data.getAvailableSeconds() : 0;

    // Format as HH:MM:SS
    double hours = secondsLeft / 3600;
    double minutes = (secondsLeft % 3600) / 60;
    double seconds = secondsLeft % 60;

    // Format as "You have X hour(s), Y minute(s) and Z second(s) left"
    String formatted = formatTimeLeft(hours, minutes, seconds);

    // Info item: Time left, formatted and wrapped (slot 20)
    ItemStack timeLeft = new ItemStack(Material.CLOCK);
    ItemMeta timeMeta = timeLeft.getItemMeta();
    timeMeta.displayName(
        Component.text("Time Left")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    String loreText = formatted;
    timeMeta.lore(
        wrapLoreLine(loreText, 25).stream()
            .map(
                line ->
                    Component.text(line, NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, true))
            .toList());
    timeLeft.setItemMeta(timeMeta);
    inv.setItem(20, timeLeft);

    // Info item: Server multiplier (slot 22)
    double baseMultiplier = PlaytimeTracker.getBaseMultiplier();

    ItemStack multiplierItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
    ItemMeta multiplierMeta = multiplierItem.getItemMeta();
    multiplierMeta.displayName(
        Component.text("Server Multiplier")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE)); // Title is white
    List<String> multiplierLoreRaw = new ArrayList<>();
    multiplierLoreRaw.add("Base Multiplier: " + baseMultiplier + "x");
    multiplierLoreRaw.add(" ");

    double weekendMultiplier = PlaytimeTracker.getWeekendMultiplier();
    boolean isWeekend = isWeekend();
    if (isWeekend) {
      multiplierLoreRaw.add("Weekend Multiplier: " + weekendMultiplier + "x (active)");
    } else {
      multiplierLoreRaw.add("Weekend Multiplier: " + weekendMultiplier + "x (inactive)");
    }
    multiplierLoreRaw.add(" ");

    double socialMultiplier = PlaytimeTracker.computeGlobalSocialMultiplier();
    multiplierLoreRaw.add("Social Multiplier: " + socialMultiplier + "x");
    multiplierLoreRaw.add(" ");

    double totalMultiplier = PlaytimeTracker.getTotalMultiplier();
    multiplierLoreRaw.add("Total Multiplier: " + totalMultiplier + "x");
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

    // Slot 30: Social Multiplier Info
    ItemStack socialInfo = new ItemStack(Material.PLAYER_HEAD);
    ItemMeta socialMetaInfo = socialInfo.getItemMeta();
    socialMetaInfo.displayName(
        Component.text("Social Multiplier Info")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    List<String> socialLoreInfoRaw = new ArrayList<>();
    socialLoreInfoRaw.add(
        "If you remain in proximity to other players, the social multiplier may increase!");
    socialLoreInfoRaw.add(" ");
    socialLoreInfoRaw.add(
        "The activation distance is " + ((int) PlaytimeTracker.getSocialDistance()) + " blocks.");
    List<Component> socialLoreInfo = new ArrayList<>();
    for (String line : socialLoreInfoRaw) {
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

    // Slot 32: Daily Hour Info
    ItemStack dailyHour = new ItemStack(Material.SLIME_BALL);
    ItemMeta dailyMeta = dailyHour.getItemMeta();
    dailyMeta.displayName(
        Component.text("Daily Hour Reset")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    List<String> dailyLoreRaw = new ArrayList<>();
    dailyLoreRaw.add("Every day at 1 AM, playtime is reset back to an hour.");
    dailyLoreRaw.add(" ");
    dailyLoreRaw.add("You can use tickets to fill your playtime bar back up!");
    List<Component> dailyLore = new ArrayList<>();
    for (String line : dailyLoreRaw) {
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

    // Info item: 5-minute tickets (minus 10 minutes)
    int tickets = 0;
    if (secondsLeft > 600) {
      tickets = (int) ((secondsLeft - 900) / 300);
    }

    ItemStack ticketItem = new ItemStack(Material.PAPER);
    ItemMeta ticketMeta = ticketItem.getItemMeta();
    ticketMeta.displayName(
        Component.text("5-Minute Casino Tickets")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    String ticketLore = "You can convert your remaining time into " + tickets + " tickets.";
    List<String> ticketLoreRaw = new ArrayList<>(wrapLoreLine(ticketLore, 25));
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

    int maxBankable = 0;
    if (secondsLeft > 600) {
      maxBankable = (int) ((secondsLeft - 900) / 300);
    }

    // Bottom row, middle slot (slot 49): Banked 5-Minute Chunks
    ItemStack bankedItem = new ItemStack(Material.ENDER_CHEST);
    ItemMeta bankedMeta = bankedItem.getItemMeta();
    bankedMeta.displayName(
        Component.text("Banked 5-Minute Chunks")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE));
    String bankedLoreText =
        "You have "
            + bankedTickets
            + " banked 5-minute chunk"
            + (bankedTickets == 1 ? "" : "s")
            + ".";
    String bankedLoreText2 =
        "You can bank up to " + maxBankable + " more chunk" + (maxBankable == 1 ? "" : "s");
    List<Component> bankedLore = new ArrayList<>();
    wrapLoreLine(bankedLoreText, 25)
        .forEach(
            l ->
                bankedLore.add(
                    Component.text(l, NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, true)));
    bankedLore.add(Component.text(" "));
    wrapLoreLine(bankedLoreText2, 25)
        .forEach(
            l ->
                bankedLore.add(
                    Component.text(l, NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, true)));
    bankedMeta.lore(bankedLore);
    bankedItem.setItemMeta(bankedMeta);
    inv.setItem(49, bankedItem);

    // Slot 48: Red pane (remove banked)
    ItemStack removeBanked = new ItemStack(Material.RED_STAINED_GLASS_PANE);
    ItemMeta removeMeta = removeBanked.getItemMeta();
    removeMeta.displayName(
        Component.text("Remove 5-minutes from Bank")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.RED));
    removeMeta
        .getPersistentDataContainer()
        .set(
            new org.bukkit.NamespacedKey(plugin, "remove_banked"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "removeBanked");
    removeBanked.setItemMeta(removeMeta);
    inv.setItem(48, removeBanked);

    // Slot 49: Green pane (add banked)
    ItemStack addBanked = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
    ItemMeta addMeta = addBanked.getItemMeta();
    addMeta.displayName(
        Component.text("Add 5-minutes from Bank")
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.GREEN));
    addMeta
        .getPersistentDataContainer()
        .set(
            new org.bukkit.NamespacedKey(plugin, "add_banked"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "addBanked");
    addBanked.setItemMeta(addMeta);
    inv.setItem(50, addBanked);

    player.openInventory(inv);

    // Update slot 20 every second with formatted time
    new BukkitRunnable() {
      @Override
      public void run() {
        Component inventoryTitle = player.getOpenInventory().title();
        if (!Component.text("Your Playtime Status").equals(inventoryTitle)) {
          cancel();
          return;
        }
        PlaytimeData data = PlaytimeTracker.getData(player.getUniqueId());
        double secondsLeft = data != null ? data.getAvailableSeconds() : 0;
        double hours = secondsLeft / 3600;
        double minutes = (secondsLeft % 3600) / 60;
        double seconds = secondsLeft % 60;
        String formatted = formatTimeLeft(hours, minutes, seconds);
        String loreText = formatted;
        ItemStack timeLeft = new ItemStack(Material.CLOCK);
        ItemMeta timeMeta = timeLeft.getItemMeta();
        timeMeta.displayName(
            Component.text("Time Left")
                .decoration(TextDecoration.ITALIC, false)
                .color(NamedTextColor.WHITE));
        timeMeta.lore(
            wrapLoreLine(loreText, 25).stream()
                .map(
                    line ->
                        Component.text(line, NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, true))
                .toList());
        timeLeft.setItemMeta(timeMeta);
        player.getOpenInventory().setItem(20, timeLeft);
      }
    }.runTaskTimer(plugin, 20L, 20L);
  }

  private boolean isWeekend() {
    java.time.DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
    return day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY;
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
