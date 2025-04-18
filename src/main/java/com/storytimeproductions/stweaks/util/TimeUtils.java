package com.storytimeproductions.stweaks.util;

import com.storytimeproductions.stweaks.config.SettingsManager;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * A utility class for time-related functions in the plugin.
 *
 * <p>This class provides methods for calculating playtime multipliers based on the current day of
 * the week. Specifically, it checks if today is a weekend (Saturday or Sunday) and applies a
 * weekend multiplier.
 */
public class TimeUtils {

  /**
   * Retrieves the multiplier for the current day.
   *
   * <p>This method checks if the current day is a weekend (Saturday or Sunday). If it is, it
   * returns the weekend multiplier configured in the plugin settings. Otherwise, it returns a
   * default multiplier of 1.0 (no multiplier).
   *
   * @return The multiplier for the current day based on the day of the week.
   */
  public static double getTodayMultiplier() {
    DayOfWeek today = LocalDate.now().getDayOfWeek();
    return (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY)
        ? SettingsManager.getWeekendMultiplier()
        : 1.0;
  }
}
