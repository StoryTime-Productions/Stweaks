package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.stweaks.util.Cosmetic;
import com.storytimeproductions.stweaks.util.CosmeticsManager;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Command executor and menu handler for the Cosmetics GUI. Handles opening the cosmetics menu,
 * pagination, and reloading cosmetics.
 */
public class CosmeticsMenuCommand implements CommandExecutor, Listener {

  private static final Component GUI_TITLE = Component.text("Cosmetics", NamedTextColor.DARK_GREEN);
  private CosmeticsManager cosmeticsManager;

  // Slots for arrows
  private static final int PREV_SLOT = 46;
  private static final int NEXT_SLOT = 52;
  private static final int GUI_SIZE = 54;

  /**
   * Constructs a new CosmeticsMenuCommand.
   *
   * @param cosmeticsManager the CosmeticsManager instance
   */
  public CosmeticsMenuCommand(CosmeticsManager cosmeticsManager) {
    this.cosmeticsManager = cosmeticsManager;
  }

  /**
   * Opens the main cosmetics menu for the player at the specified page.
   *
   * @param player the player to open the menu for
   * @param page the page number (0-based)
   */
  public static void openMainMenu(Player player, int page) {
    List<Cosmetic> cosmetics = CosmeticsManager.getAllCosmetics();
    int totalPages = (int) Math.ceil(cosmetics.size() / 14.0);
    if (page < 0) {
      page = 0;
    }
    if (page >= totalPages) {
      page = totalPages - 1;
    }

    Inventory gui =
        Bukkit.createInventory(
            null,
            GUI_SIZE,
            GUI_TITLE.append(
                Component.text(" (Page " + (page + 1) + ")", NamedTextColor.DARK_GREEN)));

    // Border: Black panes
    ItemStack borderPane = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
    for (int i = 0; i < GUI_SIZE; i++) {
      if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
        gui.setItem(i, borderPane);
      }
    }

    // Add cosmetics in a checkerboard layout
    int maxItems = 14;
    int itemsPlaced = 0;
    int startIndex = page * maxItems;
    int endIndex = Math.min(startIndex + maxItems, cosmetics.size());

    for (int y = 1; y <= 4; y++) {
      for (int x = 1; x <= 7; x++) {
        if ((x + y) % 2 == 0 && startIndex + itemsPlaced < endIndex) {
          int slot = y * 9 + x;
          Cosmetic cosmetic = cosmetics.get(startIndex + itemsPlaced);
          gui.setItem(slot, createCosmeticItem(cosmetic, page));
          itemsPlaced++;
        }
      }
    }

    // Navigation
    if (page > 0) {
      gui.setItem(PREV_SLOT, createArrow("Previous Page", page - 1, "prev_page"));
    } else {
      gui.setItem(PREV_SLOT, createPane(Material.GRAY_STAINED_GLASS_PANE, "No Previous Page"));
    }

    if (page < totalPages - 1) {
      gui.setItem(NEXT_SLOT, createArrow("Next Page", page + 1, "targeted_page_number"));
    } else {
      gui.setItem(NEXT_SLOT, createPane(Material.GRAY_STAINED_GLASS_PANE, "No Next Page"));
    }

    player.openInventory(gui);
  }

  /**
   * Opens a detailed view of a specific cosmetic for the player.
   *
   * @param player the player to open the view for
   * @param cosmetic the cosmetic to view
   */
  public static void openCosmeticView(Player player, Cosmetic cosmetic, int returnPage) {
    Inventory gui =
        Bukkit.createInventory(
            null,
            54,
            Component.text("Cosmetic: " + cosmetic.getDisplayName(), NamedTextColor.BLACK));

    // Black panes for border
    ItemStack blackPane = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
    for (int i = 0; i < 54; i++) {
      if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
        gui.setItem(i, blackPane);
      }
    }

    // Recipe on the left (slots 10-12, 19-21, 28-30)
    Material[][] recipe = cosmetic.getRecipe();
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        Material mat = recipe[row][col];
        int slot = 10 + col + row * 9;
        if (mat != null && mat != Material.AIR) {
          ItemStack matItem = new ItemStack(mat);
          ItemMeta meta = matItem.getItemMeta();
          String formatted = toTitleCase(mat.name().replace("_", " "));
          meta.displayName(Component.text(formatted, NamedTextColor.YELLOW));
          matItem.setItemMeta(meta);
          gui.setItem(slot, matItem);
        }
      }
    }

    int cosmeticCenter = 24;
    ItemStack cosmeticItem = createCosmeticItem(cosmetic, 0);
    gui.setItem(cosmeticCenter, cosmeticItem);

    int[] greenSlots = {
      cosmeticCenter - 10,
      cosmeticCenter - 9,
      cosmeticCenter - 8, // above
      cosmeticCenter - 1,
      cosmeticCenter + 1, // sides
      cosmeticCenter + 8,
      cosmeticCenter + 9,
      cosmeticCenter + 10
    }; // below
    ItemStack greenPane = createPane(Material.GREEN_STAINED_GLASS_PANE, " ");
    for (int slot : greenSlots) {
      if (slot >= 0 && slot < 54 && gui.getItem(slot) == null) {
        gui.setItem(slot, greenPane);
      }
    }

    // Crafting bench named "Recipe" under recipe grid (slot 39)
    ItemStack recipeBench = new ItemStack(Material.CRAFTING_TABLE);
    ItemMeta recipeMeta = recipeBench.getItemMeta();
    recipeMeta.displayName(Component.text("Recipe", NamedTextColor.GOLD));
    recipeBench.setItemMeta(recipeMeta);
    gui.setItem(38, recipeBench);

    // Item icon named "Item" under cosmetic item (slot 48)
    ItemStack itemIcon = new ItemStack(Material.ITEM_FRAME);
    ItemMeta itemMeta = itemIcon.getItemMeta();
    itemMeta.displayName(Component.text("Item", NamedTextColor.GREEN));
    itemIcon.setItemMeta(itemMeta);
    gui.setItem(42, itemIcon);

    ItemStack returnArrow = createArrow("Return to Page", returnPage, "return_page");
    gui.setItem(49, returnArrow);

    player.openInventory(gui);
  }

  // Utility for Title Case
  private static String toTitleCase(String input) {
    String[] words = input.toLowerCase().split(" ");
    StringBuilder sb = new StringBuilder();
    for (String word : words) {
      if (word.length() > 0) {
        sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
      }
      sb.append(" ");
    }
    return sb.toString().trim();
  }

  /**
   * Creates an ItemStack representing a cosmetic item with its display name and lore.
   *
   * @param cosmetic the Cosmetic to represent
   * @return the ItemStack for the cosmetic
   */
  private static ItemStack createCosmeticItem(Cosmetic cosmetic, int returnPage) {
    ItemStack item = new ItemStack(Material.CARVED_PUMPKIN); // Assuming model overrides this
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text(cosmetic.getDisplayName(), NamedTextColor.GREEN));
    meta.lore(
        cosmetic.getLore().stream()
            .flatMap(
                line ->
                    wrapLoreLine(line, 50).stream()
                        .map(wrapped -> Component.text(wrapped, NamedTextColor.WHITE)))
            .toList());

    String itemModel = cosmetic.getItemModel();
    meta.setItemModel(new NamespacedKey(itemModel.split(":")[0], itemModel.split(":")[1]));
    NamespacedKey idKey = new NamespacedKey("stweaks", "cosmetic_id");
    NamespacedKey returnKey = new NamespacedKey("stweaks", "return_page");
    meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, cosmetic.getId());
    if (returnPage == 0) {
      meta.getPersistentDataContainer().set(returnKey, PersistentDataType.INTEGER, returnPage);
    }
    item.setItemMeta(meta);
    return item;
  }

  /**
   * Utility method to wrap lore lines to a maximum length, not breaking words.
   *
   * @param line the line to wrap
   * @param maxLen the maximum line length
   * @return a list of wrapped lines
   */
  private static List<String> wrapLoreLine(String line, int maxLen) {
    List<String> result = new ArrayList<>();
    String[] words = line.split(" ");
    StringBuilder sb = new StringBuilder();
    for (String word : words) {
      if (sb.length() + word.length() + 1 > maxLen) {
        result.add(sb.toString());
        sb = new StringBuilder();
      }
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(word);
    }
    if (sb.length() > 0) {
      result.add(sb.toString());
    }
    return result;
  }

  /**
   * Creates a generic colored pane item.
   *
   * @param color the pane color
   * @param name the display name
   * @return the ItemStack for the pane
   */
  private static ItemStack createPane(Material color, String name) {
    ItemStack pane = new ItemStack(color);
    ItemMeta meta = pane.getItemMeta();
    meta.displayName(Component.text(name));
    pane.setItemMeta(meta);
    return pane;
  }

  /**
   * Creates an arrow item for navigation.
   *
   * @param name the display name
   * @param pageNumber the page number this arrow points to
   * @param keyName the key for the metadata ("prevPage" or "targetedPageNumber")
   * @return the ItemStack for the arrow
   */
  private static ItemStack createArrow(String name, int pageNumber, String keyName) {
    ItemStack item = new ItemStack(Material.ARROW);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text(name));
    // Use only lowercase, underscores for key names
    NamespacedKey key = new NamespacedKey("stweaks", keyName.toLowerCase());
    meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, pageNumber);
    item.setItemMeta(meta);
    return item;
  }

  /**
   * Handles the /cosmetics command, opening the menu or reloading cosmetics.
   *
   * @param sender the command sender
   * @param command the command
   * @param label the command label
   * @param args the command arguments
   * @return true if the command was handled, false otherwise
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      return false;
    }

    if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
      if (!player.isOp()) {
        player.sendMessage("You do not have permission to reload cosmetics.");
        return true;
      }
      cosmeticsManager.loadCosmetics();
      player.sendMessage("Cosmetics reloaded!");
      return true;
    }

    int page = 0;
    if (args.length == 1) {
      try {
        page = Integer.parseInt(args[0]) - 1;
        if (page < 0) {
          page = 0;
        }
      } catch (NumberFormatException e) {
        player.sendMessage("Invalid page number.");
        return true;
      }
    }
    openMainMenu(player, page);
    return true;
  }
}
