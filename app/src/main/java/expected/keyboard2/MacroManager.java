package expected.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages user-defined macros and text expansions.
 * Macros map a typed trigger (e.g. "@", "gpo", "sh") to expanded text (e.g. "git push", "#!/bin/bash").
 */
public final class MacroManager
{
  public static final String PREF_MACRO_KEYS = "macro_keys_map";

  private MacroManager() {}

  /**
   * Retrieves all saved macros as a LinkedHashMap of trigger -> expansion.
   */
  public static synchronized LinkedHashMap<String, String> getMacros(SharedPreferences prefs)
  {
    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
    if (prefs == null)
      return map;

    String json = prefs.getString(PREF_MACRO_KEYS, null);
    if (json == null || json.trim().isEmpty())
      return map;

    try
    {
      if (json.trim().startsWith("["))
      {
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++)
        {
          JSONObject obj = arr.getJSONObject(i);
          String trigger = obj.optString("trigger", "");
          String expansion = obj.optString("expansion", "");
          if (!trigger.isEmpty())
          {
            map.put(trigger, expansion);
          }
        }
      }
      else if (json.trim().startsWith("{"))
      {
        JSONObject obj = new JSONObject(json);
        java.util.Iterator<String> keys = obj.keys();
        while (keys.hasNext())
        {
          String k = keys.next();
          if (!k.isEmpty())
          {
            map.put(k, obj.optString(k, ""));
          }
        }
      }
    }
    catch (Exception e)
    {
      Logs.warn("Error parsing macro keys JSON", e);
    }
    return map;
  }

  public static LinkedHashMap<String, String> getMacros(Context context)
  {
    if (context == null)
      return new LinkedHashMap<String, String>();
    return getMacros(DirectBootAwarePreferences.get_shared_preferences(context));
  }

  /**
   * Saves the provided macros to SharedPreferences.
   */
  public static synchronized void saveMacros(SharedPreferences prefs, Map<String, String> macros)
  {
    if (prefs == null)
      return;

    try
    {
      JSONArray arr = new JSONArray();
      if (macros != null)
      {
        for (Map.Entry<String, String> entry : macros.entrySet())
        {
          String trigger = entry.getKey();
          if (trigger == null || trigger.isEmpty())
            continue;
          JSONObject obj = new JSONObject();
          obj.put("trigger", trigger);
          obj.put("expansion", entry.getValue() != null ? entry.getValue() : "");
          arr.put(obj);
        }
      }
      prefs.edit().putString(PREF_MACRO_KEYS, arr.toString()).apply();
    }
    catch (Exception e)
    {
      Logs.warn("Error saving macro keys JSON", e);
    }
  }

  public static void saveMacros(Context context, Map<String, String> macros)
  {
    if (context == null)
      return;
    saveMacros(DirectBootAwarePreferences.get_shared_preferences(context), macros);
  }

  /**
   * Returns true if there is at least one macro defined.
   */
  public static boolean hasMacros(SharedPreferences prefs)
  {
    if (prefs == null)
      return false;
    String raw = prefs.getString(PREF_MACRO_KEYS, null);
    if (raw == null || raw.trim().isEmpty() || raw.trim().equals("[]") || raw.trim().equals("{}"))
      return false;
    return !getMacros(prefs).isEmpty();
  }

  public static boolean hasMacros(Context context)
  {
    if (context == null)
      return false;
    return hasMacros(DirectBootAwarePreferences.get_shared_preferences(context));
  }
}
