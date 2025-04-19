package com.storytimeproductions.stweaks.util;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

/**
 * Utility class responsible for updating the tab list header and footer for players on the server.
 * The header typically displays the server name, while the footer can be used to show dynamic
 * information like the current multiplier or other server stats.
 *
 * <p>Uses the Adventure API for styled text formatting.
 */
public class TablistManager {

  /**
   * Updates the tab list for the given player with a custom header and footer.
   *
   * <p>The header displays the server name with styling. The footer displays the current playtime
   * multiplier, which may change depending on server events, days of the week, or other conditions.
   *
   * @param player the player whose tab list should be updated
   * @param multiplier the current playtime multiplier to display in the tab footer
   */
  public static void updateTablist(Player player, double multiplier) {
    Component header =
        Component.text(">> StoryTime SMP <<")
            .color(NamedTextColor.LIGHT_PURPLE)
            .decorate(TextDecoration.BOLD)
            .append(Component.newline());

    Component footer =
        Component.newline()
            .append(
                Component.text("Timer Multiplier: ")
                    .color(NamedTextColor.GOLD)
                    .append(Component.text("x" + multiplier).color(NamedTextColor.GREEN)));

    player.sendPlayerListHeaderAndFooter(header, footer);
  }

  /**
   * Sends a warning title to the player when their playtime is about to end, based on the time
   * remaining. Different messages are displayed depending on whether the player has 10, 5, or 1
   * minute(s) remaining.
   *
   * @param player the player who will receive the warning title
   * @param minutesRemaining the number of minutes left before the player is kicked
   */
  public static void sendPlaytimeWarningTitle(Player player, int minutesRemaining) {
    switch (minutesRemaining) {
      case 10:
        sendTitle(
            player, "10 Minutes Left", "Finish up, you’re almost done!", NamedTextColor.YELLOW);
        break;
      case 5:
        sendTitle(player, "5 Minutes Left!", "Wrap things up quickly!", NamedTextColor.GOLD);
        break;
      case 1:
        sendTitle(player, "1 Minute Remaining", "Hurry! Save your progress!", NamedTextColor.RED);
        break;
      default:
        break;
    }
  }

  /**
   * Sends a title with a specified text and color to the player. The title will have a fade-in,
   * stay, and fade-out duration, and the subtitle will have a default gray color.
   *
   * @param player the player to whom the title will be sent
   * @param titleText the text to display as the title
   * @param subtitleText the text to display as the subtitle
   * @param color the color of the title text
   */
  private static void sendTitle(
      Player player, String titleText, String subtitleText, NamedTextColor color) {
    // Create the title component
    Component title = Component.text(titleText).color(color).decorate(TextDecoration.BOLD);
    Component subtitle = Component.text(subtitleText).color(NamedTextColor.GRAY);

    // Set the fade-in, stay, and fade-out times for the title
    Title.Times times =
        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500));

    // Create a Title object using Adventure API
    Title titleObj = Title.title(title, subtitle, times);

    // Send the title to the player
    player.showTitle(titleObj);
  }
}
