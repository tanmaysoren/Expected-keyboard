package expected.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class CustomThemeManager {
  public static final String THEME_CUSTOM_KEY = "custom";

  // Preference keys
  public static final String PREF_CUSTOM_COLOR_KEY = "custom_theme_color_key";
  public static final String PREF_CUSTOM_COLOR_LETTERS = "custom_theme_color_letters";
  public static final String PREF_CUSTOM_COLOR_KEY_BG = "custom_theme_color_key_bg";
  public static final String PREF_CUSTOM_COLOR_KEYBOARD_BG = "custom_theme_color_keyboard_bg";
  public static final String PREF_CUSTOM_COLOR_ACTION = "custom_theme_color_action";
  public static final String PREF_CUSTOM_COLOR_ACTIVATED = "custom_theme_color_activated";
  public static final String PREF_CUSTOM_COLOR_SPACEBAR = "custom_theme_color_spacebar";
  public static final String PREF_CUSTOM_COLOR_SUBLABEL = "custom_theme_color_sublabel";
  public static final String PREF_CUSTOM_BG_OPACITY = "custom_theme_bg_opacity"; // 0-100%

  // Default values
  public static final int DEFAULT_COLOR_KEY = 0xFF1E2235;
  public static final int DEFAULT_COLOR_LETTERS = 0xFFF1F5F9;
  public static final int DEFAULT_COLOR_KEY_BG = 0xFF151928;
  public static final int DEFAULT_COLOR_KEYBOARD_BG = 0xFF0D111E;
  public static final int DEFAULT_COLOR_ACTION = 0xFF2A1C4E;
  public static final int DEFAULT_COLOR_ACTIVATED = 0xFF3B2D6E;
  public static final int DEFAULT_COLOR_SPACEBAR = 0xFF1C2238;
  public static final int DEFAULT_COLOR_SUBLABEL = 0xFF94A3B8;
  public static final int DEFAULT_BG_OPACITY = 100;

  public static boolean isCustomTheme(Context context) {
    SharedPreferences prefs = Config.globalPrefs();
    if (prefs == null && context != null) {
      prefs = DirectBootAwarePreferences.get_shared_preferences(context);
    }
    if (prefs == null) return false;
    String theme = prefs.getString("theme", "frostedobsidian");
    return THEME_CUSTOM_KEY.equalsIgnoreCase(theme);
  }

  public static int getColorKey(SharedPreferences prefs) {
    return prefs.getInt(PREF_CUSTOM_COLOR_KEY, DEFAULT_COLOR_KEY);
  }

  public static int getColorLetters(SharedPreferences prefs) {
    return prefs.getInt(PREF_CUSTOM_COLOR_LETTERS, DEFAULT_COLOR_LETTERS);
  }

  public static int getColorKeyBg(SharedPreferences prefs) {
    return prefs.getInt(PREF_CUSTOM_COLOR_KEY_BG, DEFAULT_COLOR_KEY_BG);
  }

  public static int getColorKeyboardBg(SharedPreferences prefs) {
    return prefs.getInt(PREF_CUSTOM_COLOR_KEYBOARD_BG, DEFAULT_COLOR_KEYBOARD_BG);
  }

  public static int getColorAction(SharedPreferences prefs) {
    return prefs.getInt(PREF_CUSTOM_COLOR_ACTION, DEFAULT_COLOR_ACTION);
  }

  public static int getColorActivated(SharedPreferences prefs) {
    return prefs.getInt(PREF_CUSTOM_COLOR_ACTIVATED, DEFAULT_COLOR_ACTIVATED);
  }

  public static int getColorSpacebar(SharedPreferences prefs) {
    return prefs.getInt(PREF_CUSTOM_COLOR_SPACEBAR, DEFAULT_COLOR_SPACEBAR);
  }

  public static int getColorSublabel(SharedPreferences prefs) {
    return prefs.getInt(PREF_CUSTOM_COLOR_SUBLABEL, DEFAULT_COLOR_SUBLABEL);
  }

  public static int getBgOpacity(SharedPreferences prefs) {
    return prefs.getInt(PREF_CUSTOM_BG_OPACITY, DEFAULT_BG_OPACITY);
  }
}
