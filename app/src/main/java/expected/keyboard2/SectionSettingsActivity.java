package expected.keyboard2;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * SectionSettingsActivity
 * Displays only the preferences for a specific section (Prediction, Layouts, Theme, Typing, Clipboard)
 * in a sleek floating popup window / dialog.
 */
public class SectionSettingsActivity extends PreferenceActivity
{
  public static final String EXTRA_SECTION = "extra_section";
  public static final String SECTION_PREDICTION = "prediction";
  public static final String SECTION_LAYOUTS = "layouts";
  public static final String SECTION_THEME = "theme";
  public static final String SECTION_TYPING = "typing";
  public static final String SECTION_CLIPBOARD = "clipboard";

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    Window window = getWindow();
    if (window != null)
    {
      WindowManager.LayoutParams params = window.getAttributes();
      float density = getResources().getDisplayMetrics().density;
      int screenWidth = getResources().getDisplayMetrics().widthPixels;
      int screenHeight = getResources().getDisplayMetrics().heightPixels;

      int width = (int) (screenWidth * 0.94f);
      int maxWidth = (int) (620 * density);
      params.width = Math.min(width, maxWidth);

      int maxHeight = (int) (screenHeight * 0.88f);
      params.height = Math.min(ViewGroup.LayoutParams.WRAP_CONTENT, maxHeight);
      params.gravity = Gravity.CENTER;
      window.setAttributes(params);
    }

    try
    {
      Config.migrate(getPreferenceManager().getSharedPreferences());
    }
    catch (Exception _e)
    {
      fallbackEncrypted();
      return;
    }

    String section = getIntent() != null ? getIntent().getStringExtra(EXTRA_SECTION) : null;
    if (SECTION_PREDICTION.equals(section))
    {
      setTitle(R.string.futo_sec_prediction_title);
      addPreferencesFromResource(R.xml.settings_prediction);
    }
    else if (SECTION_LAYOUTS.equals(section))
    {
      setTitle(R.string.futo_sec_layouts_title);
      addPreferencesFromResource(R.xml.settings_layouts);
    }
    else if (SECTION_THEME.equals(section))
    {
      setTitle(R.string.futo_sec_theme_title);
      addPreferencesFromResource(R.xml.settings_theme);
    }
    else if (SECTION_TYPING.equals(section))
    {
      setTitle(R.string.futo_sec_typing_title);
      addPreferencesFromResource(R.xml.settings_typing);
    }
    else if (SECTION_CLIPBOARD.equals(section))
    {
      setTitle(R.string.futo_sec_clipboard_title);
      addPreferencesFromResource(R.xml.settings_clipboard);
    }
    else
    {
      setTitle(R.string.app_name);
      addPreferencesFromResource(R.xml.settings);
    }

    boolean foldableDevice = FoldStateTracker.isFoldableDevice(this);
    setPrefEnabledSafe("margin_bottom_portrait_unfolded", foldableDevice);
    setPrefEnabledSafe("margin_bottom_landscape_unfolded", foldableDevice);
    setPrefEnabledSafe("horizontal_margin_portrait_unfolded", foldableDevice);
    setPrefEnabledSafe("horizontal_margin_landscape_unfolded", foldableDevice);
    setPrefEnabledSafe("keyboard_height_unfolded", foldableDevice);
    setPrefEnabledSafe("keyboard_height_landscape_unfolded", foldableDevice);
  }

  private void setPrefEnabledSafe(String key, boolean enabled)
  {
    Preference p = findPreference(key);
    if (p != null)
    {
      p.setEnabled(enabled);
    }
  }

  void fallbackEncrypted()
  {
    finish();
  }

  @Override
  protected void onStop()
  {
    DirectBootAwarePreferences
      .copy_preferences_to_protected_storage(this,
          getPreferenceManager().getSharedPreferences());
    super.onStop();
  }
}
