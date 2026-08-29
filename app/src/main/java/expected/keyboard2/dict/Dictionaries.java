package expected.keyboard2.dict;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import expected.keyboard2.Config;
import expected.keyboard2.DirectBootAwarePreferences;
import expected.keyboard2.Logs;
import expected.keyboard2.prediction.Dictionary;
import expected.keyboard2.prediction.PredictionEngine;

/** Manage and load installed dictionaries. */
public final class Dictionaries
{
  public static Dictionaries instance(Context ctx)
  {
    if (_instance == null)
    {
      SharedPreferences prefs =
        DirectBootAwarePreferences.get_protected_prefs(ctx, "dictionaries");
      _instance = new Dictionaries(ctx, prefs);
    }
    return _instance;
  }

  public void set_current_dictionary(Config config, String name)
  {
    if (config == null)
      return;
    config.current_dictionary = null;
    config.emoji_dictionary = null;
    config.active_dictionaries = new ArrayList<Dictionary>();
    config.emoji_dictionaries = new ArrayList<Dictionary>();

    if (name != null)
    {
      Dictionary dict = load(name);
      if (dict != null)
      {
        config.current_dictionary = dict;
        config.active_dictionaries.add(dict);
      }
    }

    for (String dict_name : _installed_dictionaries)
    {
      if (name != null && dict_name.equals(name))
        continue;
      Dictionary dict = load(dict_name);
      if (dict != null && !config.active_dictionaries.contains(dict))
      {
        if (config.current_dictionary == null)
          config.current_dictionary = dict;
        config.active_dictionaries.add(dict);
      }
    }
  }

  public Dictionary load(String dict_name)
  {
    if (_loaded_dictionaries.containsKey(dict_name))
      return _loaded_dictionaries.get(dict_name);
    Dictionary dict = load_uncached(dict_name);
    _loaded_dictionaries.put(dict_name, dict);
    return dict;
  }

  public Set<String> get_installed() { return _installed_dictionaries; }

  public String get_selected(Config config)
  {
    return _shared_prefs.getString(dict_selection_pref_name(config), null);
  }

  public void set_selected(Config config, String dict_name)
  {
    _shared_prefs.edit()
      .putString(dict_selection_pref_name(config), dict_name)
      .apply();
  }

  public void install(String dict_name, byte[] data) throws IOException
  {
    FileOutputStream outp = _context.openFileOutput(dict_file_name(dict_name),
        Context.MODE_PRIVATE);
    outp.write(data);
    outp.close();
    set_installed(dict_name);
  }

  public File get_install_location(String dict_name)
  {
    return _context.getFileStreamPath(dict_file_name(dict_name));
  }

  public void set_installed(String dict_name)
  {
    _installed_dictionaries.add(dict_name);
    _loaded_dictionaries.remove(dict_name);
    save();
    if (Config.globalConfig() != null)
      set_current_dictionary(Config.globalConfig(), get_selected(Config.globalConfig()));
  }

  public void uninstall(String dict_name)
  {
    _context.deleteFile(dict_file_name(dict_name));
    _installed_dictionaries.remove(dict_name);
    _loaded_dictionaries.remove(dict_name);
    save();
    if (Config.globalConfig() != null)
      set_current_dictionary(Config.globalConfig(), get_selected(Config.globalConfig()));
  }

  Context _context;
  Set<String> _installed_dictionaries;
  SharedPreferences _shared_prefs;
  Map<String, Dictionary> _loaded_dictionaries;

  static Dictionaries _instance = null;
  static final String PREF_INSTALLED_DICTS = "installed";

  Dictionaries(Context ctx, SharedPreferences prefs)
  {
    _context = ctx;
    _installed_dictionaries = new HashSet<String>();
    _shared_prefs = prefs;
    _loaded_dictionaries = new TreeMap<String, Dictionary>();
    Set<String> installed = prefs.getStringSet(PREF_INSTALLED_DICTS, null);
    if (installed != null)
      _installed_dictionaries.addAll(installed);
  }

  Dictionary load_uncached(String dict_name)
  {
    if (!_installed_dictionaries.contains(dict_name))
      return null;
    try
    {
      File file = get_install_location(dict_name);
      return Dictionary.loadFromFile(dict_name, file);
    }
    catch (Throwable e)
    {
      return null;
    }
  }

  void save()
  {
    _shared_prefs.edit()
      .putStringSet(PREF_INSTALLED_DICTS, _installed_dictionaries)
      .commit();
  }

  static String dict_file_name(String dict_name)
  {
    return dict_name + ".dict";
  }

  static String dict_selection_pref_name(Config config)
  {
    String lang_tag = (config.device_locales != null && config.device_locales.default_ != null) ?
      config.device_locales.default_.lang_tag : "";
    return "selection:" + lang_tag + "-" + config.get_current_layout();
  }
}
