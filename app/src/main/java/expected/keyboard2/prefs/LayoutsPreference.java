package expected.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import expected.keyboard2.*;
import org.json.JSONException;
import org.json.JSONObject;

public class LayoutsPreference extends ListGroupPreference<LayoutsPreference.Layout>
{
  static final String KEY = "layouts";
  static final List<Layout> DEFAULT =
    Collections.singletonList((Layout)new SystemLayout());
  static final ListGroupPreference.Serializer<Layout> SERIALIZER =
    new Serializer();

  /** Text displayed for each layout in the dialog list. */
  String[] _layout_display_names;

  public LayoutsPreference(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    setKey(KEY);
    Resources res = ctx.getResources();
    _layout_display_names = res.getStringArray(R.array.pref_layout_entries);
  }

  /** Obtained from [res/values/layouts.xml]. */
  static List<String> _unsafe_layout_ids_str = null;
  static TypedArray _unsafe_layout_ids_res = null;

  /** Layout internal names. Contains "system" and "custom". */
  public static List<String> get_layout_names(Resources res)
  {
    if (_unsafe_layout_ids_str == null)
      _unsafe_layout_ids_str = Arrays.asList(
          res.getStringArray(R.array.pref_layout_values));
    return _unsafe_layout_ids_str;
  }

  /** Layout resource id for a layout name. [-1] if not found. */
  public static int layout_id_of_name(Resources res, String name)
  {
    if (name == null || name.isEmpty() || "system".equals(name) || "custom".equals(name))
      return -1;

    // 1. Direct XML resource identifier lookup (fast, resilient, always works)
    try
    {
      int id = res.getIdentifier(name, "xml", "expected.keyboard2");
      if (id > 0)
        return id;
    }
    catch (Exception ignored) {}

    // 2. Lookup via layout_ids typed array
    try
    {
      List<String> names = get_layout_names(res);
      int i = names.indexOf(name);
      if (i >= 0)
      {
        TypedArray ta = res.obtainTypedArray(R.array.layout_ids);
        int id = -1;
        if (i < ta.length())
        {
          id = ta.getResourceId(i, -1);
        }
        ta.recycle();
        return id;
      }
    }
    catch (Exception ignored) {}

    return -1;
  }

  /** [null] for the "system" layout. */
  public static List<KeyboardData> load_from_preferences(Resources res, SharedPreferences prefs)
  {
    List<KeyboardData> layouts = new ArrayList<KeyboardData>();
    for (Layout l : load_from_preferences(KEY, prefs, DEFAULT, SERIALIZER))
    {
      if (l instanceof NamedLayout)
      {
        String name = ((NamedLayout)l).name;
        KeyboardData kd = layout_of_string(res, name);
        if (kd != null)
        {
          kd.resourceName = name;
          layouts.add(kd);
        }
      }
      else if (l instanceof CustomLayout)
        layouts.add(((CustomLayout)l).parsed);
      else // instanceof SystemLayout
        layouts.add(null);
    }
    return layouts;
  }

  /** Does not call [prefs.commit()]. */
  public static void save_to_preferences(SharedPreferences.Editor prefs, List<Layout> items)
  {
    save_to_preferences(KEY, prefs, items, SERIALIZER);
  }

  private static final java.util.Map<String, KeyboardData> s_layoutCache = new java.util.concurrent.ConcurrentHashMap<>();

  public static void clear_cache()
  {
    s_layoutCache.clear();
  }

  public static void save_keyboard_data_to_preferences(SharedPreferences.Editor editor, List<KeyboardData> list)
  {
    List<Layout> layouts = new ArrayList<Layout>();
    if (list != null)
    {
      for (KeyboardData kd : list)
      {
        if (kd == null)
          layouts.add(new SystemLayout());
        else if (kd.resourceName != null && !kd.resourceName.isEmpty() && !"system".equals(kd.resourceName))
          layouts.add(new NamedLayout(kd.resourceName));
        else if (kd.name != null && !kd.name.isEmpty() && !"system".equals(kd.name))
          layouts.add(new NamedLayout(kd.name));
        else
          layouts.add(new SystemLayout());
      }
    }
    save_to_preferences(editor, layouts);
    editor.apply();
  }

  public static KeyboardData layout_of_string(Resources res, String name)
  {
    if (name == null) return null;
    KeyboardData cached = s_layoutCache.get(name);
    if (cached != null) return cached;
    int id = layout_id_of_name(res, name);
    if (id > 0)
    {
      KeyboardData kd = KeyboardData.load(res, id);
      if (kd != null)
      {
        kd.resourceName = name;
        s_layoutCache.put(name, kd);
        return kd;
      }
    }
    // Might happen when the app is downgraded, return the system layout.
    return null;
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
  {
    super.onSetInitialValue(restoreValue, defaultValue);
    if (_values.size() == 0)
      set_values(new ArrayList<Layout>(DEFAULT), false);
  }

  String label_of_layout(Layout l)
  {
    if (l instanceof NamedLayout)
    {
      String lname = ((NamedLayout)l).name;
      int value_i = get_layout_names(getContext().getResources()).indexOf(lname);
      return value_i < 0 ? lname : _layout_display_names[value_i];
    }
    else if (l instanceof CustomLayout)
    {
      // Use the layout's name if possible
      CustomLayout cl = (CustomLayout)l;
      if (cl.parsed != null && cl.parsed.name != null
          && !cl.parsed.name.equals(""))
        return cl.parsed.name;
      else
        return getContext().getString(R.string.pref_layout_e_custom);
    }
    else // instanceof SystemLayout
      return getContext().getString(R.string.pref_layout_e_system);
  }

  @Override
  String label_of_value(Layout value, int i)
  {
    return getContext().getString(R.string.pref_layouts_item, i + 1,
        label_of_layout(value));
  }

  @Override
  AddButton on_attach_add_button(AddButton prev_btn)
  {
    if (prev_btn == null)
      return new LayoutsAddButton(getContext());
    return prev_btn;
  }

  @Override
  boolean should_allow_remove_item(Layout value)
  {
    return (_values.size() > 1 && !(value instanceof CustomLayout));
  }

  @Override
  ListGroupPreference.Serializer<Layout> get_serializer() { return SERIALIZER; }

  void select_dialog(final SelectionCallback callback)
  {
    ArrayAdapter layouts = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, _layout_display_names);
    new AlertDialog.Builder(getContext())
      .setView(View.inflate(getContext(), R.layout.dialog_edit_text, null))
      .setAdapter(layouts, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface _dialog, int which)
        {
          String name = get_layout_names(getContext().getResources()).get(which);
          switch (name)
          {
            case "system":
              callback.select(new SystemLayout());
              break;
            case "custom":
              select_custom(callback, read_initial_custom_layout());
              break;
            default:
              callback.select(new NamedLayout(name));
              break;
          }
        }
      })
      .show();
  }

  /** Dialog for specifying a custom layout. [initial_text] is the layout
      description when modifying a layout. */
  void select_custom(final SelectionCallback callback, String initial_text)
  {
    boolean allow_remove = callback.allow_remove() && _values.size() > 1;
    CustomLayoutEditDialog.show(getContext(), initial_text, allow_remove,
        new CustomLayoutEditDialog.Callback()
        {
          public void select(String text)
          {
            if (text == null)
              callback.select(null);
            else
              callback.select(CustomLayout.parse(text));
          }

          public String validate(String text)
          {
            try
            {
              KeyboardData.load_string_exn(text);
              return null; // Validation passed
            }
            catch (Exception e)
            {
              return e.getMessage();
            }
          }
        });
  }

  /** Called when modifying a layout. Custom layouts behave differently. */
  @Override
  void select(final SelectionCallback callback, Layout prev_layout)
  {
    if (prev_layout != null && prev_layout instanceof CustomLayout)
      select_custom(callback, ((CustomLayout)prev_layout).xml);
    else
      select_dialog(callback);
  }

  /** The initial text for the custom layout entry box. The qwerty_us layout is
      a good default and contains a bit of documentation. */
  String read_initial_custom_layout()
  {
    try
    {
      Resources res = getContext().getResources();
      return Utils.read_all_utf8(res.openRawResource(R.raw.latn_qwerty_us));
    }
    catch (Exception _e)
    {
      return "";
    }
  }

  class LayoutsAddButton extends AddButton
  {
    public LayoutsAddButton(Context ctx)
    {
      super(ctx);
      setLayoutResource(R.layout.pref_layouts_add_btn);
    }
  }

  /** A layout selected by the user. The only implementations are
      [NamedLayout], [SystemLayout] and [CustomLayout]. */
  public interface Layout {}

  public static final class SystemLayout implements Layout
  {
    public SystemLayout() {}
  }

  /** The name of a layout defined in [srcs/layouts]. */
  public static final class NamedLayout implements Layout
  {
    public final String name;
    public NamedLayout(String n) { name = n; }
  }

  /** The XML description of a custom layout. */
  public static final class CustomLayout implements Layout
  {
    public final String xml;
    /** Might be null. */
    public final KeyboardData parsed;
    public CustomLayout(String xml_, KeyboardData k) { xml = xml_; parsed = k; }
    public static CustomLayout parse(String xml)
    {
      KeyboardData parsed = null;
      try { parsed = KeyboardData.load_string_exn(xml); }
      catch (Exception e) {}
      return new CustomLayout(xml, parsed);
    }
  }

  /** Named layouts are serialized to strings and custom layouts to JSON
      objects with a [kind] field. */
  public static class Serializer implements ListGroupPreference.Serializer<Layout>
  {
    public Layout load_item(Object obj) throws JSONException
    {
      if (obj instanceof String)
      {
        String name = (String)obj;
        if (name.equals("system"))
          return new SystemLayout();
        return new NamedLayout(name);
      }
      JSONObject obj_ = (JSONObject)obj;
      switch (obj_.getString("kind"))
      {
        case "custom": return CustomLayout.parse(obj_.getString("xml"));
        case "system": default: return new SystemLayout();
      }
    }

    public Object save_item(Layout v) throws JSONException
    {
      if (v instanceof NamedLayout)
        return ((NamedLayout)v).name;
      if (v instanceof CustomLayout)
        return new JSONObject().put("kind", "custom")
          .put("xml", ((CustomLayout)v).xml);
      return new JSONObject().put("kind", "system");
    }
  }
}
