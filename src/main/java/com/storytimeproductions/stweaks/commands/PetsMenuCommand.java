package com.storytimeproductions.stweaks.commands;

import com.storytimeproductions.models.Pet;
import com.storytimeproductions.stweaks.util.PetsManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Command handler for the pets menu. This class manages the command to open the pets menu and
 * reloads the configuration if necessary.
 *
 * <p>It also provides methods to open the main pets menu and individual pet view menus.
 */
public class PetsMenuCommand implements CommandExecutor {
  private final JavaPlugin plugin;
  private final PetsManager petsManager;

  /**
   * Constructs the PetsMenuCommand.
   *
   * @param plugin the main plugin instance
   * @param petsManager manager responsible for loading and accessing pet data
   */
  public PetsMenuCommand(JavaPlugin plugin, PetsManager petsManager) {
    this.plugin = plugin;
    this.petsManager = petsManager;
  }

  /**
   * Executes the command to open the pets menu or reload the configuration.
   *
   * @param sender the command sender
   * @param command the command being executed
   * @param label the label of the command
   * @param args the arguments passed to the command
   * @return true if the command was executed successfully, false otherwise
   */
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
      if (!sender.isOp()) {
        sender.sendMessage(
            Component.text("You do not have permission to use this command.", NamedTextColor.RED));
        return true;
      }

      sender.sendMessage(Component.text("Pets reloaded successfully!", NamedTextColor.WHITE));
      return true;
    }

    if (!(sender instanceof Player player)) {
      return false;
    }

    petsManager.loadPets();

    openMainPetsMenu(player, 1);
    return true;
  }

  /**
   * Opens the main pets menu for the player with pagination.
   *
   * @param player the player to whom the menu is displayed
   * @param page the current page number
   */
  public void openMainPetsMenu(Player player, int page) {
    int maxItems = 14; // Maximum number of pets per page
    List<Pet> pets = petsManager.getAllPets();
    int totalPages = (int) Math.ceil(pets.size() / (double) maxItems);
    page = Math.max(1, Math.min(page, totalPages));

    Inventory menu =
        Bukkit.createInventory(
            null,
            6 * 9,
            Component.text("Pets Menu - Page " + page).decoration(TextDecoration.ITALIC, false));

    // Add border panes
    for (int i = 0; i < 54; i++) {
      if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
        menu.setItem(i, createPane(Material.BLACK_STAINED_GLASS_PANE, " "));
      }
    }

    // Add pets in a checkerboard layout
    int itemsPlaced = 0;
    int startIndex = (page - 1) * maxItems;
    int endIndex = Math.min(startIndex + maxItems, pets.size());

    for (int y = 1; y <= 4; y++) {
      for (int x = 1; x <= 7; x++) {
        if ((x + y) % 2 == 0 && startIndex + itemsPlaced < endIndex) {
          Pet pet = pets.get(startIndex + itemsPlaced);

          ItemStack item = new ItemStack(Material.PAPER);
          ItemMeta meta = item.getItemMeta();
          String petName = formatText(pet.getId() + " Pet", false);
          String petId = pet.getId();
          ItemStack petItem;

          if (petId.matches(".*-[a-zA-Z0-9_]+$")) {
            // Extract the player name after the last hyphen
            String[] parts = petId.split("-");
            String playerName = parts[parts.length - 1];
            petItem = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta petMeta = petItem.getItemMeta();
            // Set the owning player for the head
            if (petMeta instanceof SkullMeta skullMeta) {
              skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
              skullMeta.displayName(Component.text(playerName.toLowerCase() + " Pet"));
              petItem.setItemMeta(skullMeta);
            } else {
              petItem.setItemMeta(meta);
            }
          } else {
            petItem = new ItemStack(Material.PAPER);
            ItemMeta petMeta = petItem.getItemMeta();
            petMeta.displayName(Component.text(petId.replace("_", " ").toLowerCase() + " Pet"));
            petItem.setItemMeta(petMeta);
          }
          meta.displayName(Component.text(petName));
          meta.getPersistentDataContainer()
              .set(new NamespacedKey(plugin, "petId"), PersistentDataType.STRING, pet.getId());
          meta.getPersistentDataContainer()
              .set(new NamespacedKey(plugin, "returnPage"), PersistentDataType.INTEGER, page);
          meta.setItemModel(new NamespacedKey("hotbar_pets", pet.getId()));
          item.setItemMeta(meta);

          int slot = y * 9 + x;
          menu.setItem(slot, item);
          itemsPlaced++;
        }
      }
    }

    // Add navigation buttons
    if (page > 1) {
      ItemStack prevButton = createPane(Material.ARROW, "Previous Page");
      ItemMeta meta = prevButton.getItemMeta();
      meta.getPersistentDataContainer()
          .set(new NamespacedKey(plugin, "prevPage"), PersistentDataType.INTEGER, page - 1);
      prevButton.setItemMeta(meta);
      menu.setItem(46, prevButton);
    }

    if (page < totalPages) {
      ItemStack nextButton = createPane(Material.ARROW, "Next Page");
      ItemMeta meta = nextButton.getItemMeta();
      meta.getPersistentDataContainer()
          .set(new NamespacedKey(plugin, "nextPage"), PersistentDataType.INTEGER, page + 1);
      nextButton.setItemMeta(meta);
      menu.setItem(52, nextButton);
    }

    // Add total pets indicator
    ItemStack totalPets = createPane(Material.NAME_TAG, "Total Pets: " + pets.size());
    menu.setItem(49, totalPets); // Bottom row middle slot

    player.openInventory(menu);
  }

  /**
   * Converts a string to title case format (first letter of each word capitalized, rest lowercase).
   *
   * @param text the text to convert
   * @return the text in title case format
   */
  private String formatText(String text, boolean toLowerCase) {
    if (text == null || text.isEmpty()) {
      return text;
    }

    StringBuilder titleCase = new StringBuilder();
    boolean nextTitleCase = true;

    for (char c : text.toCharArray()) {
      if (Character.isSpaceChar(c) || c == '_' || c == '-') {
        nextTitleCase = true;
        titleCase.append(c == '_' ? ' ' : c); // Replace underscores with spaces
      } else if (nextTitleCase) {
        titleCase.append(Character.toUpperCase(c));
        nextTitleCase = false;
      } else {
        titleCase.append(Character.toLowerCase(c));
      }
    }

    if (toLowerCase) {
      return titleCase.toString().toLowerCase();
    }

    return titleCase.toString();
  }

  /**
   * Creates a decorative pane item.
   *
   * @param material the material of the pane
   * @param name the display name of the pane
   * @return the created ItemStack
   */
  private ItemStack createPane(Material material, String name) {
    ItemStack pane = new ItemStack(material);
    ItemMeta meta = pane.getItemMeta();
    meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
    pane.setItemMeta(meta);
    return pane;
  }

  /**
   * Opens the detailed pet view menu for a specific pet.
   *
   * @param player the player to whom the menu is displayed
   * @param pet the pet whose details are displayed
   * @param returnPage the page to return to when closing the pet view
   */
  public void openPetView(Player player, Pet pet, int returnPage) {
    Inventory menu =
        Bukkit.createInventory(
            null,
            54,
            Component.text(formatText(pet.getId() + " Pet", false))
                .decoration(TextDecoration.ITALIC, false));

    // Add border panes
    for (int i = 0; i < 54; i++) {
      if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
        menu.setItem(i, createPane(Material.BLACK_STAINED_GLASS_PANE, " "));
      }
    }

    // Display recipe label on slot 37
    ItemStack recipeLabel = new ItemStack(Material.CRAFTING_TABLE);
    ItemMeta recipeMeta = recipeLabel.getItemMeta();
    recipeMeta.displayName(Component.text("Recipe").decoration(TextDecoration.ITALIC, false));
    recipeLabel.setItemMeta(recipeMeta);
    menu.setItem(38, recipeLabel);

    // Display pet information label on slot 42
    ItemStack infoItem = new ItemStack(Material.PAPER);
    ItemMeta infoMeta = infoItem.getItemMeta();
    infoMeta.displayName(
        Component.text("Pet Information").decoration(TextDecoration.ITALIC, false));
    infoItem.setItemMeta(infoMeta);
    menu.setItem(42, infoItem);

    // Display recipe (slots 10, 11, 12, 19, 20, 21, 28, 29, 30)
    List<List<Material>> recipe = pet.getRecipe();
    if (recipe != null) {
      int[] ingredientSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
      int slotIndex = 0;

      for (List<Material> row : recipe) {
        for (Material material : row) {
          if (slotIndex >= ingredientSlots.length) {
            break;
          }

          if (material != null && material != Material.AIR) {
            ItemStack ingredientItem = new ItemStack(material);
            ItemMeta ingredientMeta = ingredientItem.getItemMeta();
            ingredientMeta.displayName(
                Component.text(formatText(material.name().replace("_", " "), false))
                    .decoration(TextDecoration.ITALIC, false));
            ingredientItem.setItemMeta(ingredientMeta);
            menu.setItem(ingredientSlots[slotIndex], ingredientItem);
          }
          slotIndex++;
        }
      }
    } else {
      // If recipe is null, show a message
      ItemStack noRecipeItem = new ItemStack(Material.BARRIER);
      ItemMeta noRecipeMeta = noRecipeItem.getItemMeta();
      noRecipeMeta.displayName(
          Component.text("No Recipe Available").decoration(TextDecoration.ITALIC, false));
      noRecipeItem.setItemMeta(noRecipeMeta);
      menu.setItem(20, noRecipeItem); // Middle of recipe area
    }

    // Display pet description on slot 14
    ItemStack descriptionItem = new ItemStack(Material.WRITTEN_BOOK);
    ItemMeta descMeta = descriptionItem.getItemMeta();

    descMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

    descMeta.displayName(Component.text("What am I?").decoration(TextDecoration.ITALIC, false));

    List<Component> descLore = new ArrayList<>();
    descLore.add(Component.text(pet.getSubtitle()).decoration(TextDecoration.ITALIC, false));
    descMeta.lore(descLore);

    descriptionItem.setItemMeta(descMeta);
    menu.setItem(14, descriptionItem);

    // Display pet preview button on slot 16
    ItemStack previewItem = new ItemStack(Material.JUKEBOX);
    ItemMeta previewMeta = previewItem.getItemMeta();
    previewMeta.displayName(
        Component.text("Click to hear me!").decoration(TextDecoration.ITALIC, false));

    List<Component> previewLore = new ArrayList<>();
    previewLore.add(Component.text("I can speak and").decoration(TextDecoration.ITALIC, false));
    previewLore.add(Component.text("make sounds!").decoration(TextDecoration.ITALIC, false));
    previewMeta.lore(previewLore);

    previewMeta
        .getPersistentDataContainer()
        .set(new NamespacedKey(plugin, "previewPet"), PersistentDataType.STRING, pet.getId());
    previewItem.setItemMeta(previewMeta);
    menu.setItem(16, previewItem);

    // Display pet itself on slot 24
    ItemStack petItem = new ItemStack(Material.PAPER);
    ItemMeta petMeta = petItem.getItemMeta();
    petMeta.displayName(
        Component.text(formatText(pet.getId(), false)).decoration(TextDecoration.ITALIC, false));
    petMeta.setItemModel(new NamespacedKey("hotbar_pets", pet.getId()));
    petItem.setItemMeta(petMeta);
    menu.setItem(24, petItem);

    // Display food requirement on slot 32
    Material foodMat = pet.getFood();
    ItemStack foodItem = new ItemStack(foodMat);
    ItemMeta foodMeta = foodItem.getItemMeta();
    foodMeta.displayName(Component.text("What do I eat?").decoration(TextDecoration.ITALIC, false));

    List<Component> foodLoreList = new ArrayList<>();
    foodLoreList.add(
        Component.text("I really like ")
            .append(Component.text(formatText(foodMat.name().replace("_", " "), true)))
            .decoration(TextDecoration.ITALIC, false));
    foodMeta.lore(foodLoreList);

    foodItem.setItemMeta(foodMeta);
    menu.setItem(32, foodItem);

    // Display perk on slot 34
    ItemStack perkItem;
    String perk = pet.getPerk();

    if (perk.startsWith("effect:")) {
      perkItem = new ItemStack(Material.POTION);
      ItemMeta perkMeta = perkItem.getItemMeta();
      perkMeta.displayName(
          Component.text("What do I give you?").decoration(TextDecoration.ITALIC, false));

      perkMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

      List<Component> perkLore = new ArrayList<>();
      String effectName = perk.substring(7);

      perkLore.add(
          Component.text("I give " + formatText(effectName, true) + " sometimes!")
              .decoration(TextDecoration.ITALIC, false));
      perkMeta.lore(perkLore);

      perkItem.setItemMeta(perkMeta);
    } else if (perk.startsWith("item:")) {
      String itemName = perk.substring(5);
      Material itemMat = Material.matchMaterial(itemName);
      perkItem = new ItemStack(itemMat != null ? itemMat : Material.CHEST);
      ItemMeta perkMeta = perkItem.getItemMeta();
      perkMeta.displayName(
          Component.text("What do I give you?").decoration(TextDecoration.ITALIC, false));

      List<Component> perkLore = new ArrayList<>();
      perkLore.add(
          Component.text("I give " + formatText(itemName.replace("_", " "), true) + "s sometimes!")
              .decoration(TextDecoration.ITALIC, false));
      perkMeta.lore(perkLore);

      perkItem.setItemMeta(perkMeta);
    } else {
      perkItem = new ItemStack(Material.NETHER_STAR);
      ItemMeta perkMeta = perkItem.getItemMeta();
      perkMeta.displayName(
          Component.text("How I can help you?").decoration(TextDecoration.ITALIC, false));

      List<Component> perkLore = new ArrayList<>();
      perkLore.add(
          Component.text("Unknown perk type: " + perk).decoration(TextDecoration.ITALIC, false));
      perkMeta.lore(perkLore);

      perkItem.setItemMeta(perkMeta);
    }
    menu.setItem(34, perkItem);

    // Add a back button to return to the main menu
    ItemStack backButton = new ItemStack(Material.ARROW);
    ItemMeta backMeta = backButton.getItemMeta();
    backMeta.displayName(
        Component.text("Back to Main Menu").decoration(TextDecoration.ITALIC, false));
    backMeta
        .getPersistentDataContainer()
        .set(new NamespacedKey(plugin, "returnPetsMenu"), PersistentDataType.INTEGER, 1);
    backMeta
        .getPersistentDataContainer()
        .set(new NamespacedKey(plugin, "returnPage"), PersistentDataType.INTEGER, returnPage);
    backButton.setItemMeta(backMeta);

    menu.setItem(49, backButton); // Bottom center slot for the back button

    player.openInventory(menu);
  }

  /**
   * Previews a pet by playing a random sound and showing a random quote.
   *
   * @param player the player who will see/hear the preview
   * @param pet the pet to preview
   */
  public void previewPet(Player player, Pet pet) {
    // Get a random burp sound
    List<String> burps = pet.getBurpSounds();
    String randomBurp = burps.get(new Random().nextInt(burps.size()));

    // Get a random quote
    List<String> quotes = pet.getQuotes();
    String randomQuote = quotes.get(new Random().nextInt(quotes.size()));

    // Play the sound and send the message
    player.playSound(player.getLocation(), randomBurp, 1.0f, 1.0f);
    player.sendMessage(Component.text(pet.getId() + ": " + randomQuote));
  }
}
