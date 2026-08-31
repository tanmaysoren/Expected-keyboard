package expected.keyboard2.layout;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import expected.keyboard2.Config;
import expected.keyboard2.KeyEventHandler;
import expected.keyboard2.KeyboardData;
import expected.keyboard2.KeyValue;
import expected.keyboard2.Pointers;
import expected.keyboard2.R;
import expected.keyboard2.SectionSettingsActivity;
import expected.keyboard2.VibratorCompat;
import expected.keyboard2.prefs.LayoutsPreference;

/**
 * Native in-keyboard Layout & Language Switcher Pane.
 * Provides instant layout switching without popup window focus crashes,
 * category filters, alphabet jump bar, and Cyberpunk theme styling.
 */
public class LayoutSwitcherPaneView extends LinearLayout
{
  public static class LayoutItem
  {
    public final String name;
    public final String displayName;
    public final String category;
    public final boolean isActive;
    public final boolean isSelected;
    public final int activeIndex;

    public LayoutItem(String name, String displayName, String category, boolean isActive, boolean isSelected, int activeIndex)
    {
      this.name = name;
      this.displayName = displayName;
      this.category = category;
      this.isActive = isActive;
      this.isSelected = isSelected;
      this.activeIndex = activeIndex;
    }
  }

  private ImageButton btnBack;
  private ImageButton btnClose;
  private ListView listView;
  private LinearLayout alphabetContainer;

  private Button chipActive;
  private Button chipPopular;
  private Button chipAll;
  private Button chipLatin;
  private Button chipCyrillic;
  private Button chipIndic;
  private Button chipOther;

  private final List<LayoutItem> allLayoutItems = new ArrayList<>();
  private final List<LayoutItem> currentDisplayItems = new ArrayList<>();
  private LayoutAdapter adapter;
  private String currentCategoryFilter = "all"; // "active", "popular", "all", "latin", "cyrillic", "indic", "other"
  private String currentLetterFilter = null;

  public LayoutSwitcherPaneView(Context context)
  {
    super(context);
  }

  public LayoutSwitcherPaneView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();

    btnBack = findViewById(R.id.layout_pane_btn_back);
    btnClose = findViewById(R.id.layout_pane_btn_close);
    listView = findViewById(R.id.layout_pane_list);
    alphabetContainer = findViewById(R.id.layout_pane_alphabet_container);

    chipActive = findViewById(R.id.chip_filter_active);
    chipPopular = findViewById(R.id.chip_filter_popular);
    chipAll = findViewById(R.id.chip_filter_all);
    chipLatin = findViewById(R.id.chip_filter_latin);
    chipCyrillic = findViewById(R.id.chip_filter_cyrillic);
    chipIndic = findViewById(R.id.chip_filter_indic);
    chipOther = findViewById(R.id.chip_filter_other);

    if (btnBack != null)
    {
      btnBack.setOnClickListener(v -> closePane(v));
    }
    if (btnClose != null)
    {
      btnClose.setOnClickListener(v -> closePane(v));
    }

    setupCategoryChips();
    loadLayoutsData();
  }

  @Override
  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    loadLayoutsData();
  }

  private void closePane(View v)
  {
    VibratorCompat.vibrate(v);
    Config config = Config.globalConfig();
    if (config != null && config.handler != null)
    {
      config.handler.key_up(
          KeyValue.getKeyByName("switch_back_layout"), Pointers.Modifiers.EMPTY);
    }
  }

  private void setupCategoryChips()
  {
    if (chipActive != null) chipActive.setOnClickListener(v -> setFilterCategory("active"));
    if (chipPopular != null) chipPopular.setOnClickListener(v -> setFilterCategory("popular"));
    if (chipAll != null) chipAll.setOnClickListener(v -> setFilterCategory("all"));
    if (chipLatin != null) chipLatin.setOnClickListener(v -> setFilterCategory("latin"));
    if (chipCyrillic != null) chipCyrillic.setOnClickListener(v -> setFilterCategory("cyrillic"));
    if (chipIndic != null) chipIndic.setOnClickListener(v -> setFilterCategory("indic"));
    if (chipOther != null) chipOther.setOnClickListener(v -> setFilterCategory("other"));
  }

  private void setFilterCategory(String category)
  {
    currentCategoryFilter = category;
    currentLetterFilter = null;
    updateCategoryChipStyles();
    applyFilters();
  }

  private void updateCategoryChipStyles()
  {
    Button[] chips = {chipActive, chipPopular, chipAll, chipLatin, chipCyrillic, chipIndic, chipOther};
    String[] tags = {"active", "popular", "all", "latin", "cyrillic", "indic", "other"};

    for (int i = 0; i < chips.length; i++)
    {
      if (chips[i] != null)
      {
        boolean isSelected = tags[i].equals(currentCategoryFilter);
        chips[i].setTextColor(isSelected ? Color.parseColor("#FFFFFF") : Color.parseColor("#94A3B8"));
      }
    }
  }

  private static String determineCategory(String name)
  {
    if (name == null) return "latin";
    String n = name.toLowerCase(Locale.ROOT);
    if (n.startsWith("latn_") || n.contains("qwerty") || n.contains("colemak") || n.contains("dvorak")
        || n.contains("azerty") || n.contains("qwertz") || n.contains("workman") || n.contains("bepo"))
    {
      return "latin";
    }
    if (n.startsWith("cyrl_") || n.contains("russian") || n.contains("ukrainian") || n.contains("bulgarian")
        || n.contains("jcuken") || n.contains("serbian") || n.contains("belarusian"))
    {
      return "cyrillic";
    }
    if (n.startsWith("deva_") || n.startsWith("beng_") || n.startsWith("taml_") || n.startsWith("telu_")
        || n.startsWith("mlym_") || n.startsWith("gujr_") || n.startsWith("guru_") || n.startsWith("knda_")
        || n.startsWith("jpan_") || n.startsWith("hang_") || n.startsWith("thai_") || n.contains("hindi")
        || n.contains("bengali") || n.contains("korean") || n.contains("japanese"))
    {
      return "indic";
    }
    return "other";
  }

  private static boolean isPopular(String name)
  {
    if (name == null) return false;
    String n = name.toLowerCase(Locale.ROOT);
    return n.contains("qwerty_us") || n.contains("qwerty_uk") || n.contains("colemak")
        || n.contains("dvorak") || n.contains("azerty") || n.contains("qwertz_de") || n.contains("spanish")
        || n.contains("french") || n.contains("german") || n.contains("russian") || n.contains("hindi")
        || n.contains("arabic") || n.contains("japanese") || n.contains("korean") || n.contains("portuguese");
  }

  private void loadLayoutsData()
  {
    allLayoutItems.clear();

    Config config = Config.globalConfig();
    KeyEventHandler.IReceiver receiver = null;
    if (config != null && config.handler instanceof KeyEventHandler)
    {
      receiver = ((KeyEventHandler) config.handler).getReceiver();
    }

    Resources res = getContext().getResources();
    List<String> layoutNames = LayoutsPreference.get_layout_names(res);
    String[] layoutDisplayNames = res.getStringArray(R.array.pref_layout_entries);

    List<KeyboardData> activeLayouts = (receiver != null) ? receiver.get_active_layouts() : null;
    int currentActiveIdx = (receiver != null) ? receiver.get_current_layout_index() : 0;

    java.util.HashSet<String> addedNames = new java.util.HashSet<>();

    // 1. First add unique layouts from available layouts list
    for (int i = 0; i < layoutNames.size(); i++)
    {
      String name = layoutNames.get(i);
      if (name == null || addedNames.contains(name)) continue;
      addedNames.add(name);

      String display = (i < layoutDisplayNames.length) ? layoutDisplayNames[i] : name;
      String category = determineCategory(name);

      // Check if in active list
      boolean inActive = false;
      int activeIndex = -1;
      boolean isSelected = false;
      if (activeLayouts != null)
      {
        for (int a = 0; a < activeLayouts.size(); a++)
        {
          KeyboardData kd = activeLayouts.get(a);
          String rName = (kd != null && kd.resourceName != null) ? kd.resourceName : null;
          String aName = (kd != null && kd.name != null) ? kd.name : "latn_qwerty_us";
          if (name.equals(rName) || name.equals(aName))
          {
            inActive = true;
            activeIndex = a;
            isSelected = (a == currentActiveIdx);
            break;
          }
        }
      }

      allLayoutItems.add(new LayoutItem(name, display, category, inActive, isSelected, activeIndex));
    }

    // 2. Add any active layouts that might not be in layoutNames
    if (activeLayouts != null)
    {
      for (int a = 0; a < activeLayouts.size(); a++)
      {
        KeyboardData kd = activeLayouts.get(a);
        String rName = (kd != null && kd.resourceName != null) ? kd.resourceName : null;
        String aName = (kd != null && kd.name != null) ? kd.name : "latn_qwerty_us";
        String primaryKey = (rName != null) ? rName : aName;
        if (!addedNames.contains(primaryKey))
        {
          addedNames.add(primaryKey);
          String display = (kd != null && kd.name != null) ? kd.name : primaryKey;
          allLayoutItems.add(new LayoutItem(primaryKey, display, determineCategory(primaryKey), true, (a == currentActiveIdx), a));
        }
      }
    }

    buildAlphabetBar();
    updateCategoryChipStyles();
    applyFilters();

    if (adapter == null)
    {
      adapter = new LayoutAdapter(getContext(), currentDisplayItems);
      listView.setAdapter(adapter);
    }
    else
    {
      adapter.notifyDataSetChanged();
    }

    final KeyEventHandler.IReceiver finalReceiver = receiver;
    listView.setOnItemClickListener((parent, view, position, id) -> {
      VibratorCompat.vibrate(view);
      if (position >= 0 && position < currentDisplayItems.size())
      {
        LayoutItem item = currentDisplayItems.get(position);
        // Fetch fresh receiver at click time to avoid stale capture requiring two clicks
        KeyEventHandler.IReceiver freshReceiver = finalReceiver;
        Config cfg = Config.globalConfig();
        if (cfg != null && cfg.handler instanceof KeyEventHandler)
        {
          KeyEventHandler.IReceiver r = ((KeyEventHandler) cfg.handler).getReceiver();
          if (r != null) freshReceiver = r;
        }
        if (freshReceiver != null)
        {
          // Single-click reliable path: use name-based switch which handles both active and new layouts
          freshReceiver.switch_to_layout_name(item.name);
        }
        Toast.makeText(getContext(), "Layout: " + item.displayName, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void buildAlphabetBar()
  {
    if (alphabetContainer == null) return;
    alphabetContainer.removeAllViews();

    // Collect all first letters
    TreeSet<Character> letters = new TreeSet<>();
    for (LayoutItem item : allLayoutItems)
    {
      if (item.displayName != null && !item.displayName.isEmpty())
      {
        char c = Character.toUpperCase(item.displayName.charAt(0));
        if (Character.isLetter(c))
        {
          letters.add(c);
        }
      }
    }

    // Add "All" quick button
    Button btnAll = new Button(getContext());
    btnAll.setText("ALL");
    btnAll.setTextSize(9f);
    btnAll.setTextColor(currentLetterFilter == null ? Color.parseColor("#FFFFFF") : Color.parseColor("#94A3B8"));
    btnAll.setBackgroundResource(R.drawable.normal_chip_bg);
    btnAll.setPadding(12, 0, 12, 0);
    btnAll.setMinWidth(0);
    btnAll.setMinHeight(0);
    LinearLayout.LayoutParams lpAll = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(24 * getResources().getDisplayMetrics().density));
    lpAll.setMargins(2, 0, 4, 0);
    btnAll.setLayoutParams(lpAll);
    btnAll.setOnClickListener(v -> {
      VibratorCompat.vibrate(v);
      currentLetterFilter = null;
      applyFilters();
      buildAlphabetBar();
    });
    alphabetContainer.addView(btnAll);

    for (Character letter : letters)
    {
      final String letStr = String.valueOf(letter);
      Button btn = new Button(getContext());
      btn.setText(letStr);
      btn.setTextSize(10f);
      boolean isSelected = letStr.equals(currentLetterFilter);
      btn.setTextColor(isSelected ? Color.parseColor("#FFFFFF") : Color.parseColor("#94A3B8"));
      btn.setBackgroundResource(R.drawable.normal_chip_bg);
      btn.setPadding(8, 0, 8, 0);
      btn.setMinWidth(0);
      btn.setMinHeight(0);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(24 * getResources().getDisplayMetrics().density));
      lp.setMargins(2, 0, 2, 0);
      btn.setLayoutParams(lp);
      btn.setOnClickListener(v -> {
        VibratorCompat.vibrate(v);
        currentLetterFilter = letStr;
        applyFilters();
        buildAlphabetBar();
      });
      alphabetContainer.addView(btn);
    }
  }

  private void applyFilters()
  {
    currentDisplayItems.clear();

    for (LayoutItem item : allLayoutItems)
    {
      // Category check
      boolean matchesCategory = false;
      if ("all".equals(currentCategoryFilter))
      {
        matchesCategory = true;
      }
      else if ("active".equals(currentCategoryFilter))
      {
        matchesCategory = item.isActive;
      }
      else if ("popular".equals(currentCategoryFilter))
      {
        matchesCategory = isPopular(item.name) || item.isActive;
      }
      else if (currentCategoryFilter.equals(item.category))
      {
        matchesCategory = true;
      }

      if (!matchesCategory) continue;

      // Letter check
      if (currentLetterFilter != null && !currentLetterFilter.isEmpty())
      {
        if (item.displayName == null || !item.displayName.toUpperCase(Locale.ROOT).startsWith(currentLetterFilter))
        {
          continue;
        }
      }

      currentDisplayItems.add(item);
    }

    if (adapter != null)
    {
      adapter.notifyDataSetChanged();
    }
  }

  private static class LayoutAdapter extends BaseAdapter
  {
    private final Context context;
    private final List<LayoutItem> items;

    public LayoutAdapter(Context context, List<LayoutItem> items)
    {
      this.context = context;
      this.items = items;
    }

    @Override
    public int getCount()
    {
      return items.size();
    }

    @Override
    public LayoutItem getItem(int position)
    {
      return items.get(position);
    }

    @Override
    public long getItemId(int position)
    {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
      ViewHolder holder;
      if (convertView == null)
      {
        convertView = LayoutInflater.from(context).inflate(R.layout.layout_picker_item, parent, false);
        holder = new ViewHolder();
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

      LayoutItem item = getItem(position);
      holder.txtTitle.setText(item.displayName);
      holder.txtSubtitle.setText(item.name);

      if (item.isSelected)
      {
        holder.imgCheck.setVisibility(View.VISIBLE);
        holder.imgCheck.setColorFilter(Color.parseColor("#3B82F6"));
        holder.imgIcon.setColorFilter(Color.parseColor("#3B82F6"));
        holder.txtTitle.setTextColor(Color.parseColor("#FFFFFF"));
        holder.txtSubtitle.setTextColor(Color.parseColor("#93C5FD"));
      }
      else if (item.isActive)
      {
        holder.imgCheck.setVisibility(View.GONE);
        holder.imgIcon.setColorFilter(Color.parseColor("#94A3B8"));
        holder.txtTitle.setTextColor(Color.parseColor("#FFFFFF"));
        holder.txtSubtitle.setTextColor(Color.parseColor("#94A3B8"));
      }
      else
      {
        holder.imgCheck.setVisibility(View.GONE);
        holder.imgIcon.setColorFilter(Color.parseColor("#64748B"));
        holder.txtTitle.setTextColor(Color.parseColor("#E2E8F0"));
        holder.txtSubtitle.setTextColor(Color.parseColor("#64748B"));
      }

      return convertView;
    }

    private static class ViewHolder
    {
      TextView txtTitle;
      TextView txtSubtitle;
      ImageView imgCheck;
      ImageView imgIcon;
    }
  }
}
