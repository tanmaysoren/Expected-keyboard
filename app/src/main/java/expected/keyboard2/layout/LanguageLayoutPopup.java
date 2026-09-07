package expected.keyboard2.layout;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import expected.keyboard2.Config;
import expected.keyboard2.KeyEventHandler;
import expected.keyboard2.KeyboardData;
import expected.keyboard2.R;
import expected.keyboard2.VibratorCompat;
import expected.keyboard2.prefs.LayoutsPreference;

public final class LanguageLayoutPopup
{
  private static PopupWindow s_currentPopup = null;

  public static boolean isShowing()
  {
    return s_currentPopup != null && s_currentPopup.isShowing();
  }

  public static void dismiss()
  {
    if (s_currentPopup != null)
    {
      try
      {
        s_currentPopup.dismiss();
      }
      catch (Exception ignored) {}
      s_currentPopup = null;
    }
  }

  public static void toggle(View anchor)
  {
    if (isShowing())
    {
      dismiss();
    }
    else if (anchor != null)
    {
      show(anchor.getContext(), anchor);
    }
  }

  /**
   * Helper class to extract and hold all colors from the active keyboard theme.
   * Completely avoids neon or hardcoded colors so the popup looks 100% native
   * to whichever keyboard theme is active.
   */
  public static class ThemeColors
  {
    public final int colorKeyboard;
    public final int colorKey;
    public final int colorKeyAction;
    public final int colorLabel;
    public final int colorSubLabel;
    public final int colorLabelActivated;
    public final float keyBorderRadius;
    public final boolean isLight;

    public ThemeColors(Context context)
    {
      int[] attrs = new int[] {
        R.attr.colorKeyboard,
        R.attr.colorKey,
        R.attr.colorKeyAction,
        R.attr.colorLabel,
        R.attr.colorSubLabel,
        R.attr.colorLabelActivated,
        R.attr.keyBorderRadius
      };

      TypedArray ta = null;
      int bg = 0xFF1B1B1B;
      int key = 0xFF333333;
      int keyAction = 0xFF3D3D3D;
      int label = 0xFFFFFFFF;
      int subLabel = 0xFFAAAAAA;
      int labelAct = 0xFF8AB4F8;
      float radius = 10f * context.getResources().getDisplayMetrics().density;

      try
      {
        ta = context.obtainStyledAttributes(attrs);
        bg = ta.getColor(0, bg);
        key = ta.getColor(1, key);
        keyAction = ta.getColor(2, key);
        label = ta.getColor(3, label);
        subLabel = ta.getColor(4, subLabel);
        labelAct = ta.getColor(5, label);
        radius = ta.getDimension(6, radius);
      }
      catch (Exception ignored) {}
      finally
      {
        if (ta != null) ta.recycle();
      }

      this.colorKeyboard = bg;
      this.colorKey = key;
      this.colorKeyAction = keyAction;
      this.colorLabel = label;
      this.colorSubLabel = subLabel;
      this.colorLabelActivated = labelAct;
      this.keyBorderRadius = radius;

      double lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0;
      this.isLight = lum > 0.5;
    }
  }

  /**
   * Resolves the primary keyboard container view from an anchor view.
   * This works whether in docked mode or floating mode.
   */
  private static View findKeyboardContainer(View anchor)
  {
    if (anchor == null) return null;

    // Check if anchor is already the container with keyboard_view
    if (anchor.findViewById(R.id.keyboard_view) != null && anchor.findViewById(R.id.candidates_view) != null)
    {
      return anchor;
    }

    // Traverse upwards to locate the keyboard container (e.g. _keyboard_container_view)
    View curr = anchor;
    while (curr != null)
    {
      if (curr.getId() == R.id.keyboard_view && curr.getParent() instanceof View)
      {
        return (View) curr.getParent();
      }
      if (curr instanceof ViewGroup && curr.findViewById(R.id.keyboard_view) != null)
      {
        return curr;
      }
      if (curr.getParent() instanceof View)
      {
        curr = (View) curr.getParent();
      }
      else
      {
        break;
      }
    }

    // Search downwards from root view
    View root = anchor.getRootView();
    if (root != null)
    {
      View kbView = root.findViewById(R.id.keyboard_view);
      if (kbView != null && kbView.getParent() instanceof View)
      {
        return (View) kbView.getParent();
      }
    }

    return anchor;
  }

  public static void show(Context context, View anchor)
  {
    dismiss();
    if (context == null || anchor == null) return;

    // 1. Resolve current keyboard theme so all colors match the keyboard
    Config cfg = Config.globalConfig();
    int themeId = (cfg != null && cfg.theme != 0) ? cfg.theme : 0;
    if (themeId == 0)
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      themeId = Config.getThemeId(context.getResources(), prefs.getString("theme", ""));
    }
    Context themedContext = new ContextThemeWrapper(context, themeId);
    final ThemeColors colors = new ThemeColors(themedContext);

    // 2. Inflate layout with the themed context
    View root = LayoutInflater.from(themedContext).inflate(R.layout.popup_language_switcher, null, false);

    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    float density = dm.density;

    // Theme the popup container background
    GradientDrawable popupBg = new GradientDrawable();
    popupBg.setShape(GradientDrawable.RECTANGLE);
    popupBg.setColor(colors.colorKeyboard);
    popupBg.setStroke((int) (1.2f * density), colors.colorKey);
    popupBg.setCornerRadius(Math.max(8 * density, Math.min(14 * density, colors.keyBorderRadius)));
    root.setBackground(popupBg);

    // Theme header components
    ImageView imgHeaderIcon = root.findViewById(R.id.popup_header_icon);
    if (imgHeaderIcon != null)
    {
      imgHeaderIcon.setColorFilter(colors.colorLabel);
    }
    TextView txtHeaderTitle = root.findViewById(R.id.popup_header_title);
    if (txtHeaderTitle != null)
    {
      txtHeaderTitle.setTextColor(colors.colorLabel);
    }
    ImageButton btnClose = root.findViewById(R.id.popup_btn_close);
    if (btnClose != null)
    {
      btnClose.setColorFilter(colors.colorSubLabel);
      btnClose.setOnClickListener(v -> dismiss());
    }

    // Filter chips
    final Button chipPopular = root.findViewById(R.id.chip_popup_popular);
    final Button chipAll = root.findViewById(R.id.chip_popup_all);
    final Button chipLatin = root.findViewById(R.id.chip_popup_latin);
    final Button chipCyrillic = root.findViewById(R.id.chip_popup_cyrillic);
    final Button chipIndic = root.findViewById(R.id.chip_popup_indic);
    final Button chipOther = root.findViewById(R.id.chip_popup_other);

    final ListView listView = root.findViewById(R.id.popup_layouts_list);

    final List<PopupLayoutItem> allItems = loadLayoutItems(themedContext);
    final List<PopupLayoutItem> displayItems = new ArrayList<>();
    final PopupLayoutAdapter adapter = new PopupLayoutAdapter(themedContext, displayItems, colors);
    listView.setAdapter(adapter);

    final String[] currentFilter = new String[]{"popular"};

    Runnable updateFilter = () -> {
      displayItems.clear();
      String cat = currentFilter[0];
      for (PopupLayoutItem it : allItems)
      {
        if ("all".equals(cat))
        {
          displayItems.add(it);
        }
        else if ("popular".equals(cat))
        {
          if (isQwertyUs(it) || it.isPopular || it.isSelected || it.isActive)
          {
            displayItems.add(it);
          }
        }
        else if (cat.equals(it.category))
        {
          displayItems.add(it);
        }
      }

      if ("popular".equals(cat))
      {
        displayItems.sort((a, b) -> {
          boolean aUs = isQwertyUs(a);
          boolean bUs = isQwertyUs(b);
          if (aUs != bUs) return aUs ? -1 : 1;
          if (a.isSelected != b.isSelected) return a.isSelected ? -1 : 1;
          if (a.isActive != b.isActive) return a.isActive ? -1 : 1;
          return a.displayName.compareToIgnoreCase(b.displayName);
        });
      }

      adapter.notifyDataSetChanged();
      listView.setSelection(0);

      // Update chip styles according to theme
      Button[] chips = {chipPopular, chipAll, chipLatin, chipCyrillic, chipIndic, chipOther};
      String[] tags = {"popular", "all", "latin", "cyrillic", "indic", "other"};
      for (int i = 0; i < chips.length; i++)
      {
        if (chips[i] != null)
        {
          boolean selected = tags[i].equals(cat);
          GradientDrawable chipBg = new GradientDrawable();
          chipBg.setShape(GradientDrawable.RECTANGLE);
          chipBg.setCornerRadius(12 * density);

          if (selected)
          {
            chipBg.setColor(colors.colorKey);
            chipBg.setStroke((int) (1.2f * density), colors.colorLabelActivated);
            chips[i].setBackground(chipBg);
            chips[i].setTextColor(colors.colorLabel);
          }
          else
          {
            chipBg.setColor(Color.TRANSPARENT);
            chipBg.setStroke((int) (1f * density), colors.colorKey);
            chips[i].setBackground(chipBg);
            chips[i].setTextColor(colors.colorSubLabel);
          }
        }
      }
    };

    if (chipPopular != null) chipPopular.setOnClickListener(v -> { currentFilter[0] = "popular"; updateFilter.run(); });
    if (chipAll != null) chipAll.setOnClickListener(v -> { currentFilter[0] = "all"; updateFilter.run(); });
    if (chipLatin != null) chipLatin.setOnClickListener(v -> { currentFilter[0] = "latin"; updateFilter.run(); });
    if (chipCyrillic != null) chipCyrillic.setOnClickListener(v -> { currentFilter[0] = "cyrillic"; updateFilter.run(); });
    if (chipIndic != null) chipIndic.setOnClickListener(v -> { currentFilter[0] = "indic"; updateFilter.run(); });
    if (chipOther != null) chipOther.setOnClickListener(v -> { currentFilter[0] = "other"; updateFilter.run(); });

    // Item click - directly switch layout without resetting to system language
    listView.setOnItemClickListener((parent, view, position, id) -> {
      VibratorCompat.vibrate(view);
      if (position >= 0 && position < displayItems.size())
      {
        PopupLayoutItem item = displayItems.get(position);
        Config currentCfg = Config.globalConfig();
        if (currentCfg != null && currentCfg.handler instanceof KeyEventHandler)
        {
          KeyEventHandler.IReceiver receiver = ((KeyEventHandler) currentCfg.handler).getReceiver();
          if (receiver != null)
          {
            receiver.switch_to_layout_name(item.name);
          }
        }
        Toast.makeText(context, "Layout: " + item.displayName, Toast.LENGTH_SHORT).show();
        dismiss();
      }
    });

    updateFilter.run();

    // 3. Precise Keyboard Container Positioning
    // Guarantee that popup appears strictly over the keyboard (in docked or floating mode),
    // and nowhere else on screen.
    View keyboardContainer = findKeyboardContainer(anchor);
    if (keyboardContainer == null) keyboardContainer = anchor;

    int[] kbLoc = new int[2];
    keyboardContainer.getLocationInWindow(kbLoc);
    int kbLeft = kbLoc[0];
    int kbTop = kbLoc[1];
    int kbWidth = keyboardContainer.getWidth();
    int kbHeight = keyboardContainer.getHeight();

    if (kbWidth <= 0) kbWidth = anchor.getWidth();
    if (kbWidth <= 0) kbWidth = dm.widthPixels;
    if (kbHeight <= 0) kbHeight = anchor.getHeight();
    if (kbHeight <= 0) kbHeight = (int) (220 * density);

    // Padding inside keyboard boundaries so popup sits strictly within the keyboard frame
    int marginH = (int) (6 * density);
    int marginV = (int) (4 * density);

    int maxW = Math.max((int) (180 * density), kbWidth - (marginH * 2));
    int maxH = Math.max((int) (140 * density), kbHeight - (marginV * 2));

    int popupWidth = Math.min(maxW, (int) (350 * density));
    int popupHeight = Math.min(maxH, (int) (230 * density));

    if (popupWidth > kbWidth) popupWidth = kbWidth;
    if (popupHeight > kbHeight) popupHeight = kbHeight;

    // Center popup directly over keyboard container
    int x = kbLeft + (kbWidth - popupWidth) / 2;
    int y = kbTop + (kbHeight - popupHeight) / 2;

    // Clamp strictly within keyboard container bounds to prevent overflowing outside the keyboard
    if (x < kbLeft) x = kbLeft;
    if (x + popupWidth > kbLeft + kbWidth) x = kbLeft + kbWidth - popupWidth;
    if (y < kbTop) y = kbTop;
    if (y + popupHeight > kbTop + kbHeight) y = kbTop + kbHeight - popupHeight;

    PopupWindow popup = new PopupWindow(root, popupWidth, popupHeight, false);
    popup.setOutsideTouchable(true);
    popup.setFocusable(false);
    popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    popup.setElevation(8 * density);
    popup.setClippingEnabled(true);
    popup.setAnimationStyle(android.R.style.Animation_Dialog);
    popup.setOnDismissListener(() -> s_currentPopup = null);

    s_currentPopup = popup;
    popup.showAtLocation(keyboardContainer, Gravity.TOP | Gravity.START, x, y);
  }

  private static List<PopupLayoutItem> loadLayoutItems(Context context)
  {
    List<PopupLayoutItem> list = new ArrayList<>();
    Resources res = context.getResources();

    List<String> names = LayoutsPreference.get_layout_names(res);
    String[] displayNames = res.getStringArray(R.array.pref_layout_entries);

    Config cfg = Config.globalConfig();
    KeyEventHandler.IReceiver receiver = null;
    if (cfg != null && cfg.handler instanceof KeyEventHandler)
    {
      receiver = ((KeyEventHandler) cfg.handler).getReceiver();
    }

    String currentActiveLayout = "";
    HashSet<String> userLayouts = new HashSet<>();
    if (receiver != null)
    {
      List<KeyboardData> activeList = receiver.get_active_layouts();
      int activeIndex = receiver.get_current_layout_index();
      if (activeList != null)
      {
        if (activeIndex >= 0 && activeIndex < activeList.size())
        {
          KeyboardData kd = activeList.get(activeIndex);
          if (kd != null && kd.name != null)
          {
            currentActiveLayout = kd.name;
          }
        }
        for (KeyboardData kd : activeList)
        {
          if (kd != null && kd.name != null)
          {
            userLayouts.add(kd.name);
          }
        }
      }
    }
    if (currentActiveLayout.isEmpty())
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      currentActiveLayout = prefs.getString("layout", "");
    }


    for (int i = 0; i < names.size(); i++)
    {
      String name = names.get(i);
      String displayName = (i < displayNames.length) ? displayNames[i] : name;
      String category = categorizeLayout(name, displayName);
      boolean isPopular = isPopularLayout(name) || isQwertyUs(name, displayName);
      boolean isActive = userLayouts.contains(name);
      boolean isSelected = name.equalsIgnoreCase(currentActiveLayout);

      list.add(new PopupLayoutItem(name, displayName, category, isPopular, isActive, isSelected));
    }

    boolean hasQwertyUs = false;
    for (PopupLayoutItem item : list)
    {
      if (isQwertyUs(item))
      {
        hasQwertyUs = true;
        break;
      }
    }
    if (!hasQwertyUs)
    {
      list.add(0, new PopupLayoutItem("latn_qwerty_us", "QWERTY (US)", "latin", true, true,
          currentActiveLayout.equalsIgnoreCase("latn_qwerty_us") || currentActiveLayout.isEmpty()));
    }

    list.sort((a, b) -> {
      boolean aUs = isQwertyUs(a);
      boolean bUs = isQwertyUs(b);
      if (aUs != bUs) return aUs ? -1 : 1;
      if (a.isSelected != b.isSelected) return a.isSelected ? -1 : 1;
      if (a.isActive != b.isActive) return a.isActive ? -1 : 1;
      if (a.isPopular != b.isPopular) return a.isPopular ? -1 : 1;
      return a.displayName.compareToIgnoreCase(b.displayName);
    });

    return list;
  }

  public static boolean isQwertyUs(PopupLayoutItem item)
  {
    if (item == null) return false;
    return isQwertyUs(item.name, item.displayName);
  }

  public static boolean isQwertyUs(String name, String displayName)
  {
    if (name != null)
    {
      String n = name.trim().toLowerCase(Locale.US);
      if (n.equals("latn_qwerty_us") || n.equals("qwerty_us") || n.equals("en_us") || n.contains("qwerty_us"))
      {
        return true;
      }
    }
    if (displayName != null)
    {
      String d = displayName.trim().toLowerCase(Locale.US);
      if (d.equals("qwerty (us)") || d.equals("qwerty us") || d.contains("qwerty (us)"))
      {
        return true;
      }
    }
    return false;
  }

  private static String categorizeLayout(String name, String displayName)
  {
    String lowerName = name.toLowerCase(Locale.US);
    String lowerDisplay = displayName.toLowerCase(Locale.US);

    if (lowerName.contains("ru") || lowerName.contains("cyrillic") || lowerName.contains("uk") ||
        lowerName.contains("bg") || lowerName.contains("be") || lowerDisplay.contains("russian") ||
        lowerDisplay.contains("ukrainian") || lowerDisplay.contains("bulgarian") || lowerDisplay.contains("cyrillic"))
    {
      return "cyrillic";
    }

    if (lowerName.contains("hi") || lowerName.contains("indic") || lowerName.contains("bengali") ||
        lowerName.contains("tamil") || lowerName.contains("telugu") || lowerName.contains("devanagari") ||
        lowerDisplay.contains("hindi") || lowerDisplay.contains("indic") || lowerDisplay.contains("devanagari"))
    {
      return "indic";
    }

    if (lowerName.contains("ar") || lowerName.contains("arabic") || lowerName.contains("he") ||
        lowerName.contains("hebrew") || lowerName.contains("fa") || lowerName.contains("persian") ||
        lowerName.contains("el") || lowerName.contains("greek") || lowerName.contains("ko") ||
        lowerName.contains("hangul") || lowerName.contains("ja") || lowerName.contains("kana"))
    {
      return "other";
    }

    return "latin";
  }

  private static boolean isPopularLayout(String name)
  {
    String lower = name.toLowerCase(Locale.US);
    return lower.contains("en_us") || lower.contains("en_gb") || lower.contains("qwerty") ||
           lower.contains("dvorak") || lower.contains("colemak") || lower.contains("es_") ||
           lower.contains("fr_") || lower.contains("de_") || lower.contains("pt_") ||
           lower.contains("ru_") || lower.contains("hi_") || lower.contains("it_") ||
           lower.contains("workman");
  }

  public static class PopupLayoutItem
  {
    public final String name;
    public final String displayName;
    public final String category;
    public final boolean isPopular;
    public final boolean isActive;
    public final boolean isSelected;

    public PopupLayoutItem(String name, String displayName, String category,
                           boolean isPopular, boolean isActive, boolean isSelected)
    {
      this.name = name;
      this.displayName = displayName;
      this.category = category;
      this.isPopular = isPopular;
      this.isActive = isActive;
      this.isSelected = isSelected;
    }
  }

  private static class PopupLayoutAdapter extends BaseAdapter
  {
    private final Context context;
    private final List<PopupLayoutItem> items;
    private final ThemeColors colors;
    private final float density;

    public PopupLayoutAdapter(Context context, List<PopupLayoutItem> items, ThemeColors colors)
    {
      this.context = context;
      this.items = items;
      this.colors = colors;
      this.density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public PopupLayoutItem getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
      ViewHolder holder;
      if (convertView == null)
      {
        convertView = LayoutInflater.from(context).inflate(R.layout.layout_picker_item, parent, false);
        holder = new ViewHolder();
        holder.root = convertView;
        holder.txtTitle = convertView.findViewById(R.id.layout_item_title);
        holder.txtSubtitle = convertView.findViewById(R.id.layout_item_subtitle);
        holder.imgCheck = convertView.findViewById(R.id.layout_item_check);
        holder.imgIcon = convertView.findViewById(R.id.layout_item_icon);
        convertView.setTag(holder);
      }
      else
      {
        holder = (ViewHolder) convertView.getTag();
      }

      PopupLayoutItem item = getItem(position);
      holder.txtTitle.setText(item.displayName);
      holder.txtSubtitle.setText(isQwertyUs(item) ? (item.name + " • English (US)") : item.name);

      if (item.isSelected)
      {
        GradientDrawable selBg = new GradientDrawable();
        selBg.setShape(GradientDrawable.RECTANGLE);
        selBg.setColor(colors.colorKey);
        selBg.setStroke((int) (1.2f * density), colors.colorLabelActivated);
        selBg.setCornerRadius(8 * density);
        holder.root.setBackground(selBg);

        holder.imgCheck.setVisibility(View.VISIBLE);
        holder.imgCheck.setColorFilter(colors.colorLabelActivated);
        holder.imgIcon.setColorFilter(colors.colorLabelActivated);
        holder.txtTitle.setTextColor(colors.colorLabel);
        holder.txtSubtitle.setTextColor(colors.colorSubLabel);
      }
      else
      {
        holder.root.setBackground(null);
        holder.imgCheck.setVisibility(View.GONE);
        holder.imgIcon.setColorFilter(colors.colorSubLabel);
        holder.txtTitle.setTextColor(colors.colorLabel);
        holder.txtSubtitle.setTextColor(colors.colorSubLabel);
      }

      return convertView;
    }

    private static class ViewHolder
    {
      View root;
      TextView txtTitle;
      TextView txtSubtitle;
      ImageView imgCheck;
      ImageView imgIcon;
    }
  }
}
