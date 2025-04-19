package com.storytimeproductions.stweaks.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

/**
 * Utility class responsible for updating the tab list header and footer for
 * players on the server.
 * The header typically displays the server name, while the footer can be used
 * to show dynamic
 * information like the current multiplier or other server stats.
 *
 * <p>
 * Uses the Adventure API for styled text formatting.
 */
public class TablistManager {

    /**
     * Updates the tab list for the given player with a custom header and footer.
     *
     * <p>
     * The header displays the server name with styling. The footer displays the
     * current playtime
     * multiplier, which may change depending on server events, days of the week, or
     * other conditions.
     *
     * @param player     the player whose tab list should be updated
     * @param multiplier the current playtime multiplier to display in the tab
     *                   footer
     */
    public static void updateTablist(Player player, double multiplier) {
        Component header = Component.text(">> StoryTime SMP <<")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
                .append(Component.newline());

        Component footer = Component.newline()
                .append(
                        Component.text("Timer Multiplier: ")
                                .color(NamedTextColor.GOLD)
                                .append(Component.text("x" + multiplier).color(NamedTextColor.GREEN)));

        player.sendPlayerListHeaderAndFooter(header, footer);
    }
}
