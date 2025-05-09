package com.storytimeproductions.stweaks.listeners;

import com.storytimeproductions.stweaks.util.BiomeTrackerManager;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * BiomeNotifier is a listener that notifies players via the action bar when they enter a new biome.
 * It tracks the player's last known biome and only sends a message if the biome has changed. The
 * message fades out after a few seconds.
 */
public class BiomeNotifier implements Listener {

  private static class BiomeMessageState {
    String queuedMessage;
    int taskId = -1;
  }

  /** Tracks the last known biome for each player by their UUID. */
  private final Plugin plugin;

  private final BiomeTrackerManager biomeTrackerManager;
  private final HashMap<UUID, BiomeMessageState> biomeMessages = new HashMap<>();
  private final HashMap<UUID, Biome> lastKnownBiome = new HashMap<>();

  /**
   * Constructs a new BiomeNotifier with a reference to the main plugin instance.
   *
   * @param plugin The plugin instance used for scheduling tasks and registering events.
   * @param biomeTrackerManager The BiomeTrackerManager instance for managing
   */
  public BiomeNotifier(Plugin plugin, BiomeTrackerManager biomeTrackerManager) {
    this.plugin = plugin;
    this.biomeTrackerManager = biomeTrackerManager;
  }

  /**
   * Handles the inventory click event for pagination in the Biome Tracker GUI. When a player clicks
   * on a navigation button (Next/Previous), this method reads the stored page number from the
   * item's persistent data and triggers the command to navigate to the correct page.
   *
   * @param event The InventoryClickEvent triggered when a player clicks an item in the inventory.
   */
  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    Inventory clickedInventory = event.getClickedInventory();
    if (clickedInventory == null) {
      return;
    }

    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem == null || !clickedItem.hasItemMeta()) {
      return;
    }

    // Check if this is our custom "BiomeTracker" inventory
    InventoryView view = player.getOpenInventory();
    String title = view.title().toString();

    if (!title.contains("Biome Tracker")) {
      return; // Only act on our GUI
    }

    ItemMeta meta = clickedItem.getItemMeta();
    PersistentDataContainer data = meta.getPersistentDataContainer();

    // Check if the clicked item is a navigation button (Previous or Next)
    if (data.has(new NamespacedKey("stweaks", "page"), PersistentDataType.INTEGER)) {
      int clickedPage = data.get(new NamespacedKey("stweaks", "page"), PersistentDataType.INTEGER);
      if (clickedPage > 0) {
        String command = "biometracker " + clickedPage;
        player.performCommand(command);
      }
    }

    event.setCancelled(true);
  }

  /**
   * Event handler for player movement. Checks if the player has entered a new biome and displays a
   * message in the action bar if so. Only operates in the "world" world.
   *
   * @param event the PlayerMoveEvent triggered when a player moves
   */
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();

    // Only process if the player is in the "world" world
    if (!"world".equals(player.getWorld().getName())) {
      return;
    }

    Location loc = player.getLocation();
    Biome currentBiome = loc.getBlock().getBiome();

    UUID uuid = player.getUniqueId();
    Biome previousBiome = lastKnownBiome.get(uuid);

    // Show message if player enters a new biome
    if (previousBiome != currentBiome) {
      lastKnownBiome.put(uuid, currentBiome);
      sendBiomeActionBar(player, currentBiome);
      String biomeKey = currentBiome.getKey().toString();
      biomeTrackerManager.markBiomeDiscovered(uuid, biomeKey);
    }
  }

  /**
   * Sends a biome name message to the player's action bar and clears it after a few seconds if
   * still in the same biome.
   *
   * @param player the player to send the message to
   * @param biome the biome the player just entered
   */
  private void sendBiomeActionBar(Player player, Biome biome) {
    String biomeName = formatBiomeName(biome);
    String message = "You've entered the " + biomeName;

    UUID uuid = player.getUniqueId();
    BiomeMessageState state = biomeMessages.computeIfAbsent(uuid, k -> new BiomeMessageState());

    // If a message is currently showing, queue the new one
    if (state.taskId != -1) {
      state.queuedMessage = message;
      return;
    }

    showActionBar(player, state, message);
  }

  private void showActionBar(Player player, BiomeMessageState state, String message) {
    player.sendActionBar(Component.text(message));

    // Simulate fade-in or duration with a delayed clear
    state.taskId =
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  player.sendActionBar(Component.empty());
                  state.taskId = -1;

                  // Show queued message if available
                  if (state.queuedMessage != null) {
                    String queued = state.queuedMessage;
                    state.queuedMessage = null;
                    showActionBar(player, state, queued);
                  }
                },
                60L)
            .getTaskId(); // 3 seconds display time
  }

  private String formatBiomeName(Biome biome) {
    String rawName = biome.toString().toLowerCase();

    // Use the part after '/' if present, otherwise after ':'
    if (rawName.contains("/")) {
      rawName = rawName.substring(rawName.indexOf("/") + 1);
    } else if (rawName.contains(":")) {
      rawName = rawName.substring(rawName.indexOf(":") + 1);
    }

    // Replace underscores with spaces
    rawName = rawName.replace('_', ' ');

    // Capitalize each word
    String[] words = rawName.split(" ");
    StringBuilder formatted = new StringBuilder();
    for (String word : words) {
      if (!word.isEmpty()) {
        formatted
            .append(Character.toUpperCase(word.charAt(0)))
            .append(word.substring(1))
            .append(" ");
      }
    }

    return formatted.toString().trim();
  }
}
