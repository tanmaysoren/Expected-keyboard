package expected.keyboard2;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity
{
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    // The preferences can't be read when in direct-boot mode. Avoid crashing
    // and don't allow changing the settings.
    // Run the config migration on this prefs as it might be different from the
    // one used by the keyboard, which have been migrated.
    try
    {
      Config.migrate(getPreferenceManager().getSharedPreferences());
    }
    catch (Exception _e) { fallbackEncrypted(); return; }
    addPreferencesFromResource(R.xml.settings);

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
    // Can't communicate with the user here.
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
