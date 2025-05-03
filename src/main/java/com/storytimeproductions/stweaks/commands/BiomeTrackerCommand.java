package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.stweaks.util.BiomeTrackerManager;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles the `/biometracker` command for opening a GUI that shows the player's discovered biomes.
 * The command opens an inventory displaying all available biomes with different colors indicating
 * whether the biome has been discovered by the player.
 */
public class BiomeTrackerCommand implements CommandExecutor {

  private final BiomeTrackerManager trackerManager;
  private final YamlConfiguration config;

  /**
   * Constructs a BiomeTrackerCommand instance.
   *
   * @param trackerManager The BiomeTrackerManager instance used to track discovered biomes.
   */
  public BiomeTrackerCommand(BiomeTrackerManager trackerManager, JavaPlugin plugin) {
    this.trackerManager = trackerManager;

    File file = new File(plugin.getDataFolder(), "biome_item.yml");
    if (!file.exists()) {
      plugin.saveResource("biome_item.yml", false); // If it doesn't exist, save the default
    }

    config = YamlConfiguration.loadConfiguration(file);
  }

  /**
   * Handles the `/biometracker` command, opening a GUI that displays biomes the player has
   * discovered and those they haven't, using different colors for each. The biomes are stored and
   * retrieved from the database.
   *
   * @param sender The command sender, typically a player.
   * @param command The command that was executed.
   * @param label The alias of the command used.
   * @param args The arguments passed with the command.
   * @return true if the command was executed successfully, false otherwise.
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      return false;
    }

    Set<Biome> allBiomes = new HashSet<>();
    World world = Bukkit.getWorld("world"); // or any loaded world
    if (world != null) {
      RegistryAccess registryAccess = RegistryAccess.registryAccess();
      Registry<Biome> biomeRegistry = registryAccess.getRegistry(RegistryKey.BIOME);
      if (biomeRegistry != null) {
        biomeRegistry.iterator().forEachRemaining(allBiomes::add);
      }
    }
    trackerManager.syncAndLoadBiomeItems(allBiomes);

    // Pagination setup
    int biomesPerPage = 14;
    int totalBiomes = allBiomes.size();
    int totalPages = (int) Math.ceil((double) totalBiomes / biomesPerPage);
    int page = args.length > 0 ? Integer.parseInt(args[0]) : 1;
    page = Math.min(Math.max(page, 1), totalPages);

    int startIndex = (page - 1) * biomesPerPage;
    int endIndex = Math.min(startIndex + biomesPerPage, totalBiomes);
    List<Biome> biomesForPage = new ArrayList<>(allBiomes);
    biomesForPage.sort(
        (b1, b2) ->
            formatBiomeName(b1.getKey().toString())
                .compareToIgnoreCase(formatBiomeName(b2.getKey().toString())));
    biomesForPage = biomesForPage.subList(startIndex, endIndex);

    Inventory gui =
        Bukkit.createInventory(null, 54, Component.text("Biome Tracker - Page " + page));

    // Set outer edge to black panes with padding between biomes
    for (int i = 0; i < 9; i++) {
      gui.setItem(i, createBlackPane()); // Top row
      gui.setItem(45 + i, createBlackPane()); // Bottom row
    }
    for (int i = 0; i < 54; i += 9) {
      gui.setItem(i, createBlackPane()); // Left column
      gui.setItem(i + 8, createBlackPane()); // Right column
    }

    int itemsPlaced = 0;
    UUID uuid = player.getUniqueId();
    Set<String> discovered = trackerManager.getDiscoveredBiomes(uuid);

    // Loop through 2nd to 5th row (1 to 4), and 2nd to 8th column (1 to 7)
    for (int y = 1; y <= 4; y++) {
      for (int x = 1; x <= 7; x++) {
        // Checkerboard condition: place only if (x + y) is even
        if ((x + y) % 2 == 0) {
          if (itemsPlaced >= biomesForPage.size()) {
            break;
          }

          Biome biome = biomesForPage.get(itemsPlaced);
          String biomeKey = biome.getKey().toString();
          boolean isDiscovered = discovered.contains(biomeKey);

          // Access the biome_item.yml mapping and get the corresponding item
          String itemName = config.getString(formatBiomeName(biomeKey));

          // Set itemName to "BARRIER" if biome is not discovered
          if (!isDiscovered) {
            itemName = "BARRIER";
          } else if (itemName == null || itemName.isEmpty()) {
            itemName = "PAPER";
          }

          // Create the item for the biome
          Material material =
              Material.getMaterial(itemName.toUpperCase()); // Converts string to Material
          if (material == null) {
            material = Material.BARRIER; // Fallback if the material is invalid
          }

          ItemStack itemStack = new ItemStack(material);
          ItemMeta meta = itemStack.getItemMeta();

          // Set the item's display name based on discovery status
          meta.displayName(
              Component.text(
                  formatBiomeName(biomeKey) + " - " + (isDiscovered ? "FOUND" : "NOT FOUND")));
          itemStack.setItemMeta(meta);

          int slotIndex = y * 9 + x;
          gui.setItem(slotIndex, itemStack);

          itemsPlaced++;
        }
      }
    }

    // Set navigation buttons
    gui.setItem(
        48,
        createNavigationPane(
            page > 1,
            "Previous",
            Material.GREEN_STAINED_GLASS_PANE,
            Material.BLACK_STAINED_GLASS_PANE,
            page - 1));
    gui.setItem(
        50,
        createNavigationPane(
            page < totalPages,
            "Next",
            Material.GREEN_STAINED_GLASS_PANE,
            Material.BLACK_STAINED_GLASS_PANE,
            page + 1));

    // Display current page number
    gui.setItem(49, createCurrentPagePane(page));

    player.openInventory(gui);
    return true;
  }

  private ItemStack createBlackPane() {
    ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta meta = blackPane.getItemMeta();
    meta.displayName(Component.empty());
    blackPane.setItemMeta(meta);
    return blackPane;
  }

  private ItemStack createNavigationPane(
      boolean canNavigate,
      String label,
      Material enabledMaterial,
      Material disabledMaterial,
      int page) {
    Material material = canNavigate ? enabledMaterial : disabledMaterial;
    ItemStack navigationPane = new ItemStack(material);
    ItemMeta meta = navigationPane.getItemMeta();
    meta.displayName(Component.text(label));

    // Store the page number in the item's persistent data container
    PersistentDataContainer data = meta.getPersistentDataContainer();
    data.set(new NamespacedKey("stweaks", "page"), PersistentDataType.INTEGER, page);

    navigationPane.setItemMeta(meta);
    return navigationPane;
  }

  private ItemStack createCurrentPagePane(int page) {
    ItemStack pagePane = new ItemStack(Material.PAPER);
    ItemMeta meta = pagePane.getItemMeta();
    meta.displayName(Component.text("Page " + page));
    pagePane.setItemMeta(meta);
    return pagePane;
  }

  /**
   * Formats the biome key into a human-readable name by capitalizing the first letter of each word
   * and replacing underscores with spaces.
   *
   * @param key The biome key (e.g., "minecraft/desert").
   * @return The formatted biome name (e.g., "Desert").
   */
  private String formatBiomeName(String key) {
    String rawName = key.toLowerCase();

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
